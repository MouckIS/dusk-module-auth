package com.dusk.module.auth.dimension.controller;

import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dimension.dto.*;
import com.dusk.module.auth.dimension.service.IDimensionValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 维度值管理控制器
 *
 * @author dusk
 */
@RestController
@RequestMapping("/api/dimension-values")
@Tag(name = "维度值管理", description = "维度值的增删改查、校验等操作")
public class DimensionValueController extends CruxBaseController {

    @Autowired
    private IDimensionValueService dimensionValueService;

    @GetMapping("/dimension/{dimensionId}")
    @Operation(summary = "根据维度ID获取维度值列表")
    public List<DimensionValueDto> getByDimensionId(@PathVariable Long dimensionId) {
        return dimensionValueService.getByDimensionId(dimensionId);
    }

    @GetMapping("/dimension/code/{dimensionCode}")
    @Operation(summary = "根据维度编码获取维度值列表")
    public List<DimensionValueDto> getByDimensionCode(@PathVariable String dimensionCode) {
        return dimensionValueService.getByDimensionCode(dimensionCode);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取维度值")
    public DimensionValueDto getById(@PathVariable Long id) {
        return dimensionValueService.getById(id);
    }

    @PostMapping
    @Operation(summary = "创建维度值")
    public DimensionValueDto create(@Valid @RequestBody DimensionValueCreateDto dto) {
        return dimensionValueService.create(dto);
    }

    @PutMapping
    @Operation(summary = "更新维度值")
    public DimensionValueDto update(@Valid @RequestBody DimensionValueUpdateDto dto) {
        return dimensionValueService.update(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除维度值")
    public void delete(@PathVariable Long id) {
        dimensionValueService.deleteById(id);
    }

    @PostMapping("/validate")
    @Operation(summary = "校验维度值")
    public DimensionValueValidateResultDto validate(@Valid @RequestBody DimensionValueValidateDto dto) {
        return dimensionValueService.validate(dto);
    }

    @GetMapping("/accessible")
    @Operation(summary = "根据用户权限获取可访问的维度值列表")
    public List<DimensionValueDto> getAccessibleValues(
            @RequestParam String dimensionCode,
            @RequestParam Long userId) {
        return dimensionValueService.getAccessibleValues(dimensionCode, userId);
    }
}
