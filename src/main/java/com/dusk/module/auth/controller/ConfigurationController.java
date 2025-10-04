package com.dusk.module.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
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
@Tag(name = "Configuration", description = "系统配置管理")
public class ConfigurationController extends CruxBaseController {
    @Autowired
    IConfigurationService configurationService;

    @AllowAnonymous
    @GetMapping("getAll")
    @Operation(summary = "获取系统参数")
    public ConfigurationDto getAll(HttpServletRequest request) {
        return configurationService.getAll(request);
    }
}
