package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.AllowAnonymous;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.module.auth.dto.configuration.ConfigurationDto;
import com.dusk.module.auth.service.IConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author kefuming
 * @date 2020-05-07 15:01
 */
@RestController
@RequestMapping("configuration")
@Api(description = "系统配置管理",tags = "Configuration")
public class ConfigurationController extends CruxBaseController {
    @Autowired
    IConfigurationService configurationService;

    @AllowAnonymous
    @GetMapping("getAll")
    @ApiOperation(value = "获取系统参数")
    public ConfigurationDto getAll(HttpServletRequest request) {
        return configurationService.getAll(request);
    }
}
