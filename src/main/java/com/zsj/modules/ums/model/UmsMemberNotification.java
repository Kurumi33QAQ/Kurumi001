package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买家通知实体，对应 ums_member_notification 表。
 *
 * 通知中心采用“先落库，再推送”的设计：
 * 用户在线时可以实时收到 WebSocket 消息；
 * 用户离线时也可以通过历史通知接口查到消息。
 */
@Data
@TableName("ums_member_notification")
public class UmsMemberNotification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String memberUsername;

    private String title;

    private String content;

    /**
     * 通知类型：
     * 1 订单通知
     * 2 秒杀通知
     * 3 系统通知
     */
    private Integer type;

    /**
     * 关联业务ID。
     * 例如订单通知可以存订单ID，秒杀通知可以存活动ID或订单ID。
     */
    private Long businessId;

    /**
     * 阅读状态：
     * 0 未读
     * 1 已读
     */
    private Integer readStatus;

    private LocalDateTime createTime;

    private LocalDateTime readTime;

    private Integer deleteStatus;
}
