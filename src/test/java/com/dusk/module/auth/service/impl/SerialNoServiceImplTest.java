package com.dusk.module.auth.service.impl;

import cn.hutool.core.date.DateUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.QSerialNo;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.repository.ISerialNoRepository;
import com.dusk.module.auth.util.TestDataBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 序列号服务单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("序列号服务测试")
@ExtendWith(MockitoExtension.class)
class SerialNoServiceImplTest {

    @Mock
    private ISerialNoRepository repository;

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private SerialNoServiceImpl serialNoService;

    @DisplayName("getSerialNos - 新建序列号，单次生成")
    @Test
    void testGetSerialNos_NewSerialNo_SingleGenerate() {
        // Arrange
        String billType = "TEST";
        LocalDateTime now = LocalDateTime.now();
        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(null);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Year, "yyyy-MM-dd", 6, 1);

        // Assert
        assertThat(result).hasLength(1);
        assertThat(result[0]).isNotEmpty();
        assertThat(result[0]).contains(DateUtil.format(now, "yyyy-MM-dd"));

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBillType()).isEqualTo(billType);
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(1L);
    }

    @DisplayName("getSerialNos - 新建序列号，批量生成")
    @Test
    void testGetSerialNos_NewSerialNo_BatchGenerate() {
        // Arrange
        String billType = "TEST";
        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(null);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 5L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Year, "yyyy-MM-dd", 6, 5);

        // Assert
        assertThat(result).hasLength(5);
        for (String serialNo : result) {
            assertThat(serialNo).isNotEmpty();
        }

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(5L);
    }

    @DisplayName("getSerialNos - 现有序列号，年度重置")
    @Test
    void testGetSerialNos_ExistingSerialNo_YearlyReset() {
        // Arrange
        String billType = "TEST";
        LocalDateTime lastYear = LocalDateTime.now().minusYears(1);
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 100L, "yyyy-MM-dd", 6, 1L);
        existing.setLastUpdateTime(lastYear);
        existing.setResetType(EnumResetType.Year);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Year, "yyyy-MM-dd", 6, 1);

        // Assert
        assertThat(result).hasLength(1);

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        // 因为跨年，应该重置为0，所以第一个序列号应该是1
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(1L);
    }

    @DisplayName("getSerialNos - 现有序列号，月度重置")
    @Test
    void testGetSerialNos_ExistingSerialNo_MonthlyReset() {
        // Arrange
        String billType = "TEST";
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 50L, "yyyy-MM-dd", 6, 1L);
        existing.setLastUpdateTime(lastMonth);
        existing.setResetType(EnumResetType.Month);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Month, "yyyy-MM-dd", 6, 1);

        // Assert
        assertThat(result).hasLength(1);

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(1L);
    }

    @DisplayName("getSerialNos - 现有序列号，日度重置")
    @Test
    void testGetSerialNos_ExistingSerialNo_DailyReset() {
        // Arrange
        String billType = "TEST";
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 25L, "yyyy-MM-dd", 6, 1L);
        existing.setLastUpdateTime(yesterday);
        existing.setResetType(EnumResetType.Day);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Day, "yyyy-MM-dd", 6, 1);

        // Assert
        assertThat(result).hasLength(1);

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(1L);
    }

    @DisplayName("getSerialNos - 现有序列号，无重置")
    @Test
    void testGetSerialNos_ExistingSerialNo_NoReset() {
        // Arrange
        String billType = "TEST";
        LocalDateTime now = LocalDateTime.now();
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 100L, "yyyy-MM-dd", 6, 1L);
        existing.setLastUpdateTime(now);
        existing.setResetType(EnumResetType.None);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 101L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.None, "yyyy-MM-dd", 6, 1);

        // Assert
        assertThat(result).hasLength(1);

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        // 应该继续从100开始，所以第一个是101
        assertThat(captor.getValue().getCurrentNo()).isEqualTo(101L);
    }

    @DisplayName("getSerialNos - 流水号超过最大长度限制")
    @Test
    void testGetSerialNos_ExceedsMaxLength() {
        // Arrange
        String billType = "TEST";
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 999990L, "yyyy-MM-dd", 6, 1L);
        existing.setLastUpdateTime(LocalDateTime.now());
        existing.setResetType(EnumResetType.None);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        // Act & Assert
        assertThatThrownBy(() -> serialNoService.getSerialNos(billType, EnumResetType.None, "yyyy-MM-dd", 6, 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("流水号超过最大限度");
    }

    @DisplayName("getSerialNos - 代码优先模式")
    @Test
    void testGetSerialNos_CodeFirst() {
        // Arrange
        String billType = "TEST";
        SerialNo existing = TestDataBuilder.buildSerialNo(billType, 100L, "yyyy-MM", 8, 1L);
        existing.setLastUpdateTime(LocalDateTime.now());
        existing.setResetType(EnumResetType.Month);

        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(existing);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 101L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act - 使用不同的dateFormat和serialLength，且codeFirst=true
        String[] result = serialNoService.getSerialNos(billType, EnumResetType.Year, "yyyy-MM-dd", 6, 1, true);

        // Assert
        assertThat(result).hasLength(1);

        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        // 代码优先模式下，应该使用传入的dateFormat
        assertThat(captor.getValue().getDateFormat()).isEqualTo("yyyy-MM-dd");
    }

    @DisplayName("getSerialNo - 单个序列号获取")
    @Test
    void testGetSerialNo_Single() {
        // Arrange
        String billType = "TEST";
        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(null);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String result = serialNoService.getSerialNo(billType, EnumResetType.Year, "yyyy-MM-dd", 6);

        // Assert
        assertThat(result).isNotEmpty();
    }

    @DisplayName("getSerialNo - 单个序列号获取（代码优先）")
    @Test
    void testGetSerialNo_Single_CodeFirst() {
        // Arrange
        String billType = "TEST";
        when(queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst())
                .thenReturn(null);

        SerialNo savedSerialNo = TestDataBuilder.buildSerialNo(billType, 1L, "yyyy-MM-dd", 6, 1L);
        when(repository.save(any(SerialNo.class))).thenReturn(savedSerialNo);

        // Act
        String result = serialNoService.getSerialNo(billType, EnumResetType.Year, "yyyy-MM-dd", 6, true);

        // Assert
        assertThat(result).isNotEmpty();
    }

    @DisplayName("getSerialNos - 分页查询")
    @Test
    void testGetSerialNos_Pagination() {
        // Arrange
        GetSerialNoInput input = new GetSerialNoInput();
        input.setBillType("TEST");
        input.setPageable(PageRequest.of(0, 10));

        List<SerialNo> content = Arrays.asList(
                TestDataBuilder.buildSerialNo("TEST", 1L, "yyyy-MM-dd", 6, 1L),
                TestDataBuilder.buildSerialNo("TEST", 2L, "yyyy-MM-dd", 6, 1L)
        );
        Page<SerialNo> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        Page<SerialNo> result = serialNoService.getSerialNos(input);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @DisplayName("getCurrentNo - 生成完整序列号（带日期和数字）")
    @Test
    void testGetCurrentNo_WithDateFormat() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        String dateFormat = "yyyy-MM-dd";
        long nextNo = 123L;
        int serialLength = 6;

        // Act
        String result = serialNoService.getCurrentNo(now, dateFormat, nextNo, serialLength);

        // Assert
        assertThat(result).contains(DateUtil.format(now, dateFormat));
        assertThat(result).endsWith("000123");
    }

    @DisplayName("getCurrentNo - 生成序列号（无日期）")
    @Test
    void testGetCurrentNo_NoDateFormat() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        long nextNo = 789L;
        int serialLength = 6;

        // Act
        String result = serialNoService.getCurrentNo(now, "", nextNo, serialLength);

        // Assert
        assertThat(result).isEqualTo("000789");
        assertThat(result).doesNotContain(DateUtil.format(now, "yyyy-MM-dd"));
    }

    @DisplayName("getCurrentNo - 数字补零")
    @Test
    void testGetCurrentNo_PaddingZeros() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        long nextNo = 1L;
        int serialLength = 10;

        // Act
        String result = serialNoService.getCurrentNo(now, "", nextNo, serialLength);

        // Assert
        assertThat(result).isEqualTo("0000000001");
    }

    @DisplayName("update - 更新序列号")
    @Test
    void testUpdate_Success() {
        // Arrange
        SerialNoEditInput input = TestDataBuilder.buildSerialNoEditInput();
        SerialNo existing = TestDataBuilder.buildSerialNo("OLD_TYPE", 10L, "yyyy", 4, 1L);
        existing.setId(input.getId());

        when(repository.findById(input.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(SerialNo.class))).thenReturn(existing);

        // Act
        serialNoService.update(input);

        // Assert
        ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
        verify(repository).save(captor.capture());
        SerialNo saved = captor.getValue();
        assertThat(saved.getBillType()).isEqualTo(input.getBillType());
        assertThat(saved.getCurrentNo()).isEqualTo(input.getCurrentNo());
    }

    @DisplayName("update - 记录不存在")
    @Test
    void testUpdate_NotFound() {
        // Arrange
        SerialNoEditInput input = TestDataBuilder.buildSerialNoEditInput();
        when(repository.findById(input.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> serialNoService.update(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据不存在");
    }

    @DisplayName("getOneById - 根据ID获取序列号")
    @Test
    void testGetOneById_Success() {
        // Arrange
        Long id = 1L;
        SerialNo expected = TestDataBuilder.buildSerialNo("TEST", 1L, "yyyy-MM-dd", 6, 1L);
        expected.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        // Act
        SerialNo result = serialNoService.getOneById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getBillType()).isEqualTo("TEST");
    }

    @DisplayName("getOneById - 记录不存在")
    @Test
    void testGetOneById_NotFound() {
        // Arrange
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> serialNoService.getOneById(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据不存在");
    }
}

