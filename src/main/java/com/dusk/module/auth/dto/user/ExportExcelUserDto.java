package com.dusk.module.auth.dto.user;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : caiwenjun
 * @date : 2023/11/4 10:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@HeadRowHeight(15)
@ColumnWidth(20)
public class ExportExcelUserDto implements Serializable {

    @ExcelProperty(index = 0, value = "人员编号")
    private String workNumber;
    @ExcelProperty(index = 1, value = "姓名")
    private String name;
    @ExcelProperty(index = 2, value = "手机号码")
    private String phoneNo;
    @ExcelProperty(index = 3, value = "人员组织")
    private String orga;
    @ExcelProperty(index = 4, value = "卡面号")
    private String accessCard;

}
