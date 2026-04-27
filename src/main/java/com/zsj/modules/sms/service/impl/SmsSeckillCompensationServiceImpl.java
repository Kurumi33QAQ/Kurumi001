package com.zsj.modules.sms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.sms.mapper.SmsSeckillActivityMapper;
import com.zsj.modules.sms.mapper.SmsSeckillRecordMapper;
import com.zsj.modules.sms.model.SmsSeckillActivity;
import com.zsj.modules.sms.model.SmsSeckillRecord;
import com.zsj.modules.sms.model.SmsSeckillRecordStatus;
import com.zsj.modules.sms.service.SmsSeckillCompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 秒杀补偿业务实现
 */
@Service
@RequiredArgsConstructor
public class SmsSeckillCompensationServiceImpl implements SmsSeckillCompensationService {

    private static final String SECKILL_STOCK_KEY_PREFIX = "sms:seckill:stock:";

    private final SmsSeckillRecordMapper smsSeckillRecordMapper;
    private final SmsSeckillActivityMapper smsSeckillActivityMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void compensateClosedOrder(Long activityId,
                                      String memberUsername,
                                      Long orderId,
                                      Integer quantity) {
        if (activityId == null || !StringUtils.hasText(memberUsername)
                || orderId == null || quantity == null || quantity <= 0) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 只有“已创建订单”的秒杀记录才允许补偿，防止重复恢复库存。
        LambdaUpdateWrapper<SmsSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(SmsSeckillRecord::getActivityId, activityId)
                .eq(SmsSeckillRecord::getMemberUsername, memberUsername)
                .eq(SmsSeckillRecord::getOrderId, orderId)
                .eq(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.ORDER_CREATED)
                .set(SmsSeckillRecord::getStatus, SmsSeckillRecordStatus.CLOSED)
                .set(SmsSeckillRecord::getUpdateTime, LocalDateTime.now());

        int recordRows = smsSeckillRecordMapper.update(null, recordWrapper);
        if (recordRows <= 0) {
            return;
        }

        stringRedisTemplate.opsForValue().increment(buildSeckillStockKey(activityId), quantity);

        LambdaUpdateWrapper<SmsSeckillActivity> activityWrapper = new LambdaUpdateWrapper<>();
        activityWrapper.eq(SmsSeckillActivity::getId, activityId)
                .eq(SmsSeckillActivity::getDeleteStatus, 0)
                .setSql("sold_count = CASE WHEN sold_count >= " + quantity + " THEN sold_count - " + quantity + " ELSE 0 END")
                .set(SmsSeckillActivity::getUpdateTime, LocalDateTime.now());

        int activityRows = smsSeckillActivityMapper.update(null, activityWrapper);
        if (activityRows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }

    private String buildSeckillStockKey(Long activityId) {
        return SECKILL_STOCK_KEY_PREFIX + activityId;
    }
}
