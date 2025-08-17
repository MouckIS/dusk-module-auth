package com.dusk.module.auth.dto.weixin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2023/11/23
 */
@NoArgsConstructor
@Data
public class WxCpUserAddressBook {

    private Integer errcode;
    private String errmsg;
    private String userid;
    private String name;
    private List<Integer> department;
    private Integer status;
    private Integer isleader;
    private Integer enable;
    private Integer hide_mobile;
    private List<Integer> order;
    private Integer main_department;
    private String alias;
    private List<Integer> is_leader_in_dept;
    private List<?> direct_leader;
}
