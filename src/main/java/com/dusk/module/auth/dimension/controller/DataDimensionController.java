package com.dusk.module.auth.dimension.controller;

import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dimension.dto.DataDimensionCreateDto;
import com.dusk.module.auth.dimension.dto.DataDimensionDto;
import com.dusk.module.auth.dimension.dto.DataDimensionPagedInputDto;
import com.dusk.module.auth.dimension.dto.DataDimensionUpdateDto;
import com.dusk.module.auth.dimension.service.IDataDimensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 数据维度管理控制器
 *
 * @author dusk
 */
@RestController
@RequestMapping("/api/data-dimensions")
@Tag(name = "数据维度管理", description = "数据维度的增删改查、导入导出等操作")
public class DataDimensionController extends CruxBaseController {

    @Autowired
    private IDataDimensionService dataDimensionService;

    @GetMapping("/page")
    @Operation(summary = "分页查询数据维度")
    public PagedResultDto<DataDimensionDto> getPage(DataDimensionPagedInputDto input) {
        return dataDimensionService.getPage(input);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询数据维度")
    public DataDimensionDto getById(@PathVariable Long id) {
        return dataDimensionService.getById(id);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "根据编码查询数据维度")
    public DataDimensionDto getByCode(@PathVariable String code) {
        return dataDimensionService.getByCode(code);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的数据维度")
    public List<DataDimensionDto> getAllEnabled() {
        return dataDimensionService.getAllEnabled();
    }

    @PostMapping
    @Operation(summary = "创建数据维度")
    public DataDimensionDto create(@Valid @RequestBody DataDimensionCreateDto dto) {
        return dataDimensionService.create(dto);
    }

    @PutMapping
    @Operation(summary = "更新数据维度")
    public DataDimensionDto update(@Valid @RequestBody DataDimensionUpdateDto dto) {
        return dataDimensionService.update(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据维度")
    public void delete(@PathVariable Long id) {
        dataDimensionService.deleteById(id);
    }

    @GetMapping("/export")
    @Operation(summary = "导出数据维度CSV")
    @IgnoreResponseAdvice
    public void exportCsv(HttpServletResponse response) {
        dataDimensionService.exportCsv(response);
    }

    @PostMapping("/import")
    @Operation(summary = "导入数据维度CSV")
    public String importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return dataDimensionService.importCsv(file.getInputStream());
    }
}
