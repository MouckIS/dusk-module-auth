package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.mqs.utils.MqttUtils;
import com.dusk.common.rpc.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.rpc.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.common.rpc.auth.service.ISerialNoRpcService;
import com.dusk.module.auth.cache.IUserFingerprintCacheService;
import com.dusk.module.auth.dto.fingerprint.IdentifyInputDto;
import com.dusk.module.auth.dto.fingerprint.RegisterFingerprintInputDto;
import com.dusk.module.auth.dto.fingerprint.SaveFingerprintInputDto;
import com.dusk.module.auth.entity.UserFingerprint;
import com.dusk.module.auth.repository.IUserFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserFingerprintServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFingerprintServiceImplTest {

    private static final long CURRENT_USER_ID = 8L;

    @Mock
    private IUserFingerprintRepository repository;
    @Mock
    private IUserFingerprintCacheService userFingerprintCacheService;
    @Mock
    private MqttUtils mqttUtils;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ISerialNoRpcService serialNoRpcService;

    private UserFingerprintServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserFingerprintServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "userFingerprintCacheService", userFingerprintCacheService);
        ReflectionTestUtils.setField(service, "mqttUtils", mqttUtils);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "serialNoRpcService", serialNoRpcService);

        UserContext userContext = new UserContext();
        userContext.setId(CURRENT_USER_ID);
        when(securityUtils.getCurrentUser()).thenReturn(userContext);
    }

    private static UserFingerprint fingerprint(Long id, Long userId, Integer userSeq) {
        UserFingerprint entity = new UserFingerprint();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setUserSeq(userSeq);
        entity.setName("finger-" + id);
        entity.setData("data-" + id);
        return entity;
    }

    private static SaveFingerprintInputDto saveInput(Long id, Long userId, String name) {
        SaveFingerprintInputDto dto = new SaveFingerprintInputDto();
        dto.setId(id);
        dto.setUserId(userId);
        dto.setName(name);
        dto.setData("payload");
        dto.setSize(64);
        return dto;
    }

    private void stubSaveAssignsId() {
        when(repository.save(any(UserFingerprint.class))).thenAnswer(invocation -> {
            UserFingerprint entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(100L);
            }
            return entity;
        });
    }

    // ---------------- registerFingerprint ----------------

    @Test
    @DisplayName("registerFingerprint：缓存已有 userSeq 时直接下发注册指令")
    void registerFingerprintUsesCachedUserSeq() {
        when(userFingerprintCacheService.getUserSeq(1L)).thenReturn(66);

        RegisterFingerprintInputDto input = new RegisterFingerprintInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");
        service.registerFingerprint(input);

        verify(mqttUtils).publishMsgAsync(eq("Fingerprint/DEV-1/Register/Start"), any());
        verify(serialNoRpcService, never()).getSerialNo(anyString(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("registerFingerprint：缓存缺失但库中已有 userSeq 时复用并回写缓存")
    void registerFingerprintReusesPersistedUserSeq() {
        when(userFingerprintCacheService.getUserSeq(1L)).thenReturn(null);
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(fingerprint(1L, 1L, 77)));

        RegisterFingerprintInputDto input = new RegisterFingerprintInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");
        service.registerFingerprint(input);

        verify(userFingerprintCacheService).saveUserSeq(1L, 77);
        verify(serialNoRpcService, never()).getSerialNo(anyString(), any(), any(), any(Integer.class));
    }

    @Test
    @DisplayName("registerFingerprint：缓存与库均无 userSeq 时通过序列号服务生成")
    void registerFingerprintGeneratesUserSeq() {
        when(userFingerprintCacheService.getUserSeq(1L)).thenReturn(null);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());
        when(serialNoRpcService.getSerialNo(eq("userSeq"), eq(EnumResetType.Never), any(), eq(5)))
                .thenReturn("00009");

        RegisterFingerprintInputDto input = new RegisterFingerprintInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");
        service.registerFingerprint(input);

        verify(userFingerprintCacheService).saveUserSeq(1L, 9);
        verify(mqttUtils).publishMsgAsync(anyString(), any());
    }

    // ---------------- saveFingerprint ----------------

    @Test
    @DisplayName("saveFingerprint：名称重复时抛出业务异常")
    void saveFingerprintThrowsOnDuplicateName() {
        when(repository.count(any(Specification.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.saveFingerprint(saveInput(null, 1L, "重复名")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("saveFingerprint：新增时校验 10 条上限")
    void saveFingerprintThrowsWhenExceedsLimit() {
        when(repository.count(any(Specification.class))).thenReturn(0L, 10L);

        assertThatThrownBy(() -> service.saveFingerprint(saveInput(null, 1L, "新指纹")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多只能录入10个指纹");
    }

    @Test
    @DisplayName("saveFingerprint：新增且未超限时落库并返回主键")
    void saveFingerprintCreatesNew() {
        when(repository.count(any(Specification.class))).thenReturn(0L, 3L);
        stubSaveAssignsId();

        assertThat(service.saveFingerprint(saveInput(null, 1L, "新指纹"))).isEqualTo(100L);
    }

    @Test
    @DisplayName("saveFingerprint：更新时复制非空属性并落库")
    void saveFingerprintUpdatesExisting() {
        when(repository.count(any(Specification.class))).thenReturn(0L);
        when(repository.findById(5L)).thenReturn(Optional.of(fingerprint(5L, 1L, 1)));
        stubSaveAssignsId();

        service.saveFingerprint(saveInput(5L, 1L, "改名后"));

        verify(repository).save(any(UserFingerprint.class));
    }

    @Test
    @DisplayName("saveFingerprint：更新目标不存在时抛出业务异常")
    void saveFingerprintThrowsWhenUpdateTargetMissing() {
        when(repository.count(any(Specification.class))).thenReturn(0L);
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveFingerprint(saveInput(5L, 1L, "改名后")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("指纹记录不存在");
    }

    // ---------------- getAll / identify ----------------

    @Test
    @DisplayName("getAll：按用户集合与名称模糊条件查询并映射为 DTO")
    void getAllReturnsMappedDtos() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(fingerprint(1L, 1L, 1)));

        GetAllInputDto input = new GetAllInputDto();
        input.setUserIds(List.of(1L));
        input.setFilter("finger");

        List<UserFingerprintDto> result = service.getAll(input);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("finger-1");
    }

    @Test
    @DisplayName("getAll：userIds 为空且无过滤条件时仍可查询")
    void getAllWithoutFilters() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        GetAllInputDto input = new GetAllInputDto();
        input.setUserIds(Collections.emptyList());

        assertThat(service.getAll(input)).isEmpty();
    }

    @Test
    @DisplayName("getAll：指定 fingerprintId 时追加主键等值条件")
    void getAllWithFingerprintId() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        GetAllInputDto input = new GetAllInputDto();
        input.setUserIds(List.of(1L));
        input.setFingerprintId("9");

        assertThat(service.getAll(input)).isEmpty();
    }

    @Test
    @DisplayName("identify：无指纹数据时抛出业务异常")
    void identifyThrowsWhenNoData() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        IdentifyInputDto input = new IdentifyInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");

        assertThatThrownBy(() -> service.identify(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到用户指纹数据");
    }

    @Test
    @DisplayName("identify：存在指纹数据时下发比对指令")
    void identifyPublishesCommand() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(fingerprint(1L, 1L, 5)));

        IdentifyInputDto input = new IdentifyInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");
        service.identify(input);

        verify(mqttUtils).publishMsgAsync(eq("Fingerprint/DEV-1/Identify/Start"), any());
    }

    @Test
    @DisplayName("identify：指定 fingerprintId 时按主键进一步过滤")
    void identifyWithFingerprintId() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(fingerprint(1L, 1L, 5)));

        IdentifyInputDto input = new IdentifyInputDto();
        input.setUserId(1L);
        input.setFingerprintId(1L);
        input.setDeviceNo("DEV-2");
        service.identify(input);

        verify(mqttUtils).publishMsgAsync(anyString(), any());
    }

    // ---------------- private 语义 ----------------

    @Test
    @DisplayName("saveFingerprintPrivate：更新他人指纹时抛出业务异常")
    void saveFingerprintPrivateThrowsWhenUpdatingOthers() {
        // saveFingerprintPrivate 的更新分支以 createId（而非 userId）判定归属
        UserFingerprint others = fingerprint(5L, 1L, 1);
        others.setCreateId(CURRENT_USER_ID + 1);
        when(repository.findById(5L)).thenReturn(Optional.of(others));
        when(repository.count(any(Specification.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.saveFingerprintPrivate(saveInput(5L, 1L, "n")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许新增/保存他人指纹记录");
    }

    @Test
    @DisplayName("saveFingerprintPrivate：更新本人指纹时正常委派")
    void saveFingerprintPrivateAllowsUpdatingSelf() {
        UserFingerprint mine = fingerprint(5L, CURRENT_USER_ID, 1);
        mine.setCreateId(CURRENT_USER_ID);
        when(repository.findById(5L)).thenReturn(Optional.of(mine));
        when(repository.count(any(Specification.class))).thenReturn(0L);
        stubSaveAssignsId();

        assertThat(service.saveFingerprintPrivate(saveInput(5L, CURRENT_USER_ID, "改名后")))
                .isEqualTo(5L);
    }

    @Test
    @DisplayName("saveFingerprintPrivate：为他人新增指纹时抛出业务异常")
    void saveFingerprintPrivateThrowsWhenCreatingForOthers() {
        assertThatThrownBy(() -> service.saveFingerprintPrivate(saveInput(null, 999L, "n")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许新增/保存他人指纹记录");
    }

    @Test
    @DisplayName("saveFingerprintPrivate：为本人新增时委派给 saveFingerprint")
    void saveFingerprintPrivateDelegatesForSelf() {
        when(repository.count(any(Specification.class))).thenReturn(0L, 0L);
        stubSaveAssignsId();

        assertThat(service.saveFingerprintPrivate(saveInput(null, CURRENT_USER_ID, "我的指纹")))
                .isEqualTo(100L);
    }

    @Test
    @DisplayName("deleteByIdsPrivate：包含他人指纹时抛出业务异常")
    void deleteByIdsPrivateThrowsOnOthers() {
        when(repository.findAllById(any())).thenReturn(List.of(fingerprint(1L, 999L, 1)));

        assertThatThrownBy(() -> service.deleteByIdsPrivate(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许删除他人指纹记录");
    }

    @Test
    @DisplayName("deleteByIdsPrivate：全部为本人指纹时执行删除")
    void deleteByIdsPrivateDeletesOwn() {
        when(repository.findAllById(any()))
                .thenReturn(List.of(fingerprint(1L, CURRENT_USER_ID, 1)));

        service.deleteByIdsPrivate(List.of(1L));

        verify(repository).deleteByIdIn(List.of(1L));
    }

    @Test
    @DisplayName("deleteByIds：直接按 id 集合删除")
    void deleteByIdsDelegatesToRepository() {
        service.deleteByIds(List.of(1L, 2L));

        verify(repository).deleteByIdIn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("registerFingerprint：并发场景下二次读取缓存命中直接返回")
    void registerFingerprintUsesSecondCacheHit() {
        when(userFingerprintCacheService.getUserSeq(1L)).thenReturn(null, 88);

        RegisterFingerprintInputDto input = new RegisterFingerprintInputDto();
        input.setUserId(1L);
        input.setDeviceNo("DEV-1");
        service.registerFingerprint(input);

        verify(mqttUtils).publishMsgAsync(anyString(), any());
        verify(userFingerprintCacheService, never()).saveUserSeq(any(), any());
    }
}
