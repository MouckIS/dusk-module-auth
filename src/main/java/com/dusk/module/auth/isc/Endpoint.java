package com.dusk.module.auth.isc;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author kefuming
 * @date 2021-11-19 9:17
 */
@Component
@Slf4j
public class Endpoint {
    @Autowired
    private IscSyncConfig config;
    @Autowired
    private IscService iscService;

    @PostConstruct
    public void init() {
        if(config.isEnable()){
            if(StrUtil.isBlank(config.getEndpoint())){
                log.error("isc endpoint 未配置！");
                return;
            }
            javax.xml.ws.Endpoint.publish(config.getEndpoint(), iscService);
            log.info("isc endpoint 已发布：" + config.getEndpoint());
        }
    }
}
