package com.dusk.module.auth.common.datafilter.impl;

import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.utils.SpringContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-06-17 8:40
 */
@Component
public class DataFilterDefinitionContext implements IDataFilterDefinitionContext {
    @Autowired
    IStationRepository stationRepository;
    @Autowired
    SpringContextUtils springUtil;

    private final Map<String, List<Long>> dataFilterDefinition = new HashMap<>();

    /**
     * 组织机构变动都需要重新初始化
     */
    @PostConstruct
    public void initialize() {
        List<Station> all = stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE);

        for (Station station : all) {
            List<Long> details = new ArrayList<>();
            details.add(station.getId());
            dataFilterDefinition.put(station.getId().toString(), details);

            List<Station> children = all.stream().filter(p -> station.getId().equals(p.getParentId())).collect(Collectors.toList());
            addAllChildren(children, all, station.getId());
        }

    }

    private void addAllChildren(List<Station> children, List<Station> all, Long parentId) {

        for (Station station : children) {
            if (dataFilterDefinition.containsKey(parentId.toString())) {
                dataFilterDefinition.get(parentId.toString()).add(station.getId());
            }
            List<Station> myChildren = all.stream().filter(p -> station.getId().equals(p.getParentId())).collect(Collectors.toList());
            addAllChildren(myChildren, all, parentId);
        }

    }

    @Override
    public Map<String, List<Long>> getDataFilterDefinition() {
        return dataFilterDefinition;
    }

    @Override
    public void refresh() {
        dataFilterDefinition.clear();
        initialize();
    }
}
