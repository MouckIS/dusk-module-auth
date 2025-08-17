package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dusk.module.auth.dto.notification.*;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.framework.utils.MqttUtils;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.common.module.auth.dto.notification.CreateNotificationInput;
import com.dusk.common.module.auth.service.INotificationRpcService;
import com.dusk.module.auth.dto.notification.*;
import com.dusk.module.auth.entity.Notification;
import com.dusk.module.auth.entity.QNotification;
import com.dusk.module.auth.entity.QUserNotification;
import com.dusk.module.auth.entity.UserNotification;
import com.dusk.module.auth.repository.INotificationRepository;
import com.dusk.module.auth.repository.IUserNotificationRepository;
import com.dusk.module.auth.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


/**
 * 用户消息的服务
 *
 * @author yuliyang
 * @date 2020-12-24 15:17:08
 */
@Slf4j
@Service
@Transactional
public class NotificationServiceImpl
        extends BaseService<UserNotification, IUserNotificationRepository>
        implements INotificationRpcService, INotificationService {


    private final Mapper mapper;
    private final MqttUtils mqttUtils;
    private final SecurityUtils securityUtils;
    private final JPAQueryFactory queryFactory;
    private final INotificationRepository notificationRepository;

    private final QNotification qNotification = QNotification.notification;
    private final QUserNotification qUserNotification = QUserNotification.userNotification;

    private final static String MQTT_TOPIC_TEMPLATE = "T/{}/crux/{}/notification";

    @Autowired
    public NotificationServiceImpl(
            Mapper mapper,
            MqttUtils mqttUtils,
            SecurityUtils securityUtils,
            JPAQueryFactory queryFactory,
            INotificationRepository notificationRepository
    ) {

        this.mapper = mapper;
        this.mqttUtils = mqttUtils;
        this.securityUtils = securityUtils;
        this.queryFactory = queryFactory;
        this.notificationRepository = notificationRepository;
    }

    /**
     * 获取用户消息列表
     *
     * @param input
     * @return
     */
    @Override
    public PagedResultDto<NotificationListOutput> getNotificationList(GetNotificationListInput input) {

        UserContext userContext = securityUtils.getCurrentUser();

        QBeanBuilder<NotificationListOutput> builder = QBeanBuilder
                .create(NotificationListOutput.class)
                .appendQEntity(qUserNotification, qNotification);

        JPAQuery<NotificationListOutput> query = queryFactory
                .select(builder.build())
                .from(qUserNotification)
                .where(qUserNotification.userId.eq(userContext.getId()))
                .leftJoin(qNotification)
                .on(qUserNotification.notificationId.eq(qNotification.id))
                .orderBy(qUserNotification.createTime.desc());

        if (input.getRead() != null) {
            query = query.where(qUserNotification.read.eq(input.getRead()));
        }
        if (input.getType() != null) {
            query = query.where(qNotification.type.eq(input.getType()));
        }
        Page<NotificationListOutput> data = (Page<NotificationListOutput>) page(query, input.getPageable());
        return DozerUtils.mapToPagedResultDto(mapper, data, NotificationListOutput.class, (s, t) -> {
            if (s.getType() != null) {
                t.setTypeValue(s.getType().getValue());
                t.setTypeName(s.getType().getDisplayName());
            }
        });
    }

    /**
     * 获取消息内容
     *
     * @param input
     * @return
     */
    @Override
    public NotificationOutput getNotification(EntityDto input) {

        NotificationOutput output = new NotificationOutput();
        QBeanBuilder<NotificationOutput> builder = QBeanBuilder
                .create(NotificationOutput.class)
                .appendQEntity(qUserNotification, qNotification);

        QueryResults<NotificationOutput> queryResults = queryFactory
                .select(builder.build())
                .from(qUserNotification)
                .where(qUserNotification.id.eq(input.getId()))
                .leftJoin(qNotification)
                .on(qUserNotification.notificationId.eq(qNotification.id))
                .fetchResults();

        if (queryResults.isEmpty()) {
            log.warn("ID为" + input.getId() + "的用户消息不存在");
        } else {
            output = queryResults.getResults().get(0);
            //将消息自动置为已读
            UserNotification userNotification = repository.getOne(output.getId());
            userNotification.setRead(true);
            repository.save(userNotification);
            if (output.getType() != null) {
                output.setTypeValue(output.getType().getValue());
                output.setTypeName(output.getType().getDisplayName());
            }
        }
        return output;
    }

    /**
     * 获取用户的消息数量
     *
     * @param input
     * @return
     */
    @Override
    public long getCount(GetNotificationListCountInput input) {

        UserContext userContext = securityUtils.getCurrentUser();

        QBeanBuilder<NotificationListOutput> builder = QBeanBuilder
                .create(NotificationListOutput.class)
                .appendQEntity(qUserNotification, qNotification);

        JPAQuery<NotificationListOutput> query = queryFactory
                .select(builder.build())
                .from(qUserNotification)
                .where(qUserNotification.userId.eq(userContext.getId()))
                .leftJoin(qNotification)
                .on(qUserNotification.notificationId.eq(qNotification.id));

        if (input.getRead() != null) {
            query = query.where(qUserNotification.read.eq(input.getRead()));
        }
        if (input.getType() != null) {
            query = query.where(qNotification.type.eq(input.getType()));
        }
        return query.fetchCount();
    }

    /**
     * 生成用户消息
     *
     * @param input
     */
    @Override
    public void createNotification(CreateNotificationInput input) {
        Notification notification = mapper.map(input, Notification.class);
        notification.setPageNavigation(JSONUtil.toJsonStr(input.getPageNavigation()));
        notificationRepository.save(notification);
        Long notificationId = notification.getId();
        NotificationOutput payload = mapper.map(notification, NotificationOutput.class);
        payload.setId(notificationId);
        if (payload.getType() != null) {
            payload.setTypeValue(notification.getType().getValue());
            payload.setTypeName(notification.getType().getDisplayName());
        }
        List<UserNotification> userNotifications = new ArrayList<>();
        input.getUserIds().forEach(userId -> {
            UserNotification userNotification = new UserNotification();
            userNotification.setNotificationId(notificationId);
            userNotification.setRead(false);
            userNotification.setUserId(userId);
            userNotifications.add(userNotification);

            //Mqtt消息推送
            String topic = StrUtil.format(MQTT_TOPIC_TEMPLATE, notification.getTenantId(), userId);
            mqttUtils.publishMsgAsync(topic, payload);
            log.info("推送主题{}，消息内容：{}", topic, JSONUtil.toJsonStr(payload));
        });
        if (!userNotifications.isEmpty()) {
            repository.saveAll(userNotifications);
        }
    }

    /**
     * 将消息的状态标记为已读
     *
     * @param input
     * @return
     */
    public void setNotificationAsRead(SetNotificationAsReadInput input) {
        final Specification<UserNotification> query =
                Specifications.where(e -> {
                    e.in(BaseEntity.Fields.id, input.getIds());
                });

        List<UserNotification> notifications = repository.findAll(query);
        if (ArrayUtil.isNotEmpty(notifications)) {
            notifications.forEach(n -> {
                n.setRead(true);
            });
            repository.saveAll(notifications);
        }
    }

    /**
     * 删除用户消息
     *
     * @param id
     * @return
     */
    @Override
    public void deleteNotification(Long id) {
        UserNotification notification = repository
                .findById(id)
                .orElseThrow(() -> new BusinessException("ID为" + id + "的用户消息不存在"));
        repository.delete(notification);
    }

    /**
     * 批量删除用户的消息
     *
     * @param input
     */
    @Override
    public void batchDeleteNotification(BatchDeleteNotificationInput input) {
        final Specification<UserNotification> query =
                Specifications.where(e -> {
                    e.in(BaseEntity.Fields.id, input.getIds());
                });
        List<UserNotification> notifications = repository.findAll(query);
        if (ArrayUtil.isNotEmpty(notifications)) {
            deleteInBatch(notifications);
        }
    }

}
