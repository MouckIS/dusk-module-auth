package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.framework.datafilter.DataFilterContextHolder;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.module.auth.dto.station.StationDto;
import com.dusk.common.module.auth.service.IStationRpcService;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import com.dusk.module.auth.service.IStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/10/11 19:48
 */
@Service
public class StationRpcService implements IStationRpcService {
    @Autowired
    private IStationService stationService;
    @Autowired
    private IStationRepository stationRepository;
    @Autowired
    private Mapper mapper;

    @Override
    public List<StationDto> getAllStations() {
        return stationService.getAllStations();
    }

    @Override
    public StationDto findOneByDisplayName(String displayName) {
        return stationService.findOneByDisplayName(displayName);
    }

    @Override
    public StationDto findOneById(Long id) {
        Station station = stationService.findById(id).orElse(null);
        return station == null ? null : mapper.map(station, StationDto.class);
    }

    @Override
    public List<StationDto> findByIds(List<Long> ids) {
        if (ids != null) {
            Specification<Station> spec = Specifications.where(e -> {
                e.in(BaseEntity.Fields.id, ids);
            });
            List<Station> list = stationService.findAll(spec);
            return DozerUtils.mapList(mapper, list, StationDto.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<StationDto> getStationsByUserId(Long userId) {
        List<Station> list = stationRepository.getStationsByUser(userId);
        return DozerUtils.mapList(mapper, list, StationDto.class);
    }

    @Override
    public StationDto getCurrentStation() {
        Long defaultOrgId = DataFilterContextHolder.getDefaultOrgId();

        if (defaultOrgId == null) {
            return null;
        }

        Station station = stationService.findById(defaultOrgId).orElse(null);

        return station != null ? mapper.map(station, StationDto.class) : null;
    }

    @Override
    public List<StationDto> getStationsByParentId(Long parentId) {
        List<Station> list = stationService.findDescendants(parentId);
        return DozerUtils.mapList(mapper, list, StationDto.class);
    }
}
