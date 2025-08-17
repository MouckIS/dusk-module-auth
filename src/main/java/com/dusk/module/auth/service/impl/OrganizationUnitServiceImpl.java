package com.dusk.module.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.entity.OrganizationManager;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.framework.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.framework.constant.TreeConstant;
import com.dusk.common.framework.datafilter.DataFilterContextHolder;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.ListResultDto;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.entity.TreeEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.service.impl.TreeService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.common.module.auth.enums.EnumResetType;
import com.dusk.common.module.auth.enums.UserStatus;
import com.dusk.common.module.auth.service.IOrganizationUnitRpcService;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.listener.ExcelDataListener;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.IOrganizationUnitRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.ISerialNoService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-13 13:42
 */
@Service
@Transactional
public class OrganizationUnitServiceImpl extends TreeService<OrganizationUnit, IOrganizationUnitRepository> implements IOrganizationUnitRpcService, IOrganizationUnitService {
    @Autowired
    private Mapper mapper;
    @Autowired
    private JPAQueryFactory queryFactory;
    @Autowired
    private IUserService userService;
    @Autowired
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Autowired
    private ISerialNoService serialNoService;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IOrganizationManagerRepository organizationManagerRepository;

    @Override
    public ListResultDto<OrganizationStationUnitDto> getExternalOrganizationUnits() {
        Long userId = LoginUserIdContextHolder.getUserId();
        User user = userRepository.findById(userId).orElseThrow();
        List<OrganizationUnit> organizationUnitList;
        Map<Long, Long> map = organizationManagerRepository.findAll().stream().collect(Collectors.toMap(OrganizationManager::getOrgId, OrganizationManager::getUserId));
        // 本单位人员获取所有的外部组织机构
        if (user.getUserType() == EUnitType.Inner) {
            organizationUnitList = findAll(Specifications.where(e->e.eq(OrganizationUnit.Fields.type, EUnitType.External)),
                    Sort.by(TreeEntity.Fields.sortIndex, TreeEntity.Fields.displayName));
        } else {
            // 外部单位人员获取所属的组织机构
            List<OrganizationUnit> unitList = getOrganizationUnitsByUser(new EntityDto(userId));
            organizationUnitList = getParentOrganizations(new EntityDto(unitList.get(0).getId()));

        }
        return DozerUtils.mapToListResultDto(dozerMapper, organizationUnitList, OrganizationStationUnitDto.class, (unit, dto) -> {
            Long orgId = unit.getId();
            if (map.containsKey(orgId)) {
                dto.setManagerId(map.get(orgId));
            }
        });
    }

    @Override
    public Page<OrganizationUnitUserListDto> getOrganizationUnitUsers(GetOrganizationUnitUsersInput input) {
        Set<Long> queryOrgaIds = getQueryOrgaIds(input.getOrganizationUnitIds(), input.isDeepQuery());
        if (queryOrgaIds.isEmpty()) {
            queryOrgaIds.add(-1L); //查询所有用户
        }
        // 默认排序
        if (CharSequenceUtil.isBlank(input.getSorting())) {
            input.setSorting(BaseEntity.Fields.id);
        }
        return repository.getOrganizationUnitUsers(queryOrgaIds, input.getFilter(), input.getPageable(), input.getType());
    }

