package com.dusk.module.auth.cache;

import java.util.Map;

/**
 * @author kefuming
 * @date 2021-07-23 15:57
 */
public interface IUserWxRelationCacheService {
    void saveWxRelation(Long userId, String appId, String openId);

    void saveWxRelationMap(Long userId, Map<String, String> hv);

    String getWxRelation(Long userId, String appId);

    Map<Object, Object> getWxRelationMap(Long userId);
}
