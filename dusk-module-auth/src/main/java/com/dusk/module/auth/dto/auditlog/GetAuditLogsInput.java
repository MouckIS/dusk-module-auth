package com.dusk.module.auth.dto.auditlog;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-15 11:10
 */
@Getter
@Setter
public class GetAuditLogsInput extends PagedAndSortedInputDto {
    @Schema(description = "开始时间")
    public LocalDateTime startDate;
    @Schema(description = "结束时间")
    public LocalDateTime endDate;
    @Schema(description = "用户名")
    public String userName;
    @Schema(description = "服务")
    public String serviceName;
    @Schema(description = "操作")
    public String methodName;
    @Schema(description = "浏览器")
    public String browserInfo;
    @Schema(description = "错误状态")
    public Boolean hasException;
    @Schema(description = "最小持续时间")
    public Integer minExecutionDuration;
    @Schema(description = "最大持续时间")
    public Integer maxExecutionDuration;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {//默认按执行时间倒序排序
            return Sort.by(Sort.Direction.DESC, AuditLog.Fields.executionTime);
        }

        String sortingStr = sorting;
        if (AuditLogListDto.Fields.userId.equals(sorting)) {
            sortingStr = AuditLog.Fields.createUser + "." + BaseEntity.Fields.id;
        } else if (AuditLogListDto.Fields.userName.equals(sorting)) {
            sortingStr = AuditLog.Fields.createUser + "." + User.Fields.userName;
        }
        return Sort.by(sortingDirection, sortingStr);
    }
}
