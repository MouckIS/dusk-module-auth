package com.dusk.module.auth.push.local;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * app消息推送就地部署
 * @author kefuming
 * @date 2022/11/24 7:49
 */
public class LocalNotificationCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String result = conditionContext.getEnvironment().getProperty("app.notification.local.enabled");
        return "true".equalsIgnoreCase(result);
    }
}
