package com.zsj.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.mapper.UmsMemberMapper;
import com.zsj.modules.ums.mapper.UmsMemberNotificationMapper;
import com.zsj.modules.ums.model.UmsMember;
import com.zsj.modules.ums.model.UmsMemberNotification;
import com.zsj.modules.ums.model.UmsMemberNotificationReadStatus;
import com.zsj.modules.ums.service.UmsMemberNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.zsj.modules.ums.websocket.MemberNotificationWebSocketSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.time.LocalDateTime;

/**
 * 买家通知业务实现。
 */
@RequiredArgsConstructor
@Service
public class UmsMemberNotificationServiceImpl implements UmsMemberNotificationService {

    private final UmsMemberNotificationMapper notificationMapper;
    private final UmsMemberMapper umsMemberMapper;
    private final MemberNotificationWebSocketSender memberNotificationWebSocketSender;

    /**
     * 创建通知时先根据用户名查询买家，保证通知一定归属于一个真实买家。
     */
    @Override
    public Long createNotification(String memberUsername,
                                   Integer type,
                                   String title,
                                   String content,
                                   Long businessId) {
        if (!StringUtils.hasText(memberUsername)) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }

        UmsMember member = getMemberByUsername(memberUsername);

        UmsMemberNotification notification = new UmsMemberNotification();
        notification.setMemberId(member.getId());
        notification.setMemberUsername(member.getUsername());
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBusinessId(businessId);
        notification.setReadStatus(UmsMemberNotificationReadStatus.UNREAD);
        notification.setDeleteStatus(0);
        notification.setCreateTime(LocalDateTime.now());

        int rows = notificationMapper.insert(notification);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        // 通知先落库。事务提交后，如果买家在线，再通过 WebSocket 推送。
        pushNotificationAndUnreadCountAfterCommit(member.getUsername(), notification);

        return notification.getId();

    }

    @Override
    public IPage<UmsMemberNotification> listMyNotifications(String memberUsername,
                                                            Integer pageNum,
                                                            Integer pageSize) {
        LambdaQueryWrapper<UmsMemberNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsMemberNotification::getMemberUsername, memberUsername)
                .eq(UmsMemberNotification::getDeleteStatus, 0)
                .orderByDesc(UmsMemberNotification::getCreateTime);

        Page<UmsMemberNotification> page = new Page<>(pageNum, pageSize);
        return notificationMapper.selectPage(page, wrapper);
    }

    @Override
    public Long countUnread(String memberUsername) {
        LambdaQueryWrapper<UmsMemberNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsMemberNotification::getMemberUsername, memberUsername)
                .eq(UmsMemberNotification::getReadStatus, UmsMemberNotificationReadStatus.UNREAD)
                .eq(UmsMemberNotification::getDeleteStatus, 0);

        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void markAsRead(String memberUsername, Long notificationId) {
        LambdaUpdateWrapper<UmsMemberNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UmsMemberNotification::getId, notificationId)
                .eq(UmsMemberNotification::getMemberUsername, memberUsername)
                .eq(UmsMemberNotification::getDeleteStatus, 0)
                .set(UmsMemberNotification::getReadStatus, UmsMemberNotificationReadStatus.READ)
                .set(UmsMemberNotification::getReadTime, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
        pushUnreadCountAfterCommit(memberUsername);
    }

    @Override
    public void markAllAsRead(String memberUsername) {
        LambdaUpdateWrapper<UmsMemberNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UmsMemberNotification::getMemberUsername, memberUsername)
                .eq(UmsMemberNotification::getReadStatus, UmsMemberNotificationReadStatus.UNREAD)
                .eq(UmsMemberNotification::getDeleteStatus, 0)
                .set(UmsMemberNotification::getReadStatus, UmsMemberNotificationReadStatus.READ)
                .set(UmsMemberNotification::getReadTime, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
        pushUnreadCountAfterCommit(memberUsername);
    }


    /**
     * 在事务提交后推送通知。
     *
     * 为什么不在 insert 后立刻推送？
     * 因为 createNotification 可能被订单、秒杀这类事务方法调用。
     * 如果事务最后回滚了，但消息已经推给用户，就会出现“用户收到通知，但数据库没有记录”的问题。
     */
    private void pushNotificationAndUnreadCountAfterCommit(String memberUsername, UmsMemberNotification notification) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    memberNotificationWebSocketSender.sendNotification(memberUsername, notification);
                    memberNotificationWebSocketSender.sendUnreadCount(memberUsername, countUnread(memberUsername));
                }
            });
            return;
        }

        memberNotificationWebSocketSender.sendNotification(memberUsername, notification);
        memberNotificationWebSocketSender.sendUnreadCount(memberUsername, countUnread(memberUsername));
    }



    private void pushUnreadCountAfterCommit(String memberUsername) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    memberNotificationWebSocketSender.sendUnreadCount(memberUsername, countUnread(memberUsername));
                }
            });
            return;
        }

        memberNotificationWebSocketSender.sendUnreadCount(memberUsername, countUnread(memberUsername));
    }




    private UmsMember getMemberByUsername(String memberUsername) {
        LambdaQueryWrapper<UmsMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsMember::getUsername, memberUsername)
                .eq(UmsMember::getDeleteStatus, 0);

        UmsMember member = umsMemberMapper.selectOne(wrapper);
        if (member == null) {
            throw new ApiException(UmsErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }
}
