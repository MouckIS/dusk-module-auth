package com.dusk.module.auth.dto.loginlog;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.module.auth.entity.UserLoginLog;
import com.dusk.module.auth.enums.LoginLogType;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Getter
@Setter
public class ListUserLoginLogInput extends PagedAndSortedInputDto {
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("日志类型")
    private LoginLogType logType;
    @ApiModelProperty("开始时间")
    private LocalDateTime beginTime;
    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    public LocalDateTime getBeginTime() {
        return beginTime == null ? null : LocalDateTime.of(beginTime.toLocalDate(), LocalTime.MIN);
    }

    public LocalDateTime getEndTime() {
        return endTime == null ? null : LocalDateTime.of(endTime.toLocalDate(), LocalTime.MAX);
    }

    @Override
    public Sort getSort() {
        if (StrUtil.isBlank(this.sorting)) {
            Sort.Direction direction = this.getSortingDirection() != null ? this.getSortingDirection() : Sort.Direction.DESC;
            return Sort.by(direction, UserLoginLog.Fields.operationTime);
        } else {
            return super.getSort();
        }
    }
}
