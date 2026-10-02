package com.dusk.module.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pengjian
 * @date 2021-12-28 16:36
 */
@Getter
@Setter
public class GetUsersByOrgInput implements Serializable {
    private List<Long> orgIds = new ArrayList<>();
    /**
     * 深度查询(即包括子节点的人员, 默认true)
     */
    private boolean deepQuery = true;
}
