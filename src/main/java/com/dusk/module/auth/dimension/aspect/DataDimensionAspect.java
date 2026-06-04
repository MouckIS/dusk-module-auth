package com.dusk.module.auth.dimension.aspect;

import com.dusk.common.core.dimension.annotation.DataDimension;
import com.dusk.common.core.dimension.context.DataDimensionContextHolder;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.dimension.service.IUserDimensionPermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据维度权限控制切面
 * <p>
 * 拦截带有 {@link DataDimension} 注解的方法，
 * 在方法执行前加载用户的维度值权限（ID、编码、名称）到线程上下文中，
 * 方法执行后清除上下文。
 * </p>
 * <p>
 * 加载到上下文后，业务代码可通过以下两种方式实现数据过滤：
 * <ul>
 *     <li>注解方式：设置 fieldName 属性，由切面自动处理（需配合 DataDimensionFilter）</li>
 *     <li>代码方式：在 Service 中手动调用 DataDimensionFilter.apply(wrapper, dimensionCode, fieldName)</li>
 * </ul>
 * </p>
 *
 * @author dusk
 */
@Aspect
@Component
@Slf4j
public class DataDimensionAspect {

    @Resource
    private IUserDimensionPermissionService permissionService;

    @Resource
    private SecurityUtils securityUtils;

    @Around("@annotation(dataDimension)")
    public Object doProcess(ProceedingJoinPoint joinPoint, DataDimension dataDimension) throws Throwable {
        String dimensionCode = dataDimension.value();
        try {
            UserContext user = securityUtils.getCurrentUser();
            if (user == null) {
                throw new BusinessException("未登录，无法进行数据维度权限控制");
            }

            // Admin用户跳过维度权限控制
            if (user.getIsAdmin() != null && user.getIsAdmin()) {
                return joinPoint.proceed();
            }

            Long userId = user.getId();

            // 加载用户有权限的维度值ID
            List<Long> accessibleValueIds = permissionService.getAccessibleValueIds(userId, dimensionCode);
            // 加载用户有权限的维度值编码
            List<String> accessibleValueCodes = permissionService.getAccessibleValueCodes(userId, dimensionCode);
            // 加载用户有权限的维度值名称
            List<String> accessibleValueNames = permissionService.getAccessibleValueNames(userId, dimensionCode);

            if (dataDimension.strict() && accessibleValueIds.isEmpty()) {
                throw new BusinessException("您没有维度[" + dimensionCode + "]的任何数据访问权限");
            }

            // 将维度权限设置到上下文中
            DataDimensionContextHolder.setAccessibleValueIds(dimensionCode, accessibleValueIds);
            DataDimensionContextHolder.setAccessibleValueCodes(dimensionCode, accessibleValueCodes);
            DataDimensionContextHolder.setAccessibleValueNames(dimensionCode, accessibleValueNames);

            return joinPoint.proceed();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Throwable ex) {
            log.error("数据维度权限控制拦截器异常", ex);
            throw ex;
        } finally {
            DataDimensionContextHolder.clear(dimensionCode);
        }
    }
}
