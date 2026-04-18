package com.zsj.modules.pms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.pms.dto.PmsProductCreateDTO;
import com.zsj.modules.pms.dto.PmsProductQueryDTO;
import com.zsj.modules.pms.model.PmsProduct;
import com.zsj.modules.pms.service.PmsProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理接口
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/product")
public class PmsProductController {

    private final PmsProductService pmsProductService;

    /**
     * 新增商品
     */
    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PmsProductCreateDTO dto) {
        Long id = pmsProductService.create(dto);
        return CommonResult.success(id, "新增商品成功");
    }


    /**
     * 商品分页查询
     */
    @GetMapping("/list")
    public CommonResult<IPage<PmsProduct>> list(@ModelAttribute PmsProductQueryDTO queryDTO) {
        IPage<PmsProduct> page = pmsProductService.listPage(queryDTO);
        return CommonResult.success(page, "查询商品列表成功");
    }


    /**
     * 修改商品上架状态（0下架，1上架）
     */
    @PostMapping("/publish/status")
    public CommonResult<String> updatePublishStatus(@RequestParam Long id, @RequestParam Integer publishStatus) {
        pmsProductService.updatePublishStatus(id, publishStatus);
        return CommonResult.success("修改商品上架状态成功");
    }


    /**
     * 逻辑删除商品
     */
    @PostMapping("/delete")
    public CommonResult<String> delete(@RequestParam Long id) {
        pmsProductService.delete(id);
        return CommonResult.success("删除商品成功");
    }

}
