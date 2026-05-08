package com.zsj.modules.sms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.sms.dto.SmsSeckillActivityQueryDTO;
import com.zsj.modules.sms.dto.SmsSeckillActivityPortalDTO;
import com.zsj.modules.sms.dto.SmsSeckillSubmitDTO;
import com.zsj.modules.sms.service.SmsSeckillActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 买家端秒杀活动接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/member/seckill")
public class SmsSeckillPortalController {

    private final SmsSeckillActivityService smsSeckillActivityService;

    /**
     * 秒杀活动列表
     */
    @GetMapping("/list")
    public CommonResult<IPage<SmsSeckillActivityPortalDTO>> list(@ModelAttribute SmsSeckillActivityQueryDTO queryDTO) {
        IPage<SmsSeckillActivityPortalDTO> page = smsSeckillActivityService.listPortalPage(queryDTO);
        return CommonResult.success(page, "获取秒杀活动列表成功");
    }

    /**
     * 秒杀活动详情
     */
    @GetMapping("/detail")
    public CommonResult<SmsSeckillActivityPortalDTO> detail(@RequestParam Long id) {
        SmsSeckillActivityPortalDTO activity = smsSeckillActivityService.getPortalDetail(id);
        return CommonResult.success(activity, "获取秒杀活动详情成功");
    }


    /**
     * 提交秒杀请求
     */
    @PostMapping("/submit")
    public CommonResult<Long> submit(@Valid @RequestBody SmsSeckillSubmitDTO dto,
                                       Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        Long recordId = smsSeckillActivityService.submitSeckill(authentication.getName(), dto);
        return CommonResult.success(recordId, "秒杀请求已受理，请等待处理结果");

    }

}
