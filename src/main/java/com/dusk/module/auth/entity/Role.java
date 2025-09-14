package com.dusk.module.auth.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.entity.CreationEntity;
import com.dusk.common.framework.entity.FullAuditedEntity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author kefuming
 * @date 2020-04-28 10:24
 */
@Entity
@Table(name = "sys_role")
@Getter
@Setter
@FieldNameConstants
@NamedEntityGraph(name = "role.all", attributeNodes = {
        @NamedAttributeNode("createUser"),
        @NamedAttributeNode("permissions"),
        @NamedAttributeNode("userRoles")
})
@NamedEntityGraph(name = "role.permission", attributeNodes = {
        @NamedAttributeNode("createUser"),
        @NamedAttributeNode("permissions")
})
@NamedEntityGraph(name = "role.creater", attributeNodes = {
        @NamedAttributeNode("createUser"),
        @NamedAttributeNode("permissions")
})
@NamedEntityGraph(name = "role.all.IncludeUserRole",
        attributeNodes = {
                @NamedAttributeNode(value = "createUser", subgraph = "createUser.userRole")
        },
        subgraphs = {
                @NamedSubgraph(name = "createUser.userRole",
                        attributeNodes = {
                                @NamedAttributeNode("userRoles"),
                        })
        })
public class Role extends FullAuditedEntity {
    private static final long serialVersionUID = -1718814251722841234L;

    public Role() {
        permissions = new ArrayList<>();
    }

    //code
    private String roleCode;
    //name
    private String roleName;

    private boolean isDefault;

    @OneToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = CreationEntity.Fields.createId, referencedColumnName = BaseEntity.Fields.id, insertable = false, updatable = false)
    private User createUser;

    @OneToMany(targetEntity = GrantPermission.class, mappedBy = GrantPermission.Fields.role, cascade = CascadeType.ALL)
    private List<GrantPermission> permissions;

    @ManyToMany
    @JoinTable(name = "sys_user_role", joinColumns = {@JoinColumn(name = "role_id")}, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private List<User> userRoles;

    public void addPermission(GrantPermission p) {
        permissions.add(p);
    }

    public void addPermissions(List<GrantPermission> p) {
        permissions.addAll(p);
    }

    public void clearPermission() {
        //modify by wangji 仅仅移除businessKey为空部分得
        List<GrantPermission> collect = permissions.stream().filter(p -> StrUtil.isEmpty(p.getBusinessKey())).collect(Collectors.toList());
        permissions.removeAll(collect);
    }

    public void setPermissionRole() {
        permissions.forEach(s -> s.setRole(this));
    }
}
