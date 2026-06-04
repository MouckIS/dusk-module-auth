package com.dusk.module.auth.dimension.aspect;

import com.dusk.common.core.dimension.annotation.DimensionValue;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.dimension.service.IUserDimensionPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 维度值权限控制切面
 * <p>
 * 拦截带有 {@link DimensionValue} 注解的方法，
 * 检查当前用户是否有指定维度值的访问权限。
 * </p>
 * <p>
 * 支持参数为单个值（String）或值列表（Collection），
 * 使用维度值编码进行权限校验。
 * </p>
 *
 * @author dusk
 */
@Aspect
@Component
@Slf4j
public class DimensionValueAspect {

    @Autowired
    private IUserDimensionPermissionService permissionService;

    @Autowired
    private SecurityUtils securityUtils;

    @Around("@annotation(dimensionValue)")
    public Object doProcess(ProceedingJoinPoint joinPoint, DimensionValue dimensionValue) throws Throwable {
        try {
            UserContext user = securityUtils.getCurrentUser();
            if (user == null) {
                throw new BusinessException("未登录，无法进行维度值权限控制");
            }

            // Admin用户跳过权限控制
            if (user.getIsAdmin() != null && user.getIsAdmin()) {
                return joinPoint.proceed();
            }

            String dimensionCode = dimensionValue.dimension();
            String valueParam = dimensionValue.valueParam();

            // 从方法参数中获取维度值
            Object valueObj = getParameterValue(joinPoint, valueParam);
            if (valueObj == null) {
                return joinPoint.proceed();
            }

            // 获取用户有权限的维度值编码
            List<String> accessibleCodes = permissionService.getAccessibleValueCodes(user.getId(), dimensionCode);
            Set<String> accessibleSet = new HashSet<>(accessibleCodes);

            // 支持单个值或值列表
            if (valueObj instanceof Collection<?> valueCollection) {
                for (Object item : valueCollection) {
                    if (item != null && !accessibleSet.contains(item.toString())) {
                        throw new BusinessException(dimensionValue.message() + ": " + item);
                    }
                }
            } else {
                String valueCode = valueObj.toString();
                if (!accessibleSet.contains(valueCode)) {
                    throw new BusinessException(dimensionValue.message() + ": " + valueCode);
                }
            }

            return joinPoint.proceed();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Throwable ex) {
            log.error("维度值权限控制拦截器异常", ex);
            throw ex;
        }
    }

    /**
     * 从方法参数中获取指定参数名的值
     */
    private Object getParameterValue(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        // 优先使用 Spring 提供的参数名数组（编译时参数名信息）
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName)) {
                    return args[i];
                }
            }
        }

        // 兜底：使用反射获取参数名
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                return args[i];
            }
        }

        log.warn("未找到参数: {}", paramName);
        return null;
    }
}
