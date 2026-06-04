package com.dusk.module.auth.dimension.service;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogDto;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogPagedInputDto;
import com.dusk.module.auth.dimension.entity.DimensionOperationLog;
import com.dusk.module.auth.dimension.enums.DimensionOperationType;
import com.dusk.module.auth.dimension.repository.IDimensionOperationLogRepository;

/**
 * 维度操作日志服务接口
 *
 * @author dusk
 */
public interface IDimensionOperationLogService extends IBaseService<DimensionOperationLog, IDimensionOperationLogRepository> {

    /**
     * 分页查询操作日志
     *
     * @param input 查询参数
     * @return 分页结果
     */
    PagedResultDto<DimensionOperationLogDto> getPage(DimensionOperationLogPagedInputDto input);

    /**
     * 记录操作日志
     *
     * @param operationType 操作类型
     * @param targetType    操作目标类型
     * @param targetId      操作目标ID
     * @param targetName    操作目标名称
     * @param detail        操作详情
     */
    void log(DimensionOperationType operationType, String targetType, Long targetId, String targetName, String detail);
}
