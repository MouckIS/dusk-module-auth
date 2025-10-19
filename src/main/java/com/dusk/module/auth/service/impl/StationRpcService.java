package com.dusk.module.auth.service.impl;

import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.dto.station.StationDto;
import com.dusk.common.rpc.auth.service.IStationRpcService;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.mapper.StationMapper;
import com.dusk.module.auth.repository.IStationRepository;
import com.dusk.module.auth.service.IStationService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/10/11 19:48
 */
@Service
public class StationRpcService implements IStationRpcService {
    @Resource
    private IStationService stationService;
    @Resource
    private IStationRepository stationRepository;

    private final StationMapper mapper = StationMapper.INSTANCE;

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
        return station == null ? null : mapper.toDto(station);
    }

    @Override
    public List<StationDto> findByIds(List<Long> ids) {
        if (ids != null) {
            Specification<Station> spec = Specifications.where(e -> {
                e.in(BaseEntity.Fields.id, ids);
            });
            List<Station> list = stationService.findAll(spec);
            return MapperUtil.mapList(list, mapper::toDto);
        }
        return new ArrayList<>();
    }

    @Override
    public List<StationDto> getStationsByUserId(Long userId) {
        List<Station> list = stationRepository.getStationsByUser(userId);
        return MapperUtil.mapList(list, mapper::toDto);
    }

    @Override
    public StationDto getCurrentStation() {
        Long defaultOrgId = DataFilterContextHolder.getDefaultOrgId();

        if (defaultOrgId == null) {
            return null;
        }

        Station station = stationService.findById(defaultOrgId).orElse(null);

        return station != null ? mapper.toDto(station) : null;
    }

    @Override
    public List<StationDto> getStationsByParentId(Long parentId) {
        List<Station> list = stationService.findDescendants(parentId);
        return MapperUtil.mapList(list, mapper::toDto);
    }
}
