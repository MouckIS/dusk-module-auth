package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.dto.station.*;
import com.github.dozermapper.core.Mapper;
import org.apache.commons.lang.StringUtils;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.entity.TreeEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.service.impl.TreeService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.module.auth.dto.station.StationDto;
import com.dusk.common.module.auth.enums.EnumResetType;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.dto.station.*;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IStationRepository;
import com.dusk.module.auth.service.ISerialNoService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class StationServiceImpl extends TreeService<Station,IStationRepository> implements IStationService  {
    @Autowired
    private Mapper mapper;
    @Autowired
    private IUserService userService;
    @Autowired
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Autowired
    private ISerialNoService serialNoService;

    @Override
    public Station createOrUpdate(CreateOrUpdateStationInput input) {
        Station result = super.createOrUpdate(input);
        validDisplayNameUnique(result);
        dataFilterDefinitionContext.refresh();
        return result;
    }

    @Override
    public void deleteById(long id) {
        super.deleteById(id);
        dataFilterDefinitionContext.refresh();
    }

    @Override
    public <S extends Station> List<S> saveAll(Iterable<S> iterable) {
        List<S> result = super.saveAll(iterable);
        dataFilterDefinitionContext.refresh();
        return result;
    }

    @Override
    public void removeUserFromStation(RemoveUserFromStationInput input) {
        Station station = findById(input.getStationId()).orElseThrow(() -> new BusinessException("未找到相应的厂站"));
        station.setUsers(station.getUsers().stream().filter(u -> !u.getId().equals(input.getUserId())).collect(Collectors.toList()));
        save(station);
    }

    @Override
    public void addUsersToStation(AddUsersToStationInput input) {
        Station station = findById(input.getStationId()).orElseThrow(() -> new BusinessException("未找到相应的厂站"));
        input.getUserIds().forEach(userId -> {
            if (station.getUsers().stream().noneMatch(u -> u.getId().equals(userId))) {
                User user = new User();
                user.setId(userId);
                station.getUsers().add(user);
            }
        });
        save(station);
    }

    @Override
    public Page<StationUserListDto> getStationUsers(GetStationUsersInput input) {
        Set<Long> queryStationIds = getQueryStationIds(input.getStationIds(), input.isDeepQuery());
        if (queryStationIds.isEmpty()) {
            queryStationIds.add(-1L); //查询所有用户
        }
        return repository.getStationUsers(queryStationIds, input.getFilter(), input.getUserType(), input.getPageable());
    }

    private List<Station> getAllStationsByUserId(Long id) {
        List<Station> stations = repository.getStationsByUser(id);
        Set<Long> queryIds = getQueryStationIds(stations.stream().map(BaseEntity::getId).distinct().collect(Collectors.toList()), true);
        Specification<Station> spec = Specifications.where(e -> {
            e.in(BaseEntity.Fields.id, queryIds);
        });
        return findAll(spec);
    }

    @Override
    public List<StationsOfLoginUserDto> getStationsForFrontByUserId(Long id) {
        List<Station> stations = this.getAllStationsByUserId(id);
        User u = userService.getUserById(id);

        return DozerUtils.mapList(dozerMapper, stations, StationsOfLoginUserDto.class, (s, t) -> {
            t.setName(s.getDisplayName());
            t.setMainStation(stations.stream().anyMatch(station -> s.getId().equals(station.getParentId())));//存在一个厂站其parentId为当前站id则认为该站为集控站
            t.setDefaultBy(s.getId().equals(u.getDefaultStation()));
            t.setValue(s.getId());
        });
    }

    @Override
    public List<StationDto> getAllStations() {
        return DozerUtils.mapList(mapper, findAll(Sort.by(TreeEntity.Fields.sortIndex, TreeEntity.Fields.displayName)), StationDto.class);
    }

    @Override
    public Page<StationUserDto> getNotAssignedStationUsers(GetNotAssignedStationUsersInput input) {
        if (input.getStationId() == null) {
            return Page.empty();
        }
        return repository.getNotAssignedStationUsers(input.getStationId(), input.getFilter(), input.getUserType(), input.getPageable());
    }

    @Override
    public StationDto findOneByDisplayName(String displayName) {
        if (StringUtils.isBlank(displayName)) {
            return null;
        }
        Specification<Station> spec = Specifications.where(e -> {
            e.eq(TreeEntity.Fields.displayName, displayName);
        });
        List<Station> list = findAll(spec);

        return list.isEmpty() ? null : mapper.map(list.get(0), StationDto.class);
    }

    /**
     * 覆盖调treeService中的getSerialNos， 不需要通过rpc调用
     *
     * @param count
     * @return
     */
    @Override
    protected String[] getSerialNos(int count) {
        String[] serialNos = serialNoService.getSerialNos(getEntityClass().getName(), EnumResetType.Never, "", 12, count);
        String[] result = new String[count];
        for (int i = 0; i < serialNos.length; i++) {
            result[i] = Integer.parseInt(serialNos[i]) + "";
            //去除前导0
        }
        return result;
    }

    /**
     * 同一层级名称唯一性校验
     */
    private void validDisplayNameUnique(Station station){
        List<Station> list = findAll(Specifications.where(e -> {
            e.eq(TreeEntity.Fields.displayName, station.getDisplayName());
            e.isNull(station.getParentId() == null, TreeEntity.Fields.parentId);
            e.eq(station.getParentId() != null, TreeEntity.Fields.parentId, station.getParentId());
        }));
        if(list.size() > 1){
            throw new BusinessException("该层级下已存在相同名称的厂站！");
        }
    }


    private Set<Long> getQueryStationIds(List<Long> ids, boolean isDeepQuery) {
        Set<Long> result = new HashSet<>();
        if (isDeepQuery) {
            for (Long id : ids) {
                result.addAll(getAllDescendantIds(id, true));
            }
        } else {
            result.addAll(ids);
        }
        return result;
    }
}