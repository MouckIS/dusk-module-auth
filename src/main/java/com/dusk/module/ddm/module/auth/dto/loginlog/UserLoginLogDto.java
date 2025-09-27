package com.dusk.module.ddm.module.auth.dto.loginlog;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.jpa.querydsl.QBeanMapper;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.enums.LoginLogType;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Setter
@Getter
public class UserLoginLogDto extends EntityDto {
    @ApiModelProperty("用户id")
    @QBeanMapper(target = QUser.class, field = BaseEntity.Fields.id)
    private Long userId;
    @ApiModelProperty("用户名称")
    private String userName;
    @ApiModelProperty("登录日志类型")
    private LoginLogType logType;
    @ApiModelProperty("登录日志类型名称")
    private String logTypeName;
    @ApiModelProperty("操作时间")
    private LocalDateTime operationTime;
}
