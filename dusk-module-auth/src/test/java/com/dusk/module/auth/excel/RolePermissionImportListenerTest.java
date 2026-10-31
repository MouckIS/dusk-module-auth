package com.dusk.module.auth.excel;

import com.dusk.module.auth.dto.role.RoleDto;
import com.dusk.module.auth.dto.role.RolePermissionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RolePermissionImportListener} 单元测试，目标：100% 分支覆盖。
 */
class RolePermissionImportListenerTest {

    private static LinkedHashMap<Integer, String> row(Integer key, String value) {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private static LinkedHashMap<Integer, String> permissionRow(String displayName, String name, String granted) {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        map.put(0, displayName);
        map.put(4, name);
        map.put(7, granted);
        return map;
    }

    @Test
    @DisplayName("invoke：按行号解析角色编码、名称、默认标识与权限列表")
    void invoke_parsesAllRows() {
        RolePermissionImportListener listener = new RolePermissionImportListener();

        listener.invoke(row(2, "R001"), null);
        listener.invoke(row(2, "管理员"), null);
        listener.invoke(row(2, "是"), null);
        listener.invoke(new LinkedHashMap<>(), null);
        listener.invoke(permissionRow("角色管理", "Pages.Role", "是"), null);
        listener.invoke(permissionRow("用户管理", "Pages.User", "否"), null);
        listener.doAfterAllAnalysed(null);

        RoleDto roleDto = listener.getRoleDto();
        assertThat(roleDto.getRoleCode()).isEqualTo("R001");
        assertThat(roleDto.getRoleName()).isEqualTo("管理员");
        assertThat(roleDto.isDefault()).isTrue();
        assertThat(roleDto.getPermissionList()).hasSize(2);
        RolePermissionDto first = roleDto.getPermissionList().get(0);
        assertThat(first.getDisplayName()).isEqualTo("角色管理");
        assertThat(first.getName()).isEqualTo("Pages.Role");
        assertThat(first.isGranted()).isTrue();
        RolePermissionDto second = roleDto.getPermissionList().get(1);
        assertThat(second.getName()).isEqualTo("Pages.User");
        assertThat(second.isGranted()).isFalse();
    }

    @Test
    @DisplayName("invoke：默认标识为否时解析为 false")
    void invoke_defaultFalse() {
        RolePermissionImportListener listener = new RolePermissionImportListener();

        listener.invoke(row(2, "否"), null);

        assertThat(listener.getRoleDto().isDefault()).isFalse();
    }

    @Test
    @DisplayName("invoke：默认标识为空时解析为 false")
    void invoke_defaultNull() {
        RolePermissionImportListener listener = new RolePermissionImportListener();

        listener.invoke(new LinkedHashMap<>(), null);

        assertThat(listener.getRoleDto().isDefault()).isFalse();
    }
}
