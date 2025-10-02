package com.dusk.module.auth.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.dto.orga.ImportOrganizationExcelDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @CreateTime 2023/11/2
 */
@Getter
public class ExcelDataListener extends AnalysisEventListener<ImportOrganizationExcelDto> {
    private final Map<String, ImportOrganizationExcelDto> orgMap = new HashMap<>();
    private final Map<String, String> typeMap = new HashMap<>();
    private Map<Integer, List<ImportOrganizationExcelDto>> map;
    private String rootUnitName = "";

    @Override
    public void invoke(ImportOrganizationExcelDto data, AnalysisContext context) {
        orgMap.put(data.getDisplayName(), data);
        rootUnitName = data.getRootName();
        if (StrUtil.isNotBlank(data.getTypeStr())) {
            typeMap.put(data.getDisplayName(), data.getTypeStr());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 所有数据解析完成后的操作
        orgMap.forEach((k, v) -> {
            setType(v);
            setDeep(v);
        });
        map = orgMap.values()
                .stream()
                .collect(Collectors.groupingBy(ImportOrganizationExcelDto::getDeep));
    }

    private void setType(ImportOrganizationExcelDto dto) {
        if (typeMap.containsKey(dto.getParentOrg())) {
            String type = typeMap.get(dto.getParentOrg());
            EUnitType unitType = EUnitType.valueOf(type);
            typeMap.put(dto.getDisplayName(), type);
            dto.setTypeStr(type);
            dto.setType(unitType);
        } else {
            ImportOrganizationExcelDto parentDto = orgMap.get(dto.getParentOrg());
            if (parentDto != null) {
                setType(parentDto);
            }
        }
    }

    private void setDeep(ImportOrganizationExcelDto dto) {
        int deep = 1;
        ImportOrganizationExcelDto tmp = dto;
        while (StrUtil.isNotBlank(tmp.getParentOrg())) {
            tmp = orgMap.get(tmp.getParentOrg());
            deep++;
        }
        dto.setDeep(deep);
    }
}
