package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.enums.EnumResetType;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.repository.ISerialNoRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SerialNoServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SerialNoServiceImplTest {

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private ISerialNoRepository repository;

    private SerialNoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SerialNoServiceImpl();
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubFetchFirst(SerialNo existing) {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetchFirst()).thenReturn(existing);
    }

    private static SerialNo existing(String dateFormat, int noLength, EnumResetType resetType,
                                     long currentNo, LocalDateTime lastUpdateTime) {
        SerialNo serialNo = new SerialNo();
        serialNo.setBillType("BILL");
        serialNo.setDateFormat(dateFormat);
        serialNo.setNoLength(noLength);
        serialNo.setResetType(resetType);
        serialNo.setCurrentNo(currentNo);
        serialNo.setLastUpdateTime(lastUpdateTime);
        return serialNo;
    }

    @Test
    @DisplayName("getSerialNos：流水号记录不存在时新建并保存")
    void getSerialNosCreatesRecordWhenAbsent() {
        stubFetchFirst(null);

        String[] result = service.getSerialNos("BILL", EnumResetType.Never, "yyyyMMdd", 4, 2);

        assertThat(result).hasSize(2);
        assertThat(result[0]).matches("\\d{8}0001");
        assertThat(result[1]).matches("\\d{8}0002");
        verify(repository).save(any(SerialNo.class));
    }

    @Test
    @DisplayName("getSerialNos：codeFirst=false 时沿用记录中的格式化配置")
    void getSerialNosUsesStoredConfigWhenNotCodeFirst() {
        stubFetchFirst(existing("yyyyMMdd", 6, EnumResetType.Never, 5, LocalDateTime.now()));

        String[] result = service.getSerialNos("BILL", EnumResetType.Day, "IGNORED", 1, 1, false);

        assertThat(result[0]).matches("\\d{8}000006");
    }

    @Test
    @DisplayName("getSerialNos：codeFirst=true 时使用入参覆盖记录配置")
    void getSerialNosOverridesConfigWhenCodeFirst() {
        stubFetchFirst(existing("IGNORED", 1, EnumResetType.Never, 5, LocalDateTime.now()));

        String[] result = service.getSerialNos("BILL", EnumResetType.Never, "yyyy", 6, 1, true);

        assertThat(result[0]).matches("\\d{4}000006");
    }

    @Test
    @DisplayName("getSerialNos：resetType=Day 且日期未变更时不重置")
    void getSerialNosKeepsCurrentNoForDayWhenSameDay() {
        stubFetchFirst(existing("yyyyMMdd", 4, EnumResetType.Day, 7, LocalDateTime.now()));

        String[] result = service.getSerialNos("BILL", EnumResetType.Day, "yyyyMMdd", 4, 1);

        assertThat(result[0]).endsWith("0008");
    }

    @Test
    @DisplayName("getSerialNos：resetType=Day 且日期变更时重置为 0")
    void getSerialNosResetsForDayWhenDayChanged() {
        stubFetchFirst(existing("yyyyMMdd", 4, EnumResetType.Day, 7,
                LocalDateTime.now().minusDays(1)));

        String[] result = service.getSerialNos("BILL", EnumResetType.Day, "yyyyMMdd", 4, 1);

        assertThat(result[0]).endsWith("0001");
    }

    @Test
    @DisplayName("getSerialNos：resetType=Month 且月份变更时重置为 0")
    void getSerialNosResetsForMonthWhenMonthChanged() {
        stubFetchFirst(existing("yyyyMMdd", 4, EnumResetType.Month, 7,
                LocalDateTime.now().minusMonths(1)));

        String[] result = service.getSerialNos("BILL", EnumResetType.Month, "yyyyMMdd", 4, 1);

        assertThat(result[0]).endsWith("0001");
    }

    @Test
    @DisplayName("getSerialNos：resetType=Year 且年份变更时重置为 0")
    void getSerialNosResetsForYearWhenYearChanged() {
        stubFetchFirst(existing("yyyyMMdd", 4, EnumResetType.Year, 7,
                LocalDateTime.now().minusYears(1)));

        String[] result = service.getSerialNos("BILL", EnumResetType.Year, "yyyyMMdd", 4, 1);

        assertThat(result[0]).endsWith("0001");
    }

    @Test
    @DisplayName("getSerialNos：resetType=Never 时走 default 分支不重置")
    void getSerialNosKeepsCurrentNoForNever() {
        stubFetchFirst(existing("yyyyMMdd", 4, EnumResetType.Never, 7, LocalDateTime.now()));

        String[] result = service.getSerialNos("BILL", EnumResetType.Never, "yyyyMMdd", 4, 1);

        assertThat(result[0]).endsWith("0008");
    }

    @Test
    @DisplayName("getSerialNos：超出流水号位数上限时抛出业务异常")
    void getSerialNosThrowsWhenOverflow() {
        stubFetchFirst(null);

        assertThatThrownBy(() -> service.getSerialNos("BILL", EnumResetType.Never, "", 0, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("流水号超过最大限度");
    }

    @Test
    @DisplayName("getCurrentNo：dateFormat 为空时不追加日期前缀")
    void getCurrentNoWithoutDateFormat() {
        assertThat(service.getCurrentNo(LocalDateTime.now(), "", 3, 4)).isEqualTo("0003");
    }

    @Test
    @DisplayName("getSerialNo：四参数重载返回单个流水号")
    void getSerialNoDelegatesToGetSerialNos() {
        stubFetchFirst(null);

        assertThat(service.getSerialNo("BILL", EnumResetType.Never, "", 4)).matches("\\d{4}");
    }

    @Test
    @DisplayName("getSerialNo：五参数重载返回单个流水号")
    void getSerialNoWithCodeFirstDelegates() {
        stubFetchFirst(null);

        assertThat(service.getSerialNo("BILL", EnumResetType.Never, "", 4, true)).matches("\\d{4}");
    }

    /**
     * 分页查询的 Specification 只有在 toPredicate 被调用时才会执行内部条件拼接，
     * 因此这里用 mock 的 root/query/builder 强制求值，以覆盖 lambda 内的分支。
     */
    @SuppressWarnings("unchecked")
    private void stubFindAllEvaluatingSpec() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Specification<SerialNo> spec = invocation.getArgument(0);
            spec.toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
            return Page.empty();
        });
    }

    @Test
    @DisplayName("getSerialNos：分页查询 billType 为空时仅返回仓储结果")
    void getSerialNosPageWithoutBillType() {
        stubFindAllEvaluatingSpec();
        GetSerialNoInput input = new GetSerialNoInput();

        assertThat(service.getSerialNos(input)).isEmpty();
    }

    @Test
    @DisplayName("getSerialNos：分页查询 billType 非空时追加模糊条件")
    void getSerialNosPageWithBillType() {
        stubFindAllEvaluatingSpec();
        GetSerialNoInput input = new GetSerialNoInput();
        input.setBillType("BILL");

        assertThat(service.getSerialNos(input)).isEmpty();
    }

    @Test
    @DisplayName("update：记录存在时复制属性并保存")
    void updateSavesWhenFound() {
        SerialNo entity = existing("", 4, EnumResetType.Never, 1, LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        SerialNoEditInput input = new SerialNoEditInput();
        input.setId(1L);
        input.setCurrentNo(42L);
        service.update(input);

        assertThat(entity.getCurrentNo()).isEqualTo(42L);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("update：记录不存在时抛出业务异常")
    void updateThrowsWhenAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        SerialNoEditInput input = new SerialNoEditInput();
        input.setId(1L);

        assertThatThrownBy(() -> service.update(input)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOneById：记录存在时返回实体")
    void getOneByIdReturnsEntity() {
        SerialNo entity = existing("", 4, EnumResetType.Never, 1, LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(service.getOneById(1L)).isSameAs(entity);
    }

    @Test
    @DisplayName("getOneById：记录不存在时抛出业务异常")
    void getOneByIdThrowsWhenAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOneById(1L)).isInstanceOf(BusinessException.class);
    }
}
