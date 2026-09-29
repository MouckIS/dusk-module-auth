package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.dto.dashboard.CopyModuleItemInput;
import com.dusk.module.auth.dto.dashboard.CopyModuleItemsInput;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateModule;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateModuleItem;
import com.dusk.module.auth.dto.dashboard.GetModuleInput;
import com.dusk.module.auth.dto.dashboard.ModuleDetailDto;
import com.dusk.module.auth.dto.dashboard.ModuleListDto;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;
import com.dusk.module.auth.entity.dashboard.DashboardZoneItemRef;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleItemRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardZoneItemRefRepository;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DashBoardModuleServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的数据大屏模块管理逻辑。
 *
 * <p>{@code findT}/{@code createOrUpdate} 来自同包的 {@link CreateOrUpdateService}，
 * 为 {@code protected}，本测试类同包可直接桩化，从而隔离基础设施逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashBoardModuleServiceImplTest {

    @Mock
    private IDashBoardModuleRepository moduleRepository;
    @Mock
    private IDashBoardModuleItemRepository moduleItemRepository;
    @Mock
    private IDashBoardZoneItemRefRepository zoneItemRefRepository;
    @Mock
    private JPAQueryFactory queryFactory;

    private DashBoardModuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new DashBoardModuleServiceImpl());
        ReflectionTestUtils.setField(service, "repository", moduleRepository);
        ReflectionTestUtils.setField(service, "moduleRepository", moduleRepository);
        ReflectionTestUtils.setField(service, "moduleItemRepository", moduleItemRepository);
        ReflectionTestUtils.setField(service, "zoneItemRefRepository", zoneItemRefRepository);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static DashboardModule module(Long id, String name) {
        DashboardModule m = new DashboardModule();
        m.setId(id);
        m.setName(name);
        m.setCode("c" + id);
        m.setCenterModule(false);
        return m;
    }

    private static DashboardModuleItem item(Long id, Long moduleId, String name) {
        DashboardModuleItem i = new DashboardModuleItem();
        i.setId(id);
        i.setName(name);
        i.setCode("code" + id);
        i.setModuleId(moduleId);
        i.setDetailPath("/p");
        i.setDataSource("ds");
        i.setChartType("bar");
        return i;
    }

    // ------------------------------------------------------------------
    // saveModule
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveModule：centerModule 为空时补默认值并新建")
    void saveModule_defaultCenterModule() {
        CreateOrUpdateModule input = new CreateOrUpdateModule();
        input.setName("m1");
        input.setCenterModule(null);
        when(moduleRepository.findByName("m1")).thenReturn(null);
        DashboardModule created = module(1L, "m1");
        doReturn(created).when(service).createOrUpdate(any(), any(), any(Class.class));

        assertThat(service.saveModule(input)).isSameAs(created);
        assertThat(input.getCenterModule()).isFalse();
    }

    @Test
    @DisplayName("saveModule：同名且非自身时抛业务异常")
    void saveModule_duplicateName() {
        CreateOrUpdateModule input = new CreateOrUpdateModule();
        input.setName("m1");
        input.setId(2L);
        when(moduleRepository.findByName("m1")).thenReturn(module(1L, "m1"));

        assertThatThrownBy(() -> service.saveModule(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("saveModule：同名但为自身时允许更新")
    void saveModule_sameIdAllowed() {
        CreateOrUpdateModule input = new CreateOrUpdateModule();
        input.setName("m1");
        input.setId(1L);
        when(moduleRepository.findByName("m1")).thenReturn(module(1L, "m1"));
        DashboardModule updated = module(1L, "m1");
        doReturn(updated).when(service).createOrUpdate(any(), any(), any(Class.class));

        assertThat(service.saveModule(input)).isSameAs(updated);
    }

    // ------------------------------------------------------------------
    // copyItem / copyModuleItems
    // ------------------------------------------------------------------

    @Test
    @DisplayName("copyItem：源统计项不存在抛业务异常")
    void copyItem_sourceNotFound() {
        CopyModuleItemInput input = new CopyModuleItemInput();
        input.setSourceModuleItemId(1L);
        input.setTargetModuleId(2L);
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.copyItem(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("统计项");
    }

    @Test
    @DisplayName("copyItem：目标模块不存在抛业务异常")
    void copyItem_targetNotFound() {
        CopyModuleItemInput input = new CopyModuleItemInput();
        input.setSourceModuleItemId(1L);
        input.setTargetModuleId(2L);
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 5L, "src")));
        when(moduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.copyItem(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模块");
    }

    @Test
    @DisplayName("copyItem：复制统计项到目标模块")
    void copyItem_success() {
        CopyModuleItemInput input = new CopyModuleItemInput();
        input.setSourceModuleItemId(1L);
        input.setTargetModuleId(2L);
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 5L, "src")));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module(2L, "target")));

        service.copyItem(input);

        verify(moduleItemRepository).save(any(DashboardModuleItem.class));
    }

    @Test
    @DisplayName("copyModuleItems：批量复制模块下全部统计项")
    void copyModuleItems() {
        CopyModuleItemsInput input = new CopyModuleItemsInput();
        input.setSourceModuleId(1L);
        input.setTargetModuleId(2L);
        when(moduleItemRepository.findAllByModuleId(1L))
                .thenReturn(List.of(item(1L, 1L, "a"), item(2L, 1L, "b")));

        service.copyModuleItems(input);

        verify(moduleItemRepository).saveAll(any());
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getModuleList：无条件分页查询")
    void getModuleList_noFilter() {
        JPAQuery query = mock(JPAQuery.class, org.mockito.Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        doReturn(new PageImpl<>(List.of(new ModuleListDto()))).when(service).page(any(), any());
        GetModuleInput input = new GetModuleInput();

        PagedResultDto<ModuleListDto> result = service.getModuleList(input);

        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getModuleList：按名称与编号过滤")
    void getModuleList_withFilters() {
        JPAQuery query = mock(JPAQuery.class, org.mockito.Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        doReturn(new PageImpl<>(List.of())).when(service).page(any(), any());
        GetModuleInput input = new GetModuleInput();
        input.setName("abc");
        input.setCode("001");

        assertThat(service.getModuleList(input).getItems()).isEmpty();
    }

    @Test
    @DisplayName("moduleDetail：返回模块及统计项明细")
    void moduleDetail() {
        DashboardModule m = module(1L, "m1");
        doReturn(m).when(service).findT(1L);
        when(moduleItemRepository.findAllByModuleIdOrderByCreateTime(1L))
                .thenReturn(List.of(item(9L, 1L, "i9")));

        ModuleDetailDto detail = service.moduleDetail(1L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getModuleItems()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteModule：未被引用时删除统计项与模块")
    void deleteModule_success() {
        DashboardModule m = module(1L, "m1");
        doReturn(m).when(service).findT(1L);
        when(zoneItemRefRepository.findAllByModuleId(1L)).thenReturn(List.of());

        service.deleteModule(1L);

        verify(moduleItemRepository).deleteByModuleId(1L);
        verify(moduleRepository).delete(m);
    }

    @Test
    @DisplayName("deleteModule：已被大屏使用时禁止删除")
    void deleteModule_inUse() {
        DashboardModule m = module(1L, "m1");
        doReturn(m).when(service).findT(1L);
        when(zoneItemRefRepository.findAllByModuleId(1L))
                .thenReturn(List.of(new DashboardZoneItemRef()));

        assertThatThrownBy(() -> service.deleteModule(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法删除");
    }

    @Test
    @DisplayName("removeModuleItem：未被引用时删除统计项")
    void removeModuleItem_success() {
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 2L, "i")));
        when(zoneItemRefRepository.findAllByModuleItemId(1L)).thenReturn(List.of());

        service.removeModuleItem(1L);

        verify(moduleItemRepository).delete(any(DashboardModuleItem.class));
    }

    @Test
    @DisplayName("removeModuleItem：已被使用时禁止删除")
    void removeModuleItem_inUse() {
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 2L, "i")));
        when(zoneItemRefRepository.findAllByModuleItemId(1L))
                .thenReturn(List.of(new DashboardZoneItemRef()));

        assertThatThrownBy(() -> service.removeModuleItem(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法删除");
    }

    @Test
    @DisplayName("removeModuleItem：记录不存在抛业务异常")
    void removeModuleItem_notFound() {
        when(moduleItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeModuleItem(1L)).isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 统计项保存
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveModuleItem：同模块同名校验冲突抛异常")
    void saveModuleItem_nameConflict() {
        CreateOrUpdateModuleItem input = new CreateOrUpdateModuleItem();
        input.setModuleId(1L);
        input.setName("dup");
        when(moduleItemRepository.findAllByModuleIdAndName(1L, "dup"))
                .thenReturn(List.of(item(9L, 1L, "dup")));

        assertThatThrownBy(() -> service.saveModuleItem(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在名称");
    }

    @Test
    @DisplayName("saveModuleItem：无 id 时新建统计项")
    void saveModuleItem_create() {
        CreateOrUpdateModuleItem input = new CreateOrUpdateModuleItem();
        input.setModuleId(1L);
        input.setName("new");
        when(moduleItemRepository.findAllByModuleIdAndName(1L, "new")).thenReturn(List.of());

        assertThat(service.saveModuleItem(input)).isNotNull();
        verify(moduleItemRepository).save(any(DashboardModuleItem.class));
    }

    @Test
    @DisplayName("saveModuleItem：有 id 时更新已有统计项")
    void saveModuleItem_update() {
        CreateOrUpdateModuleItem input = new CreateOrUpdateModuleItem();
        input.setId(5L);
        input.setModuleId(1L);
        input.setName("upd");
        when(moduleItemRepository.findAllByModuleIdAndName(1L, "upd")).thenReturn(List.of());
        when(moduleItemRepository.findById(5L)).thenReturn(Optional.of(item(5L, 1L, "old")));

        service.saveModuleItem(input);

        verify(moduleItemRepository).save(any(DashboardModuleItem.class));
    }

    @Test
    @DisplayName("saveModuleItem：有 id 但记录不存在抛业务异常")
    void saveModuleItem_updateNotFound() {
        CreateOrUpdateModuleItem input = new CreateOrUpdateModuleItem();
        input.setId(5L);
        input.setModuleId(1L);
        input.setName("upd");
        when(moduleItemRepository.findAllByModuleIdAndName(1L, "upd")).thenReturn(List.of());
        when(moduleItemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveModuleItem(input))
                .isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 导出 / 导入
    // ------------------------------------------------------------------

    @Test
    @DisplayName("exportModule：以 JSON 附件形式写出模块配置")
    void exportModule_success() throws IOException {
        doReturn(List.of(module(1L, "m1"))).when(service).findAll();
        when(moduleItemRepository.findAllByModuleIdOrderByCreateTime(anyLong()))
                .thenReturn(List.of(item(2L, 1L, "i")));
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ServletOutputStream servletOut = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(servletOut);

        service.exportModule(response);

        verify(response).setContentType("text/plain;charset=UTF-8");
        verify(servletOut).close();
        assertThat(out.size()).isZero();
    }

    @Test
    @DisplayName("exportModule：写出失败时包装为业务异常")
    void exportModule_ioException() throws IOException {
        doReturn(List.of(module(1L, "m1"))).when(service).findAll();
        when(moduleItemRepository.findAllByModuleIdOrderByCreateTime(anyLong()))
                .thenReturn(List.of(item(2L, 1L, "i")));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("io"));

        assertThatThrownBy(() -> service.exportModule(response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("导出模块配置失败");
    }

    @Test
    @DisplayName("importModule：配置为空抛业务异常")
    void importModule_emptyConfig() throws IOException {
        Path file = Files.createTempFile("dusk-module-empty", ".json");
        Files.write(file, "[]".getBytes(StandardCharsets.UTF_8));
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.getOriginalFilename()).thenReturn(file.toString());
        when(upload.getInputStream()).thenReturn(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service.importModule(upload, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置为空");

        Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("importModule：读取上传流失败包装为业务异常")
    void importModule_ioException() throws IOException {
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.getOriginalFilename()).thenReturn("dusk-module-io.json");
        when(upload.getInputStream()).thenThrow(new IOException("io"));

        assertThatThrownBy(() -> service.importModule(upload, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("导入模块配置失败");
    }

    @Test
    @DisplayName("importModule：格式不正确（缺少 code）抛业务异常")
    void importModule_missingCode() throws IOException {
        String json = "[{\"name\":\"m1\",\"moduleItems\":[]}]";
        Path file = Files.createTempFile("dusk-module-nocode", ".json");
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.getOriginalFilename()).thenReturn(file.toString());
        when(upload.getInputStream())
                .thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        doReturn(new ArrayList<>()).when(service).findAll();

        assertThatThrownBy(() -> service.importModule(upload, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格式不正确");

        Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("importModule：新增模块并按权限写入统计项")
    void importModule_newModuleWritesItems() throws IOException {
        String json = "[{\"name\":\"m1\",\"code\":\"c1\",\"centerModule\":false,"
                + "\"moduleItems\":[{\"code\":\"i1\",\"name\":\"item1\"}]}]";
        Path file = Files.createTempFile("dusk-module-import", ".json");
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.getOriginalFilename()).thenReturn(file.toString());
        when(upload.getInputStream())
                .thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        doReturn(new ArrayList<>()).when(service).findAll();
        doReturn(module(100L, "m1")).when(service).save(any(DashboardModule.class));
        com.dusk.module.auth.dto.dashboard.ModuleItemPermissionInput permission =
                new com.dusk.module.auth.dto.dashboard.ModuleItemPermissionInput();
        permission.setBusinessItemPermission(new ArrayList<>(List.of("i1")));

        service.importModule(upload, permission);

        verify(moduleItemRepository).saveAll(any());
        Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("importModule：已存在模块时更新模块并同步统计项")
    void importModule_updateExistingModule() throws IOException {
        String json = "[{\"name\":\"m1\",\"code\":\"c1\",\"centerModule\":false,"
                + "\"moduleItems\":[{\"code\":\"i1\",\"name\":\"item1\"}]}]";
        Path file = Files.createTempFile("dusk-module-update", ".json");
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.getOriginalFilename()).thenReturn(file.toString());
        when(upload.getInputStream())
                .thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        DashboardModuleItem repoItem = item(7L, 1L, "item1");
        repoItem.setCode("i1");
        doReturn(new ArrayList<>(List.of(module(1L, "m1")))).when(service).findAll();
        when(moduleItemRepository.findAllByModuleIdOrderByCreateTime(1L))
                .thenReturn(new ArrayList<>(List.of(repoItem)));
        when(moduleRepository.findByName("m1")).thenReturn(module(1L, "m1"));
        doReturn(module(1L, "m1")).when(service).save(any(DashboardModule.class));
        doReturn(item(7L, 1L, "item1")).when(service).saveModuleItem(any());

        service.importModule(upload, null);

        verify(service).saveModuleItem(any(CreateOrUpdateModuleItem.class));
        Files.deleteIfExists(file);
    }
}
