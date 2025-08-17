package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.UtBeanUtils;
import com.dusk.module.auth.cache.IUserWxRelationCacheService;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.repository.IUserWxRelationRepository;
import com.dusk.module.auth.service.IUserWxRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-07-23 15:37
 */
@Service
public class UserWxRelationServiceImpl extends BaseService<UserWxRelation, IUserWxRelationRepository> implements IUserWxRelationService {
    @Autowired
    IUserWxRelationCacheService userWxRelationCacheService;

    @Override
    public String getOpenId(Long userId, String appId) {
        String openId = userWxRelationCacheService.getWxRelation(userId, appId);
        if (StrUtil.isNotBlank(openId)) {
            return openId;
        }

        Optional<UserWxRelation> userWxRelationOptional = findOne(Specifications.where(e -> {
            e.eq(UserWxRelation.Fields.userId, userId);
            e.eq(UserWxRelation.Fields.appId, appId);
        }));
        if (userWxRelationOptional.isEmpty()) {
            return null;
        }
        openId = userWxRelationOptional.get().getOpenId();
        userWxRelationCacheService.saveWxRelation(userId, appId, openId);

        return openId;
    }

    @Override
    public void saveRelationList(List<UserWxRelation> userWxRelationList) {
        if (userWxRelationList.isEmpty()) {
            return;
        }

        List<UserWxRelation> sourceAll = findAll(Specifications.where(e ->
                e.in(UserWxRelation.Fields.userId, userWxRelationList.stream().map(UserWxRelation::getUserId).collect(Collectors.toList()))));

        sourceAll.forEach(source -> {
            userWxRelationList.stream().filter(target -> target.getUserId().equals(source.getUserId()) && target.getAppId().equals(source.getAppId()))
                    .findFirst().ifPresent(target -> {
                UtBeanUtils.copyNotNullProperties(source, target);
                UtBeanUtils.copyNotNullProperties(target, source);
            });
        });
        saveAll(userWxRelationList);
        
        userWxRelationList.forEach(relation -> {
            userWxRelationCacheService.saveWxRelation(relation.getUserId(), relation.getAppId(), relation.getOpenId());
        });
    }
}
