package com.dusk.module.auth.listener;

import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.dto.orga.ImportOrganizationExcelDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExcelDataListener} 单元测试，目标：100% 分支覆盖。
 */
class ExcelDataListenerTest {

    private static ImportOrganizationExcelDto dto(String displayName, String parentOrg, String typeStr, String rootName) {
        ImportOrganizationExcelDto dto = new ImportOrganizationExcelDto();
        dto.setDisplayName(displayName);
        dto.setParentOrg(parentOrg);
        dto.setTypeStr(typeStr);
        dto.setRootName(rootName);
        return dto;
    }

    @Test
    @DisplayName("invoke + doAfterAllAnalysed：继承父级类型并按深度分组")
    void invoke_and_doAfterAllAnalysed() {
        ExcelDataListener listener = new ExcelDataListener();
        ImportOrganizationExcelDto root = dto("Root", null, "Inner", "物业A");
        ImportOrganizationExcelDto child = dto("C1", "Root", "", "物业A");
        ImportOrganizationExcelDto grandChild = dto("C2", "C1", null, "物业A");
        ImportOrganizationExcelDto external = dto("C3", "Root", "External", "物业A");

        listener.invoke(root, null);
        listener.invoke(child, null);
        listener.invoke(grandChild, null);
        listener.invoke(external, null);

        assertThat(listener.getTypeMap())
                .containsEntry("Root", "Inner")
                .containsEntry("C3", "External")
                .doesNotContainKeys("C1", "C2");

        listener.doAfterAllAnalysed(null);

        assertThat(listener.getRootUnitName()).isEqualTo("物业A");
        assertThat(listener.getOrgMap()).hasSize(4);

        assertThat(root.getDeep()).isEqualTo(1);
        assertThat(child.getDeep()).isEqualTo(2);
        assertThat(grandChild.getDeep()).isEqualTo(3);
        assertThat(child.getType()).isEqualTo(EUnitType.Inner);
        // 子节点类型继承父节点（分析阶段会覆盖 invoke 阶段声明的类型）
        assertThat(external.getType()).isEqualTo(EUnitType.Inner);

        assertThat(listener.getMap()).containsOnlyKeys(1, 2, 3);
        assertThat(listener.getMap().get(1)).extracting(ImportOrganizationExcelDto::getDisplayName)
                .containsExactly("Root");
        assertThat(listener.getMap().get(2)).extracting(ImportOrganizationExcelDto::getDisplayName)
                .containsExactlyInAnyOrder("C1", "C3");
        assertThat(listener.getMap().get(3)).extracting(ImportOrganizationExcelDto::getDisplayName)
                .containsExactly("C2");
    }

    @Test
    @DisplayName("setType：父级不在组织列表且类型未知时保持未设置")
    void setType_parentMissing() {
        ExcelDataListener listener = new ExcelDataListener();
        listener.invoke(dto("Root", null, "Inner", "物业A"), null);
        ImportOrganizationExcelDto orphan = dto("Orphan", "Ghost", null, "物业A");

        ReflectionTestUtils.invokeMethod(listener, "setType", orphan);

        assertThat(orphan.getType()).isNull();
        assertThat(orphan.getTypeStr()).isNull();
    }

    @Test
    @DisplayName("invoke：空类型字符串不写入类型映射")
    void invoke_blankTypeNotCached() {
        ExcelDataListener listener = new ExcelDataListener();

        listener.invoke(dto("A", null, "   ", "root"), null);

        assertThat(listener.getTypeMap()).isEmpty();
        assertThat(listener.getOrgMap()).containsKey("A");
    }

    @Test
    @DisplayName("getOrgUsers 分组：空数据时 map 为空分组")
    void doAfterAllAnalysed_empty() {
        ExcelDataListener listener = new ExcelDataListener();

        listener.doAfterAllAnalysed(null);

        assertThat(listener.getMap()).isEmpty();
        assertThat(listener.getRootUnitName()).isEmpty();
    }

    @Test
    @DisplayName("setDeep：父链完整时按层级累加")
    void setDeep_countsHierarchy() {
        ExcelDataListener listener = new ExcelDataListener();
        ImportOrganizationExcelDto a = dto("A", null, "Inner", "root");
        ImportOrganizationExcelDto b = dto("B", "A", null, "root");
        listener.invoke(a, null);
        listener.invoke(b, null);
        listener.doAfterAllAnalysed(null);

        assertThat(a.getDeep()).isEqualTo(1);
        assertThat(b.getDeep()).isEqualTo(2);
        assertThat(listener.getMap().get(2)).hasSize(1);
        assertThat(listener.getMap().get(2).get(0).getDisplayName()).isEqualTo("B");
        assertThat(listener.getMap().values().stream().flatMap(List::stream).count()).isEqualTo(2);
    }
}
