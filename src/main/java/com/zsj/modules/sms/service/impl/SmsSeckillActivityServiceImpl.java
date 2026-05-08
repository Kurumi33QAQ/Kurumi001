package com.zsj.modules.sms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.oms.service.OmsOrderService;
import com.zsj.modules.sms.component.SeckillOrderMessageProducer;
import com.zsj.modules.sms.dto.SmsSeckillActivityQueryDTO;
import com.zsj.modules.sms.dto.SmsSeckillSubmitDTO;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import com.zsj.modules.sms.mapper.SmsSeckillActivityMapper;
import com.zsj.modules.sms.mapper.SmsSeckillMqFailLogMapper;
import com.zsj.modules.sms.mapper.SmsSeckillRecordMapper;
import com.zsj.modules.sms.model.SmsSeckillActivity;
import com.zsj.modules.sms.model.SmsSeckillMqFailLog;
import com.zsj.modules.sms.model.SmsSeckillMqFailLogHandleStatus;
import com.zsj.modules.sms.model.SmsSeckillRecord;
import com.zsj.modules.sms.service.SmsSeckillActivityService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.model.UmsMemberNotificationType;
import com.zsj.modules.ums.service.UmsMemberNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zsj.modules.sms.dto.SmsSeckillActivityPortalDTO;
import com.zsj.modules.sms.model.SmsSeckillActivityStatus;
import com.zsj.modules.sms.model.SmsSeckillRecordStatus;



import java.time.LocalDateTime;
import java.util.Arrays;


/**
 * 秒杀活动业务实现
 */
@Service
@RequiredArgsConstructor
public class SmsSeckillActivityServiceImpl implements SmsSeckillActivityService {

    private final SmsSeckillActivityMapper smsSeckillActivityMapper;
    private final SmsSeckillRecordMapper smsSeckillRecordMapper;
    private final SeckillOrderMessageProducer seckillOrderMessageProducer;
    private final OmsOrderService omsOrderService;
    private final UmsMemberNotificationService umsMemberNotificationService;
    private final SmsSeckillMqFailLogMapper smsSeckillMqFailLogMapper;

    private static final String SECKILL_STOCK_KEY_PREFIX = "sms:seckill:stock:";
    private static final String SECKILL_USER_KEY_PREFIX = "sms:seckill:users:";
    private static final long SECKILL_STOCK_TTL_SECONDS = 24 * 60 * 60;

    private static final Long LUA_SUCCESS = 0L;
    private static final Long LUA_STOCK_NOT_ENOUGH = 1L;
    private static final Long LUA_REPEAT = 2L;
    private static final Long LUA_INVALID_QUANTITY = 3L;


    private final StringRedisTemplate stringRedisTemplate;



    /**
     * 秒杀预扣库存 Lua 脚本。
     *
     * 返回码：
     * 0 成功
     * 1 库存不足
     * 2 重复秒杀
     * 3 购买数量非法
     */
    private static final String SECKILL_RESERVE_LUA = """
        local stockKey = KEYS[1]
        local userKey = KEYS[2]

        local memberUsername = ARGV[1]
        local quantity = tonumber(ARGV[2])
        local initialStock = tonumber(ARGV[3])
        local ttlSeconds = tonumber(ARGV[4])

        if quantity == nil or quantity <= 0 then
            return 3
        end

        if redis.call('EXISTS', stockKey) == 0 then
            redis.call('SET', stockKey, initialStock)
            redis.call('EXPIRE', stockKey, ttlSeconds)
        end

        if redis.call('SISMEMBER', userKey, memberUsername) == 1 then
            return 2
        end

        local stock = tonumber(redis.call('GET', stockKey))
        if stock == nil or stock < quantity then
            return 1
        end

        redis.call('DECRBY', stockKey, quantity)
        redis.call('SADD', userKey, memberUsername)
        redis.call('EXPIRE', userKey, ttlSeconds)

        return 0
        """;

    /**
     * 秒杀库存回滚 Lua 脚本。
     *
     * 用于订单创建失败、订单取消、超时关闭后的 Redis 补偿。
     */
    private static final String SECKILL_ROLLBACK_LUA = """
        local stockKey = KEYS[1]
        local userKey = KEYS[2]

        local memberUsername = ARGV[1]
        local quantity = tonumber(ARGV[2])

        if quantity == nil or quantity <= 0 then
            return 0
        end

        redis.call('INCRBY', stockKey, quantity)
        redis.call('SREM', userKey, memberUsername)

        return 1
        """;





