package com.zsj.modules.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsj.modules.sms.model.SmsSeckillMqFailLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀 MQ 失败日志 Mapper。
 */
@Mapper
public interface SmsSeckillMqFailLogMapper extends BaseMapper<SmsSeckillMqFailLog> {
}
