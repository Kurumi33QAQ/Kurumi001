package com.zsj.modules.oms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.modules.oms.dto.OmsOrderCreateDTO;
import com.zsj.modules.oms.dto.OmsOrderQueryDTO;
import com.zsj.modules.oms.dto.OmsOrderStatusUpdateDTO;
import com.zsj.modules.oms.mapper.OmsOrderMapper;
import com.zsj.modules.oms.model.OmsOrder;
import com.zsj.modules.oms.service.OmsOrderService;
import com.zsj.modules.pms.mapper.PmsProductMapper;
import com.zsj.modules.pms.service.PmsProductService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.pms.model.PmsProduct;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;



/**
 * 订单业务实现
 */
@RequiredArgsConstructor
@Service
public class OmsOrderServiceImpl implements OmsOrderService {

    private final OmsOrderMapper omsOrderMapper;
    private final PmsProductService pmsProductService;
    private final PmsProductMapper pmsProductMapper;



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


    /**
     * 买家创建订单（基础版：单商品下单）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(String memberUsername, OmsOrderCreateDTO dto) {
        // 1) 基础参数兜底校验（Controller 已经 @Valid，这里再做服务层保护）
        if (!StringUtils.hasText(memberUsername) || dto == null
                || dto.getProductId() == null
                || dto.getQuantity() == null
                || dto.getQuantity() < 1) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 2) 商品存在校验（不存在会由 getById 抛异常）
        PmsProduct product = pmsProductService.getById(dto.getProductId());

        // 3) 商品上架校验（买家只能买上架商品）
        if (product.getPublishStatus() == null || product.getPublishStatus() != 1) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 4) 库存充足校验（本步先校验，不扣库存）
        if (product.getStock() == null || product.getStock() < dto.getQuantity()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }


        // 5) 同步扣库存 + 加销量（基础版）
        // 说明：这里是“当前读值回写”，后续高并发场景再升级为更强原子方案
        int originStock = product.getStock();
        int originSale = product.getSale() == null ? 0 : product.getSale();

        LambdaUpdateWrapper<PmsProduct> productUpdateWrapper = new LambdaUpdateWrapper<>();
        productUpdateWrapper.eq(PmsProduct::getId, product.getId())
                .eq(PmsProduct::getDeleteStatus, 0)
                .eq(PmsProduct::getPublishStatus, 1)
                .eq(PmsProduct::getStock, originStock) // 简单并发保护：库存被别人改过则更新失败
                .set(PmsProduct::getStock, originStock - dto.getQuantity())
                .set(PmsProduct::getSale, originSale + dto.getQuantity())
                .set(PmsProduct::getUpdateTime, LocalDateTime.now());

        int stockRows = pmsProductMapper.update(null, productUpdateWrapper);
        if (stockRows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }



        // 6) 计算订单金额（基础版先按单商品价格 * 数量）
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        // 7) 创建订单（基础版：先不扣库存）
        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn());
        order.setMemberUsername(memberUsername);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 0: 待支付
        order.setNote(dto.getNote());
        order.setDeleteStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        int rows = omsOrderMapper.insert(order);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        return order.getId();
    }

    /**
     * 生成订单号（基础版）
     */
    private String generateOrderSn() {
        long now = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "M" + now + random;
    }


    /**
     * 买家取消订单（基础版）
     */
    @Override
    public void cancelOrder(String memberUsername, Long orderId) {
        if (!StringUtils.hasText(memberUsername) || orderId == null) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 1) 订单存在性
        OmsOrder order = getById(orderId);

        // 2) 订单归属校验（只能取消自己的）
        if (!memberUsername.equals(order.getMemberUsername())) {
            throw new ApiException(ResultCode.FORBIDDEN);
        }

        // 3) 仅待支付订单允许取消（status=0）
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ApiException(UmsErrorCode.ORDER_NOT_CANCELABLE);
        }

        // 4) 更新为已取消（这里先约定 status=4 为已取消）
        LambdaUpdateWrapper<OmsOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getDeleteStatus, 0)
                .eq(OmsOrder::getStatus, 0) // 防止并发重复取消
                .set(OmsOrder::getStatus, 4)
                .set(OmsOrder::getUpdateTime, LocalDateTime.now());

        int rows = omsOrderMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }



}
