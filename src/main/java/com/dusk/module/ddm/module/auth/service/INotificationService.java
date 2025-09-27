package com.dusk.module.ddm.module.auth.service;

import com.dusk.module.auth.dto.notification.*;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dto.notification.*;
import com.dusk.module.auth.entity.UserNotification;
import com.dusk.module.auth.repository.IUserNotificationRepository;


/**
 * @author yuliyang
 * @date 2020-12-24 15:17:08
 */
public interface INotificationService extends IBaseService<UserNotification, IUserNotificationRepository> {

    /**
     * 获取用户消息列表
     *
     * @param input
     * @return
     */
    PagedResultDto<NotificationListOutput> getNotificationList(GetNotificationListInput input);

    /**
     * 获取消息内容
     *
     * @param input
     * @return
     */
    NotificationOutput getNotification(EntityDto input);

    /**
     * 获取用户的消息数量
     *
     * @param input
     * @return
     */
    long getCount(GetNotificationListCountInput input);

    /**
     * 将消息的状态标记为已读
     *
     * @param input
     * @return
     */
    void setNotificationAsRead(SetNotificationAsReadInput input);

    /**
     * 删除消息
     *
     * @param id
     * @return
     */
    void deleteNotification(Long id);

    /**
     * 批量删除用户的消息
     *
     * @param input
     */
    void batchDeleteNotification(BatchDeleteNotificationInput input);

}
