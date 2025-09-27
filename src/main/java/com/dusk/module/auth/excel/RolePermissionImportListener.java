package com.dusk.module.auth.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.module.auth.dto.role.RolePermissionDto;
import com.dusk.module.auth.dto.role.RoleDto;

import java.util.LinkedHashMap;

/**
 * @author: pengmengjiang
 * @date: 2021/2/5 15:58
 */
public class RolePermissionImportListener extends AnalysisEventListener<LinkedHashMap<Integer, String>> {

    private int currentRow;
    @Getter
    private RoleDto roleDto;

    public RolePermissionImportListener() {
        roleDto = new RoleDto();
    }

    @Override
    public void invoke(LinkedHashMap<Integer, String> data, AnalysisContext context) {
        switch (currentRow) {
            case 0 -> {
                readRoleCode(data);
            }
            case 1 -> {
                readRoleName(data);
            }
            case 2 -> {
                readRoleDefault(data);
            }
            case 3 -> {
            }
            default -> {
                readRolePermission(data);
            }
        }
        currentRow++;
    }

    void readRoleCode(LinkedHashMap<Integer, String> data) {
        roleDto.setRoleCode(data.get(2));
    }

    void readRoleName(LinkedHashMap<Integer, String> data) {
        roleDto.setRoleName(data.get(2));
    }

    void readRoleDefault(LinkedHashMap<Integer, String> data) {
        String value = data.get(2);
        if (StringUtils.equals("是", value)) {
            roleDto.setDefault(true);
        } else {
            roleDto.setDefault(false);
        }
    }

    void readRolePermission(LinkedHashMap<Integer, String> data) {
        RolePermissionDto permission = new RolePermissionDto();
        permission.setDisplayName(data.get(0));
        permission.setName(data.get(4));
        permission.setGranted(StringUtils.equals("是", data.get(7)));
        roleDto.addPermission(permission);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }
}
