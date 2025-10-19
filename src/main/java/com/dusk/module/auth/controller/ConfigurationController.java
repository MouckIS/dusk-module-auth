package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dto.configuration.ConfigurationDto;
import com.dusk.module.auth.service.IConfigurationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kefuming
 * @date 2020-05-07 15:01
 */
@RestController
@RequestMapping("configuration")
@Api(description = "系统配置管理",tags = "Configuration")
public class ConfigurationController extends CruxBaseController {
    @Resource
    private IConfigurationService configurationService;

    @AllowAnonymous
    @GetMapping("getAll")
    @ApiOperation(value = "获取系统参数")
    public ConfigurationDto getAll(HttpServletRequest request) {
        return configurationService.getAll(request);
    }
}
