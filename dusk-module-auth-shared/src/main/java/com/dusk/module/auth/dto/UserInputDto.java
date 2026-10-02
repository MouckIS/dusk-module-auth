package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author duanxiaokang
 * @date 2020/9/22 14:26
 */
@Getter
@Setter
@Schema(description = "查询用户列表的实体类")
public class UserInputDto extends PagedAndSortedInputDto {
    @Schema(description = "模糊查找[姓名、账号、电子邮箱、手机号]")
    public String filter;
    @Schema(description = "用户权限[name]")
    private String permission;
    @Schema(description = "用户角色[id]")
    private Long roleId;
    @Schema(description = "只显示锁定用户")
    private boolean onlyLockedUsers;
    private LocalDateTime localDateTime;
    @Schema(description = "账号类型")
    private EUnitType userType = EUnitType.Inner;
    @Schema(description = "用户名列表")
    private List<String> userNameList;
}
