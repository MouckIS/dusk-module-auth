package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * @author pengjian
 * @date 2020-05-13 14:31
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
public class OrganizationUnitUserListDto implements Serializable {
    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "所属组织机构id")
    private Long organizationUnitId;

    @Schema(description = "所属组织机构名称")
    private String organizationUnitName;

    public OrganizationUnitUserListDto(Long id, String name, String userName, String emailAddress, Long organizationUnitId, String organizationUnitName){
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.emailAddress = emailAddress;
        this.organizationUnitId = organizationUnitId;
        this.organizationUnitName = organizationUnitName;
    }

}
