package com.dusk.module.auth.common.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author kefuming
 * @date 2021-07-16 16:45
 */
@UtilityClass
public class DubboCustomUtils {
    private static final Logger log = Logger.getLogger(DubboCustomUtils.class.getName());

    /**
     * Dubbo 3.x：尝试使用 ApplicationModel / ConsumerModel 的反射访问
     * @param serviceUniqueName 微服务名称
     * @return 该微服务是否有可用的提供者
     */
    public boolean isValidRpcService(String serviceUniqueName) {
        try {
            Class<?> appModelCls = Class.forName("org.apache.dubbo.rpc.model.ApplicationModel");
            // 尝试若干可能的静态方法名以获取 ConsumerModel 或 ConsumerModel 列表
            String[] staticCandidates = new String[]{"getInstance", "getApplicationModel", "getModel"};
            Object appModel = null;
            for (String mName : staticCandidates) {
                try {
                    Method m = appModelCls.getMethod(mName);
                    appModel = m.invoke(null);
                    if (appModel != null) break;
                } catch (NoSuchMethodException ignored) {}
            }

            // 如果没有拿到 appModel，也尝试直接使用静态方法 getAllConsumerModels/getConsumerModel
            // 先尝试直接静态方法获取单个 consumerModel
            String[] staticConsumerMethods = new String[]{"getConsumerModel", "getConsumer"};
            for (String mName : staticConsumerMethods) {
                try {
                    Method m = appModelCls.getMethod(mName, String.class);
                    Object consumerModel = m.invoke(null, serviceUniqueName);
                    if (checkConsumerModelInvokers(consumerModel)) {
                        return true;
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            // 若有 appModel 实例，尝试从 appModel 中拿到所有 consumer models
            if (appModel != null) {
                String[] instanceConsumerMethods = new String[]{
                        "allConsumerModels", "getAllConsumerModels", "getConsumerModels", "getConsumers"
                };
                for (String mName : instanceConsumerMethods) {
                    try {
                        Method m = appModel.getClass().getMethod(mName);
                        Object consumers = m.invoke(appModel);
                        if (consumers instanceof Collection) {
                            @SuppressWarnings("unchecked")
                            Collection<Object> coll = (Collection<Object>) consumers;
                            for (Object consumerModel : coll) {
                                if (matchesServiceUniqueName(consumerModel, serviceUniqueName) && checkConsumerModelInvokers(consumerModel)) {
                                    return true;
                                }
                            }
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (ClassNotFoundException e) {
            log.log(Level.FINE, "Dubbo ApplicationModel not found on classpath");
        } catch (Throwable t) {
            log.log(Level.FINE, "尝试通过 Dubbo 3 模型 API 检查失败", t);
        }

        // 兜底：若无法确定，则返回 false（或根据业务调整为 true）
        return false;
    }

    /**
     * 检查 providerConsumerRegTable/path 返回的集合中有没有可用 invoker
     */
    private boolean isInvokerCollectionAvailable(Object collObj) {
        if (!(collObj instanceof Collection)) return false;
        @SuppressWarnings("unchecked")
        Collection<Object> coll = (Collection<Object>) collObj;
        if (coll.isEmpty()) return false;
        for (Object invoker : coll) {
            if (invokeIsAvailable(invoker)) return true;
        }
        return false;
    }

    /**
     * 给定 ConsumerModel 实例，反射查找其中的 invoker 列表并判断任一 invoker 是否可用
     * @param consumerModel ConsumerModel 实例
     * @return 是否有可用的 invoker
     */
    private boolean checkConsumerModelInvokers(Object consumerModel) {
        if (consumerModel == null) return false;
        Method[] methods = consumerModel.getClass().getMethods();
        for (Method m : methods) {
            // 找返回 Collection 且无参数的方法，可能是获取 invokers 的方法
            if (m.getParameterCount() == 0 && Collection.class.isAssignableFrom(m.getReturnType())) {
                try {
                    Object res = m.invoke(consumerModel);
                    if (isInvokerCollectionAvailable(res)) return true;
                } catch (IllegalAccessException | InvocationTargetException ignored) {}
            }
        }
        return false;
    }

    /**
     * 通过反射尝试调用 invoker.isAvailable()
     * @param invoker invoker 对象
     * @return 是否可用
     */
    private boolean invokeIsAvailable(Object invoker) {
        if (invoker == null) return false;
        try {
            Method isAvailable = invoker.getClass().getMethod("isAvailable");
            Object r = isAvailable.invoke(invoker);
            return r instanceof Boolean && (Boolean) r;
        } catch (NoSuchMethodException e) {
            // 有可能 invoker 是代理或不同接口，实现类上没有该方法签名，尝试通过查找接口方法
            for (Class<?> iface : invoker.getClass().getInterfaces()) {
                try {
                    Method isAvailable = iface.getMethod("isAvailable");
                    Object r = isAvailable.invoke(invoker);
                    return r instanceof Boolean && (Boolean) r;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * 判断 consumerModel 是否与 serviceUniqueName 匹配（尝试若干可能的属性方法）
     * @param consumerModel ConsumerModel 实例
     * @param serviceUniqueName 微服务唯一名称
     * @return 是否匹配
     */
    private boolean matchesServiceUniqueName(Object consumerModel, String serviceUniqueName) {
        if (consumerModel == null) return false;
        String[] candidateNames = new String[]{"getServiceKey", "getServiceInterface", "getServiceInterfaceName", "getServiceName", "getName"};
        for (String name : candidateNames) {
            try {
                Method m = consumerModel.getClass().getMethod(name);
                Object val = m.invoke(consumerModel);
                if (val != null && serviceUniqueName.equals(val.toString())) {
                    return true;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
        return false;
    }
}