    @Override
    public Page<OrganizationUnitUserInfoListDto> getOrganizationUnitUsersInfo(GetOrganizationUnitUsersExtInput input) {
        QOrganizationUnit qOrganizationUnit = QOrganizationUnit.organizationUnit;
        QUser qUser = QUser.user;
        QBean<OrganizationUnitUserInfoListDto> bean = QBeanBuilder.create(OrganizationUnitUserInfoListDto.class)
                .appendField(qUser.id.as(OrganizationUnitUserInfoListDto.Fields.id))
                .appendField(qOrganizationUnit.id.as(OrganizationUnitUserInfoListDto.Fields.organizationUnitId))
                .appendField(qOrganizationUnit.displayName.as(OrganizationUnitUserInfoListDto.Fields.organizationUnitName))
                .appendQEntity(qOrganizationUnit, qUser).build();
        JPAQuery<OrganizationUnitUserInfoListDto> query = queryFactory.select(bean)
                .from(qOrganizationUnit)
                .innerJoin(qOrganizationUnit.users, qUser);

        Set<Long> queryOrgaIds = getQueryOrgaIds(input.getOrganizationUnitIds(), input.isDeepQuery());
        if (!queryOrgaIds.isEmpty()) {
            query.where(qOrganizationUnit.id.in(queryOrgaIds));
        }
        if (CharSequenceUtil.isNotBlank(input.getFilter())) {
            query.where(qUser.name.likeIgnoreCase("%" + input.getFilter() + "%")
                    .or(qUser.userName.likeIgnoreCase("%" + input.getFilter() + "%")));
        }
        if (input.getType() != null) {
            query.where(qOrganizationUnit.type.eq(input.getType()));
        }
        if (!input.isDisplayDimissionUsers()) {
            query.where(qUser.userStatus.eq(UserStatus.OnJob));
        }
        // 默认排序
        if (CharSequenceUtil.isBlank(input.getSorting())) {
            query.orderBy(qOrganizationUnit.id.asc());
            // input.setSorting(BaseEntity.Fields.id);
        }

        return (Page<OrganizationUnitUserInfoListDto>) page(query, input.getPageable());
    }

    /**
     * 获取组织机构的用户下拉列表
     *
     * @param input
     */
    @Override
    public Page<OrganizationUnitUserForSelectDto> getOrganizationUnitUsersForSelect(GetOrganizationUnitUsersForSelectInput input) {
        if (input.getOrgId() == null) {
            return Page.empty();
        }
        return repository.getOrganizationUnitUsersForSelect(input.getOrgId(), input.getFilter(), input.getPageable());
    }

