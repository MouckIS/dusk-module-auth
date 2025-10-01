package com.dusk.module.auth.impl;


import com.dusk.common.mqs.config.AppConfig;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeaturePusher;
import com.dusk.module.auth.service.IFeatureRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-07-26 10:16
 */
@Component
@Slf4j
public class FeaturePusher implements IFeaturePusher {
    @Autowired
    AppConfig appConfig;

    @Reference
    IFeatureRpcService featureService;

    @Async
    @Override
    public void provideFeatureInfo(IFeatureDefinitionContext context) {
        try {
            featureService.updateFeature(appConfig.getApplicationName(), context.getFeatures());
        } catch (Exception ex) {
            log.error("推送特性列表异常：" + ex.getMessage());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException iex) {
                log.warn("终止推送特性列表！");
            }
            this.provideFeatureInfo(context);
        }
    }
}
