package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户消息
 *
 * @author kefuming
 * @date 2020/12/24 15:12
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "sys_user_notification")
public class UserNotification extends FullAuditedEntity {

    /**
     * 用户的Id
     */
    private Long userId;

    /**
     * 消息是否已读
     */
    private Boolean read;

    /**
     * 消息的Id
     */
    private Long notificationId;
}
