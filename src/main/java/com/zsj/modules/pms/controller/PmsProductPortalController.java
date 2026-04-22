package com.zsj.modules.pms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.pms.dto.PmsProductQueryDTO;
import com.zsj.modules.pms.model.PmsProduct;
import com.zsj.modules.pms.service.PmsProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 买家端商品浏览接口（Portal）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/product")
public class PmsProductPortalController {

    private final PmsProductService pmsProductService;

    /**
     * 买家商品列表：仅展示已上架商品
     */
    @GetMapping("/list")
    public CommonResult<IPage<PmsProduct>> list(@ModelAttribute PmsProductQueryDTO queryDTO) {
        // 买家端强制只看上架商品
        queryDTO.setPublishStatus(1);
        IPage<PmsProduct> page = pmsProductService.listPage(queryDTO);
        return CommonResult.success(page, "获取买家商品列表成功");
    }

    /**
     * 买家商品详情：仅允许查看已上架商品
     */
    @GetMapping("/detail")
    public CommonResult<PmsProduct> detail(@RequestParam Long id) {
        PmsProduct product = pmsProductService.getById(id);

        // 买家端不暴露下架商品
        if (product.getPublishStatus() == null || product.getPublishStatus() != 1) {
            return CommonResult.failed("商品不存在或已下架");
        }

        return CommonResult.success(product, "获取买家商品详情成功");
    }

}
