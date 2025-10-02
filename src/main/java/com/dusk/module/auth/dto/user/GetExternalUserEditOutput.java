package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;

import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2023/1/11
 */
@Getter
@Setter
public class GetExternalUserEditOutput extends EntityDto {
    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("手机号")
    private String phoneNo;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("门禁卡号")
    private String accessCard;

    @ApiModelProperty("工号")
    private String workNumber;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("账号名")
    private String userName;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("所属厂站")
    private Long defaultStation;

    @ApiModelProperty("激活开始日期")
    private LocalDate activeStartDate;

    @ApiModelProperty("激活结束日期")
    private LocalDate activeEndDate;

    @ApiModelProperty("用户是否激活 未激活无法使用 常见用于使用手机号验证激活或者邮箱验证激活")
    private boolean active;

    @ApiModelProperty("下次登陆必须强制修改密码")
    private boolean shouldChangePasswordOnNextLogin;

    @ApiModelProperty("角色列表")
    private List<UserRoleDto> roles;

    @ApiModelProperty("组织单位")
    private List<UserOrgaDto> userOrgaDtoList;

    @ApiModelProperty("管理的组织")
    private List<UserOrgaDto> managerOrgDtos;
}
