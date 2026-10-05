package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.module.auth.cache.IUserWxRelationCacheService;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.repository.IUserWxRelationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserWxRelationServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class UserWxRelationServiceImplTest {

    @Mock
    private IUserWxRelationRepository repository;
    @Mock
    private IUserWxRelationCacheService userWxRelationCacheService;

    private UserWxRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserWxRelationServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "userWxRelationCacheService", userWxRelationCacheService);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
    }

    private static UserWxRelation relation(Long userId, String appId, String openId) {
        UserWxRelation relation = new UserWxRelation();
        relation.setUserId(userId);
        relation.setAppId(appId);
        relation.setOpenId(openId);
        return relation;
    }

    @Test
    @DisplayName("getOpenId：缓存命中时直接返回")
    void getOpenIdReturnsCachedValue() {
        when(userWxRelationCacheService.getWxRelation(1L, "app")).thenReturn("open-id");

        assertThat(service.getOpenId(1L, "app")).isEqualTo("open-id");
    }

    @Test
    @DisplayName("getOpenId：缓存未命中且库中不存在时返回 null")
    void getOpenIdReturnsNullWhenAbsent() {
        when(userWxRelationCacheService.getWxRelation(1L, "app")).thenReturn("  ");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThat(service.getOpenId(1L, "app")).isNull();
    }

    @Test
    @DisplayName("getOpenId：缓存未命中但库中存在时回填缓存")
    void getOpenIdFillsCacheFromRepository() {
        when(userWxRelationCacheService.getWxRelation(1L, "app")).thenReturn(null);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(relation(1L, "app", "open-id")));

        assertThat(service.getOpenId(1L, "app")).isEqualTo("open-id");
        verify(userWxRelationCacheService).saveWxRelation(1L, "app", "open-id");
    }

    @Test
    @DisplayName("saveRelationList：空列表直接返回，不访问仓储")
    void saveRelationListSkipsWhenEmpty() {
        service.saveRelationList(Collections.emptyList());

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("saveRelationList：按 userId + appId 匹配合并并刷新缓存")
    void saveRelationListMergesAndRefreshesCache() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(relation(1L, "app", "old")));

        List<UserWxRelation> targets = List.of(
                relation(1L, "app", "new"),
                relation(1L, "other", "x"),
                relation(2L, "app", "y"));

        service.saveRelationList(targets);

        verify(repository).saveAll(targets);
        verify(userWxRelationCacheService, org.mockito.Mockito.times(3))
                .saveWxRelation(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("saveRelationList：userId 相同但 appId 不同时不合并")
    void saveRelationListDoesNotMergeWhenAppIdDiffers() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(relation(1L, "app", "old")));

        List<UserWxRelation> targets = List.of(relation(1L, "zzz", "x"));

        service.saveRelationList(targets);

        verify(repository).saveAll(targets);
    }

    @Test
    @DisplayName("saveRelationList：无匹配来源记录时不合并")
    void saveRelationListWithoutMatchingSource() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(relation(9L, "app", "old")));

        List<UserWxRelation> targets = List.of(relation(1L, "app", "new"));

        service.saveRelationList(targets);

        verify(repository).saveAll(targets);
    }
}
