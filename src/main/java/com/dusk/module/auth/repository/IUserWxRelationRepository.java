package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.UserWxRelation;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-07-23 15:35
 */
public interface IUserWxRelationRepository extends IBaseRepository<UserWxRelation> {
    UserWxRelation findByAppIdAndOpenId(String appId, String openId);
    UserWxRelation findFirstByUserIdAndAppId(Long userId, String appId);
    List<UserWxRelation> findAllByUserIdIn(List<Long> userIds);
    Integer countAllByUserId(Long userId);
    Integer countAllByOpenId(String openId);

    Integer countAllByUserIdAndOpenId(Long userId, String openId);
    void deleteAllByUserId(Long userId);
}
