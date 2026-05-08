package com.zsj.modules.sms.model;

/**
 * 秒杀 MQ 失败日志处理状态常量。
 */
public final class SmsSeckillMqFailLogHandleStatus {

    private SmsSeckillMqFailLogHandleStatus() {
    }

    /**
     * 待处理。
     */
    public static final int PENDING = 0;

    /**
     * 已重投。
     */
    public static final int REQUEUED = 1;

    /**
     * 已忽略。
     */
    public static final int IGNORED = 2;

    /**
     * 处理成功。
     */
    public static final int SUCCESS = 3;

    /**
     * 处理失败。
     */
    public static final int FAILED = 4;
}
