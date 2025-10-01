package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.ReflectUtil;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.entity.ExtendField;
import com.dusk.module.auth.entity.QExtendField;
import com.dusk.module.auth.repository.IExtendFieldRepository;
import com.dusk.module.auth.service.IExtendFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ExtendFieldServiceImpl extends BaseService<ExtendField,IExtendFieldRepository> implements IExtendFieldService  {
    @Autowired
    private JPAQueryFactory queryFactory;

    QExtendField qExtendField = QExtendField.extendField;
    private EntityPathBase<? extends BaseEntity> entityPathBase;

    @Override
    public void addOrUpdateField(Long entityId, String entityClass, String key, String value) {
        ExtendField record = queryFactory.selectFrom(qExtendField).where(qExtendField.entityId.eq(entityId), qExtendField.entityClass.eq(entityClass), qExtendField.key.eq(key)).fetchFirst();
        if(record == null){
            record = new ExtendField();
            record.setEntityId(entityId);
            record.setEntityClass(entityClass);
            record.setKey(key);
            record.setValue(value);
        }else{
            record.setValue(value);
        }
        save(record);
    }

    @Override
    public String getValue(Long entityId, String entityClass, String key) {
        return queryFactory.select(qExtendField.value).from(qExtendField).where(qExtendField.entityId.eq(entityId), qExtendField.entityClass.eq(entityClass), qExtendField.key.eq(key)).fetchFirst();
    }

    @Override
    public Long getEntityId(String entityClass, EntityPathBase<? extends BaseEntity> entityPathBase, String key, String value) {
        return queryFactory.select(qExtendField.entityId).from(qExtendField)
                .innerJoin(entityPathBase).on(qExtendField.entityId.eq((NumberPath<Long>) ReflectUtil.getFieldValue(entityPathBase,BaseEntity.Fields.id)))
                .where(qExtendField.entityClass.eq(entityClass), qExtendField.key.eq(key), qExtendField.value.eq(value)).fetchFirst();
    }

    @Override
    public List<Long> getEntityIds(String entityClass, EntityPathBase<? extends BaseEntity> entityPathBase, String key, List<String> values) {
        return queryFactory.select(qExtendField.entityId).from(qExtendField)
                .innerJoin(entityPathBase).on(qExtendField.entityId.eq((NumberPath<Long>) ReflectUtil.getFieldValue(entityPathBase,BaseEntity.Fields.id)))
                .where(qExtendField.entityClass.eq(entityClass), qExtendField.key.eq(key), qExtendField.value.in(values)).fetch();
    }
}