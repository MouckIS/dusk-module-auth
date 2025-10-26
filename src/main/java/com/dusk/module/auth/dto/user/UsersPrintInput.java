package com.dusk.module.auth.dto.user;

import com.dusk.module.auth.enums.UserPrintType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *
 * @author caiwenjun
 * @date 2024/1/5 11:07
 */
@Getter
@Setter
@Schema(description = "用户信息打印实体类")
public class UsersPrintInput {
    @Schema(description = "人员id")
    public List<Long> userIds;

    @Enumerated(EnumType.STRING)
    @Schema(description = "人员打印类型")
    public UserPrintType printType = UserPrintType.INNER;

    @Schema(description = "项目名称")
    public String projectName;

    @Schema(description = "工种")
    public String workType;

}
