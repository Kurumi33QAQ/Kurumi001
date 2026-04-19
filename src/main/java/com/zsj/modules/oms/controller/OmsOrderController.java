package com.zsj.modules.oms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.dto.OmsOrderStatusUpdateDTO;
import com.zsj.modules.oms.model.OmsOrder;
import com.zsj.modules.oms.service.OmsOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理接口
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/order")
public class OmsOrderController {

    private final OmsOrderService omsOrderService;

    /**
     * 订单分页查询
     */
    @PreAuthorize("hasAuthority('oms:order:read')")
    @GetMapping("/list")
    public CommonResult<IPage<OmsOrder>> list(@ModelAttribute OmsOrderQueryDTO queryDTO) {
        return CommonResult.success(omsOrderService.listPage(queryDTO), "查询订单列表成功");
    }


    /**
     * 订单详情
     */
    @PreAuthorize("hasAuthority('oms:order:read')")
    @GetMapping("/detail")
    public CommonResult<OmsOrder> detail(@RequestParam Long id) {
        return CommonResult.success(omsOrderService.getById(id), "查询订单详情成功");
    }


    /**
     * 更新订单状态
     */
    @PreAuthorize("hasAuthority('oms:order:write')")
    @PostMapping("/status/update")
    public CommonResult<String> updateStatus(@Valid @RequestBody OmsOrderStatusUpdateDTO dto) {
        omsOrderService.updateStatus(dto);
        return CommonResult.success("更新订单状态成功");
    }
}
