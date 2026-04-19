package com.zsj.modules.pms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.modules.pms.dto.PmsProductCreateDTO;
import com.zsj.modules.pms.dto.PmsProductQueryDTO;
import com.zsj.modules.pms.dto.PmsProductUpdateDTO;
import com.zsj.modules.pms.model.PmsProduct;

import java.util.List;

/**
 * 商品业务接口
 */
public interface PmsProductService {

    /**
     * 新增商品
     */
    Long create(PmsProductCreateDTO dto);

    /**
     * 商品分页查询
     */
    IPage<PmsProduct> listPage(PmsProductQueryDTO queryDTO);

    /**
     * 修改商品上架状态
     */
    void updatePublishStatus(Long id, Integer publishStatus);

    /**
     * 逻辑删除商品
     */
    void delete(Long id);

    /**
     * 修改商品基础信息
     */
    void update(PmsProductUpdateDTO dto);

    /**
     * 查询商品详情
     */
    PmsProduct getById(Long id);

    /**
     * 批量修改商品上架状态
     */
    int updatePublishStatusBatch(List<Long> ids, Integer publishStatus);

    /**
     * 批量逻辑删除商品
     */
    int deleteBatch(List<Long> ids);

}
