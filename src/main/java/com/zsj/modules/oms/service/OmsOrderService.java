package com.zsj.modules.oms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.modules.oms.dto.OmsOrderCreateDTO;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.dto.OmsOrderStatusUpdateDTO;
import com.zsj.modules.oms.model.OmsOrder;

/**
 * 订单业务接口
 */
public interface OmsOrderService {

    /**
     * 订单分页查询
     */
    IPage<OmsOrder> listPage(OmsOrderQueryDTO queryDTO);

    /**
     * 查询订单详情
     */
    OmsOrder getById(Long id);

    /**
     * 更新订单状态
     */
    void updateStatus(OmsOrderStatusUpdateDTO dto);

    /**
     * 买家创建订单（基础版：单商品下单）
     */
    Long createOrder(String memberUsername, OmsOrderCreateDTO dto);

    /**
     * 买家取消订单（基础版）
     */
    void cancelOrder(String memberUsername, Long orderId);

    /**
     * 自动取消超时未支付订单
     * @param timeoutMinutes 超时时间（分钟）
     * @return 本次自动取消数量
     */
    int autoCancelTimeoutOrders(int timeoutMinutes);

}
