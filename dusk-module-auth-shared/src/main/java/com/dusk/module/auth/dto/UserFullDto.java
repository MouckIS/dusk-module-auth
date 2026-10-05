package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.module.auth.dto.orga.OrganizationUnitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pengjian
 * @date 2024/6/6 10:32
 */
@Getter
@Setter
public class UserFullDto extends EntityDto {
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "姓名拼音")
    private String surName;
    @Schema(description = "账号")
    private String userName;
    @Schema(description = "电子邮箱地址")
    private String emailAddress;
    @Schema(description = "电话号码")
    private String phoneNo;
    @Schema(description = "是否管理员")
    private boolean admin;
    @Schema(description = "头像图片")
    private String profilePictureId;
    @Schema(description = "工号")
    public String workNumber;
    @Schema(description = "签字图片")
    public Long signaturePictureId;
    @Schema(description = "岗位")
    private String job;
    @Schema(description = "门禁卡号")
    private String accessCard;
    @Schema(description = "类型")
    private EUnitType userType;
    @Schema(description = "状态")
    private UserStatus userStatus;
    @Schema(description = "第三方Id")
    private String foreignId;
    @Schema(description = "角色列表")
    private List<UserRoleDto> userRoles;
    @Schema(description = "专业列表")
    private List<String> specialityList = new ArrayList<>();
    @Schema(description = "用户所在的组织机构及父级组织机构")
    private List<OrganizationUnitDto> orgWithParentsList = new ArrayList<>();

}
