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

import java.time.LocalDateTime;

/**
 * 买家通知业务实现。
 */
@RequiredArgsConstructor
@Service
public class UmsMemberNotificationServiceImpl implements UmsMemberNotificationService {

    private final UmsMemberNotificationMapper notificationMapper;
    private final UmsMemberMapper umsMemberMapper;

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
