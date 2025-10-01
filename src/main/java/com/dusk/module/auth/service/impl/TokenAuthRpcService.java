package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.commom.rpc.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.commom.rpc.auth.dto.RoleSimpleDto;
import com.dusk.commom.rpc.auth.service.ITokenAuthRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.constant.AuthConstant;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jwt.JwtTokenFactory;
import com.dusk.common.core.jwt.extractor.JwtHeaderTokenExtractor;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2023/2/9 19:23
 */
@Service(retries = 0, timeout = 2000)
@Slf4j
public class TokenAuthRpcService implements ITokenAuthRpcService {
    @Autowired
    private TokenAuthManager tokenAuthManager;

    @Autowired
    RedisUtil<String> redisUtil;

    @Autowired
    JwtTokenFactory jwtTokenFactory;

    @Autowired
    IRoleService roleService;

    private static final String JWT_NONUSER_TOKEN_PREFIX = "CRUX:AUTH:NONUSER:TOKEN:";


    @Override
    public String generateTokenForNonUser(GenerateTokenForNonUserInput input) {
        String identify = input.getIdentify();
        if (StrUtil.isBlank(identify)) {
            throw new BusinessException("唯一标识不能为空!");
        }
        UserContext context = new UserContext();
        context.setIsAdmin(false);
        context.setTenantId(TenantContextHolder.getTenantId());
        context.setIdentify(input.getIdentify());

        List<RoleSimpleDto> roleInfos = roleService.getByRoleNames(input.getRoles());
        if (roleInfos.isEmpty()) {
            throw new BusinessException("角色配置错误，无法签发");
        }
        List<GrantedAuthority> authorities = roleInfos.stream()
                .map(p -> new SimpleGrantedAuthority(AuthConstant.TYPE_ROLE + p.getId()))
                .collect(Collectors.toList());
        context.setAuthorities(authorities);

        String token = tokenAuthManager.generateToken(context, input.getTime(), input.getUnit());
        //移除旧token
        removeToken(identify);
        redisUtil.setCache(JWT_NONUSER_TOKEN_PREFIX + identify, token, input.getTime(), input.getUnit());
        return jwtTokenFactory.getJwtTokenId(token);
    }

    @Override
    public void removeToken(String identify) {
        String key = JWT_NONUSER_TOKEN_PREFIX + identify;
        if (redisUtil.hasKey(key)) {
            tokenAuthManager.removeToken(JwtHeaderTokenExtractor.HEADER_PREFIX + redisUtil.getCache(key));
            redisUtil.deleteCache(key);
        }
    }
}
