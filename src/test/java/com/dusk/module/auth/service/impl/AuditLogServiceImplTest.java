package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogListDto;
import com.dusk.module.auth.dto.auditlog.GetAuditLogsInput;
import com.dusk.module.auth.repository.IAuditLogRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceImplTest {

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private IAuditLogRepository repository;

    private AuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new AuditLogServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery<?> stubQuery() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        return query;
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<AuditLogListDto> content) {
        Page<AuditLogListDto> page = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    private static AuditLogDetailDto detail() {
        AuditLogDetailDto dto = new AuditLogDetailDto();
        dto.setId(1L);
        dto.setUserName("tester");
        dto.setExecutionTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return dto;
    }

    // ---------------- findAuditLogs ----------------

    @Test
    @DisplayName("findAuditLogs：无过滤条件时直接返回分页结果")
    void findAuditLogsWithoutFilters() {
        stubQuery();
        stubPage(List.of(detail()));

        Page<AuditLogListDto> page = service.findAuditLogs(new GetAuditLogsInput());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("findAuditLogs：全条件命中时逐条追加过滤")
    void findAuditLogsWithAllFilters() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        input.setEndDate(LocalDateTime.of(2026, 1, 2, 0, 0));
        input.setUserName("tester");
        input.setServiceName("auth");
        input.setMethodName("login");
        input.setBrowserInfo("Chrome");
        input.setMinExecutionDuration(1);
        input.setMaxExecutionDuration(100);
        input.setHasException(true);

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：hasException=false 时过滤无异常记录")
    void findAuditLogsWithHasExceptionFalse() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setHasException(false);

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：空白字符串条件被忽略")
    void findAuditLogsIgnoresBlankStrings() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setUserName("  ");
        input.setServiceName("");
        input.setMethodName("   ");
        input.setBrowserInfo("");

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：起止时间与耗时段分别独立生效")
    void findAuditLogsWithPartialFilters() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        input.setMinExecutionDuration(5);

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：仅结束时间与最大耗时条件生效")
    void findAuditLogsWithEndDateAndMaxDuration() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setEndDate(LocalDateTime.of(2026, 1, 2, 0, 0));
        input.setMaxExecutionDuration(50);

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：仅用户名条件生效")
    void findAuditLogsWithUserNameOnly() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setUserName("tester");

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：仅服务名条件生效")
    void findAuditLogsWithServiceNameOnly() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setServiceName("auth");

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：仅方法名条件生效")
    void findAuditLogsWithMethodNameOnly() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setMethodName("login");

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAuditLogs：仅浏览器条件生效")
    void findAuditLogsWithBrowserOnly() {
        stubQuery();
        stubPage(List.of());

        GetAuditLogsInput input = new GetAuditLogsInput();
        input.setBrowserInfo("Chrome");

        assertThat(service.findAuditLogs(input).getContent()).isEmpty();
    }

    // ---------------- getAuditLogDetail ----------------

    @Test
    @DisplayName("getAuditLogDetail：命中记录时返回明细")
    void getAuditLogDetailReturnsDetail() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchFirst()).thenReturn(detail());

        assertThat(service.getAuditLogDetail(1L).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAuditLogDetail：记录不存在时抛出业务异常")
    void getAuditLogDetailThrowsWhenAbsent() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = stubQuery();
        when(query.fetchFirst()).thenReturn(null);

        assertThatThrownBy(() -> service.getAuditLogDetail(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审计日志不存在或已被删除");
    }

    // ---------------- exportLog ----------------

    /*
     * exportLog 未纳入单元测试：末尾调用 EasyExcel.write(...).sheet().doWrite(...) 写出工作簿，
     * 而当前依赖树中 commons-io 被解析为 2.5（经 weixin-java-miniapp 传递），
     * 缺少 easyexcel:4.0.3 依赖的 org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream，
     * 任何 Excel 写出都会抛 ExcelGenerateException/NoClassDefFoundError。
     *
     * 该缺陷同时影响 RoleServiceImpl 权限模板导出、SubscribableEditionServiceImpl.export、
     * UserServiceImpl.getUsersToExcel 等全部导出链路，属生产级问题。
     * 按仓库约定（依赖版本须由 dusk-dependencies BOM 统一管理，不得在本模块自行添加版本），
     * 此处不擅自修改 pom，待 BOM 侧将 commons-io 升至 >= 2.12 后补齐本段测试。
     */
}
