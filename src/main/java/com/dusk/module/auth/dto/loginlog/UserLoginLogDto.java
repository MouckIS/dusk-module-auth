package com.dusk.module.auth.dto.loginlog;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.jpa.querydsl.QBeanMapper;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.enums.LoginLogType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Setter
@Getter
public class UserLoginLogDto extends EntityDto {
    @Schema(description = "用户id")
    @QBeanMapper(target = QUser.class, field = BaseEntity.Fields.id)
    private Long userId;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "登录日志类型")
    private LoginLogType logType;
    @Schema(description = "登录日志类型名称")
    private String logTypeName;
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}
