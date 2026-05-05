package com.zsj.modules.ums.controller;

import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.security.component.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zsj.modules.sms.component.SeckillOrderMessageProducer;
import com.zsj.modules.sms.dto.SeckillOrderMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import java.util.List;

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

}
