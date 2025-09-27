package com.dusk.module.ddm.module.auth.dto.auditlog;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.User;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-15 11:10
 */
@Data
public class GetAuditLogsInput extends PagedAndSortedInputDto {
    @ApiModelProperty("开始时间")
    public LocalDateTime startDate;
    @ApiModelProperty("结束时间")
    public LocalDateTime endDate;
    @ApiModelProperty("用户名")
    public String userName;
    @ApiModelProperty("服务")
    public String serviceName;
    @ApiModelProperty("操作")
    public String methodName;
    @ApiModelProperty("浏览器")
    public String browserInfo;
    @ApiModelProperty("错误状态")
    public Boolean hasException;
    @ApiModelProperty(value = "最小持续时间")
    public Integer minExecutionDuration;
    @ApiModelProperty(value = "最大持续时间")
    public Integer maxExecutionDuration;

    @Override
    protected Sort getSort() {
        if(StringUtils.isBlank(sorting)){//默认按执行时间倒序排序
            return Sort.by(Sort.Direction.DESC, AuditLog.Fields.executionTime);
        }

        String sortingStr = sorting;
        if(AuditLogListDto.Fields.userId.equals(sorting)){
            sortingStr = AuditLog.Fields.createUser + "." + BaseEntity.Fields.id;
        }else if(AuditLogListDto.Fields.userName.equals(sorting)){
            sortingStr =AuditLog.Fields.createUser + "." + User.Fields.userName;
        }
        return Sort.by(sortingDirection, sortingStr);
    }
}
