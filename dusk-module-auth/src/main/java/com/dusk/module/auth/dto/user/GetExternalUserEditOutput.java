package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2023/1/11
 */
@Getter
@Setter
public class GetExternalUserEditOutput extends EntityDto {
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "门禁卡号")
    private String accessCard;

    @Schema(description = "工号")
    private String workNumber;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "账号名")
    private String userName;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "所属厂站")
    private Long defaultStation;

    @Schema(description = "激活开始日期")
    private LocalDate activeStartDate;

    @Schema(description = "激活结束日期")
    private LocalDate activeEndDate;

    @Schema(description = "用户是否激活 未激活无法使用 常见用于使用手机号验证激活或者邮箱验证激活")
    private boolean active;

    @Schema(description = "下次登陆必须强制修改密码")
    private boolean shouldChangePasswordOnNextLogin;

    @Schema(description = "角色列表")
    private List<UserRoleDto> roles;

    @Schema(description = "组织单位")
    private List<UserOrgaDto> userOrgaDtoList;

    @Schema(description = "管理的组织")
    private List<UserOrgaDto> managerOrgDtos;
}
