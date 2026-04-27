package com.zsj.modules.sms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.oms.service.OmsOrderService;
import com.zsj.modules.sms.dto.SmsSeckillActivityQueryDTO;
import com.zsj.modules.sms.dto.SmsSeckillSubmitDTO;
import com.zsj.modules.sms.mapper.SmsSeckillActivityMapper;
import com.zsj.modules.sms.mapper.SmsSeckillRecordMapper;
import com.zsj.modules.sms.model.SmsSeckillActivity;
import com.zsj.modules.sms.model.SmsSeckillRecord;
import com.zsj.modules.sms.service.SmsSeckillActivityService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zsj.modules.sms.dto.SmsSeckillActivityPortalDTO;
import com.zsj.modules.sms.model.SmsSeckillActivityStatus;
import com.zsj.modules.sms.model.SmsSeckillRecordStatus;


import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


/**
 * 秒杀活动业务实现
 */
@Service
@RequiredArgsConstructor
public class SmsSeckillActivityServiceImpl implements SmsSeckillActivityService {

    private final SmsSeckillActivityMapper smsSeckillActivityMapper;
    private final SmsSeckillRecordMapper smsSeckillRecordMapper;
    private final OmsOrderService omsOrderService;

    private static final String SECKILL_STOCK_KEY_PREFIX = "sms:seckill:stock:";
    private static final long SECKILL_STOCK_TTL_HOURS = 24;

    private final StringRedisTemplate stringRedisTemplate;





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
     * 校验同一用户是否存在未关闭的秒杀记录
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
     * Redis 预扣秒杀库存
     */
    private boolean reserveSeckillStock(SmsSeckillActivity activity, Integer quantity) {
        String stockKey = buildSeckillStockKey(activity.getId());

        // 首次访问时用数据库库存初始化 Redis 库存。
        Boolean hasKey = stringRedisTemplate.hasKey(stockKey);
        if (!Boolean.TRUE.equals(hasKey)) {
            stringRedisTemplate.opsForValue().set(
                    stockKey,
                    String.valueOf(activity.getSeckillStock()),
                    SECKILL_STOCK_TTL_HOURS,
                    TimeUnit.HOURS
            );
        }

        Long remainStock = stringRedisTemplate.opsForValue().decrement(stockKey, quantity);
        if (remainStock == null) {
            return false;
        }

        if (remainStock < 0) {
            rollbackSeckillStock(activity.getId(), quantity);
            return false;
        }

        return true;
    }

    /**
     * 回滚 Redis 秒杀库存
     */
    private void rollbackSeckillStock(Long activityId, Integer quantity) {
        if (activityId == null || quantity == null || quantity <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(buildSeckillStockKey(activityId), quantity);
    }

    private String buildSeckillStockKey(Long activityId) {
        return SECKILL_STOCK_KEY_PREFIX + activityId;
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
     * 买家提交秒杀请求（基础版：活动规则校验）
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

        checkRepeatSeckill(memberUsername, dto.getActivityId());

        boolean stockReserved = reserveSeckillStock(activity, dto.getQuantity());
        if (!stockReserved) {
            throw new ApiException(UmsErrorCode.SECKILL_STOCK_NOT_ENOUGH);
        }

        SmsSeckillRecord record = new SmsSeckillRecord();
        record.setActivityId(activity.getId());
        record.setMemberUsername(memberUsername);
        record.setQuantity(dto.getQuantity());
        record.setStatus(SmsSeckillRecordStatus.QUALIFIED);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            smsSeckillRecordMapper.insert(record);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            rollbackSeckillStock(activity.getId(), dto.getQuantity());
            throw new ApiException(UmsErrorCode.SECKILL_REPEAT);
        } catch (Exception e) {
            rollbackSeckillStock(activity.getId(), dto.getQuantity());
            throw e;
        }

        try {
            Long orderId = omsOrderService.createSeckillOrder(
                    memberUsername,
                    activity.getId(),
                    activity.getProductId(),
                    activity.getSeckillPrice(),
                    dto.getQuantity()
            );

            LambdaUpdateWrapper<SmsSeckillRecord> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SmsSeckillRecord::getId, record.getId())
                    .eq(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.QUALIFIED)
                    .set(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.ORDER_CREATED)
                    .set(SmsSeckillRecord::getOrderId, orderId)
                    .set(SmsSeckillRecord::getUpdateTime, LocalDateTime.now());

            int rows = smsSeckillRecordMapper.update(null, updateWrapper);
            if (rows <= 0) {
                throw new ApiException(ResultCode.FAILED);
            }

            increaseActivitySoldCount(activity.getId(), dto.getQuantity());

            return orderId;
        } catch (Exception e) {
            rollbackSeckillStock(activity.getId(), dto.getQuantity());
            throw e;
        }



    }


}
