package com.dusk.module.auth.dto.user;

import lombok.Data;
import com.dusk.common.framework.excel.ExcelHeader;

/**
 * @author kefuming
 * @date 2020/5/28 10:59
 */
@Data
public class UserExcellDto {
    @ExcelHeader(value="姓名",sort=1)
    private String name;
    @ExcelHeader(value="账号",sort=2)
    private String userName;
    @ExcelHeader("电子邮件")
    private String emailAddress;
    @ExcelHeader(value="电话号码",sort=3)
    private String phoneNo;
    @ExcelHeader("创建时间")
    private String createTime;
    @ExcelHeader("角色名称")
    private String roleNames;
    @ExcelHeader("所属组织机构")
    private String orgNamePath;
    @ExcelHeader("账号类型")
    private String userType;
}
