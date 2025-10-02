package com.dusk.module.auth.entity;

import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.enums.OrgLabel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-13 11:55
 */
@Getter
@Setter
@Entity
@Table(name = "sys_organization")
@FieldNameConstants
@ToString(exclude = "users")
public class OrganizationUnit extends TreeEntity {
    /**
     * 编码
     */
    private String code;

    /**
     * 是否为厂站
     */
    private Boolean station = false;

    /**
     *  厂站是否可用，仅作用厂站字段
     */
    private Boolean stationEnabled = true;

    /**
     * 组织机构管理的用户
     */
    @ManyToMany
    @JoinTable(name = "sys_org_user", joinColumns = { @JoinColumn(name = "org_id") }, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private List<User> users = new ArrayList<>();

    /**
     * 类型
     */
    @Enumerated(EnumType.STRING)
    private EUnitType type;

    /**
     * 标签
     */
    @Enumerated(EnumType.STRING)
    private OrgLabel label;

    /**
     * 组织机构的描述
     */
    private String description;
}
