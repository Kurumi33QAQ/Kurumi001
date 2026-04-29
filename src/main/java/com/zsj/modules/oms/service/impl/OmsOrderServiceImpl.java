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
import com.zsj.modules.oms.model.OmsOrderCloseType;
import com.zsj.modules.oms.model.OmsOrderType;
import com.zsj.modules.oms.service.OmsOrderService;
import com.zsj.modules.pms.mapper.PmsProductMapper;
import com.zsj.modules.pms.service.PmsProductService;
import com.zsj.modules.sms.service.SmsSeckillCompensationService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.service.UmsMemberNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.pms.model.PmsProduct;
import com.zsj.modules.oms.model.OmsOrderStatus;
import com.zsj.modules.ums.model.UmsMemberNotificationType;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final SmsSeckillCompensationService smsSeckillCompensationService;
    private final UmsMemberNotificationService umsMemberNotificationService;




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
        if (queryDTO.getCloseType() != null) {
            if (!OmsOrderCloseType.isValid(queryDTO.getCloseType())) {
                throw new ApiException(ResultCode.VALIDATE_FAILED);
            }
            wrapper.eq(OmsOrder::getCloseType, queryDTO.getCloseType());
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
        if (!OmsOrderStatus.isValid(status)) {
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
        order.setStatus(OmsOrderStatus.PENDING_PAYMENT);
        order.setNote(dto.getNote());
        order.setDeleteStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setProductId(product.getId());
        order.setProductQuantity(dto.getQuantity());
        order.setCloseType(OmsOrderCloseType.NONE);
        order.setOrderType(OmsOrderType.NORMAL);
        order.setSourceId(null);




        int rows = omsOrderMapper.insert(order);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        // 订单创建成功后生成买家通知。通知先落库，后续再接 WebSocket 实时推送。
        umsMemberNotificationService.createNotification(
                memberUsername,
                UmsMemberNotificationType.ORDER,
                "订单创建成功",
                "你的订单已创建成功，请尽快完成支付。",
                order.getId()
        );

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
    @Transactional(rollbackFor = Exception.class)
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
        if (order.getStatus() == null || order.getStatus() != OmsOrderStatus.PENDING_PAYMENT) {
            throw new ApiException(UmsErrorCode.ORDER_NOT_CANCELABLE);
        }

        // 4) 更新为已取消（这里先约定 status=4 为已取消）
        LambdaUpdateWrapper<OmsOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OmsOrder::getId, orderId)
                .eq(OmsOrder::getDeleteStatus, 0)
                .eq(OmsOrder::getStatus, OmsOrderStatus.PENDING_PAYMENT)
                .set(OmsOrder::getStatus, OmsOrderStatus.CLOSED)
                .set(OmsOrder::getUpdateTime, LocalDateTime.now())
                .set(OmsOrder::getCloseType, OmsOrderCloseType.USER_CANCEL);


        int rows = omsOrderMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        // 取消成功后，根据订单类型执行不同补偿
        compensateAfterOrderClosed(order);

    }


    /**
     * 取消订单后回补库存（基础版）
     */
    private void restoreStockOnCancel(OmsOrder order) {
        if (order.getOrderType() != null && order.getOrderType() == OmsOrderType.SECKILL) {
            return;
        }


        if (order.getProductId() == null || order.getProductQuantity() == null || order.getProductQuantity() <= 0) {
            return;
        }

        Integer qty = order.getProductQuantity();

        LambdaUpdateWrapper<PmsProduct> productWrapper = new LambdaUpdateWrapper<>();
        productWrapper.eq(PmsProduct::getId, order.getProductId())
                .setSql("stock = stock + " + qty + ", sale = CASE WHEN sale >= " + qty + " THEN sale - " + qty + " ELSE 0 END")
                .set(PmsProduct::getUpdateTime, LocalDateTime.now());

        int productRows = pmsProductMapper.update(null, productWrapper);
        if (productRows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }


    /**
     * 订单关闭后根据订单类型执行补偿
     */
    private void compensateAfterOrderClosed(OmsOrder order) {
        if (order.getOrderType() != null && order.getOrderType() == OmsOrderType.SECKILL) {
            smsSeckillCompensationService.compensateClosedOrder(
                    order.getSourceId(),
                    order.getMemberUsername(),
                    order.getId(),
                    order.getProductQuantity()
            );
            return;
        }

        restoreStockOnCancel(order);
    }




    /**
     * 自动取消超时未支付订单（基础版：改状态 + 恢复库存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCancelTimeoutOrders(int timeoutMinutes) {
        int effectiveTimeoutMinutes = timeoutMinutes <= 0 ? 30 : timeoutMinutes;
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(effectiveTimeoutMinutes);

        // 1) 先查出“待支付 + 未删除 + 已超时”的订单
        LambdaQueryWrapper<OmsOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OmsOrder::getStatus, OmsOrderStatus.PENDING_PAYMENT)
                .eq(OmsOrder::getDeleteStatus, 0)
                .le(OmsOrder::getCreateTime, deadline);

        List<OmsOrder> timeoutOrders = omsOrderMapper.selectList(queryWrapper);
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return 0;
        }

        // 2) 逐单取消（带并发保护），成功后回补库存
        int successCount = 0;
        for (OmsOrder order : timeoutOrders) {
            LambdaUpdateWrapper<OmsOrder> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getDeleteStatus, 0)
                    .eq(OmsOrder::getStatus, OmsOrderStatus.PENDING_PAYMENT) // 防并发：只取消仍是待支付的订单
                    .set(OmsOrder::getStatus, OmsOrderStatus.CLOSED)
                    .set(OmsOrder::getNote, "系统自动关闭：超时未支付")
                    .set(OmsOrder::getCloseType, OmsOrderCloseType.TIMEOUT_AUTO_CLOSE)
                    .set(OmsOrder::getUpdateTime, LocalDateTime.now());

            int rows = omsOrderMapper.update(null, updateWrapper);
            if (rows > 0) {
                compensateAfterOrderClosed(order);
                successCount++;
            }

        }

        return successCount;
    }


    /**
     * 创建秒杀订单（基础版：同步创建订单，不扣普通商品库存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeckillOrder(String memberUsername,
                                   Long activityId,
                                   Long productId,
                                   BigDecimal seckillPrice,
                                   Integer quantity) {
        if (!StringUtils.hasText(memberUsername)
                || activityId == null
                || productId == null
                || seckillPrice == null
                || quantity == null
                || quantity < 1) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        PmsProduct product = pmsProductService.getById(productId);
        if (product.getPublishStatus() == null || product.getPublishStatus() != 1) {
            throw new ApiException(UmsErrorCode.SECKILL_PRODUCT_INVALID);
        }

        BigDecimal totalAmount = seckillPrice.multiply(BigDecimal.valueOf(quantity));

        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn());
        order.setMemberUsername(memberUsername);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(OmsOrderStatus.PENDING_PAYMENT);
        order.setNote("秒杀订单");
        order.setDeleteStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setProductId(productId);
        order.setProductQuantity(quantity);
        order.setCloseType(OmsOrderCloseType.NONE);
        order.setOrderType(OmsOrderType.SECKILL);
        order.setSourceId(activityId);

        int rows = omsOrderMapper.insert(order);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        return order.getId();
    }


}
