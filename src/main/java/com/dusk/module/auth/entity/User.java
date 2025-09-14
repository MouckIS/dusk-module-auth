package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.common.module.auth.enums.UserStatus;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-04-28 9:57
 */
@Entity
@Table(name = "sys_user")
@Data
@FieldNameConstants
@NamedEntityGraph(name = "user.role", attributeNodes = {
        @NamedAttributeNode(value = "userRoles"),
})
@ToString(exclude = {"userRoles", "organizationUnit", "tenant"})
public class User extends FullAuditedEntity {
    private static final long serialVersionUID = -8696275845907521988L;
    /**
     * 手机号
     */
    private String phoneNo;

    /**
     * 手机号认证过
     */
    private boolean phoneConfirmed;
    /**
     * 邮箱地址
     */
    private String emailAddress;

    /**
     * 邮箱认证过
     */
    private boolean emailConfirmed;
    /**
     * 用户姓名
     */
    private String name;

    /**
     * 姓名拼英
     */
    private String surName;

    /**
     * 账号
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否是admin
     */
    private boolean admin;

    /**
     * 租户是否激活 未激活无法使用 常见用于使用手机号验证激活或者邮箱验证激活
     */
    private boolean active;

/*    @OneToMany(targetEntity = UserRole.class, mappedBy = UserRole.Fields.user)
    private List<UserRole> userRoles;*/
    /**
     * 关联角色
     */
    @ManyToMany
    @JoinTable(name = "sys_user_role", joinColumns = {@JoinColumn(name = "user_id")}, inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private List<Role> userRoles = new ArrayList<>();

    /**
     * 组织机构
     */
    @ManyToMany
    @JoinTable(name = "sys_org_user", joinColumns = {@JoinColumn(name = "user_id")}, inverseJoinColumns = {@JoinColumn(name = "org_id")})
    private List<OrganizationUnit> organizationUnit = new ArrayList<>();


    //。。。剩下的自己拓展
    /**
     * 访问失败次数
     */
    @Column(columnDefinition = "Integer default 0")
    private Integer accessFailedCount;
    /**
     * 账号锁定日期
     */
    private LocalDateTime lockoutEndDateUtc;

    /**
     * 头像id
     */
    private Long profilePictureId;

    /**
     * 默认厂站id
     */
    private Long defaultStation;

    /**
     * 下次登陆必须强制修改密码
     */
    @Column(columnDefinition = "boolean default false")
    private boolean shouldChangePasswordOnNextLogin;

    /**
     * 工号
     */
    private String workNumber;

    /**
     * 签字图片
     */
    private Long signaturePictureId;

    /**
     * 宿主字段，手动赋值的
     */
    @Transient
    private Tenant tenant;


    /**
     * 激活开始日期
     */
    private LocalDate activeStartDate;

    /**
     * 激活结束日期
     */
    private LocalDate activeEndDate;


    /***********以下是合并person表字段**************/
    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 门禁卡号
     */
    private String accessCard;

    /**
     * 类型
     */
    @Enumerated(EnumType.STRING)
    private EUnitType userType;

    /**
     * 岗位
     */
    private String job;

    /**
     * 入厂时间
     */
    private LocalDate enterDate;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

}
