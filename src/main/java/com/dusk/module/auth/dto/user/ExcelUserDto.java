package com.dusk.module.auth.dto.user;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author kefuming
 * @CreateTime 2023/11/2
 */
@Getter
@Setter
public class ExcelUserDto {
    @ExcelProperty(value = "人员编号")
    private String workNumber;
    @ExcelProperty(value = "姓名")
    private String name;
    @ExcelProperty(value = "手机号码")
    private String phoneNo;
    @ExcelProperty(value = "人员组织")
    private String orga;
    @ExcelProperty(value = "卡面号")
    private String accessCard;
}
