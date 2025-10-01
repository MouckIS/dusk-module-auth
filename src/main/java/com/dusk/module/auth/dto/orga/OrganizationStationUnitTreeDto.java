package com.dusk.module.auth.dto.orga;

import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.entity.User;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2023/11/03
 */
@Getter
@Setter
public class OrganizationStationUnitTreeDto extends OrganizationStationUnitDto{
    private List<OrganizationStationUnitTreeDto> children;

    private List<User> userList;
}
