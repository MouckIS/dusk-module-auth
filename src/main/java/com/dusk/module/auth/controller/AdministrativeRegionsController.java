package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.administrativeregions.RegionsDto;
import com.dusk.module.auth.service.IAdministrativeRegionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
@Tag(name = "AdministrativeRegions", description = "中国行政区域")
public class AdministrativeRegionsController {
    @Resource
    private IAdministrativeRegionsService administrativeRegionsService;

    @GetMapping("getRegions")
    @Operation(summary = "获取省市区")
    public List<RegionsDto> getRegions() {
        return administrativeRegionsService.getRegions();
    }

    @GetMapping("getStreet/{id}")
    @Operation(summary = "获取街道")
    public List<RegionsDto> getStreet(@PathVariable String id) {
        return administrativeRegionsService.getStreet(id);
    }
}