    private Set<Long> getQueryOrgaIds(List<Long> ids, boolean isDeepQuery) {
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

    @Override
    public OrganizationUnit create(CreateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = mapper.map(input, OrganizationUnit.class);
        OrganizationUnit save = save(organizationUnit);
        updateOrgManager(save.getId(), input.getManagerId());
        validDisplayNameUnique(organizationUnit);
        return organizationUnit;
    }

    @Override
    public OrganizationUnit createExternalOrganization(CreateOrganizationUnitInput input) {
        Long id = LoginUserIdContextHolder.getUserId();
        User user = userRepository.findById(id).orElseThrow();
        if (input.getParentId() == null && user.getUserType() == EUnitType.External) {
            throw new BusinessException("外部单位人员不允许创建根节点");
        }
        return create(input);
    }

    /**
     * 更新组织管理层更新
     * @param orgId 组织id
     * @param userId user id
     */
    private void updateOrgManager(Long orgId, Long userId) {
        if (userId != null) {
            organizationManagerRepository.deleteByOrgId(orgId);
            OrganizationManager manager = new OrganizationManager(orgId, userId);
            organizationManagerRepository.save(manager);
        }
    }

    @Override
    public OrganizationUnit update(UpdateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的组织机构"));
        mapper.map(input, organizationUnit);
        save(organizationUnit);
        updateOrgManager(organizationUnit.getId(), input.getManagerId());
        validDisplayNameUnique(organizationUnit);
        return organizationUnit;
    }

    @Override
    public OrganizationUnit move(MoveOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的组织机构"));
        save(organizationUnit);
        validDisplayNameUnique(organizationUnit);
        return organizationUnit;
    }

    @Override
    public void deleteOrgById(Long orgId) {
        // 删除组织机构下的用户
        GetOrganizationUnitUsersInput input = new GetOrganizationUnitUsersInput();
        input.setUnPage(true);
        input.setOrganizationUnitIds(Collections.singletonList(orgId));
        List<Long> userIdList = getOrganizationUnitUsers(input).stream().map(OrganizationUnitUserListDto::getId).collect(Collectors.toList());
        userService.deleteUserByIds(userIdList);
        // 删除管理层信息
        organizationManagerRepository.deleteByOrgId(orgId);
        // 删除组织机构
        deleteById(orgId);
    }

    @Override
    public <S extends OrganizationUnit> List<S> saveAll(Iterable<S> iterable) {
        List<S> result = super.saveAll(iterable);
        return result;
    }

    /**
     * 设置父组织机构
     *
     * @param organizationUnit
     * @param parentId
     */
    private void setParent(OrganizationUnit organizationUnit, Long parentId) {
        OrganizationUnit parent = parentId == null ? null : findById(parentId).orElseThrow(() -> new BusinessException("未找到相应的父组织机构"));
        if (parent != null) { //判断所选的父组织机构的所有上级机构中是否包含当前的组织机构
            List<OrganizationUnit> list = getParentOrganizations(new EntityDto(parentId));
            if (list.stream().anyMatch(e -> e.getId().equals(organizationUnit.getId()))) {
                throw new BusinessException("不能选择子组织机构作为父组织机构！");
            }
        }
        organizationUnit.setParentId(parentId);
    }

    @Override
    public void removeUserFromOrganizationUnit(UserToOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = findById(input.getOrganizationUnitId()).orElseThrow(() -> new BusinessException("未找到相应的组织机构"));
        organizationUnit.setUsers(organizationUnit.getUsers().stream().filter(u -> !u.getId().equals(input.getUserId())).collect(Collectors.toList()));
        save(organizationUnit);
    }

    @Override
    public void addUsersToOrganizationUnit(UsersToOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = findById(input.getOrganizationUnitId()).orElseThrow(() -> new BusinessException("未找到相应的组织机构"));
        input.getUserIds().forEach(userId -> {
            if (organizationUnit.getUsers().stream().noneMatch(u -> u.getId().equals(userId))) {
                User user = new User();
                user.setId(userId);
                organizationUnit.getUsers().add(user);
            }
        });
        save(organizationUnit);
    }

    @Override
    public List<OrganizationUnit> getOrganizationUnitsByUser(EntityDto input) {
        return repository.getOrganizationUnitsByUser(input.getId());
    }


    @Override
    public List<OrganizationUnit> getParentOrganizations(EntityDto input) {
        List<OrganizationUnit> result = new ArrayList<>();
        Optional<OrganizationUnit> optional = repository.findById(input.getId());
        if (optional.isPresent()) {
            OrganizationUnit organizationUnit = optional.get();
            result.add(organizationUnit);
            result.addAll(getAncestors(input.getId()));
        }
        return result;
    }

    @Override
    public List<OrganizationUnit> getStations() {
        return repository.findByStationAndStationEnabled(true, true);
    }

    @Override
    public Page<User> findUsers(GetOrganizationUnitUsersInput input) {
        Set<Long> queryOrgaIds = getQueryOrgaIds(input.getOrganizationUnitIds(), input.isDeepQuery());
        if (queryOrgaIds.isEmpty()) {
            queryOrgaIds.add(-1L); //查询所有用户
        }
        return repository.findUsers(queryOrgaIds, input.getFilter(), input.getPageable(), input.getType());
    }

    @Override
    public List<Long> getUserIdsByOrgIdAndNameLike(String name, Long orgId, Boolean deepQuery) {
        if (StrUtil.isNotBlank(name) || orgId != null) {
            Set<Long> queryOrgaIds = null;
            EUnitType type = null;
            if (orgId != null) {
                queryOrgaIds = getQueryOrgaIds(Collections.singletonList(orgId), deepQuery);
                OrganizationUnitDto unitDto = findOneById(orgId);
                type = unitDto.getType();
            }
            return repository.getUserIds(queryOrgaIds, name, type);
        } else {
            return null;
        }
    }

    @Override
    public List<OrganizationUnit> getStationsByUserId(Long id) {
        List<OrganizationUnit> userOrgas = repository.getOrganizationUnitsByUser(id);
        Set<Long> queryIds = getQueryOrgaIds(userOrgas.stream().map(BaseEntity::getId).distinct().collect(Collectors.toList()), true);
        Specification<OrganizationUnit> spec = Specifications.where(e -> {
            e.in(BaseEntity.Fields.id, queryIds).eq(OrganizationUnit.Fields.station, true).eq(OrganizationUnit.Fields.stationEnabled, true);
        });
        return findAll(spec);
    }

    @Override
    public List<OrganizationUnit> getAllStationsByUserId(Long id) {
        List<OrganizationUnit> userOrgas = repository.getOrganizationUnitsByUser(id);
        Set<Long> queryIds = getQueryOrgaIds(userOrgas.stream().map(BaseEntity::getId).distinct().collect(Collectors.toList()), true);
        Specification<OrganizationUnit> spec = Specifications.where(e -> {
            e.in(BaseEntity.Fields.id, queryIds).eq(OrganizationUnit.Fields.station, true);
        });
        return findAll(spec);
    }

    @Override
    public List<StationsOfLoginUserDto> getStationsForFrontByUserId(Long id) {
        List<OrganizationUnit> stations = this.getStationsByUserId(id);
        User u = userService.getUserById(id);
        return DozerUtils.mapList(dozerMapper, stations, StationsOfLoginUserDto.class, (s, t) -> {
           t.setValue(s.getId());
           t.setName(s.getDisplayName());
           t.setDefaultBy(s.getId().equals(u.getDefaultStation()));
        });
    }

    @Override
    public List<OrganizationUnitDto> getAllOrgas() {
        return DozerUtils.mapList(mapper, findAll(), OrganizationUnitDto.class);
    }

    @Override
    public OrganizationUnitDto findOneByDisplayName(String displayName) {
        if (StringUtils.isBlank(displayName)) {
            return null;
        }
        Specification<OrganizationUnit> spec = Specifications.where(e -> {
            e.eq(TreeEntity.Fields.displayName, displayName);
        });
        List<OrganizationUnit> list = findAll(spec);

        return list.isEmpty() ? null : mapper.map(list.get(0), OrganizationUnitDto.class);
    }

    @Override
    public OrganizationUnitDto findOneById(Long id) {
        if (id == null) {
            return null;
        }
        Optional result = findById(id);
        return result.isEmpty() ? null : mapper.map(result.get(), OrganizationUnitDto.class);
    }

    @Override
    public List<OrganizationUnitDto> findByIds(List<Long> ids) {
        if (ids != null) {
            List<OrganizationUnit> data = repository.findByIdIn(ids);
            return DozerUtils.mapList(mapper, data, OrganizationUnitDto.class);
        }
        return null;
    }

    @Override
    public List<OrganizationUnitDto> getOrganizationUnitsByUserId(Long userId) {
        return DozerUtils.mapList(mapper, repository.getOrganizationUnitsByUser(userId), OrganizationUnitDto.class);
    }

    @Override
    public Map<Long, List<OrganizationUnitDto>> getOrganizationUnitMapByOrgIds(List<Long> orgIds) {
        HashMap<Long, List<OrganizationUnitDto>> map = new HashMap<>(orgIds.size());
        Map<Long, OrganizationUnitDto> unitDtoMap = findByIds(orgIds).stream().collect(Collectors.toMap(OrganizationUnitDto::getId, s -> s));
        orgIds.forEach(orgId -> {
            List<OrganizationUnit> organizationUnits = findDescendants(orgId);
            List<OrganizationUnitDto> dtoList = DozerUtils.mapList(dozerMapper, organizationUnits, OrganizationUnitDto.class);
            OrganizationUnitDto mapOrDefault = unitDtoMap.getOrDefault(orgId, null);
            dtoList.add(mapOrDefault);
            map.put(orgId, dtoList);
        });
        return map;
    }

    @Override
    public OrganizationUnitDto getCurrentOrganization() {
        Long defaultOrgId = DataFilterContextHolder.getDefaultOrgId();

        if (defaultOrgId == null) {
            return null;
        }

        OrganizationUnit organizationUnit = findById(defaultOrgId).orElse(null);

        return organizationUnit != null ? mapper.map(organizationUnit, OrganizationUnitDto.class) : null;
    }

    @Override
    public PagedResultDto<OrganizationUnitUserListDto> getOrganizationUnitUsers(PagedAndSortedInputDto pageReq, String filter, boolean deepQuery, Long... orgIds) {
        GetOrganizationUnitUsersInput req = mapper.map(pageReq, GetOrganizationUnitUsersInput.class);
        req.setDeepQuery(deepQuery);
        req.setFilter(filter);
        if (orgIds != null) {
            for (Long orgId : orgIds) {
                req.getOrganizationUnitIds().add(orgId);
            }
        }
        Page<OrganizationUnitUserListDto> pageResult = getOrganizationUnitUsers(req);
        return new PagedResultDto<>(pageResult.getTotalElements(), pageResult.getContent());
    }


    /**
     * 获取当前用户下的所有厂站-（包含普通节点的最小树结构）
     *
     * @param id
     * @return
     */
    @Override
    public List<OrganizationUnit> getStationTreeByUserId(Long id) {
        List<OrganizationUnit> result = new ArrayList<>();
        List<OrganizationUnit> stations = getStationsByUserId(id);

        for (OrganizationUnit station : stations) {
            if (result.stream().noneMatch(e -> e.getId().equals(station.getId()))) {
                result.add(station);
                List<OrganizationUnit> descendants = findDescendants(station.getId());
                List<OrganizationUnit> children = descendants.stream().filter(e -> station.getId().equals(e.getParentId())).collect(Collectors.toList());
                for (OrganizationUnit child : children) {
                    addMatchChildrenToList(child, listDescendants(child, descendants), result);
                }
            }
        }
        return result;
    }


    /**
     * 当前组织机构的所有后代中存在厂站， 则添加当前组织机构
     *
     * @param org         当前组织机构id
     * @param descendants 当前组织机构的所有后代
     */
    private void addMatchChildrenToList(OrganizationUnit org, List<OrganizationUnit> descendants, List<OrganizationUnit> targetList) {
        if (targetList.stream().anyMatch(e -> e.getId().equals(org.getId()))) {
            return;
        }
        if (descendants.stream().anyMatch(e -> BooleanUtil.isTrue(e.getStation()))) {
            targetList.add(org);
            List<OrganizationUnit> children = descendants.stream().filter(e -> org.getId().equals(e.getParentId())).collect(Collectors.toList());
            for (OrganizationUnit child : children) {
                addMatchChildrenToList(child, listDescendants(child, descendants), targetList);
            }
        }
    }

    /**
     * 查找所有后裔
     *
     * @param organizationUnit
     * @param all
     * @return
     */
    private List<OrganizationUnit> listDescendants(OrganizationUnit organizationUnit, List<OrganizationUnit> all) {
        return all.stream().filter(e -> e.getPath().startsWith(organizationUnit.getPath() + TreeConstant.PATH_DELIMITER)).collect(Collectors.toList());
    }


    @Override
    public List<OrganizationUnitDto> getStationsByParentId(Long orgId) {
        // 递归查询所有子机构
        List<OrganizationUnitDto> result = new ArrayList<>();
        List<OrganizationUnit> organizationUnitList = repository.getStationsByParentId(orgId);
        if (organizationUnitList.size() > 0) {
            result.addAll(DozerUtils.mapList(mapper, organizationUnitList, OrganizationUnitDto.class));
            for (OrganizationUnit organizationUnit : organizationUnitList) {
                result.addAll(getStationsByParentId(organizationUnit.getId()));
            }
        }
        return result;
    }

    @Override
    public List<OrganizationUnit> findByCodes(List<String> codes) {
        Specification<OrganizationUnit> spec = Specifications.where(e -> {
            e.isNotNull(OrganizationUnit.Fields.code).in(OrganizationUnit.Fields.code, codes);
        });
        return findAll(spec);
    }

    @Override
    public void deleteByCodes(List<String> deleteBizOrgIds) {
        if (deleteBizOrgIds.isEmpty()) {
            return;
        }
        List<OrganizationUnit> list = findByCodes(deleteBizOrgIds);
        for (OrganizationUnit organizationUnit : list) {
            delete(organizationUnit);
        }
    }

    @Override
    public void setStationEnabled(Long id, boolean enabled) {
        OrganizationUnit unit = findById(id).orElseThrow(() -> new BusinessException("未找到相应的组织机构"));
        if (Boolean.TRUE.equals(unit.getStation())) {
            unit.setStationEnabled(enabled);
            save(unit);
        } else {
            throw new BusinessException("不能对非厂站单位设置");
        }
    }

    @Override
    public List<OrganizationUnitDto> saveAllOrgas(List<OrganizationUnitDto> orgList) {
        List<OrganizationUnit> list = new ArrayList<>();
        List<OrganizationUnitDto> addList = new ArrayList<>();
        List<OrganizationUnitDto> updateList = new ArrayList<>();
        orgList.forEach(i -> {
            if (i.getId() == null) {
                addList.add(i);
            } else {
                updateList.add(i);
            }
        });

        if (!addList.isEmpty()) {
            list.addAll(addList.stream().map(s -> {
                OrganizationUnit t = new OrganizationUnit();
                BeanUtils.copyProperties(s, t);
                Long parentId = s.getParentId();
                if (parentId != null) {
                    setParent(t, parentId);
                }
                return t;
            }).collect(Collectors.toList()));
        }
        if (!updateList.isEmpty()) {
            List<Long> idList = updateList.stream().map(i -> i.getId()).collect(Collectors.toList());
            List<OrganizationUnit> organizationUnitList = repository.findAllById(idList);
            organizationUnitList.forEach(i -> {
                Optional<OrganizationUnitDto> optional = updateList.stream().filter(dto -> i.getId().equals(dto.getId())).findAny();
                if (optional.isPresent()) {
                    OrganizationUnitDto input = optional.get();
                    BeanUtils.copyProperties(input, i);
                    Long parentId = input.getParentId();
                    if (parentId != null && !parentId.equals(i.getParentId())) {
                        setParent(i, parentId);
                    }
                }
            });
            list.addAll(organizationUnitList);
        }

        if (!list.isEmpty()) {
            list = this.saveAll(list);
        }
        return list.stream().map(s -> {
            OrganizationUnitDto t = new OrganizationUnitDto();
            BeanUtils.copyProperties(s, t);
            return t;
        }).collect(Collectors.toList());
    }

    @Override
    public List<OrganizationUnitDto> getStationsByCurrUserAndStation(Long userId) {
        return DozerUtils.mapList(mapper, repository.getStationsByCurrUserAndStation(userId), OrganizationUnitDto.class);
    }

    @Override
    public void importUnitByExcel(MultipartFile file) {
        try {
            ExcelDataListener listener = new ExcelDataListener();
            EasyExcel.read(file.getInputStream(), ImportOrganizationExcelDto.class, listener).sheet().doRead();
            String rootUnitName = listener.getRootUnitName();

            CreateOrganizationUnitInput innerInput = new CreateOrganizationUnitInput();
            CreateOrganizationUnitInput externalInput = new CreateOrganizationUnitInput();
            innerInput.setDisplayName(rootUnitName);
            externalInput.setDisplayName(rootUnitName + 1);
            innerInput.setType(EUnitType.Inner);
            externalInput.setType(EUnitType.External);
            OrganizationUnit innerRoot = create(innerInput);
            OrganizationUnit externalOrganization = createExternalOrganization(externalInput);

            AtomicReference<Map<String, Long>> orgNameIdMap = new AtomicReference<>();
            listener.getMap().forEach((deep, list) -> {
                List<OrganizationUnit> units;
                if (deep == 1) {
                    units = DozerUtils.mapList(dozerMapper, list, OrganizationUnit.class, (s, t) -> {
                        EUnitType unitType = "Inner".equals(s.getTypeStr()) ? EUnitType.Inner : EUnitType.External;
                        Long parentId = "Inner".equals(s.getTypeStr()) ? innerRoot.getId() : externalOrganization.getId();
                        t.setType(unitType);
                        t.setParentId(parentId);
                    });
                } else {
                    units = DozerUtils.mapList(dozerMapper, list, OrganizationUnit.class, (s, t) -> {
                        EUnitType unitType = "Inner".equals(s.getTypeStr()) ? EUnitType.Inner : EUnitType.External;
                        t.setType(unitType);
                        Long parentId = orgNameIdMap.get().get(s.getParentOrg());
                        t.setParentId(parentId);
                    });
                }
                saveAll(units);
                orgNameIdMap.set(units.stream().collect(Collectors.toMap(OrganizationUnit::getDisplayName, OrganizationUnit::getId)));
            });
        }catch (IOException ioException) {
            throw new BusinessException(ioException.getMessage());
        }

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
    private void validDisplayNameUnique(OrganizationUnit org){
        List<OrganizationUnit> list = findAll(Specifications.where(e -> {
            e.eq(TreeEntity.Fields.displayName, org.getDisplayName());
            e.isNull(org.getParentId() == null, TreeEntity.Fields.parentId);
            e.eq(org.getParentId() != null, TreeEntity.Fields.parentId, org.getParentId());
        }));
        if(list.size() > 1){
            throw new BusinessException("该层级下已存在相同名称的组织机构！");
        }
    }
}
