package com.dusk.module.auth.dto.administrativeregions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @description:
 * @author: 黄辉鸿
 * @create: 2022-04-27 11:41
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegionsDto {
    private Integer id;
    private String na;
    private Integer pId;
    private Boolean hc = false;
}
