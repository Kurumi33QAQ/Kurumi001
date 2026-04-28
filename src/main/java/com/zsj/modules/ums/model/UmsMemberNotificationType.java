package com.zsj.modules.ums.model;

/**
 * 买家通知类型常量。
 */
public final class UmsMemberNotificationType {

    private UmsMemberNotificationType() {
    }

    /**
     * 订单通知，例如订单创建成功、订单关闭。
     */
    public static final int ORDER = 1;

    /**
     * 秒杀通知，例如秒杀成功、秒杀失败。
     */
    public static final int SECKILL = 2;

    /**
     * 系统通知，例如管理员发送公告。
     */
    public static final int SYSTEM = 3;
}
