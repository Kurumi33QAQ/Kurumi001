package com.zsj.modules.ums.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.modules.ums.model.UmsMemberNotification;

/**
 * 买家通知业务接口。
 */
public interface UmsMemberNotificationService {

    /**
     * 创建买家通知。
     *
     * 后续订单创建、秒杀结果、管理员系统通知都会复用这个方法。
     */
    Long createNotification(String memberUsername,
                            Integer type,
                            String title,
                            String content,
                            Long businessId);

    /**
     * 查询当前买家的通知列表。
     */
    IPage<UmsMemberNotification> listMyNotifications(String memberUsername,
                                                     Integer pageNum,
                                                     Integer pageSize);

    /**
     * 查询当前买家的未读通知数量。
     */
    Long countUnread(String memberUsername);

    /**
     * 标记当前买家的一条通知为已读。
     */
    void markAsRead(String memberUsername, Long notificationId);

    /**
     * 标记当前买家的所有通知为已读。
     */
    void markAllAsRead(String memberUsername);
}
