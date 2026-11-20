package com.dusk.module.auth.impl;


import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeaturePusher;
import com.dusk.module.auth.service.IFeatureRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired
    IFeatureRpcService featureRpcService;
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 推送特性清单。
     *
     * <p>单体化前 {@code featureRpcService} 是跨进程 Dubbo 引用，失败后原实现用
     * {@code Thread.sleep(10000)} + 自递归无限重试；现在是同进程内的
     * {@code FeatureRpcServiceImpl} 本地 bean，失败通常意味着真正的逻辑错误，
     * 无限递归重试只会耗尽 {@code @Async} 线程池，因此改为单次调用、异常仅记日志。</p>
     */
    @Async
    @Override
    public void provideFeatureInfo(IFeatureDefinitionContext context) {
        try {
            List<TenantFeature> tenantFeatureList = context.getFeatures();
            featureRpcService.updateFeature(applicationName, tenantFeatureList);
        } catch (Exception ex) {
            log.error("推送特性列表异常：{}", ex.getMessage(), ex);
        }
    }
}
