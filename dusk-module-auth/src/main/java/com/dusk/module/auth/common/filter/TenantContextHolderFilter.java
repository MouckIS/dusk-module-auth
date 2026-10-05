package com.dusk.module.auth.common.filter;

import cn.hutool.core.text.CharSequenceUtil;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.exception.TenantNotValidException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.UserContextUtils;
import com.dusk.module.auth.dto.TenantInfoDto;
import com.dusk.module.auth.service.ITenantRpcService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author kefuming
 * @date 2020-04-27 15:36
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextHolderFilter extends GenericFilterBean {

    public static final String TENANT_ID_HEADER = "TENANT-ID";
    @DubboReference
    ITenantRpcService tenantRpcService;
    @Resource
    UserContextUtils userContextUtils;
    @Value("${app.tenant-domain-level:4}")
    int tenantDomainLevel;

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        Long tenantId = 0L;
        Long userId = null;
        //如果已经登陆，用用户请求头里解析的租户为准
        UserContext userContext = userContextUtils.getUserContext(request);
        if (userContext != null) {
            //解析上下文
            tenantId = userContext.getTenantId();
            userId = userContext.getId();
            log.debug("tenantId in Authorization: {}", tenantId);
        } else {
            String tenantIdStr = request.getHeader(TENANT_ID_HEADER);
            //旧写法适配
            if (CharSequenceUtil.isEmpty(tenantIdStr)) {
                String TENANT_ID_HEADER_OLD = "TENANT_ID";
                tenantIdStr = request.getHeader(TENANT_ID_HEADER_OLD);
            }
            if (!StringUtils.isBlank(tenantIdStr)) {
                log.debug("tenantId in header: {}", tenantIdStr);
                tenantId = Long.parseLong(tenantIdStr);
                TenantInfoDto tenantInfo = tenantRpcService.findById(tenantId);
                if (tenantInfo == null || !tenantInfo.isEnabled()) {
                    errorResponse(request);
                }
            } else {
                String TENANT_NAME_HEADER = "TENANT-NAME";
                String tenantName = request.getHeader(TENANT_NAME_HEADER);
                //旧写法适配
                if (CharSequenceUtil.isEmpty(tenantName)) {
                    String TENANT_NAME_HEADER_OLD = "TENANT_NAME";
                    tenantName = request.getHeader(TENANT_NAME_HEADER_OLD);
                }
                if (!StringUtils.isBlank(tenantName)) {
                    log.debug("tenantName in header: {}", tenantName);
                    //通过name查询租户，不存在则抛异常 友好化
                    TenantInfoDto tenantInfo = tenantRpcService.findByTenantName(tenantName);
                    if (tenantInfo == null || !tenantInfo.isEnabled()) {
                        errorResponse(request);
                    }
                    if (tenantInfo != null) {
                        tenantId = tenantInfo.getId();
                    }
                } else {
                    tenantId = getTenantBySubDomain(request);
                }
            }
        }

        LoginUserIdContextHolder.setUserId(userId);
        TenantContextHolder.setTenantId(tenantId);
        filterChain.doFilter(request, response);
        TenantContextHolder.clear();
        LoginUserIdContextHolder.clear();
    }

    /**
     * 通过泛域名头匹配用户租户
     *
     * @param request
     * @return
     */
    @SneakyThrows
    Long getTenantBySubDomain(HttpServletRequest request) {
        String host = request.getHeader("x-forwarded-host");  //代理
        if (!StringUtils.isEmpty(host)) {
            if (CharSequenceUtil.indexOf(host, ',') != -1) { //多个代理取首个
                host = StringUtils.substringBefore(host, ",");
            }
        } else {
            host = request.getHeader("host");
        }
        if ( CharSequenceUtil.startWith(host, "www.")) {
            host = StringUtils.substringAfter(host, "www.");
        }
        if (CharSequenceUtil.indexOf(host, ':') != -1) {
            host = StringUtils.substringBefore(host, ":");
        }
        if (host.startsWith("[")) { //IP v6 访问
            return null;
        }
        if (StringUtils.isNumeric(CharSequenceUtil.replace(host, ".", ""))) {    // IP v4 访问
            return null;
        }
        String[] split = StringUtils.split(host, ".");
        if (split.length == tenantDomainLevel) {
            //泛域名多级域名只匹配最高级domain即第一个
            TenantInfoDto tenantInfo = tenantRpcService.findByTenantName(split[0]);
            if (tenantInfo != null && tenantInfo.isEnabled()) {
                return tenantInfo.getId();
            }
        }
        return null;
    }

    private void errorResponse(HttpServletRequest request) {
        TenantNotValidException exception = new TenantNotValidException("租户不存在，请检查");
        request.setAttribute("exception", exception);
        throw exception;
    }
}
