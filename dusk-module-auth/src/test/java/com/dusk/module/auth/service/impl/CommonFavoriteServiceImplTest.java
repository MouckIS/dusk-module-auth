package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.CommonFavoriteDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Sequence;
import com.dusk.module.auth.entity.CommonFavorite;
import com.dusk.module.auth.repository.ICommonFavoriteRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CommonFavoriteServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommonFavoriteServiceImplTest {

    private static final long CURRENT_USER_ID = 42L;
    private static final String TYPE = "dashboard";

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private Sequence sequence;
    @Mock
    private ICommonFavoriteRepository repository;

    private CommonFavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new CommonFavoriteServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "sequence", sequence);
        ReflectionTestUtils.setField(service, "repository", repository);
        LoginUserIdContextHolder.setUserId(CURRENT_USER_ID);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery<?> stubSelectFrom(long count) {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetchCount()).thenReturn(count);
        return query;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery<?> stubSelect(Object fetched) {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetchOne()).thenReturn(fetched);
        return query;
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<CommonFavoriteDto> content, long total) {
        Page<CommonFavoriteDto> page = new PageImpl<>(content, Pageable.unpaged(), total);
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    private static CommonFavoriteDto dto(Long id, String name) {
        CommonFavoriteDto dto = new CommonFavoriteDto();
        dto.setId(id);
        dto.setName(name);
        dto.setType(TYPE);
        return dto;
    }

    private static CommonFavorite favorite(Long id, String name, Long createId) {
        CommonFavorite entity = new CommonFavorite();
        entity.setId(id);
        entity.setName(name);
        entity.setType(TYPE);
        entity.setCreateId(createId);
        return entity;
    }

    // ---------------- save ----------------

    @Test
    @DisplayName("save：新增且名称未被占用时分配序列号并落库")
    void saveCreatesWhenIdAbsent() {
        stubSelectFrom(0L);
        when(sequence.nextId()).thenReturn(1000L);

        CommonFavoriteDto input = dto(null, "我的看板");

        CommonFavoriteDto result = service.save(input);

        assertThat(result.getId()).isEqualTo(1000L);
        verify(repository).save(any(CommonFavorite.class));
    }

    @Test
    @DisplayName("save：新增但名称已存在时抛出业务异常")
    void saveThrowsWhenNameTakenOnCreate() {
        stubSelectFrom(1L);

        assertThatThrownBy(() -> service.save(dto(null, "重名")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该名称数据已存在");
    }

    @Test
    @DisplayName("save：更新且名称未变化时跳过重名校验")
    void saveUpdatesWithoutNameChange() {
        CommonFavorite existing = favorite(1L, "原名", CURRENT_USER_ID);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.save(dto(1L, "原名"));

        verify(repository).save(existing);
    }

    @Test
    @DisplayName("save：更新且改名后无重名时落库")
    void saveUpdatesWithNewUniqueName() {
        CommonFavorite existing = favorite(1L, "原名", CURRENT_USER_ID);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        stubSelectFrom(0L);

        service.save(dto(1L, "新名"));

        assertThat(existing.getName()).isEqualTo("新名");
    }

    @Test
    @DisplayName("save：更新且改名后重名时抛出业务异常")
    void saveThrowsWhenNewNameTaken() {
        CommonFavorite existing = favorite(1L, "原名", CURRENT_USER_ID);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        stubSelectFrom(1L);

        assertThatThrownBy(() -> service.save(dto(1L, "占用名")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("save：更新时目标记录不存在则抛出业务异常")
    void saveThrowsWhenUpdateTargetMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(dto(1L, "任意")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据不存在或已被删除");
    }

    // ---------------- deleteById ----------------

    @Test
    @DisplayName("deleteById：类型匹配且为本人收藏时删除")
    void deleteByIdRemovesOwnFavorite() {
        when(repository.findById(1L)).thenReturn(Optional.of(favorite(1L, "n", CURRENT_USER_ID)));

        service.deleteById(1L, TYPE);

        verify(repository).delete(any(CommonFavorite.class));
    }

    @Test
    @DisplayName("deleteById：createId 为空的历史数据允许删除")
    void deleteByIdAllowsNullCreateId() {
        when(repository.findById(1L)).thenReturn(Optional.of(favorite(1L, "n", null)));

        service.deleteById(1L, TYPE);

        verify(repository).delete(any(CommonFavorite.class));
    }

    @Test
    @DisplayName("deleteById：类型不匹配时抛出业务异常")
    void deleteByIdThrowsOnTypeMismatch() {
        when(repository.findById(1L)).thenReturn(Optional.of(favorite(1L, "n", CURRENT_USER_ID)));

        assertThatThrownBy(() -> service.deleteById(1L, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("类型不匹配");
    }

    @Test
    @DisplayName("deleteById：他人收藏时抛出业务异常")
    void deleteByIdThrowsOnOtherUserFavorite() {
        when(repository.findById(1L)).thenReturn(Optional.of(favorite(1L, "n", CURRENT_USER_ID + 1)));

        assertThatThrownBy(() -> service.deleteById(1L, TYPE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除他人的收藏");
    }

    @Test
    @DisplayName("deleteById：记录不存在时静默返回")
    void deleteByIdSilentlyReturnsWhenAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        service.deleteById(1L, TYPE);

        verify(repository, never()).delete(any(CommonFavorite.class));
    }

    // ---------------- setPublic ----------------

    @Test
    @DisplayName("setPublic：本人收藏可切换公开状态")
    void setPublicUpdatesOwnFavorite() {
        CommonFavorite entity = favorite(1L, "n", CURRENT_USER_ID);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.setPublic(1L, true);

        assertThat(entity.getIsPublic()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("setPublic：记录不存在时抛出业务异常")
    void setPublicThrowsWhenAbsent() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setPublic(1L, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该收藏不存在或已被删除");
    }

    @Test
    @DisplayName("setPublic：他人收藏时抛出业务异常")
    void setPublicThrowsOnOtherUserFavorite() {
        when(repository.findById(1L)).thenReturn(Optional.of(favorite(1L, "n", CURRENT_USER_ID + 1)));

        assertThatThrownBy(() -> service.setPublic(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能设置他人的收藏");
    }

    // ---------------- get ----------------

    @Test
    @DisplayName("get：命中记录时返回 DTO")
    void getReturnsDtoWhenFound() {
        stubSelect(dto(1L, "n"));

        assertThat(service.get(1L, TYPE).getName()).isEqualTo("n");
    }

    @Test
    @DisplayName("get：记录不存在时抛出业务异常")
    void getThrowsWhenAbsent() {
        stubSelect(null);

        assertThatThrownBy(() -> service.get(1L, TYPE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该收藏不存在或已被删除");
    }

    // ---------------- list ----------------

    @Test
    @DisplayName("list：onlyMine=true 时仅返回本人的收藏")
    void listOnlyMine() {
        stubSelect(null);
        stubPage(List.of(dto(1L, "n")), 1L);

        PagedResultDto<CommonFavoriteDto> result =
                service.list(true, new PagedAndSortedInputDto(), TYPE);

        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("list：onlyMine=false 时额外包含公开收藏")
    void listIncludesPublic() {
        stubSelect(null);
        stubPage(List.of(), 0L);

        PagedResultDto<CommonFavoriteDto> result =
                service.list(false, new PagedAndSortedInputDto(), TYPE, "other");

        assertThat(result.getItems()).isEmpty();
    }
}
