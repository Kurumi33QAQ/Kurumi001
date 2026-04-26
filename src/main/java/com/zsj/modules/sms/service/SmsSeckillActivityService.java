package com.zsj.modules.sms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.modules.sms.dto.SmsSeckillActivityPortalDTO;
import com.zsj.modules.sms.dto.SmsSeckillActivityQueryDTO;
import com.zsj.modules.sms.dto.SmsSeckillSubmitDTO;

/**
 * 秒杀活动业务接口
 */
public interface SmsSeckillActivityService {

    /**
     * 买家端查询可见秒杀活动
     */
    IPage<SmsSeckillActivityPortalDTO> listPortalPage(SmsSeckillActivityQueryDTO queryDTO);

    /**
     * 买家端查询秒杀活动详情
     */
    SmsSeckillActivityPortalDTO getPortalDetail(Long id);

    /**
     * 买家提交秒杀请求（基础版骨架）
     */
    String submitSeckill(String memberUsername, SmsSeckillSubmitDTO dto);

}
