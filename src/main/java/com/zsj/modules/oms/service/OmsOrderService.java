package com.zsj.modules.oms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

}
