package com.zsj.modules.ums.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.model.UmsMemberNotification;
import com.zsj.modules.ums.service.UmsMemberNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家通知接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/notification")
public class UmsMemberNotificationController {

    private final UmsMemberNotificationService notificationService;

    /**
     * 查询我的通知列表。
     */
    @GetMapping("/list")
    public CommonResult<IPage<UmsMemberNotification>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        IPage<UmsMemberNotification> page = notificationService.listMyNotifications(
                authentication.getName(),
                pageNum,
                pageSize
        );
        return CommonResult.success(page, "获取我的通知列表成功");
    }

    /**
     * 查询我的未读通知数量。
     */
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        Long count = notificationService.countUnread(authentication.getName());
        return CommonResult.success(count, "获取未读通知数成功");
    }

    /**
     * 标记单条通知为已读。
     */
    @PostMapping("/read")
    public CommonResult<String> read(@RequestParam Long id,
                                     Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        notificationService.markAsRead(authentication.getName(), id);
        return CommonResult.success("标记已读成功");
    }

    /**
     * 标记全部通知为已读。
     */
    @PostMapping("/read-all")
    public CommonResult<String> readAll(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        notificationService.markAllAsRead(authentication.getName());
        return CommonResult.success("全部标记已读成功");
    }
}
