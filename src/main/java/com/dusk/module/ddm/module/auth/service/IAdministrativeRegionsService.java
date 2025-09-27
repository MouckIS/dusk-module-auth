package com.dusk.module.ddm.module.auth.service;

import com.dusk.module.auth.dto.administrativeregions.RegionsDto;

import java.util.List;

/**
 * @description:
 * @author: 黄辉鸿
 * @create: 2022-04-27 09:26
 **/
public interface IAdministrativeRegionsService {
    List<RegionsDto> getRegions();

    List<RegionsDto> getStreet(String id);
}
