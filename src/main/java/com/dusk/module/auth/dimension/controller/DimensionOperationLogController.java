package com.dusk.module.auth.dimension.controller;

import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogDto;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogPagedInputDto;
import com.dusk.module.auth.dimension.service.IDimensionOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 维度操作日志控制器
 *
 * @author dusk
 */
@RestController
@RequestMapping("/api/dimension-operation-logs")
@Tag(name = "维度操作日志", description = "维度操作日志的查询，支持按时间范围、操作类型和用户过滤")
public class DimensionOperationLogController extends CruxBaseController {

    @Autowired
    private IDimensionOperationLogService operationLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询维度操作日志")
    public PagedResultDto<DimensionOperationLogDto> getPage(DimensionOperationLogPagedInputDto input) {
        return operationLogService.getPage(input);
    }
}
