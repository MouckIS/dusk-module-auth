package com.dusk.module.ddm.module.auth.service.impl;

import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.module.auth.service.IUserWxRelationRpcService;
import com.dusk.module.auth.service.IUserWxRelationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author kefuming
 * @date 2021-07-23 17:31
 */
@Service
public class UserWxRelationRpcServiceImpl implements IUserWxRelationRpcService {
    @Autowired
    IUserWxRelationService userWxRelationService;

    @Override
    public String getOpenId(Long userId, String appId) {
        return userWxRelationService.getOpenId(userId, appId);
    }
}
