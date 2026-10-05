package com.dusk.module.auth.dto.orga;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Author: pengmengjiang
 * @Date: 2021/2/23 16:31
 */

@Getter
@Setter
public class OrganizationUnitUserDto implements Serializable {
    private Long orgId;
    private Long userId;

    public OrganizationUnitUserDto(){

    }

    public OrganizationUnitUserDto(Long orgId,Long userId){
        this.orgId = orgId;
        this.userId = userId;
    }
}
