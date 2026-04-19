package com.zsj.modules.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsj.modules.oms.model.OmsOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OmsOrderMapper extends BaseMapper<OmsOrder> {
}
