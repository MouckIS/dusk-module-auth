package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.module.auth.enums.UserPrintType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.List;

/**
 *
 * @author caiwenjun
 * @date 2024/1/5 11:07
 */
@Data
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
