package com.zsj.modules.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.pms.dto.PmsProductCreateDTO;
import com.zsj.modules.pms.dto.PmsProductUpdateDTO;
import com.zsj.modules.pms.mapper.PmsProductMapper;
import com.zsj.modules.pms.model.PmsProduct;
import com.zsj.modules.pms.service.PmsProductService;
import com.zsj.modules.ums.enums.UmsErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.modules.pms.dto.PmsProductQueryDTO;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品业务实现
 */
@RequiredArgsConstructor
@Service
public class PmsProductServiceImpl implements PmsProductService {

    private final PmsProductMapper pmsProductMapper;

    @Override
    public Long create(PmsProductCreateDTO dto) {
        PmsProduct product = new PmsProduct();
        product.setName(dto.getName());
        product.setSubTitle(dto.getSubTitle());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setSale(0);
        product.setPic(dto.getPic());
        product.setPublishStatus(0); // 默认下架
        product.setDeleteStatus(0);  // 默认未删除
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        int rows = pmsProductMapper.insert(product);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
        return product.getId();
    }


    /**
     * 商品分页查询（名称模糊 + 上架状态）
     */
    @Override
    public IPage<PmsProduct> listPage(PmsProductQueryDTO queryDTO) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<>();

        // 只查未删除商品
        wrapper.eq(PmsProduct::getDeleteStatus, 0);

        // 名称模糊查询
        if (StringUtils.hasText(queryDTO.getName())) {
            wrapper.like(PmsProduct::getName, queryDTO.getName());
        }

        // 上架状态筛选
        if (queryDTO.getPublishStatus() != null) {
            wrapper.eq(PmsProduct::getPublishStatus, queryDTO.getPublishStatus());
        }

        // 价格区间筛选
        if (queryDTO.getMinPrice() != null) {
            wrapper.ge(PmsProduct::getPrice, queryDTO.getMinPrice());
        }
        if (queryDTO.getMaxPrice() != null) {
            wrapper.le(PmsProduct::getPrice, queryDTO.getMaxPrice());
        }

        // 边界校验：最低价不能大于最高价
        if (queryDTO.getMinPrice() != null && queryDTO.getMaxPrice() != null
                && queryDTO.getMinPrice().compareTo(queryDTO.getMaxPrice()) > 0) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        // 按更新时间倒序，再按ID倒序
        wrapper.orderByDesc(PmsProduct::getUpdateTime)
                .orderByDesc(PmsProduct::getId);

        Page<PmsProduct> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return pmsProductMapper.selectPage(page, wrapper);
    }


    /**
     * 修改商品上架状态（0下架，1上架）
     */
    @Override
    public void updatePublishStatus(Long id, Integer publishStatus) {
        if (publishStatus == null || (publishStatus != 0 && publishStatus != 1)) {
            throw new ApiException(UmsErrorCode.PRODUCT_PUBLISH_STATUS_INVALID);
        }

        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PmsProduct::getId, id)
                .eq(PmsProduct::getDeleteStatus, 0)
                .set(PmsProduct::getPublishStatus, publishStatus)
                .set(PmsProduct::getUpdateTime, java.time.LocalDateTime.now());

        int rows = pmsProductMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }


    /**
     * 逻辑删除商品（deleteStatus=1）
     */
    @Override
    public void delete(Long id) {
        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PmsProduct::getId, id)
                .eq(PmsProduct::getDeleteStatus, 0)
                .set(PmsProduct::getDeleteStatus, 1)
                .set(PmsProduct::getUpdateTime, java.time.LocalDateTime.now());

        int rows = pmsProductMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }


    /**
     * 修改商品基础信息
     */
    @Override
    public void update(PmsProductUpdateDTO dto) {
        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PmsProduct::getId, dto.getId())
                .eq(PmsProduct::getDeleteStatus, 0)
                .set(PmsProduct::getName, dto.getName())
                .set(PmsProduct::getSubTitle, dto.getSubTitle())
                .set(PmsProduct::getPrice, dto.getPrice())
                .set(PmsProduct::getStock, dto.getStock())
                .set(PmsProduct::getPic, dto.getPic())
                .set(PmsProduct::getUpdateTime, java.time.LocalDateTime.now());

        int rows = pmsProductMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }
    }


    /**
     * 查询商品详情（仅未删除商品）
     */
    @Override
    public PmsProduct getById(Long id) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PmsProduct::getId, id)
                .eq(PmsProduct::getDeleteStatus, 0);

        PmsProduct product = pmsProductMapper.selectOne(wrapper);
        if (product == null) {
            throw new ApiException(ResultCode.FAILED);
        }
        return product;
    }


    /**
     * 批量修改商品上架状态（0下架，1上架）
     *
     * @return 影响行数
     */
    @Override
    public int updatePublishStatusBatch(List<Long> ids, Integer publishStatus) {
        if (publishStatus == null || (publishStatus != 0 && publishStatus != 1)) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(PmsProduct::getId, ids)
                .eq(PmsProduct::getDeleteStatus, 0)
                .set(PmsProduct::getPublishStatus, publishStatus)
                .set(PmsProduct::getUpdateTime, java.time.LocalDateTime.now());

        return pmsProductMapper.update(null, wrapper);
    }


    /**
     * 批量逻辑删除商品（deleteStatus=1）
     *
     * @return 影响行数
     */
    @Override
    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED);
        }

        LambdaUpdateWrapper<PmsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(PmsProduct::getId, ids)
                .eq(PmsProduct::getDeleteStatus, 0)
                .set(PmsProduct::getDeleteStatus, 1)
                .set(PmsProduct::getUpdateTime, java.time.LocalDateTime.now());

        return pmsProductMapper.update(null, wrapper);
    }

}