    /**
     * 转换为买家端展示对象
     */
    private SmsSeckillActivityPortalDTO toPortalDTO(SmsSeckillActivity activity) {
        SmsSeckillActivityPortalDTO dto = new SmsSeckillActivityPortalDTO();
        dto.setId(activity.getId());
        dto.setProductId(activity.getProductId());
        dto.setName(activity.getName());
        dto.setSeckillPrice(activity.getSeckillPrice());
        dto.setSeckillStock(activity.getSeckillStock());
        dto.setSoldCount(activity.getSoldCount());
        dto.setPerLimit(activity.getPerLimit());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setStatus(activity.getStatus());

        Integer activityStatus = calculateActivityStatus(activity);
        dto.setActivityStatus(activityStatus);
        dto.setActivityStatusText(SmsSeckillActivityStatus.getText(activityStatus));
        return dto;
    }

    /**
     * 计算秒杀活动展示状态
     */
    private Integer calculateActivityStatus(SmsSeckillActivity activity) {
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            return SmsSeckillActivityStatus.DISABLED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            return SmsSeckillActivityStatus.NOT_STARTED;
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            return SmsSeckillActivityStatus.ENDED;
        }

        int stock = activity.getSeckillStock() == null ? 0 : activity.getSeckillStock();
        int sold = activity.getSoldCount() == null ? 0 : activity.getSoldCount();
        if (stock <= 0 || sold >= stock) {
            return SmsSeckillActivityStatus.SOLD_OUT;
        }

