package com.zsj.modules.ums.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.security.component.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zsj.modules.sms.component.SeckillOrderMessageProducer;
import com.zsj.modules.sms.config.SeckillRabbitMqConfig;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import com.zsj.modules.sms.mapper.SmsSeckillMqFailLogMapper;
import com.zsj.modules.sms.mapper.SmsSeckillRecordMapper;
import com.zsj.modules.sms.model.SmsSeckillMqFailLog;
import com.zsj.modules.sms.model.SmsSeckillMqFailLogHandleStatus;
import com.zsj.modules.sms.model.SmsSeckillRecord;
import com.zsj.modules.sms.model.SmsSeckillRecordStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;


import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 开发环境调试接口：
 * 仅在 dev profile 下生效，生产环境不会加载该控制器。
 */
@Profile("dev")
@RestController
@RequiredArgsConstructor
public class DevDebugController {

    private final UmsAdminService umsAdminService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SeckillOrderMessageProducer seckillOrderMessageProducer;
    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final SmsSeckillMqFailLogMapper smsSeckillMqFailLogMapper;
    private final SmsSeckillRecordMapper smsSeckillRecordMapper;
    private final ObjectMapper objectMapper;


    /**
     * 查看当前登录用户的权限列表（调试用）
     */
    @GetMapping("/demo/admin/authorities")
    public CommonResult<List<String>> authorities(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        String username = authentication.getName();
        List<String> authorities = umsAdminService.getAuthorityList(username);
        return CommonResult.success(authorities, "获取权限列表成功");
    }


    /**
     * 手动清理某个用户的权限缓存（开发调试）
     */
    @GetMapping("/demo/admin/cache/evict")
    public CommonResult<String> evictAuthorityCache(@RequestParam String username) {
        umsAdminService.evictAuthorityCache(username);
        return CommonResult.success("已清理用户权限缓存：" + username);
    }

    /**
     * 查看权限缓存当前大小（开发调试）
     */
    @GetMapping("/demo/admin/cache/size")
    public CommonResult<Integer> authorityCacheSize() {
        return CommonResult.success(umsAdminService.getAuthorityCacheSize(), "获取缓存大小成功");
    }


    /**
     * 查看黑名单数量（调试）
     */
    @GetMapping("/demo/admin/blacklist/size")
    public CommonResult<Integer> blacklistSize() {
        return CommonResult.success(tokenBlacklistService.size(), "获取黑名单数量成功");
    }

    /**
     * 手动触发一次过期清理（调试）
     */
    @PostMapping("/demo/admin/blacklist/clean")
    public CommonResult<Integer> cleanBlacklistNow() {
        int removed = tokenBlacklistService.cleanExpired();
        return CommonResult.success(removed, "手动清理黑名单成功");
    }


    /**
     * 发送一条秒杀 MQ 测试消息。
     *
     * 仅用于开发环境验证 RabbitMQ 通道是否可用。
     */
    @PostMapping("/demo/seckill/mq/send-test")
    public CommonResult<String> sendSeckillMqTest(@RequestParam(defaultValue = "1") Long activityId,
                                                  @RequestParam(defaultValue = "700010") Long productId,
                                                  @RequestParam(defaultValue = "buyer_001") String memberUsername) {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRecordId(System.currentTimeMillis());
        message.setActivityId(activityId);
        message.setProductId(productId);
        message.setMemberUsername(memberUsername);
        message.setQuantity(1);
        message.setSeckillPrice(new BigDecimal("9.90"));
        message.setCreateTime(LocalDateTime.now());

        seckillOrderMessageProducer.sendSeckillOrderMessage(message);
        return CommonResult.success("秒杀 MQ 测试消息发送成功");
    }

