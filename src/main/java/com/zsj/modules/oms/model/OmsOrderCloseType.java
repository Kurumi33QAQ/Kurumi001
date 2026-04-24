package com.zsj.modules.oms.model;

/**
 * 订单关闭类型
 * 0未关闭, 1用户取消, 2超时关闭
 */
public final class OmsOrderCloseType {

    private OmsOrderCloseType() {
    }

    public static final int NONE = 0;
    public static final int USER_CANCEL = 1;
    public static final int TIMEOUT_AUTO_CLOSE = 2;

    public static boolean isValid(Integer closeType) {
        return closeType != null && closeType >= NONE && closeType <= TIMEOUT_AUTO_CLOSE;
    }

}
