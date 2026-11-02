package com.dusk.module.auth.excel;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.metadata.dto.ui.CheckBox;
import com.dusk.module.metadata.dto.ui.ComboBox;
import com.dusk.module.metadata.dto.ui.Item;
import com.dusk.module.metadata.dto.ui.ItemSource;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link EditionExcelExportWriter} 单元测试，目标：>=95% 分支覆盖（含内部类）。
 *
 * <p>POI 5.5.1 运行期需要 commons-io 2.7+ 的 {@code UnsynchronizedByteArrayOutputStream}，
 * 而当前依赖解析到 commons-io 2.5，直接 new XSSFWorkbook() 会抛 NoClassDefFoundError。
 * 因此这里用 mock 的 {@link Workbook}/{@link Sheet} 替代真实工作簿（仅测试代码，不修改依赖）。</p>
 */
class EditionExcelExportWriterTest {

    private final Map<Integer, Row> rows = new HashMap<>();
    private final Map<String, String> cellValues = new LinkedHashMap<>();
    private final List<DataValidation> validations = new ArrayList<>();
    private final AtomicInteger rowCount = new AtomicInteger();
    private Sheet sheet;
    private Workbook workbook;
    private DataValidationHelper validationHelper;
    private DataValidationConstraint constraint;
    private DataValidation dataValidation;

    /**
     * 绕过构造器（构造器会创建依赖 commons-io 2.7+ 的 XSSFWorkbook）。
     */
    private static EditionExcelExportWriter allocateWriter() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            return (EditionExcelExportWriter) unsafeClass.getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, EditionExcelExportWriter.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SubscribableEdition edition() {
        SubscribableEdition edition = new SubscribableEdition();
        edition.setDisplayName("专业版");
        return edition;
    }

    private static TenantFeature feature(String name, String parentName, String displayName,
                                         com.dusk.module.metadata.dto.ui.InputType inputType, String value) {
        TenantFeature feature = new TenantFeature();
        feature.setName(name);
        feature.setParentName(parentName);
        feature.setDisplayName(displayName);
        feature.setFeatureValue(value);
        feature.setInputType(inputType);
        return feature;
    }

    private static ComboBox comboBox(String value, String displayText) {
        return new ComboBox(List.of(new Item(value, displayText)));
    }

    @BeforeEach
    void setUp() {
        workbook = mock(Workbook.class);
        sheet = mock(Sheet.class);
        validationHelper = mock(DataValidationHelper.class);
        constraint = mock(DataValidationConstraint.class);
        dataValidation = mock(DataValidation.class);
        rows.clear();
        cellValues.clear();
        validations.clear();
        rowCount.set(0);

        when(sheet.getDataValidationHelper()).thenReturn(validationHelper);
        when(validationHelper.createExplicitListConstraint(any(String[].class))).thenReturn(constraint);
        when(validationHelper.createValidation(any(DataValidationConstraint.class), any())).thenReturn(dataValidation);
        when(sheet.getPhysicalNumberOfRows()).thenAnswer(invocation -> rowCount.get());
        when(sheet.getColumnWidth(anyInt())).thenReturn(100);
        when(sheet.createRow(anyInt())).thenAnswer(invocation -> createRow(invocation.getArgument(0, Integer.class)));
        when(sheet.getRow(anyInt())).thenAnswer(invocation -> rows.get(invocation.getArgument(0, Integer.class)));
        doAnswer(invocation -> validations.add(invocation.getArgument(0)))
                .when(sheet).addValidationData(any(DataValidation.class));
    }

    private Row createRow(int rowIndex) {
        Row row = mock(Row.class);
        when(row.getRowNum()).thenReturn(rowIndex);
        when(row.getLastCellNum()).thenReturn((short) 8);
        when(row.createCell(anyInt())).thenAnswer(invocation -> createCell(rowIndex, invocation.getArgument(0, Integer.class)));
        rows.put(rowIndex, row);
        rowCount.incrementAndGet();
        return row;
    }

    private Cell createCell(int rowIndex, int columnIndex) {
        Cell cell = mock(Cell.class);
        when(cell.getRowIndex()).thenReturn(rowIndex);
        when(cell.getColumnIndex()).thenReturn(columnIndex);
        doAnswer(invocation -> {
            cellValues.put(rowIndex + ":" + columnIndex, invocation.getArgument(0));
            return null;
        }).when(cell).setCellValue(anyString());
        return cell;
    }

