package com.zsj.modules.ums.controller;

import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.service.UmsAdminService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开发环境调试接口：
 * 仅在 dev profile 下生效，生产环境不会加载该控制器。
 */
@Profile("dev")
@RestController
public class DevDebugController {

    private final UmsAdminService umsAdminService;

    public DevDebugController(UmsAdminService umsAdminService) {
        this.umsAdminService = umsAdminService;
    }

    /**
     * 查看当前登录用户的权限列表（调试用）
     */
    @GetMapping("/demo/admin/authorities")
    public CommonResult<List<String>> authorities(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        String username = authentication.getName();
        List<String> authorities = umsAdminService.getAuthorityList(username);
        return CommonResult.success(authorities, "获取权限列表成功");
    }


    /**
     * 手动清理某个用户的权限缓存（开发调试）
     */
    @GetMapping("/demo/admin/cache/evict")
    public CommonResult<String> evictAuthorityCache(@RequestParam String username) {
        umsAdminService.evictAuthorityCache(username);
        return CommonResult.success("已清理用户权限缓存：" + username);
    }

    /**
     * 查看权限缓存当前大小（开发调试）
     */
    @GetMapping("/demo/admin/cache/size")
    public CommonResult<Integer> authorityCacheSize() {
        return CommonResult.success(umsAdminService.getAuthorityCacheSize(), "获取缓存大小成功");
    }

}
