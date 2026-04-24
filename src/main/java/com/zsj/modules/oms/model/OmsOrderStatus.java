package com.zsj.modules.oms.model;

/**
 * 订单状态常量
 * 0待支付, 1待发货, 2已发货, 3已完成, 4已关闭
 */
public final class OmsOrderStatus {

    private OmsOrderStatus() {
    }

    public static final int PENDING_PAYMENT = 0;
    public static final int PENDING_DELIVERY = 1;
    public static final int DELIVERED = 2;
    public static final int COMPLETED = 3;
    public static final int CLOSED = 4;

    public static boolean isValid(Integer status) {
        return status != null && status >= PENDING_PAYMENT && status <= CLOSED;
    }
}
