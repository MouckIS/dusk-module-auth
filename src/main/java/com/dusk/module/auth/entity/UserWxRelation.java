package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.annotation.LogicDelete;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.CreationEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
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
