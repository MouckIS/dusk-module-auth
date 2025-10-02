package com.dusk.module.auth.dto.user;

import com.dusk.common.rpc.auth.dto.UserEditDto;
import lombok.Data;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.enums.ELevel;

import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/15 17:22
 */
@Data
public class GetUserForEditOutput {

    private UserEditDto user;

    private List<UserRoleDto> roles;

    private List<Long> memberedOrganizationUnits;

    private List<UserOrgaDto> userOrgaDtoList;

    /**
     * 员工级别
     */
    private ELevel level;

    private List<StationsOfLoginUserDto> stations;

    private String job;

    private LocalDate enterDate;
    /**
     * 管理的组织
     */
    private List<UserOrgaDto> managerOrgDtos;
}
