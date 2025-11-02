package com.dusk.module.auth.impl;


import com.dusk.common.mqs.config.AppConfig;
import com.dusk.module.auth.common.util.DubboCustomUtils;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeaturePusher;
import com.dusk.module.auth.service.IFeatureRpcService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-07-26 10:16
 */
@Component
@Slf4j
public class FeaturePusher implements IFeaturePusher {
    @Resource
    private AppConfig appConfig;

    @DubboReference
    IFeatureRpcService featureRpcService;

    @Async
    @Override
    public void provideFeatureInfo(IFeatureDefinitionContext context) {
        try {
            List<TenantFeature> tenantFeatureList = context.getFeatures();
            featureRpcService.updateFeature(appConfig.getApplicationName(), tenantFeatureList);
        } catch (Exception ex) {
            log.error("推送特性列表异常：{}", ex.getMessage());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException iex) {
                log.warn("终止推送特性列表！");
            }
            this.provideFeatureInfo(context);
        }
    }
}
