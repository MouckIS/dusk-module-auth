package com.dusk.module.auth.common.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-12-25 11:08
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(WxMaProperties.class)
public class WxMaConfiguration {
    private final WxMaProperties properties;

    private static Map<String, WxMaService> maServices = new HashMap<>();

    @Autowired
    public WxMaConfiguration(WxMaProperties properties) {
        this.properties = properties;
    }

    /**
     * 找到指定的微信小程序配置
     * @param appid
     * @return
     */
    public static WxMaService getMaService(String appid) {
        WxMaService wxService = maServices.get(appid);
        if (wxService == null) {
            throw new BusinessException(String.format("未找到对应appid=[%s]的配置，请核实！", appid));
        }
        return wxService;
    }

    @PostConstruct
    public void init() {
        List<WxMaProperties.Config> configs = this.properties.getConfigs();
        if (configs != null) {
            maServices = configs.stream()
                    .map(a -> {
                        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
                        config.setAppid(a.getAppid());
                        config.setSecret(a.getSecret());
                        config.setToken(a.getToken());
                        config.setAesKey(a.getAesKey());
                        config.setMsgDataFormat(a.getMsgDataFormat());
                        WxMaService service = new WxMaServiceImpl();
                        service.setWxMaConfig(config);
                        return service;
                    }).collect(Collectors.toMap(s -> s.getWxMaConfig().getAppid(), a -> a));
        }
    }
}
