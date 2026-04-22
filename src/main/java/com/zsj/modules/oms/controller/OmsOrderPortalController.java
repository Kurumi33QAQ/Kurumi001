package com.zsj.modules.oms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.oms.dto.OmsOrderCreateDTO;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.model.OmsOrder;
import com.zsj.modules.oms.service.OmsOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 买家端订单接口（Portal）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/order")
public class OmsOrderPortalController {

    private final OmsOrderService omsOrderService;

    /**
     * 买家创建订单
     */
    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody OmsOrderCreateDTO dto,
                                     Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        Long orderId = omsOrderService.createOrder(authentication.getName(), dto);
        return CommonResult.success(orderId, "创建订单成功");
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
