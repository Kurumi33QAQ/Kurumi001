package com.zsj.modules.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsj.modules.pms.model.PmsProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 */
@Mapper
public interface PmsProductMapper extends BaseMapper<PmsProduct> {
}
