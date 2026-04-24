package com.zsj.modules.oms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.oms.component.OmsOrderIdempotencyService;
import com.zsj.modules.oms.dto.OmsOrderCreateDTO;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.model.OmsOrder;
import com.zsj.modules.oms.service.OmsOrderService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 买家端订单接口（Portal）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/order")
public class OmsOrderPortalController {

    private final OmsOrderService omsOrderService;
    private final OmsOrderIdempotencyService omsOrderIdempotencyService;

    /**
     * 买家创建订单
     */
    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody OmsOrderCreateDTO dto,
                                     @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                     Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            return CommonResult.validateFailed("请求头 X-Idempotency-Key 不能为空");
        }

        String memberUsername = authentication.getName();

        // 已有成功结果：直接返回历史订单ID（幂等返回）
        Long existedOrderId = omsOrderIdempotencyService.getCreatedOrderId(memberUsername, idempotencyKey);
        if (existedOrderId != null) {
            return CommonResult.success(existedOrderId, "重复提交，返回已创建订单");
        }

        // 尝试获取幂等锁：拿不到表示重复提交正在处理中
        boolean locked = omsOrderIdempotencyService.tryLock(memberUsername, idempotencyKey);
        if (!locked) {
            return CommonResult.failed(UmsErrorCode.ORDER_SUBMIT_DUPLICATE);
        }

        try {
            Long orderId = omsOrderService.createOrder(memberUsername, dto);
            omsOrderIdempotencyService.markSuccess(memberUsername, idempotencyKey, orderId);
            return CommonResult.success(orderId, "创建订单成功");
        } catch (Exception e) {
            omsOrderIdempotencyService.releaseLock(memberUsername, idempotencyKey);
            throw e;
        }

    }

    /**
     * 我的订单列表（只看当前登录买家）
     */
    @GetMapping("/list")
    public CommonResult<IPage<OmsOrder>> myOrders(@ModelAttribute OmsOrderQueryDTO queryDTO,
                                                  Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        // 强制当前用户，只允许查自己的订单
        queryDTO.setMemberUsername(authentication.getName());

        IPage<OmsOrder> page = omsOrderService.listPage(queryDTO);
        return CommonResult.success(page, "获取我的订单列表成功");
    }

    /**
     * 我的订单详情（强制校验订单归属）
     */
    @GetMapping("/detail")
    public CommonResult<OmsOrder> myOrderDetail(@RequestParam Long id,
                                                Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        OmsOrder order = omsOrderService.getById(id);

        // 只允许查看自己的订单
        if (!authentication.getName().equals(order.getMemberUsername())) {
            return CommonResult.forbidden(null);
        }

        return CommonResult.success(order, "获取我的订单详情成功");
    }

    /**
     * 买家取消订单（仅待支付可取消）
     */
    @PostMapping("/cancel")
    public CommonResult<String> cancel(@RequestParam Long orderId,
                                       Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        omsOrderService.cancelOrder(authentication.getName(), orderId);
        return CommonResult.success("取消订单成功");
    }

}
