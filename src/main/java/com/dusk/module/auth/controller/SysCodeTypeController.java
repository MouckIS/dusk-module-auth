package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;
import com.dusk.module.auth.service.ISysCodeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统常量配置类型管理
 */
@RestController
@RequestMapping("/sysCodeType")
@Api(tags = "sysCodeType", description = "系统常量类型管理")
public class SysCodeTypeController extends CruxBaseController {
    @Autowired
    ISysCodeTypeService sysCodeTypeService;

    @ApiOperation("查询配置类型定义")
    @GetMapping("/list")
    public List<SysCodeTypeDto> list() {
        return sysCodeTypeService.list();
    }
}
