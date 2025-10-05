package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.configuration.ConfigurationDto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author kefuming
 * @date 2020-05-07 15:29
 */
public interface IConfigurationService {
    ConfigurationDto getAll(HttpServletRequest request);
}
