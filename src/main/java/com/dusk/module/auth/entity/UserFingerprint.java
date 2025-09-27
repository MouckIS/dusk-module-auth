package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.common.module.auth.enums.FingerprintFromEnum;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * @author kefuming
 * @date 2020-12-15 8:43
 */
@Getter
@Setter
@Entity
@Table(name = "user_fingerprint")
@FieldNameConstants
public class UserFingerprint extends FullAuditedEntity {
    //用户id
    private Long userId;
    //用户序列
    private Integer userSeq;
    //指纹名称n
    private String name;
    //指纹数据
    private String data;
    //指纹数据大小
    private Integer size;
    //指纹来源
    @Enumerated(EnumType.STRING)
    private FingerprintFromEnum fromEnum;
}
