package com.dusk.module.auth.common.util;

import lombok.experimental.UtilityClass;
import org.apache.dubbo.registry.support.ConsumerInvokerWrapper;
import org.apache.dubbo.registry.support.ProviderConsumerRegTable;

import java.util.Set;

/**
 * @author kefuming
 * @date 2021-07-16 16:45
 */
@UtilityClass
public class DubboCustomUtils {
    public boolean isValidRpcService(String serviceUniqueName) {
        boolean result = false;

        Set<ConsumerInvokerWrapper> consumerInvokerSet = ProviderConsumerRegTable.getConsumerInvoker(serviceUniqueName);
        if (consumerInvokerSet.isEmpty()) {
            return false;
        }

        for (ConsumerInvokerWrapper invoker : consumerInvokerSet) {
            if (invoker.isAvailable()) {
                result = true;
                break;
            }
        }

        return result;
    }
}