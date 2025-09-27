package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.common.module.auth.enums.NotificationType;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 消息
 *
 * @author kefuming
 * @date 2020/12/24 15:02
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "sys_notification")
public class Notification extends FullAuditedEntity {

    /**
     * 标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息提醒类型
     */
    private NotificationType type;

    /**
     * 消息附带的页面跳转参数
     */
    private String pageNavigation;
}
