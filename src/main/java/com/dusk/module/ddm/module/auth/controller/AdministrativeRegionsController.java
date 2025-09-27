package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.module.auth.dto.administrativeregions.RegionsDto;
import com.dusk.module.auth.service.IAdministrativeRegionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description:
 * @author: 黄辉鸿
 * @create: 2022-04-27 09:24
 **/
@RestController
@RequestMapping("administrativeRegions")
@Api(tags = "AdministrativeRegions", description = "中国行政区域")
public class AdministrativeRegionsController {
    @Autowired
    IAdministrativeRegionsService administrativeRegionsService;

    @GetMapping("getRegions")
    @ApiOperation("获取省市区")
    public List<RegionsDto> getRegions() {
        return administrativeRegionsService.getRegions();
    }

    @GetMapping("getStreet/{id}")
    @ApiOperation("获取街道")
    public List<RegionsDto> getStreet(@PathVariable String id) {
        return administrativeRegionsService.getStreet(id);
    }
}
