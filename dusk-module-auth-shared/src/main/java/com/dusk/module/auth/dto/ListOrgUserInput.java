package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : kefuming
 * @date : 2024/7/30 下午3:13
 */
@Getter
@Setter
public class ListOrgUserInput extends PagedAndSortedInputDto {
    /**
     * 模糊查找[姓名、账号、电子邮箱、手机号、工号]
     */
    private String filter;

    /**
     * 查询用户姓名，filter字段为空才生效
     */
    private String name;

    /**
     * 组织id
     */
    private Long orgId;

    /**
     * 深度查询(即包括子节点的人员, 默认true)
     */
    private boolean deepQuery = true;

    /**
     * 是否过滤离职人员
     */
    private boolean filterDimissionUsers = true;
}
