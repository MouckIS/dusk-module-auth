package com.dusk.module.auth.service;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.entity.ExtendField;
import com.dusk.module.auth.repository.IExtendFieldRepository;

import java.util.List;

public interface IExtendFieldService extends IBaseService<ExtendField,IExtendFieldRepository> {

    void addOrUpdateField(Long entityId, String entityClass, String key, String value);

    String getValue(Long entityId, String entityClass, String key);

    Long getEntityId(String entityClass, EntityPathBase<? extends BaseEntity> entityPathBase, String key, String value);

    List<Long> getEntityIds(String entityClass, EntityPathBase<? extends BaseEntity> entityPathBase, String key, List<String> values);
}