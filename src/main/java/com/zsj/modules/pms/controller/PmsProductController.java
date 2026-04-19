package com.zsj.modules.pms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.pms.dto.*;
import com.zsj.modules.pms.model.PmsProduct;
import com.zsj.modules.pms.service.FileUploadService;
import com.zsj.modules.pms.service.PmsProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品管理接口
 */
@RequiredArgsConstructor
@RestController
//@RequestMapping("/demo/product")
public class PmsProductController {

    private final PmsProductService pmsProductService;
    private final FileUploadService fileUploadService;

    /**
     * 新增商品
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping("/demo/product/create")
    public CommonResult<Long> create(@Valid @RequestBody PmsProductCreateDTO dto) {
        Long id = pmsProductService.create(dto);
        return CommonResult.success(id, "新增商品成功");
    }


    /**
     * 商品分页查询
     */
    @PreAuthorize("hasAuthority('pms:product:read')")
    @GetMapping("/demo/product/list")
    public CommonResult<IPage<PmsProduct>> list(@ModelAttribute PmsProductQueryDTO queryDTO) {
        IPage<PmsProduct> page = pmsProductService.listPage(queryDTO);
        return CommonResult.success(page, "查询商品列表成功");
    }


    /**
     * 修改商品上架状态（0下架，1上架）
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping("/demo/product/publish/status")
    public CommonResult<String> updatePublishStatus(@RequestParam Long id, @RequestParam Integer publishStatus) {
        pmsProductService.updatePublishStatus(id, publishStatus);
        return CommonResult.success("修改商品上架状态成功");
    }


    /**
     * 逻辑删除商品
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping("/demo/product/delete")
    public CommonResult<String> delete(@RequestParam Long id) {
        pmsProductService.delete(id);
        return CommonResult.success("删除商品成功");
    }


    /**
     * 修改商品基础信息
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PutMapping("/demo/product/update")
    public CommonResult<String> update(@Valid @RequestBody PmsProductUpdateDTO dto) {
        pmsProductService.update(dto);
        return CommonResult.success("修改商品成功");
    }


    /**
     * 查询商品详情
     */
    @PreAuthorize("hasAuthority('pms:product:read')")
    @GetMapping("/demo/product/detail")
    public CommonResult<PmsProduct> detail(@RequestParam Long id) {
        return CommonResult.success(pmsProductService.getById(id), "查询商品详情成功");
    }


    /**
     * 批量修改上架状态
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping("/demo/product/publish/status/batch")
    public CommonResult<Integer> updatePublishStatusBatch(@Valid @RequestBody PmsProductBatchPublishDTO dto) {
        int rows = pmsProductService.updatePublishStatusBatch(dto.getIds(), dto.getPublishStatus());
        return CommonResult.success(rows, "批量修改商品上架状态成功");
    }


    /**
     * 批量逻辑删除商品
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping("/demo/product/delete/batch")
    public CommonResult<Integer> deleteBatch(@Valid @RequestBody PmsProductBatchDeleteDTO dto) {
        int rows = pmsProductService.deleteBatch(dto.getIds());
        return CommonResult.success(rows, "批量删除商品成功");
    }


    /**
     * 图片上传到 OSS
     */
    @PreAuthorize("hasAuthority('pms:product:write')")
    @PostMapping(value = "/demo/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<UploadResultDTO> upload(@RequestPart("file") MultipartFile file) {
        UploadResultDTO result = fileUploadService.uploadImage(file);
        return CommonResult.success(result, "上传成功");
    }
}
