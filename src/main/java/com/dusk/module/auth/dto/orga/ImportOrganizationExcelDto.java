package com.dusk.module.auth.dto.orga;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.enums.EUnitType;

/**
 * @Author kefuming
 * @CreateTime 2023/11/2
 */
@Getter
@Setter
public class ImportOrganizationExcelDto {
    @ExcelProperty(value = "所属物业组织")
    private String rootName;

    @ExcelProperty(value = "人员组织编号")
    private String code;

    @ExcelProperty(value = "人员组织名称")
    private String displayName;

    @ExcelProperty(value = "上级组织")
    private String parentOrg;

    @ExcelProperty(value = "类型")
    private String typeStr;

    @ExcelProperty(value = "备注")
    private String description;

    @ExcelIgnore
    private int deep = 1;

    @ExcelIgnore
    private EUnitType type;
}
