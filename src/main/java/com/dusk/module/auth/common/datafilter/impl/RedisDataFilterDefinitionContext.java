package com.dusk.module.auth.common.datafilter.impl;

import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.redis.RedisCacheCondition;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.utils.SpringContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-06-17 8:46
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
public class RedisDataFilterDefinitionContext implements IDataFilterDefinitionContext {
    private final String REDIS_KEY = "CRUX:AUTH:DATA:FILTER:DEFINITION";
    @Autowired
    IStationRepository stationRepository;
    @Autowired(required = false)
    RedisUtil<Object> redisUtil;
    @Autowired
    SpringContextUtils springUtil;

    /**
     * 组织机构变动都需要重新初始化
     */
    @PostConstruct
    public void initialize() {

        Map<String, List<Long>> dataFilterDefinition = new HashMap<>();

        List<Station> all = stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE);

        for (Station station : all) {
            List<Long> details = new ArrayList<>();
            details.add(station.getId());
            dataFilterDefinition.put(station.getId().toString(), details);

            List<Station> children = all.stream().filter(p -> station.getId().equals(p.getParentId())).collect(Collectors.toList());
            addAllChildren(children, all, station.getId(), dataFilterDefinition);
        }

        redisUtil.setCache(REDIS_KEY, dataFilterDefinition);

    }

    private void addAllChildren(List<Station> children, List<Station> all, Long parentId, Map<String, List<Long>> dataFilterDefinition) {

        for (Station station : children) {

            if (dataFilterDefinition.containsKey(parentId.toString())) {
                dataFilterDefinition.get(parentId.toString()).add(station.getId());
            }

            List<Station> myChildren = all.stream().filter(p -> station.getId().equals(p.getParentId())).collect(Collectors.toList());
            addAllChildren(myChildren, all, parentId, dataFilterDefinition);
        }

    }

    @Override
    public Map<String, List<Long>> getDataFilterDefinition() {
        Object cache = redisUtil.getCache(REDIS_KEY);
        if (cache != null) {
            return (Map<String, List<Long>>) cache;
        }
        return new HashMap<>();
    }

    @Override
    public void refresh() {
        initialize();
    }
}
