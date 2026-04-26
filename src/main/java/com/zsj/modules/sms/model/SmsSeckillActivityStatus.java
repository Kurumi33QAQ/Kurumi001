package com.zsj.modules.sms.model;

/**
 * 秒杀活动展示状态
 */
public final class SmsSeckillActivityStatus {

    private SmsSeckillActivityStatus() {
    }

    public static final int DISABLED = 0;
    public static final int NOT_STARTED = 1;
    public static final int IN_PROGRESS = 2;
    public static final int ENDED = 3;
    public static final int SOLD_OUT = 4;

    public static String getText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case DISABLED -> "未启用";
            case NOT_STARTED -> "未开始";
            case IN_PROGRESS -> "进行中";
            case ENDED -> "已结束";
            case SOLD_OUT -> "已售罄";
            default -> "未知";
        };
    }
}
