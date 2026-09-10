package com.dusk.module.auth.impl;


import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeaturePusher;
import com.dusk.module.auth.service.IFeatureRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将本模块收集到的特性定义通过 IFeatureRpcService 推送到特性缓存。
 * <p>
 * 注意：IFeatureRpcService 的 provider（FeatureRpcServiceImpl）与本类同属一个应用，属于 Dubbo 自调用。
 * 启动早期（provider 尚未导出/注册到注册中心）发起调用会报
 * “No provider available from registry ... invokers: 0”，因此触发时机放在 ApplicationReadyEvent（见 FeatureManager）
 *
 * @author kefuming
 * @date 2021-07-26 10:16
 */
@Component
@Slf4j
public class FeaturePusher implements IFeaturePusher {
    @DubboReference(check = false)
    private IFeatureRpcService featureRpcService;
    @Value("${spring.application.name}")
    private String applicationName;

    @Async
    @Override
    public void provideFeatureInfo(IFeatureDefinitionContext context) {
        try {
            List<TenantFeature> tenantFeatureList = context.getFeatures();
            featureRpcService.updateFeature(applicationName, tenantFeatureList);
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
