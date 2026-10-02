package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.mqs.utils.MqttUtils;
import com.dusk.module.auth.dto.notification.CreateNotificationInput;
import com.dusk.module.auth.enums.NotificationType;
import com.dusk.module.auth.dto.notification.BatchDeleteNotificationInput;
import com.dusk.module.auth.dto.notification.GetNotificationListCountInput;
import com.dusk.module.auth.dto.notification.GetNotificationListInput;
import com.dusk.module.auth.dto.notification.NotificationListOutput;
import com.dusk.module.auth.dto.notification.NotificationOutput;
import com.dusk.module.auth.dto.notification.SetNotificationAsReadInput;
import com.dusk.module.auth.entity.UserNotification;
import com.dusk.module.auth.repository.INotificationRepository;
import com.dusk.module.auth.repository.IUserNotificationRepository;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    private static final long CURRENT_USER_ID = 99L;

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private IUserNotificationRepository repository;
    @Mock
    private INotificationRepository notificationRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MqttUtils mqttUtils;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new NotificationServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "notificationRepository", notificationRepository);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "mqttUtils", mqttUtils);

        UserContext userContext = new UserContext();
        userContext.setId(CURRENT_USER_ID);
        when(securityUtils.getCurrentUser()).thenReturn(userContext);
    }

    /**
     * 用 RETURNS_SELF 构造可链式调用的 JPAQuery mock：from/where/leftJoin/on/orderBy
     * 都会返回同一个 mock，便于对多分支拼接过程做黑盒桩化。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery<?> stubQuery() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        return query;
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<NotificationListOutput> content, long total) {
        // 注意：getNotificationList 返回的是 data.getTotalPages() 而非 totalElements，
        // 因此这里必须使用「真正分页」的 Pageable，unpaged 会让 totalPages 恒为 1。
        Page<NotificationListOutput> page =
                new PageImpl<>(content, PageRequest.of(0, 10), total);
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    private static NotificationListOutput output(NotificationType type) {
        NotificationListOutput dto = new NotificationListOutput();
        dto.setId(1L);
        dto.setType(type);
        return dto;
    }

    // ---------------- getNotificationList ----------------

    @Test
    @DisplayName("getNotificationList：无过滤条件时返回全部消息并回填类型名称")
    void getNotificationListWithoutFilters() {
        stubQuery();
        stubPage(List.of(output(NotificationType.ALERT)), 1L);

        PagedResultDto<NotificationListOutput> result =
                service.getNotificationList(new GetNotificationListInput());

        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getTypeName())
                .isEqualTo(NotificationType.ALERT.getDisplayName());
    }

    @Test
    @DisplayName("getNotificationList：type 为 null 时跳过类型回填")
    void getNotificationListSkipsTypeBackfillWhenTypeNull() {
        stubQuery();
        stubPage(List.of(output(null)), 1L);

        PagedResultDto<NotificationListOutput> result =
                service.getNotificationList(new GetNotificationListInput());

        assertThat(result.getItems().getFirst().getTypeName()).isNull();
    }

    @Test
    @DisplayName("getNotificationList：read 与 type 都非空时追加两个过滤条件")
    void getNotificationListWithReadAndTypeFilters() {
        stubQuery();
        stubPage(List.of(output(NotificationType.NOTIFICATION)), 1L);

        GetNotificationListInput input = new GetNotificationListInput();
        input.setRead(false);
        input.setType(NotificationType.NOTIFICATION);

        assertThat(service.getNotificationList(input).getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getNotificationList：read=false 时只追加已读条件")
    void getNotificationListWithReadOnly() {
        stubQuery();
        stubPage(List.of(), 0L);

        GetNotificationListInput input = new GetNotificationListInput();
        input.setRead(false);

        assertThat(service.getNotificationList(input).getItems()).isEmpty();
    }

    @Test
    @DisplayName("getNotificationList：type 非空时只追加类型条件")
    void getNotificationListWithTypeOnly() {
        stubQuery();
        stubPage(List.of(), 0L);

        GetNotificationListInput input = new GetNotificationListInput();
        input.setType(NotificationType.ALERT);

        assertThat(service.getNotificationList(input).getTotalCount()).isZero();
    }

    // ---------------- getNotification ----------------

    @Test
    @DisplayName("getNotification：命中记录时自动置为已读并回填类型")
    void getNotificationMarksAsRead() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        NotificationOutput found = new NotificationOutput();
        found.setId(5L);
        found.setType(NotificationType.ALERT);
        when(query.fetchResults())
                .thenReturn(new QueryResults<NotificationOutput>(List.of(found), null, null, 1L));

        UserNotification entity = new UserNotification();
        entity.setId(5L);
        when(repository.getOne(5L)).thenReturn(entity);

        NotificationOutput result = service.getNotification(new EntityDto(5L));

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getTypeName()).isEqualTo(NotificationType.ALERT.getDisplayName());
        assertThat(entity.getRead()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("getNotification：命中记录但类型为 null 时跳过回填")
    void getNotificationWithoutType() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        NotificationOutput found = new NotificationOutput();
        found.setId(6L);
        when(query.fetchResults())
                .thenReturn(new QueryResults<NotificationOutput>(List.of(found), null, null, 1L));
        when(repository.getOne(6L)).thenReturn(new UserNotification());

        NotificationOutput result = service.getNotification(new EntityDto(6L));

        assertThat(result.getTypeName()).isNull();
        assertThat(result.getId()).isEqualTo(6L);
    }

    @Test
    @DisplayName("getNotification：记录不存在时返回空对象且不写库")
    void getNotificationReturnsEmptyWhenAbsent() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchResults())
                .thenReturn(new QueryResults<NotificationOutput>(List.of(), null, null, 0L));

        NotificationOutput result = service.getNotification(new EntityDto(404L));

        assertThat(result.getId()).isNull();
        verify(repository, never()).save(any(UserNotification.class));
    }

    // ---------------- getCount ----------------

    @Test
    @DisplayName("getCount：无过滤条件时直接返回计数")
    void getCountWithoutFilters() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchCount()).thenReturn(3L);

        assertThat(service.getCount(new GetNotificationListCountInput())).isEqualTo(3L);
    }

    @Test
    @DisplayName("getCount：read 与 type 非空时追加过滤条件")
    void getCountWithFilters() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchCount()).thenReturn(1L);

        GetNotificationListCountInput input = new GetNotificationListCountInput();
        input.setRead(true);
        input.setType(NotificationType.ALERT);

        assertThat(service.getCount(input)).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCount：仅 read 非空时追加已读条件")
    void getCountWithReadOnly() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchCount()).thenReturn(0L);

        GetNotificationListCountInput input = new GetNotificationListCountInput();
        input.setRead(false);

        assertThat(service.getCount(input)).isZero();
    }

    @Test
    @DisplayName("getCount：仅 type 非空时追加类型条件")
    void getCountWithTypeOnly() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchCount()).thenReturn(2L);

        GetNotificationListCountInput input = new GetNotificationListCountInput();
        input.setType(NotificationType.NOTIFICATION);

        assertThat(service.getCount(input)).isEqualTo(2L);
    }

    // ---------------- createNotification ----------------

    @Test
    @DisplayName("createNotification：为多个用户生成消息并逐个 MQTT 推送")
    void createNotificationPushesPerUser() {
        CreateNotificationInput input = new CreateNotificationInput();
        input.setTitle("标题");
        input.setContent("内容");
        input.setType(NotificationType.ALERT);
        input.setPageNavigation("nav");
        input.setUserIds(List.of(1L, 2L));

        service.createNotification(input);

        verify(notificationRepository).save(any());
        verify(mqttUtils, times(2)).publishMsgAsync(anyString(), any());
        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("createNotification：无接收人时不写用户消息表")
    void createNotificationWithoutUsers() {
        CreateNotificationInput input = new CreateNotificationInput();
        input.setTitle("标题");
        input.setType(NotificationType.NOTIFICATION);
        input.setUserIds(new ArrayList<>());

        service.createNotification(input);

        verify(notificationRepository).save(any());
        verify(repository, never()).saveAll(any());
        verify(mqttUtils, never()).publishMsgAsync(anyString(), any());
    }

    @Test
    @DisplayName("createNotification：类型为空时跳过类型回填但仍推送")
    void createNotificationWithoutType() {
        CreateNotificationInput input = new CreateNotificationInput();
        input.setTitle("标题");
        input.setUserIds(List.of(3L));

        service.createNotification(input);

        verify(mqttUtils).publishMsgAsync(anyString(), any());
    }

    // ---------------- 标记已读 / 删除 ----------------

    @Test
    @DisplayName("setNotificationAsRead：命中记录时全部置为已读并保存")
    void setNotificationAsReadSavesAll() {
        UserNotification first = new UserNotification();
        UserNotification second = new UserNotification();
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(first, second));

        SetNotificationAsReadInput input = new SetNotificationAsReadInput();
        input.setIds(List.of(1L, 2L));
        service.setNotificationAsRead(input);

        assertThat(first.getRead()).isTrue();
        assertThat(second.getRead()).isTrue();
        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("setNotificationAsRead：查询结果为空时仍会调用 saveAll（ArrayUtil.isNotEmpty 对 List 恒为 true）")
    void setNotificationAsReadStillSavesEmptyList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        SetNotificationAsReadInput input = new SetNotificationAsReadInput();
        input.setIds(List.of(1L));
        service.setNotificationAsRead(input);

        // 记录当前真实行为：空集合守卫实际失效，saveAll 收到空列表（对 Spring Data 而言是空操作）
        verify(repository).saveAll(List.of());
    }

    @Test
    @DisplayName("deleteNotification：记录存在时删除")
    void deleteNotificationDeletesExisting() {
        UserNotification entity = new UserNotification();
        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        service.deleteNotification(7L);

        verify(repository).delete(entity);
    }

    @Test
    @DisplayName("deleteNotification：记录不存在时抛出业务异常")
    void deleteNotificationThrowsWhenAbsent() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNotification(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户消息不存在");
    }

    @Test
    @DisplayName("batchDeleteNotification：命中记录时批量删除")
    void batchDeleteNotificationDeletesInBatch() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(new UserNotification(), new UserNotification()));

        BatchDeleteNotificationInput input = new BatchDeleteNotificationInput();
        input.setIds(List.of(1L, 2L));
        service.batchDeleteNotification(input);

        verify(repository).deleteInBatch(any());
    }

    @Test
    @DisplayName("batchDeleteNotification：查询结果为空时仍会触发 deleteInBatch（守卫同样失效，但 Spring Data 对空集合直接返回）")
    void batchDeleteNotificationStillInvokesEmptyBatch() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        BatchDeleteNotificationInput input = new BatchDeleteNotificationInput();
        input.setIds(List.of(1L));
        service.batchDeleteNotification(input);

        verify(repository).deleteInBatch(List.of());
    }

    @Test
    @DisplayName("getNotificationList：类型回填写入数值型 typeValue")
    void getNotificationListBackfillsTypeValue() {
        stubQuery();
        stubPage(List.of(output(NotificationType.ALERT)), 1L);

        PagedResultDto<NotificationListOutput> result =
                service.getNotificationList(new GetNotificationListInput());

        assertThat(result.getItems().getFirst().getTypeValue())
                .isEqualTo(NotificationType.ALERT.getValue());
        verify(repository, never()).deleteById(anyLong());
    }
}
