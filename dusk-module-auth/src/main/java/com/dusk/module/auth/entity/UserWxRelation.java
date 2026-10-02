package com.dusk.module.auth.entity;

import com.dusk.common.core.annotation.LogicDelete;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * 用户微信关系表
 *
 * @author kefuming
 * @date 2021-07-23 15:13
 */
@Getter
@Setter
@Entity
@Table(name = "user_wx_relation")
@FieldNameConstants
public class UserWxRelation extends CreationEntity {
    private Long userId;
    private String appId;
    private String openId;

    @Column(name = EntityConstant.LAST_MODIFY_ID)
    private Long lastModifyId;

    @Column(name = EntityConstant.LAST_MODIFY_TIME)
    private LocalDateTime lastModifyTime;

    @Version
    @Column(name = EntityConstant.VERSION)
    private int version;

    @LogicDelete
    @Column(name = EntityConstant.DR)
    private int dr;
}
