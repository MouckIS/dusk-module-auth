package com.dusk.module.auth.common.syscode.impl;

import com.github.dozermapper.core.Mapper;
import com.dusk.common.core.lock.annotation.Lock4j;
import com.dusk.common.core.redis.RedisCacheCondition;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.syscode.SysCodeData;
import com.dusk.common.core.syscode.SysCodeDefinitionDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;
import com.dusk.module.auth.common.syscode.ISysCodeCache;
import com.dusk.module.auth.entity.SysCodeValue;
import com.dusk.module.auth.repository.ISysCodeValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-08-16 11:07
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
public class RedisSysCodeCache implements ISysCodeCache {

    private final String AUTH_SYSCODE_DEFINITION_KEY = "CRUX:AUTH:SYSCODE:DEFINITION";

    @Autowired
    RedisUtil<Object> redisUtil;

    @Autowired
    ISysCodeValueRepository codeValueRepository;

    @Autowired
    Mapper dozerMapper;


    @Override
    @Transactional
    @Lock4j
    public void pushSysCode(String applicationName, Map<String, SysCodeDefinitionDto> definition) {
        //初始化数据库
        if (definition != null) {
            List<SysCodeDefinitionDto> collect = definition.values().stream().filter(p -> p.isExtendOwner() & p.getInitData() != null).collect(Collectors.toList());
            for (SysCodeDefinitionDto dto : collect) {
                List<SysCodeValue> saveData = new ArrayList<>();
                List<SysCodeValue> oldData = codeValueRepository.findByTypeCode(dto.getTypeCode());
                for (SysCodeData data : dto.getInitData()) {
                    SysCodeValue codeValue;
                    Optional<SysCodeValue> first = oldData.stream().filter(p -> p.getCode().equals(data.getCode())).findFirst();
                    first.ifPresent(oldData::remove);
                    codeValue = first.orElseGet(SysCodeValue::new);
                    codeValue.setCode(data.getCode());
                    codeValue.setSortIndex(data.getSortIndex());
                    codeValue.setTypeCode(dto.getTypeCode());
                    codeValue.setValue(data.getValue());
                    saveData.add(codeValue);
                }
                if (saveData.size() > 0) {
                    codeValueRepository.saveAll(saveData);
                }
            }
        }
        Map<String, Map<String, SysCodeDefinitionDto>> definitionCache = getDefinition();
        definitionCache.put(applicationName, definition);
        redisUtil.setCache(AUTH_SYSCODE_DEFINITION_KEY, definitionCache);
    }

    @Override
    public List<SysCodeTypeDto> getSysCodeType() {
        Map<String, Map<String, SysCodeDefinitionDto>> definition = getDefinition();
        List<SysCodeTypeDto> results = new ArrayList<>();
        definition.values().forEach(p -> {
            results.addAll(p.values());
        });
        return results;
    }


    private Map<String, Map<String, SysCodeDefinitionDto>> getDefinition() {
        Object cache = redisUtil.getCache(AUTH_SYSCODE_DEFINITION_KEY);
        if (cache != null) {
            return (Map<String, Map<String, SysCodeDefinitionDto>>) cache;
        }
        return new HashMap<>();
    }
}
