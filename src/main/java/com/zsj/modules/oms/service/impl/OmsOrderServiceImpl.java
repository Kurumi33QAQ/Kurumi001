package com.zsj.modules.oms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.dto.OmsOrderStatusUpdateDTO;
import com.zsj.modules.oms.mapper.OmsOrderMapper;
import com.zsj.modules.oms.model.OmsOrder;
import com.zsj.modules.oms.service.OmsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;

/**
 * 订单业务实现
 */
@RequiredArgsConstructor
@Service
public class OmsOrderServiceImpl implements OmsOrderService {

    private final OmsOrderMapper omsOrderMapper;

    @Override
    public IPage<OmsOrder> listPage(OmsOrderQueryDTO queryDTO) {
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleteStatus, 0);

        if (StringUtils.hasText(queryDTO.getOrderSn())) {
            wrapper.like(OmsOrder::getOrderSn, queryDTO.getOrderSn());
        }
        if (StringUtils.hasText(queryDTO.getMemberUsername())) {
            wrapper.like(OmsOrder::getMemberUsername, queryDTO.getMemberUsername());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(OmsOrder::getStatus, queryDTO.getStatus());
        }

        wrapper.orderByDesc(OmsOrder::getUpdateTime)
                .orderByDesc(OmsOrder::getId);

        Page<OmsOrder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return omsOrderMapper.selectPage(page, wrapper);
    }


    @Override
    public OmsOrder getById(Long id) {
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getId, id)
                .eq(OmsOrder::getDeleteStatus, 0);

        OmsOrder order = omsOrderMapper.selectOne(wrapper);
        if (order == null) {
            throw new ApiException(ResultCode.FAILED);
        }
        return order;
    }


    @Override
    public void updateStatus(OmsOrderStatusUpdateDTO dto) {
        Integer status = dto.getStatus();
        if (status < 0 || status > 4) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        LambdaUpdateWrapper<OmsOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OmsOrder::getId, dto.getId())
                .eq(OmsOrder::getDeleteStatus, 0)
                .set(OmsOrder::getStatus, status)
                .set(OmsOrder::getNote, dto.getNote())
                .set(OmsOrder::getUpdateTime, java.time.LocalDateTime.now());

        int rows = omsOrderMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }

}