    private EditionExcelExportWriter newWriter() {
        EditionExcelExportWriter writer = allocateWriter();
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "workbook", workbook);
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "sheet", sheet);
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "validationHelper", validationHelper);
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "edition", edition());
        return writer;
    }

    @Test
    @DisplayName("convertPermission：按父子关系组装树、标记授权并计算深度")
    void convertPermission_buildsTreeAndDepth() {
        Permission root = new Permission("P1", "权限1", MultiTenancySides.All);
        Permission child = root.createChildPermission("P2", "权限2");
        Permission leaf1 = child.createChildPermission("P3", "权限3");
        Permission leaf2 = child.createChildPermission("P4", "权限4");
        Permission root2 = new Permission("P5", "权限5", MultiTenancySides.All);

        EditionExcelExportWriter writer = newWriter();
        writer.setDefinitionPermissions(List.of(root, child, leaf1, leaf2, root2));
        writer.setEditionPermissions(new ArrayList<>(List.of("P2", "P4")));
        writer.setEditionFeatures(List.of());

        List<EditionExcelExportWriter.InnerPermission> roots = writer.convertPermission();

        assertThat(roots).extracting(EditionExcelExportWriter.InnerPermission::getName)
                .containsExactly("P1", "P5");
        EditionExcelExportWriter.InnerPermission p1 = roots.get(0);
        EditionExcelExportWriter.InnerPermission p2 = p1.getChildren().get(0);
        assertThat(p2.getChildren()).extracting(EditionExcelExportWriter.InnerPermission::getName)
                .containsExactly("P3", "P4");
        assertThat(p1.getFullPathName()).isEqualTo("权限1");
        assertThat(p2.getFullPathName()).isEqualTo("权限1->权限2");
        assertThat(p2.getChildren().get(0).getFullPathName()).isEqualTo("权限1->权限2->权限3");
        assertThat(p1.isGranted()).isFalse();
        assertThat(p2.isGranted()).isTrue();
        assertThat(p2.getChildren()).extracting(EditionExcelExportWriter.InnerPermission::isGranted)
                .containsExactly(false, true);
        assertThat(p1.getDepth()).isEqualTo(2);
        assertThat(p2.getDepth()).isEqualTo(1);
        assertThat(writer.maxPermissionDepth).isEqualTo(2);
        assertThat(writer.hasWrite).isFalse();
    }

    @Test
    @DisplayName("convertFeature：按 parentName 组装特性树并计算全路径")
    void convertFeature_buildsTree() {
        TenantFeature root = feature("F1", null, "特性1", null, "v1");
        TenantFeature child = feature("F2", "F1", "特性2", null, "v2");
        TenantFeature grandChild = feature("F3", "F2", "特性3", null, "v3");
        TenantFeature anotherRoot = feature("F4", null, "特性4", null, "v4");

        EditionExcelExportWriter writer = newWriter();
        writer.setEditionFeatures(List.of(root, child, grandChild, anotherRoot));

        List<EditionExcelExportWriter.InnerFeature> roots = writer.convertFeature();

        assertThat(roots).extracting(EditionExcelExportWriter.InnerFeature::getName)
                .containsExactly("F1", "F4");
        EditionExcelExportWriter.InnerFeature f1 = roots.get(0);
        EditionExcelExportWriter.InnerFeature f2 = f1.getChildren().get(0);
        assertThat(f2.getChildren()).extracting(EditionExcelExportWriter.InnerFeature::getName)
                .containsExactly("F3");
        assertThat(f1.getFullPathName()).isEqualTo("特性1");
        assertThat(f2.getFullPathName()).isEqualTo("特性1->特性2");
        assertThat(f2.getChildren().get(0).getFullPathName()).isEqualTo("特性1->特性2->特性3");
    }

    private EditionExcelExportWriter fullWriter() {
        Permission root = new Permission("P1", "权限1", MultiTenancySides.All);
        Permission child = root.createChildPermission("P2", "权限2");
        Permission leaf1 = child.createChildPermission("P3", "权限3");
        Permission leaf2 = child.createChildPermission("P4", "权限4");
        Permission root2 = new Permission("P5", "权限5", MultiTenancySides.All);

        TenantFeature f1 = feature("F1", null, "特性1", comboBox("v2", "选项2"), "v2");
        TenantFeature f2 = feature("F2", "F1", "特性2", new CheckBox(), "true");
        TenantFeature f3 = feature("F3", null, "特性3", null, "raw");
        TenantFeature f4 = feature("F4", null, "特性4", comboBox("v1", "选项1"), "unknown");
        TenantFeature f5 = feature("F5", "F1", "特性5", null, "raw5");
        TenantFeature f6 = feature("F6", "F5", "特性6", null, "raw6");
        TenantFeature f7 = feature("F7", null, "特性7", null, "raw7");
        TenantFeature f8 = feature("F8", null, "特性8", null, "raw8");

        EditionExcelExportWriter writer = newWriter();
        writer.setDefinitionPermissions(List.of(root, child, leaf1, leaf2, root2));
        writer.setEditionPermissions(new ArrayList<>(List.of("P2", "P4")));
        writer.setEditionFeatures(List.of(f1, f2, f3, f4, f5, f6, f7, f8));
        return writer;
    }

    @Test
    @DisplayName("write：写出权限树、特性下拉/复选校验与单元格内容")
    void write_fullWorkbook() {
        EditionExcelExportWriter writer = fullWriter();

        writer.write();

        assertThat(cellValues).containsEntry("0:1", "版本名称").containsEntry("0:2", "专业版");
        assertThat(cellValues).containsEntry("2:0", "P1").containsEntry("2:1", "权限1").containsEntry("2:2", "否");
        assertThat(cellValues).containsEntry("3:1", "权限1->权限2").containsEntry("3:2", "是");
        assertThat(cellValues).containsEntry("5:2", "是");
        assertThat(cellValues).containsEntry("6:0", "P5");

        assertThat(cellValues).containsEntry("1:6", "特性").containsEntry("1:7", "特性值");
        assertThat(cellValues).containsEntry("2:5", "F1").containsEntry("2:6", "特性1")
                .containsEntry("2:7", "v2->选项2");
        assertThat(cellValues).containsEntry("3:5", "F2").containsEntry("3:7", "true");
        assertThat(cellValues).containsEntry("4:5", "F5");
        assertThat(cellValues).containsEntry("5:5", "F6");
        assertThat(cellValues).containsEntry("6:5", "F3");
        assertThat(cellValues).containsEntry("7:5", "F4").containsEntry("7:7", "unknown");
        assertThat(cellValues).containsEntry("8:5", "F7");
        assertThat(cellValues).containsEntry("9:5", "F8");

        // 权限列下拉 + 2 个 ComboBox + 1 个 CheckBox
        assertThat(validations).hasSize(4);
    }

    @Test
    @DisplayName("write：无权限无特性时只写表头且不添加数据校验")
    void write_emptyPermissionsAndFeatures() {
        EditionExcelExportWriter writer = newWriter();
        writer.setDefinitionPermissions(List.of());
        writer.setEditionPermissions(new ArrayList<>());
        writer.setEditionFeatures(List.of());

        writer.write();

        assertThat(rows).containsOnlyKeys(0, 1);
        assertThat(validations).isEmpty();
        assertThat(cellValues).containsEntry("1:1", "权限名");
    }

    @Test
    @DisplayName("write：已写过后再次调用直接返回")
    void write_whenAlreadyWritten() {
        EditionExcelExportWriter writer = newWriter();
        writer.hasWrite = true;

        writer.write();

        verifyNoInteractions(sheet);
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("write：已存在最大权限深度时保留原值")
    void write_maxPermissionDepthAlreadySet() {
        EditionExcelExportWriter writer = fullWriter();
        writer.maxPermissionDepth = 3;

        writer.write();

        assertThat(writer.maxPermissionDepth).isEqualTo(3);
        assertThat(rows).hasSizeGreaterThan(2);
    }

    @Test
    @DisplayName("InnerPermission/InnerFeature：默认构造为空子列表，addChild 可追加")
    void innerTypes_basicBehavior() {
        EditionExcelExportWriter.InnerPermission permission = new EditionExcelExportWriter.InnerPermission();
        EditionExcelExportWriter.InnerPermission child = new EditionExcelExportWriter.InnerPermission();
        child.setDisplayName("子");
        permission.addChild(child);

        assertThat(permission.getChildren()).containsExactly(child);

        EditionExcelExportWriter.InnerFeature feature = new EditionExcelExportWriter.InnerFeature();
        EditionExcelExportWriter.InnerFeature childFeature = new EditionExcelExportWriter.InnerFeature();
        childFeature.setDisplayName("子特性");
        feature.addChild(childFeature);

        assertThat(feature.getChildren()).containsExactly(childFeature);
        ItemSource itemSource = new ItemSource(List.of(new Item("v", "d")));
        assertThat(itemSource.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("write：特性行超过已有行数时创建新行")
    void write_createsRowsWhenFeaturesOutnumberRows() {
        EditionExcelExportWriter writer = newWriter();
        writer.setDefinitionPermissions(List.of());
        writer.setEditionPermissions(new ArrayList<>());
        writer.setEditionFeatures(List.of(
                feature("F1", null, "特性1", null, "v1"),
                feature("F2", null, "特性2", null, "v2"),
                feature("F3", null, "特性3", null, "v3")));

        writer.write();

        assertThat(cellValues).containsEntry("2:5", "F1")
                .containsEntry("3:5", "F2")
                .containsEntry("4:5", "F3");
        assertThat(rows).containsKeys(0, 1, 2, 3, 4);
        // 空特性表不添加校验
        assertThat(validations).isEmpty();
    }

    @Test
    @DisplayName("write：样式列宽调整不影响已写内容（覆盖 autoSizeColumn 循环）")
    void write_autosizesColumns() {
        EditionExcelExportWriter writer = fullWriter();

        writer.write();

        org.mockito.Mockito.verify(sheet, org.mockito.Mockito.times(8))
                .autoSizeColumn(anyInt(), org.mockito.ArgumentMatchers.eq(true));
        org.mockito.Mockito.verify(sheet).setColumnHidden(0, true);
        org.mockito.Mockito.verify(sheet).setColumnHidden(EditionExcelExportWriter.FEATURE_START_INDEX, true);
    }
}
