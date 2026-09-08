package com.dusk.module.auth.impl;


import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.service.IFeatureRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author duanxiaokang
 * @date 2020/5/29 13:50
 */
@Service
@Slf4j
public class FeatureChecker implements IFeatureChecker {
    @DubboReference
    private IFeatureRpcService featureRpcService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public String getValue(String name) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return "";
        }
        return getValue(tenantId, name);
    }

    @Override
    public String getValue(Long tenantId, String name) {
        return this.getFeatureValue(tenantId, name);
    }

    @Override
    public boolean isEnabled(String name) {
        // TODO: 2020/5/29 宿主是否所有特性可用
        return "true".equalsIgnoreCase(getValue(name));
    }

    @Override
    public boolean isEnabled(Long tenantId, String name) {
        return "true".equalsIgnoreCase(getValue(tenantId, name));
    }


    private String getFeatureValue(Long tenantId, String name) {
        return featureRpcService.getValue(applicationName, tenantId, name);
    }
}
