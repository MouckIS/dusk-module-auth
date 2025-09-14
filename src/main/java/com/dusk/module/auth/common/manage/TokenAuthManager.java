package com.dusk.module.auth.common.manage;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.jpa.Sequence;
import com.dusk.common.framework.jwt.JwtSettings;
import com.dusk.common.framework.jwt.JwtTokenFactory;
import com.dusk.common.framework.jwt.extractor.JwtHeaderTokenExtractor;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.redis.RedisUtil;
import com.dusk.common.framework.utils.UserContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2021-03-17 10:27
 */
@Component
@Slf4j
public class TokenAuthManager {
    @Autowired
    RedisUtil<String> redisUtil;
    @Autowired
    JwtTokenFactory jwtTokenFactory;
    @Autowired
    private JwtSettings jwtSettings;
    @Autowired
    UserContextUtils userContextUtils;
    @Autowired
    Sequence sequence;
    @Autowired
    JwtHeaderTokenExtractor jwtHeaderTokenExtractor;

    private static final String JWT_TOKEN_PREFIX = "CRUX:AUTH:TOKEN:";

    /**
     * 生成登陆token，并缓存
     *
     * @param userContext
     * @return
     */
    public String generateToken(UserContext userContext) {
        return generateToken(userContext, jwtSettings.getExpirationTime(), TimeUnit.MINUTES);
    }

    public String generateToken(UserContext userContext, long time, TimeUnit unit) {
        String id = String.valueOf(sequence.nextId());
        String jwtToken = jwtTokenFactory.createJwtToken(userContext, id, time, unit);
        String redisKey = getRedisTokenKey(id);
        //value - 内容改写为实际的jwt token 目的是为了 签发短token 给设备端使用
        redisUtil.setCache(redisKey, jwtToken, time, unit);
        return jwtToken;

    }

    /**
     * 检测token有效性，并反序列化
     *
     * @param authorization
     * @return
     */
    public UserContext checkTokenValid(String authorization) {
        try {
            String token = jwtHeaderTokenExtractor.extract(authorization);
            String tokenId = jwtTokenFactory.getJwtTokenId(token);
            String cache = redisUtil.getCache(getRedisTokenKey(tokenId));
            if (StrUtil.isNotEmpty(cache)) {
                return userContextUtils.getUserContext(authorization);
            }
        } catch (Exception ex) {
            log.error("检查token有效性异常：", ex);
        }
        return null;
    }

    /**
     * 检测token有效性，并反序列化
     *
     * @param request
     * @return
     */
    public UserContext checkTokenValid(HttpServletRequest request) {
        String authorization = userContextUtils.getAuthorization(request);
        return checkTokenValid(authorization);
    }


    /**
     * 移除token缓存
     *
     * @param authorization
     */
    public void removeToken(String authorization) {
        String token = jwtHeaderTokenExtractor.extract(authorization);
        String id = jwtTokenFactory.getJwtTokenId(token);
        redisUtil.deleteCache(getRedisTokenKey(id));
    }


    /**
     * 根据tokenId 获取缓存token
     *
     * @param tokenId
     * @return
     */
    public String getToken(String tokenId) {
        return redisUtil.getCache(getRedisTokenKey(tokenId));
    }


    private String getRedisTokenKey(String id) {
        return JWT_TOKEN_PREFIX + id;
    }
}