        return SmsSeckillActivityStatus.IN_PROGRESS;
    }

    /**
     * 获取并校验可参与秒杀的活动
     */
    private SmsSeckillActivity getValidActivityForSubmit(Long activityId) {
        SmsSeckillActivity activity = smsSeckillActivityMapper.selectById(activityId);
        if (activity == null || activity.getDeleteStatus() == null || activity.getDeleteStatus() != 0) {
            throw new ApiException(UmsErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new ApiException(UmsErrorCode.SECKILL_ACTIVITY_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new ApiException(UmsErrorCode.SECKILL_NOT_STARTED);
        }

        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new ApiException(UmsErrorCode.SECKILL_ENDED);
        }

        int stock = activity.getSeckillStock() == null ? 0 : activity.getSeckillStock();
        int sold = activity.getSoldCount() == null ? 0 : activity.getSoldCount();
        if (stock <= 0 || sold >= stock) {
            throw new ApiException(UmsErrorCode.SECKILL_STOCK_NOT_ENOUGH);
        }

        return activity;
    }

    /**
     * 数据库层重复秒杀校验（基础版方案）。
     *
     * 当前 Lua 优化版已把重复校验前置到 Redis：
     * sms:seckill:users:{activityId}
     *
     * 因此 submitSeckill 主流程暂时不调用该方法。
     * 保留它用于兜底方案说明，或后续 Redis 异常降级时使用。
     */
    private void checkRepeatSeckill(String memberUsername, Long activityId) {
        LambdaQueryWrapper<SmsSeckillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsSeckillRecord::getActivityId, activityId)
                .eq(SmsSeckillRecord::getMemberUsername, memberUsername)
                .in(SmsSeckillRecord::getStatus,
                        SmsSeckillRecordStatus.QUALIFIED,
                        SmsSeckillRecordStatus.ORDER_CREATED);

        Long count = smsSeckillRecordMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(UmsErrorCode.SECKILL_REPEAT);
        }
    }


    /**
     * 使用 Lua 原子预扣秒杀库存。
     *
     * Lua 内部一次完成：
     * 1. 初始化库存
     * 2. 判断用户是否重复秒杀
     * 3. 判断库存是否充足
     * 4. 扣减库存
     * 5. 记录用户已抢
     */
    private void reserveSeckillStock(String memberUsername, SmsSeckillActivity activity, Integer quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SECKILL_RESERVE_LUA);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
                script,
                Arrays.asList(
                        buildSeckillStockKey(activity.getId()),
                        buildSeckillUserKey(activity.getId())
                ),
                memberUsername,
                String.valueOf(quantity),
                String.valueOf(activity.getSeckillStock()),
                String.valueOf(SECKILL_STOCK_TTL_SECONDS)
        );

        if (LUA_SUCCESS.equals(result)) {
            return;
        }

        if (LUA_STOCK_NOT_ENOUGH.equals(result)) {
            throw new ApiException(UmsErrorCode.SECKILL_STOCK_NOT_ENOUGH);
        }

        if (LUA_REPEAT.equals(result)) {
            throw new ApiException(UmsErrorCode.SECKILL_REPEAT);
        }

        if (LUA_INVALID_QUANTITY.equals(result)) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        throw new ApiException(ResultCode.FAILED);
    }


    /**
     * 回滚 Redis 秒杀预扣。
     *
     * 不只恢复库存，还要移除用户已抢标记。
     * 否则用户取消订单后，即使库存恢复，也仍然会被 Redis 判断为重复秒杀。
     */
    private void rollbackSeckillStock(Long activityId, String memberUsername, Integer quantity) {
        if (activityId == null || !StringUtils.hasText(memberUsername) || quantity == null || quantity <= 0) {
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SECKILL_ROLLBACK_LUA);
        script.setResultType(Long.class);

        stringRedisTemplate.execute(
                script,
                Arrays.asList(
                        buildSeckillStockKey(activityId),
                        buildSeckillUserKey(activityId)
                ),
                memberUsername,
                String.valueOf(quantity)
        );
    }


    private String buildSeckillStockKey(Long activityId) {
        return SECKILL_STOCK_KEY_PREFIX + activityId;
    }

    private String buildSeckillUserKey(Long activityId) {
        return SECKILL_USER_KEY_PREFIX + activityId;
    }


    /**
     * 同步增加秒杀活动已售数量
     */
    private void increaseActivitySoldCount(Long activityId, Integer quantity) {
        if (activityId == null || quantity == null || quantity <= 0) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        LambdaUpdateWrapper<SmsSeckillActivity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SmsSeckillActivity::getId, activityId)
                .eq(SmsSeckillActivity::getDeleteStatus, 0)
                .setSql("sold_count = sold_count + " + quantity)
                .set(SmsSeckillActivity::getUpdateTime, LocalDateTime.now());

        int rows = smsSeckillActivityMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }








    @Override
    public IPage<SmsSeckillActivityPortalDTO> listPortalPage(SmsSeckillActivityQueryDTO queryDTO) {
        LambdaQueryWrapper<SmsSeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsSeckillActivity::getDeleteStatus, 0)
                .eq(SmsSeckillActivity::getStatus, 1);

        if (StringUtils.hasText(queryDTO.getName())) {
            wrapper.like(SmsSeckillActivity::getName, queryDTO.getName());
        }

        wrapper.orderByAsc(SmsSeckillActivity::getStartTime)
                .orderByDesc(SmsSeckillActivity::getId);

        Page<SmsSeckillActivity> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<SmsSeckillActivity> activityPage = smsSeckillActivityMapper.selectPage(page, wrapper);
        return activityPage.convert(this::toPortalDTO);
    }

    @Override
    public SmsSeckillActivityPortalDTO getPortalDetail(Long id) {
        LambdaQueryWrapper<SmsSeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsSeckillActivity::getId, id)
                .eq(SmsSeckillActivity::getDeleteStatus, 0)
                .eq(SmsSeckillActivity::getStatus, 1);

        SmsSeckillActivity activity = smsSeckillActivityMapper.selectOne(wrapper);
        if (activity == null) {
            throw new ApiException(ResultCode.FAILED);
        }
        return toPortalDTO(activity);
    }

    /**
     * Lua 已经完成 Redis 层防重复，这里不再前置查询数据库，减少秒杀入口数据库压力。
     * 数据库插入异常仍然保留兜底，防止 Redis 状态异常导致重复记录。
     * checkRepeatSeckill(memberUsername, dto.getActivityId());
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitSeckill(String memberUsername, SmsSeckillSubmitDTO dto) {
        if (!StringUtils.hasText(memberUsername) || dto == null
                || dto.getActivityId() == null
                || dto.getQuantity() == null
                || dto.getQuantity() < 1) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        SmsSeckillActivity activity = getValidActivityForSubmit(dto.getActivityId());

        if (dto.getQuantity() > activity.getPerLimit()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // Lua 已经完成 Redis 层防重复和预扣库存，这里不再前置查询数据库，减少秒杀入口数据库压力。
        reserveSeckillStock(memberUsername, activity, dto.getQuantity());

        SmsSeckillRecord record = new SmsSeckillRecord();
        record.setActivityId(activity.getId());
        record.setMemberUsername(memberUsername);
        record.setQuantity(dto.getQuantity());
        record.setStatus(SmsSeckillRecordStatus.PROCESSING);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            smsSeckillRecordMapper.insert(record);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            rollbackSeckillStock(activity.getId(), memberUsername, dto.getQuantity());
            throw new ApiException(UmsErrorCode.SECKILL_REPEAT);
        } catch (Exception e) {
            rollbackSeckillStock(activity.getId(), memberUsername, dto.getQuantity());
            throw e;
        }

        try {
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setRecordId(record.getId());
            message.setActivityId(activity.getId());
            message.setProductId(activity.getProductId());
            message.setMemberUsername(memberUsername);
            message.setQuantity(dto.getQuantity());
            message.setSeckillPrice(activity.getSeckillPrice());
            message.setCreateTime(LocalDateTime.now());

            seckillOrderMessageProducer.sendSeckillOrderMessage(message);
            return record.getId();
        } catch (Exception e) {
            rollbackSeckillStock(activity.getId(), memberUsername, dto.getQuantity());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrderFromMessage(SeckillOrderMessage message) {
        if (message == null
                || message.getRecordId() == null
                || message.getActivityId() == null
                || message.getProductId() == null
                || !StringUtils.hasText(message.getMemberUsername())
                || message.getQuantity() == null
                || message.getQuantity() < 1
                || message.getSeckillPrice() == null) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 先抢占秒杀记录，避免 RabbitMQ 重投或多个消费者并发时重复创建订单。
        LambdaUpdateWrapper<SmsSeckillRecord> claimWrapper = new LambdaUpdateWrapper<>();
        claimWrapper.eq(SmsSeckillRecord::getId, message.getRecordId())
                .eq(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.PROCESSING)
                .set(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.CREATING)
                .set(SmsSeckillRecord::getUpdateTime, LocalDateTime.now());

        int claimRows = smsSeckillRecordMapper.update(null, claimWrapper);
        if (claimRows <= 0) {
            return;
        }

        Long orderId;
        try {
            SmsSeckillRecord record = smsSeckillRecordMapper.selectById(message.getRecordId());
            if (record == null
                    || !message.getActivityId().equals(record.getActivityId())
                    || !message.getMemberUsername().equals(record.getMemberUsername())
                    || !message.getQuantity().equals(record.getQuantity())) {
                throw new ApiException(ResultCode.FAILED);
            }

            orderId = omsOrderService.createSeckillOrder(
                    message.getMemberUsername(),
                    message.getActivityId(),
                    message.getProductId(),
                    message.getSeckillPrice(),
                    message.getQuantity()
            );
        } catch (Exception e) {
            failSeckillOrderMessage(message);
            return;
        }

        LambdaUpdateWrapper<SmsSeckillRecord> finishWrapper = new LambdaUpdateWrapper<>();
        finishWrapper.eq(SmsSeckillRecord::getId, message.getRecordId())
                .eq(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.CREATING)
                .set(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.ORDER_CREATED)
                .set(SmsSeckillRecord::getOrderId, orderId)
                .set(SmsSeckillRecord::getUpdateTime, LocalDateTime.now());

        int finishRows = smsSeckillRecordMapper.update(null, finishWrapper);
        if (finishRows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        increaseActivitySoldCount(message.getActivityId(), message.getQuantity());

        umsMemberNotificationService.createNotification(
                message.getMemberUsername(),
                UmsMemberNotificationType.SECKILL,
                "秒杀成功",
                "恭喜你秒杀成功，订单已生成，请尽快完成支付。",
                orderId
        );

        markFailLogSuccessIfNecessary(message);
    }

    /**
     * 重投消息处理成功后，回写失败日志状态。
     *
     * 普通秒杀消息没有 failLogId，不会进入这个逻辑。
     */
    private void markFailLogSuccessIfNecessary(SeckillOrderMessage message) {
        if (message.getFailLogId() == null) {
            return;
        }

        LambdaUpdateWrapper<SmsSeckillMqFailLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SmsSeckillMqFailLog::getId, message.getFailLogId())
                .set(SmsSeckillMqFailLog::getHandleStatus, SmsSeckillMqFailLogHandleStatus.SUCCESS)
                .set(SmsSeckillMqFailLog::getHandleRemark, "重投消息已成功创建秒杀订单")
                .set(SmsSeckillMqFailLog::getUpdateTime, LocalDateTime.now());
        smsSeckillMqFailLogMapper.update(null, updateWrapper);
    }

    /**
     * 秒杀异步下单失败补偿。
     *
     * 这里处理的是“订单尚未创建成功”的失败：标记秒杀记录失败，并回滚 Redis 预扣库存和用户参与标记。
     */
    private void failSeckillOrderMessage(SeckillOrderMessage message) {
        LambdaUpdateWrapper<SmsSeckillRecord> failWrapper = new LambdaUpdateWrapper<>();
        failWrapper.eq(SmsSeckillRecord::getId, message.getRecordId())
                .eq(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.CREATING)
                .set(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.FAILED)
                .set(SmsSeckillRecord::getUpdateTime, LocalDateTime.now());
        smsSeckillRecordMapper.update(null, failWrapper);

        rollbackSeckillStock(message.getActivityId(), message.getMemberUsername(), message.getQuantity());

        umsMemberNotificationService.createNotification(
                message.getMemberUsername(),
                UmsMemberNotificationType.SECKILL,
                "秒杀失败",
                "很抱歉，本次秒杀下单失败，预扣库存已恢复，你可以稍后再试。",
                message.getRecordId()
        );
    }


}