    /**
     * 发送一条秒杀死信测试消息。
     *
     * 该接口绕过正常队列，直接发送到死信交换机，用于验证 DLQ 消费者是否能把失败消息落库。
     */
    @PostMapping("/demo/seckill/mq/send-dead-test")
    public CommonResult<String> sendSeckillDeadMqTest(@RequestParam(defaultValue = "1") Long activityId,
                                                      @RequestParam(defaultValue = "700010") Long productId,
                                                      @RequestParam(defaultValue = "buyer_001") String memberUsername,
                                                      @RequestParam(defaultValue = "8888888") Long recordId) {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRecordId(recordId);
        message.setActivityId(activityId);
        message.setProductId(productId);
        message.setMemberUsername(memberUsername);
        message.setQuantity(1);
        message.setSeckillPrice(new BigDecimal("9.90"));
        message.setCreateTime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_EXCHANGE,
                SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_ROUTING_KEY,
                message
        );
        return CommonResult.success("秒杀死信 MQ 测试消息发送成功，recordId=" + recordId);
    }

    /**
     * 查询秒杀 MQ 队列消息数量。
     *
     * 用于开发阶段观察正常队列和死信队列是否有积压消息。
     */
    @GetMapping("/demo/seckill/mq/queue-size")
    public CommonResult<Map<String, Object>> seckillMqQueueSize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(SeckillRabbitMqConfig.SECKILL_ORDER_QUEUE,
                getQueueMessageCount(SeckillRabbitMqConfig.SECKILL_ORDER_QUEUE));
        result.put(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE,
                getQueueMessageCount(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE));
        return CommonResult.success(result, "获取秒杀 MQ 队列消息数量成功");
    }

    private Long getQueueMessageCount(String queueName) {
        Properties properties = rabbitAdmin.getQueueProperties(queueName);
        if (properties == null) {
            return null;
        }

        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        if (count instanceof Number number) {
            return number.longValue();
        }

        return null;
    }

    /**
     * 手动重投一条秒杀死信消息。
     *
     * 注意：该接口会从死信队列取走一条消息，再重新发送到正常秒杀下单队列。
     * 仅用于开发环境验证和人工补偿演示。
     */
    @PostMapping("/demo/seckill/mq/dlq/requeue-one")
    public CommonResult<String> requeueOneSeckillDeadMessage() {
        Object message = rabbitTemplate.receiveAndConvert(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE);
        if (message == null) {
            return CommonResult.success("死信队列暂无消息");
        }

        if (!(message instanceof SeckillOrderMessage seckillOrderMessage)) {
            return CommonResult.failed("死信消息类型不正确，无法重投：" + message.getClass().getName());
        }

        seckillOrderMessageProducer.sendSeckillOrderMessage(seckillOrderMessage);
        return CommonResult.success("已重投一条秒杀死信消息，recordId=" + seckillOrderMessage.getRecordId());
    }

    /**
     * 分页查询秒杀 MQ 失败日志。
     *
     * 开发阶段用于观察死信消息是否已经落库，以及后续人工补偿处理状态。
     */
    @GetMapping("/demo/seckill/mq/fail-log/list")
    public CommonResult<IPage<SmsSeckillMqFailLog>> listSeckillMqFailLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) String memberUsername) {

        LambdaQueryWrapper<SmsSeckillMqFailLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(handleStatus != null, SmsSeckillMqFailLog::getHandleStatus, handleStatus)
                .eq(activityId != null, SmsSeckillMqFailLog::getActivityId, activityId)
                .eq(memberUsername != null && !memberUsername.isBlank(),
                        SmsSeckillMqFailLog::getMemberUsername,
                        memberUsername)
                .orderByDesc(SmsSeckillMqFailLog::getCreateTime);

        Page<SmsSeckillMqFailLog> page = new Page<>(pageNum, pageSize);
        IPage<SmsSeckillMqFailLog> result = smsSeckillMqFailLogMapper.selectPage(page, wrapper);
        return CommonResult.success(result, "获取秒杀 MQ 失败日志成功");
    }

    /**
     * 按失败日志ID手动重投秒杀消息。
     *
     * 从数据库失败日志中恢复原始消息，再投递到正常秒杀下单队列。
     */
    @PostMapping("/demo/seckill/mq/fail-log/requeue")
    public CommonResult<String> requeueSeckillFailLog(@RequestParam Long id) {
        SmsSeckillMqFailLog failLog = smsSeckillMqFailLogMapper.selectById(id);
        if (failLog == null) {
            return CommonResult.failed("失败日志不存在");
        }

        if (failLog.getHandleStatus() != null
                && SmsSeckillMqFailLogHandleStatus.IGNORED == failLog.getHandleStatus()) {
            return CommonResult.failed("该失败日志已忽略，不能重投");
        }

        SeckillOrderMessage message;
        try {
            message = objectMapper.readValue(failLog.getMessageBody(), SeckillOrderMessage.class);
        } catch (JsonProcessingException e) {
            markFailLogRequeueFailed(id, "消息体解析失败：" + e.getMessage());
            return CommonResult.failed("消息体解析失败，无法重投");
        }

        message.setFailLogId(id);
        seckillOrderMessageProducer.sendSeckillOrderMessage(message);

        LambdaUpdateWrapper<SmsSeckillMqFailLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SmsSeckillMqFailLog::getId, id)
                .set(SmsSeckillMqFailLog::getHandleStatus, SmsSeckillMqFailLogHandleStatus.REQUEUED)
                .set(SmsSeckillMqFailLog::getRequeueCount,
                        failLog.getRequeueCount() == null ? 1 : failLog.getRequeueCount() + 1)
                .set(SmsSeckillMqFailLog::getLastRequeueTime, LocalDateTime.now())
                .set(SmsSeckillMqFailLog::getHandleRemark, "已手动重投到正常秒杀下单队列")
                .set(SmsSeckillMqFailLog::getUpdateTime, LocalDateTime.now());
        smsSeckillMqFailLogMapper.update(null, updateWrapper);

        return CommonResult.success("失败日志已重投，id=" + id + "，recordId=" + message.getRecordId());
    }

    private void markFailLogRequeueFailed(Long id, String reason) {
        LambdaUpdateWrapper<SmsSeckillMqFailLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SmsSeckillMqFailLog::getId, id)
                .set(SmsSeckillMqFailLog::getHandleStatus, SmsSeckillMqFailLogHandleStatus.FAILED)
                .set(SmsSeckillMqFailLog::getHandleRemark, reason)
                .set(SmsSeckillMqFailLog::getUpdateTime, LocalDateTime.now());
        smsSeckillMqFailLogMapper.update(null, updateWrapper);
    }

    /**
     * 忽略一条秒杀 MQ 失败日志。
     *
     * 适用于测试数据、无效消息、活动已结束等不需要再重投的失败记录。
     */
    @PostMapping("/demo/seckill/mq/fail-log/ignore")
    public CommonResult<String> ignoreSeckillFailLog(@RequestParam Long id,
                                                     @RequestParam(required = false) String remark) {
        SmsSeckillMqFailLog failLog = smsSeckillMqFailLogMapper.selectById(id);
        if (failLog == null) {
            return CommonResult.failed("失败日志不存在");
        }

        if (failLog.getHandleStatus() != null
                && SmsSeckillMqFailLogHandleStatus.SUCCESS == failLog.getHandleStatus()) {
            return CommonResult.failed("该失败日志已处理成功，不能忽略");
        }

        String handleRemark = remark == null || remark.isBlank()
                ? "人工确认忽略"
                : remark;

        LambdaUpdateWrapper<SmsSeckillMqFailLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SmsSeckillMqFailLog::getId, id)
                .set(SmsSeckillMqFailLog::getHandleStatus, SmsSeckillMqFailLogHandleStatus.IGNORED)
                .set(SmsSeckillMqFailLog::getHandleRemark, handleRemark)
                .set(SmsSeckillMqFailLog::getUpdateTime, LocalDateTime.now());
        smsSeckillMqFailLogMapper.update(null, updateWrapper);

        return CommonResult.success("失败日志已忽略，id=" + id);
    }

    /**
     * 创建一条可恢复的秒杀失败日志测试数据。
     *
     * 会同时创建 PROCESSING 状态的秒杀记录和 PENDING 状态的失败日志，
     * 用于验证失败日志重投后能否回写 SUCCESS。
     */
    @PostMapping("/demo/seckill/mq/fail-log/create-recoverable-test")
    public CommonResult<Map<String, Object>> createRecoverableFailLogTest(
            @RequestParam(defaultValue = "1") Long activityId,
            @RequestParam(defaultValue = "700010") Long productId,
            @RequestParam(defaultValue = "buyer_001") String memberUsername,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(defaultValue = "9.90") BigDecimal seckillPrice) throws JsonProcessingException {

        SmsSeckillRecord record = new SmsSeckillRecord();
        record.setActivityId(activityId);
        record.setMemberUsername(memberUsername);
        record.setQuantity(quantity);
        record.setStatus(SmsSeckillRecordStatus.PROCESSING);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        smsSeckillRecordMapper.insert(record);

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRecordId(record.getId());
        message.setActivityId(activityId);
        message.setProductId(productId);
        message.setMemberUsername(memberUsername);
        message.setQuantity(quantity);
        message.setSeckillPrice(seckillPrice);
        message.setCreateTime(LocalDateTime.now());

        SmsSeckillMqFailLog failLog = new SmsSeckillMqFailLog();
        failLog.setRecordId(record.getId());
        failLog.setActivityId(activityId);
        failLog.setProductId(productId);
        failLog.setMemberUsername(memberUsername);
        failLog.setQuantity(quantity);
        failLog.setSeckillPrice(seckillPrice);
        failLog.setQueueName(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE);
        failLog.setExchangeName(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_EXCHANGE);
        failLog.setRoutingKey(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_ROUTING_KEY);
        failLog.setMessageBody(objectMapper.writeValueAsString(message));
        failLog.setFailReason("开发环境可恢复失败日志测试数据");
        failLog.setFailCount(1);
        failLog.setHandleStatus(SmsSeckillMqFailLogHandleStatus.PENDING);
        failLog.setRequeueCount(0);
        failLog.setCreateTime(LocalDateTime.now());
        failLog.setUpdateTime(LocalDateTime.now());
        smsSeckillMqFailLogMapper.insert(failLog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getId());
        result.put("failLogId", failLog.getId());
        result.put("recordStatus", SmsSeckillRecordStatus.PROCESSING);
        result.put("failLogStatus", SmsSeckillMqFailLogHandleStatus.PENDING);
        return CommonResult.success(result, "创建可恢复失败日志测试数据成功");
    }

}
