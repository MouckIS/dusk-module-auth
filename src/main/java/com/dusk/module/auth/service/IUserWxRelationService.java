package com.dusk.module.auth.service;

import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.repository.IUserWxRelationRepository;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-07-23 15:36
 */
public interface IUserWxRelationService extends IBaseService<UserWxRelation, IUserWxRelationRepository> {
    String getOpenId(Long userId, String appId);

    void saveRelationList(List<UserWxRelation> userWxRelationList);
}
