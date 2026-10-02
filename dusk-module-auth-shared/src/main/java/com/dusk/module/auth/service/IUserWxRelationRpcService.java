package com.dusk.module.auth.service;

/**
 * @author 潘彦霖
 * @date 2021-07-23 17:29
 */
public interface IUserWxRelationRpcService {
    String getOpenId(Long userId, String appId);
}
