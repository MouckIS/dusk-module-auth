package com.dusk.module.auth.dto.station;

import com.dusk.common.core.enums.EUnitType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ListStationUsersInput implements Serializable {
    /**
     * 厂站id
     */
    private List<Long> stationIds = new ArrayList<>();
    /**
     * 搜索关键字(姓名/账号)
     */
    private String filter;
    /**
     * 深度查询(即包括子节点的人员, 默认true)
     */
    private boolean deepQuery = true;
    /**
     * 用户类型
     */
    private EUnitType userType;
}
