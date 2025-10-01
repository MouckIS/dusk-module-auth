package com.dusk.module.auth.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.dusk.commom.rpc.auth.dto.*;
import com.dusk.commom.rpc.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.commom.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.commom.rpc.auth.dto.orga.OrganizationUnitUserDto;
import com.dusk.commom.rpc.auth.service.IUserRpcService;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.core.utils.UtBeanUtils;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.dto.user.GetUsersInput;
import com.dusk.module.auth.dto.user.UserIdAndPermissionDto;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IRoleService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-08-19 17:14
 */
@Service(retries = 0, timeout = 2000)
@Transactional
@Slf4j
public class UserRpcServiceImpl extends BaseService<User, IUserRepository> implements IUserRpcService {
    @Autowired
    IUserService userService;

    @Autowired
    Mapper dozerMapper;

    @Autowired
    IOrganizationUnitService organizationUnitService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    JPAQueryFactory queryFactory;

    @Autowired
    IRoleService roleService;

    @Autowired
    IUserManage userManage;

    @Autowired
    TokenAuthManager tokenAuthManager;

    @Autowired
    IOrganizationManagerRepository orgManagerRepository;

    QUser qUser = QUser.user;

    @Override
    public PagedResultDto<UserFullListDto> getUsers(PagedAndSortedInputDto input) {
        Page<User> pageResult = repository.findAll(input.getPageable());
        List<UserFullListDto> list = DozerUtils.mapList(dozerMapper, pageResult.getContent(), UserFullListDto.class);
        return new PagedResultDto<>(pageResult.getTotalElements(), list);
    }

    @Override
    public PagedResultDto<UserFullListDto> getUsers(UserInputDto input) {
        GetUsersInput userInput = dozerMapper.map(input, GetUsersInput.class);
        Page<User> pageResult = userService.getUsers(userInput);
        List<UserFullListDto> list = DozerUtils.mapList(dozerMapper, pageResult.getContent(), UserFullListDto.class);
        return new PagedResultDto<>(pageResult.getTotalElements(), list);
    }

    @Override
    public PagedResultDto<UserFullListSyncDto> getUsersForSync(PagedAndSortedInputDto input) {
        Page<User> pageResult = repository.findAll(input.getPageable());
        List<UserFullListSyncDto> list = DozerUtils.mapList(dozerMapper, pageResult.getContent(), UserFullListSyncDto.class, (s, t) -> {
            t.setOrgIds(s.getOrganizationUnit().stream().map(BaseEntity::getId).collect(Collectors.toList()));
        });
        return new PagedResultDto<>(pageResult.getTotalElements(), list);
    }

    @Override
    public UserFullListDto getUserFullById(Long userId) {
        return repository.findById(userId).map(value -> dozerMapper.map(value, UserFullListDto.class)).orElse(null);
    }

    @Override
    public List<UserFullListDto> getUserFullByIds(List<Long> userIds) {
        List<User> users = repository.findByIdIn(userIds);
        return DozerUtils.mapList(dozerMapper, users, UserFullListDto.class);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        if (userId != null) {
            return userManage.getUserPermissions(userId);
        }
        return new ArrayList<>();
    }


    @Override
    public List<Long> getUserIdsByNameLike(String name) {
        List<User> all = findAll(Specifications.where(e -> {
            e.eq(User.Fields.userStatus, UserStatus.OnJob);
            e.contains(User.Fields.name, name);
            e.in(User.Fields.userType, EUnitType.Inner);
        }));
        return all.stream().map(BaseEntity::getId).collect(Collectors.toList());
    }

    @Override
    public List<UserFullListDto> getUsersByUserNameStartWith(String head) {
        return getUsersByUserNameStartWith(head, Collections.singletonList(EUnitType.Inner));
    }


    @Override
    public List<UserFullListDto> getUsersByUserNameStartWith(String head, List<EUnitType> userTypes) {
        JPAQuery<User> query = queryFactory.selectFrom(qUser).where(qUser.userName.startsWith(head).and(qUser.userType.in(userTypes)).and(qUser.userStatus.eq(UserStatus.OnJob)));
        List<User> users = query.fetch();
        return DozerUtils.mapList(dozerMapper, users, UserFullListDto.class);
    }

    @Override
    public List<Long> getUserIdsByPermissionsAnd(String[] permissions) {
        return getUserIdsByPermissionsAnd(permissions, Collections.singletonList(EUnitType.Inner));
    }

    @Override
    public List<Long> getUserIdsByPermissionsAnd(String[] permissions, List<EUnitType> userTypes) {
        List<Long> result = new ArrayList<>();
        List<UserIdAndPermissionDto> data = repository.getUserIdsByPermissionsAnd(permissions, userTypes);
        Map<Long, List<UserIdAndPermissionDto>> collect = data.stream().collect(Collectors.groupingBy(UserIdAndPermissionDto::getId));
        collect.forEach((key, value) -> {
            List<String> all = value.stream().map(UserIdAndPermissionDto::getName).collect(Collectors.toList());
            long count = Arrays.stream(permissions).filter(p -> !all.contains(p)).count();
            if (count == 0 && !result.contains(key)) {
                result.add(key);
            }
        });
        return result;
    }

    @Override
    public List<Long> getUserIdsByPermissionsOr(String[] permissions) {
        return getUserIdsByPermissionsOr(permissions, Collections.singletonList(EUnitType.Inner));
    }

    @Override
    public List<Long> getUserIdsByPermissionsOr(String[] permissions, List<EUnitType> userTypes) {
        return repository.getUserIdsByPermissions(permissions, userTypes);
    }

    @Override
    public List<Long> getUserIdsByPermission(String permission) {
        return getUserIdsByPermissionsOr(new String[]{permission});
    }

    @Override
    public List<Long> getUserIdsByPermission(String permission, List<EUnitType> userTypes) {
        return null;
    }

    @Override
    public boolean checkUserContainOneOfPermissions(Long userId, String[] permissions) {
        if (permissions != null) {
            List<String> all = userManage.getUserPermissions(userId);
            long count = Arrays.stream(permissions).filter(all::contains).count();
            return count > 0;
        }
        return false;
    }

    @Override
    public boolean checkUserContainPermissions(Long userId, String[] permissions) {
        if (permissions != null) {
            List<String> all = userManage.getUserPermissions(userId);
            long count = Arrays.stream(permissions).filter(p -> !all.contains(p)).count();
            return count == 0;
        }
        return false;
    }

    @Override
    public boolean checkUserContainPermission(Long userId, String permission) {
        return checkUserContainPermissions(userId, new String[]{permission});
    }


    @Override
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public Long createOrUpdateUser(CreateOrUpdateUserInput createOrUpdateUserInput) {
        return userService.createOrUpdateUser(createOrUpdateUserInput);
    }

    @Override
    public void createOrUpdateUsers(List<CreateOrUpdateUserInput> inputList) {
        inputList.forEach(param -> {
            userService.createOrUpdateUser(param);
        });
    }

    @Override
    public UserFullListDto getUserByUserName(String userName) {
        return repository.findByUserName(userName).map(value -> dozerMapper.map(value, UserFullListDto.class)).orElse(null);
    }

    @Override
    public UserFullListDto getUserByName(String name) {
        List<User> users = repository.findByName(name);
        if (users.size() > 1) {
            throw new BusinessException("出现重名的用户【" + name + "】");
        }
        if (users.size() == 1) {
            return dozerMapper.map(users.get(0), UserFullListDto.class);
        }
        return null;
    }

    @Override
    public UserFullListDto getSuperiorUserFullById(Long userId) {
        return userService.getSuperiorUserFullById(userId);
    }

    @Override
    public Long getSuperiorId(Long userId) {
        return userService.getSuperiorId(userId);
    }

    @Override
    public List<UserFullListDto> getUsersContainsName(String name) {
        return getUsersContainsName(name, Collections.singletonList(EUnitType.Inner));
    }

    @Override
    public List<UserFullListDto> getUsersContainsName(String name, List<EUnitType> userTypes) {
        List<User> users = repository.findAll(Specifications.where(e -> {
            e.contains(User.Fields.name, name);
            e.in(User.Fields.userType, userTypes);
            e.eq(User.Fields.userStatus, UserStatus.OnJob);
        }));
        return DozerUtils.mapList(dozerMapper, users, UserFullListDto.class);
    }

    @Override
    public UserFullListDto getUserByWorkNumber(String workNumber) {
        return repository.findByWorkNumber(workNumber).map(value -> dozerMapper.map(value, UserFullListDto.class)).orElse(null);
    }

    @Override
    public void syncUserAndOrg(List<UserSimpleDto> saveUserList, List<OrganizationUnitDto> saveOrgList, List<OrganizationUnitUserDto> saveOrgUserList, List<Long> resignedUserIds) {


        // 删除离职员工
        if (Objects.nonNull(resignedUserIds) && !resignedUserIds.isEmpty()) {
            queryFactory.delete(QUser.user).where(QUser.user.id.in(resignedUserIds)).execute();
            log.info("删除离职员工 {}", resignedUserIds);
        }

        List<User> persistUserList = new ArrayList<>();
        List<OrganizationUnit> persistOrgList = new ArrayList<>();
        List<User> userList = findAll();
        List<OrganizationUnit> orgList = queryFactory.selectFrom(QOrganizationUnit.organizationUnit).fetch();
        List<Role> roleList = queryFactory.selectFrom(QRole.role).where(QRole.role.isDefault.eq(true)).fetch();


        for (OrganizationUnitDto unitDto : saveOrgList) {
            OrganizationUnit orgUnit = null;
            var it = orgList.iterator();
            boolean exists = false;
            while (it.hasNext()) {
                var next = it.next();
                if (next.getId().equals(unitDto.getId())) {
                    orgUnit = next;
                    it.remove();
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                orgUnit = new OrganizationUnit();
                orgUnit.setDisplayName(unitDto.getDisplayName());
                orgUnit.setCode(unitDto.getCode());
                orgUnit.setId(unitDto.getId());
            }
            if (unitDto.getParentId() == null) {
                orgUnit.setParentId(null);
            } else {
                orgUnit.setParentId(unitDto.getParentId());
            }
            persistOrgList.add(orgUnit);
        }

        for (OrganizationUnit child : persistOrgList) {
            for (OrganizationUnit parent : persistOrgList) {
                if (child.getParentId() != null && parent.getId().equals(child.getParentId())) {
                    child.setParentId(parent.getId());
                }
            }
        }
        int sortIndex = 1;
        for (OrganizationUnit unit : persistOrgList) {
            if (unit.getParentId() == null) {
                recursiveSaveOrganizationUnit(sortIndex++, unit, persistOrgList);
            }
        }

//        organizationUnitService.deleteInBatch(orgList);

        for (UserSimpleDto userDto : saveUserList) {
            User user = null;
            boolean exists = false;
            Iterator<User> userIterator = userList.iterator();
            while (userIterator.hasNext()) {
                var next = userIterator.next();
                if (BooleanUtils.isTrue(next.isAdmin())) {
                    userIterator.remove();
                    continue;
                }
                if (next.getId().equals(userDto.getId())) {
                    user = next;
                    exists = true;
                    userIterator.remove();
                    break;
                }
            }
            if (!exists) {
                user = new User();
                UtBeanUtils.copyNotNullProperties(userDto, user);
                log.info("新增的用户 {}", user);
            }
            user.setActive(true);
            user.setAccessFailedCount(0);
            if (StringUtils.isBlank(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getWorkNumber()));
                user.setShouldChangePasswordOnNextLogin(true);
            }
            if (user.getUserRoles().isEmpty()) {
                user.getUserRoles().addAll(roleList);
            }
            persistUserList.add(user);
            user.getOrganizationUnit().clear();
            var it = saveOrgUserList.iterator();
            while (it.hasNext()) {
                var next = it.next();
                if (next.getUserId().equals(user.getId())) {
                    var optional = persistOrgList.stream().filter(o -> o.getId().equals(next.getOrgId())).findFirst();
                    if (optional.isPresent()) {
                        user.getOrganizationUnit().add(optional.get());
                    }
                    it.remove();
                }
            }
        }
        batchSave(persistUserList);
//        deleteInBatch(userList);    // TODO: 2021/2/24 发布事件通知其他模块删除的用户


    }

    @Override
    public void syncFromLdap(List<UserSimpleDto> saveUserList, List<String> resignedUserNames, Map<String, List<OrganizationUnitDto>> userOrgMap) {
        Map<String, List<OrganizationUnit>> userOrgs = new HashMap<>();

        syncUserOrg(userOrgMap, userOrgs);

        syncUser(saveUserList, resignedUserNames, userOrgs);

    }

    @Override
    public List<Long> getCurrentRoles(Long userId) {
        return userService.getCurrentRoles(userId);
    }

    @Override
    public List<UserOrgDto> getUserOrgByIds(List<Long> userIds) {
        List<User> userList = repository.findAllById(userIds);
        return DozerUtils.mapList(dozerMapper, userList, UserOrgDto.class, ((user, userOrgDto) -> {
            List<OrganizationUnit> units = user.getOrganizationUnit();
            List<OrganizationUnitDto> unitDtos = DozerUtils.mapList(dozerMapper, units, OrganizationUnitDto.class);
            userOrgDto.setDtos(unitDtos);
        }));
    }

    @Override
    public List<UserFullListDto> getUsersByOrg(GetUsersByOrgInput input) {
        if(input.getOrgIds() == null || input.getOrgIds().isEmpty()){
            return new ArrayList<>();
        }
        GetOrganizationUnitUsersInput queryInput = new GetOrganizationUnitUsersInput();
        queryInput.setUnPage(true);
        queryInput.setOrganizationUnitIds(input.getOrgIds());
        queryInput.setDeepQuery(input.isDeepQuery());
        List<User> users = organizationUnitService.findUsers(queryInput).getContent();
        return DozerUtils.mapList(dozerMapper, users, UserFullListDto.class);
    }

    @Override
    public void removeToken(String authorization) {
        tokenAuthManager.removeToken(authorization);
    }

    private void syncUser(List<UserSimpleDto> saveUserList, List<String> resignedUserNames, Map<String, List<OrganizationUnit>> userOrgs) {
        List<Role> roleList = queryFactory.selectFrom(QRole.role).where(QRole.role.isDefault.eq(true)).fetch();
        if (CollectionUtil.isNotEmpty(resignedUserNames)) {
            log.info("删除离职员工 {}", resignedUserNames);
            // 删除离职员工
            queryFactory.delete(qUser).where(qUser.userName.in(resignedUserNames)).execute();
        }

        if (CollectionUtil.isNotEmpty(saveUserList)) {
            List<User> userList = new ArrayList<>();
            for (UserSimpleDto userSimpleDto : saveUserList) {
                User user = new User();
                if (Objects.nonNull(userSimpleDto.getId())) {
                    user = findById(userSimpleDto.getId()).orElse(new User());
                }
                if (Objects.isNull(user.getId())) {
                    dozerMapper.map(userSimpleDto, user);
                    // 新增的用户
                    user.setAccessFailedCount(0);
                    user.setActive(true);
                    user.setUserRoles(roleList);
                }
                // 获取用户组织，不在则添加
                List<OrganizationUnit> organizationUnits = getUserOrgs(user, userOrgs);
                user.setOrganizationUnit(organizationUnits);

                if (StringUtils.isBlank(user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(user.getWorkNumber()));
                    user.setShouldChangePasswordOnNextLogin(true);
                }
                userList.add(user);
            }

            batchSave(userList);
        }
    }

    // 去重 org，
    private List<OrganizationUnit> getUserOrgs(User user, Map<String, List<OrganizationUnit>> userOrgMap) {

        List<OrganizationUnit> organizationUnits = new ArrayList<>();
        Long userId = user.getId();
        if (Objects.nonNull(userId)) {
            EntityDto entityDto = new EntityDto();
            entityDto.setId(userId);
            organizationUnits = Optional.ofNullable(organizationUnitService.getOrganizationUnitsByUser(entityDto)).orElse(new ArrayList<>());
        }
        List<OrganizationUnit> userOrgs = userOrgMap.getOrDefault(user.getUserName(), new ArrayList<>());
        // 只同步最后一个层级
        if (CollectionUtil.isNotEmpty(userOrgs)) {
            int size = userOrgs.size();
            organizationUnits.add(userOrgs.get(size - 1));
        }

        Map<String, Boolean> orgCodeMap = new HashMap<>();
        List<OrganizationUnit> result = new ArrayList<>();
        for (OrganizationUnit organizationUnit : organizationUnits) {
            Boolean exist = orgCodeMap.get(organizationUnit.getCode());
            if (Objects.equals(exist, Boolean.TRUE)) {
                continue;
            }
            result.add(organizationUnit);
            orgCodeMap.put(organizationUnit.getCode(), Boolean.TRUE);

        }

        return result;
    }

    private void syncUserOrg(Map<String, List<OrganizationUnitDto>> userOrgMap, Map<String, List<OrganizationUnit>> userOrgs) {
        if (CollectionUtil.isEmpty(userOrgMap)) {
            return;
        }

        List<OrganizationUnit> orgList = queryFactory.selectFrom(QOrganizationUnit.organizationUnit).fetch();

        Map<String, Boolean> orgSavedCache = new HashMap<>();
        Map<String, OrganizationUnit> orgCache = new HashMap<>();


        userOrgMap.forEach((userName, orgs) -> {

            if (CollectionUtil.isNotEmpty(orgs)) {

                OrganizationUnit parent = null;
                for (int i = 0, len = orgs.size(); i < len; i++) {
                    OrganizationUnitDto orgDto = orgs.get(i);
                    OrganizationUnit orgUnit = Optional.ofNullable(findOrg(orgDto, orgList, orgCache)).orElse(new OrganizationUnit());

                    if (i > 0) {
                        // 避免parentId == id 造成死循环
                        if (Objects.nonNull(parent) && !Objects.equals(parent.getId(), orgUnit.getId())) {
                            orgUnit.setParentId(parent.getId());
                        }
                    }

                    // 覆盖信息
                    orgUnit.setDisplayName(orgDto.getDisplayName());
                    orgUnit.setCode(orgDto.getCode());
                    orgUnit.setSortIndex(Optional.ofNullable(orgUnit.getSortIndex()).orElse(i));

                    boolean inserted = Objects.isNull(orgUnit.getId());

                    Boolean hadSaved = orgSavedCache.computeIfAbsent(orgUnit.getCode(), k -> Boolean.FALSE);
                    if (!hadSaved) {
                        organizationUnitService.saveAndFlush(orgUnit);
                        orgSavedCache.put(orgUnit.getCode(), Boolean.TRUE);
                    }

                    if (inserted) {
                        orgList.add(orgUnit);
                    }

                    List<OrganizationUnit> organizationUnits = userOrgs.computeIfAbsent(userName, k -> new ArrayList<>());
                    organizationUnits.add(orgUnit);
                    userOrgs.put(userName, organizationUnits);

                    parent = orgUnit;
                }

            }

        });
    }

    private OrganizationUnit findOrg(OrganizationUnitDto orgDto, List<OrganizationUnit> orgList, Map<String, OrganizationUnit> orgCache) {
        // 不重复循环判断机构
        String code = orgDto.getCode();
        OrganizationUnit orgUnit = orgCache.get(code);
        if (Objects.isNull(orgUnit)) {
            for (OrganizationUnit org : orgList) {
                if (Objects.equals(org.getCode(), code)) {
                    orgUnit = org;
                    orgCache.put(code, orgUnit);
                    break;
                }
            }
        }

        return orgUnit;
    }

    @Override
    public void syncUserByWeChatFromLdap(UserSimpleDto userDto, List<OrganizationUnitDto> employeeOrgList, OrganizationUnitUserDto orgUserDto) {
        var optional = findById(userDto.getId());
        User user = null;
        if (optional.isPresent()) {
            user = optional.get();
            UtBeanUtils.copyNotNullProperties(userDto, user);
        } else {
            user = dozerMapper.map(userDto, User.class);
            List<Role> roleList = queryFactory.selectFrom(QRole.role).where(QRole.role.isDefault.eq(true)).fetch();
            user.getUserRoles().addAll(roleList);
        }
        List<OrganizationUnit> organizationUnitList = new ArrayList<>();
        int i = 0;
        for (OrganizationUnitDto unitDto : employeeOrgList) {
            Optional<OrganizationUnit> op = organizationUnitService.findById(unitDto.getId());
            OrganizationUnit org = null;
            if (op.isPresent()) {
                org = op.get();
            } else {
                org = dozerMapper.map(unitDto, OrganizationUnit.class);
            }
            if (unitDto.getParentId() == null) {
                org.setParentId(null);
            } else {
                org.setParentId(organizationUnitList.get(i - 1) == null ? null : organizationUnitList.get(i - 1).getId());
            }
            organizationUnitService.save(org);
            organizationUnitList.add(org);
            i++;
        }
        user.setAccessFailedCount(0);
        user.setActive(true);
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getWorkNumber()));
            user.setShouldChangePasswordOnNextLogin(true);
        }
        user.getOrganizationUnit().clear();
        for (OrganizationUnit unit : organizationUnitList) {
            if (unit.getId().equals(orgUserDto.getOrgId())) {
                user.getOrganizationUnit().add(unit);
                break;
            }
        }
        save(user);
    }

    @Override
    public List<UserSimpleDto> loadAllUserList() {
        return queryFactory.select(createUserSimpleQBean()).from(qUser).fetch();
    }


    @Override
    public UserSimpleDto getUserSimpleDto(Long userId) {
        return queryFactory.select(createUserSimpleQBean()).from(qUser).where(qUser.id.eq(userId)).fetchOne();
    }

    @Override
    public List<UserSimpleDto> getUserSimpleDto(Long... userId) {
        if(Objects.isNull(userId)||userId.length==0){
            return new ArrayList<>();
        }
        List<UserSimpleDto> list;
        if(userId.length==1){
            list = new ArrayList<>();
            UserSimpleDto user = getUserSimpleDto(userId[0]);
            if(!Objects.isNull(user)){
                list.add(user);
            }
        }else{
            list = queryFactory.select(createUserSimpleQBean()).from(qUser).where(qUser.id.in(userId)).fetch();
        }
        return list;
    }

    @Override
    public List<UserSimpleDto> getUserSimpleDto(Collection<Long> userId) {
        if(Objects.isNull(userId)||userId.size()==0){
            return new ArrayList<>();
        }
        List<UserSimpleDto> list;
        if(userId.size()==1){
            list = new ArrayList<>();
            UserSimpleDto user = getUserSimpleDto(userId.iterator().next());
            if(!Objects.isNull(user)){
                list.add(user);
            }
        }else{
            list = queryFactory.select(createUserSimpleQBean()).from(qUser).where(qUser.id.in(userId)).fetch();
        }
        return list;
    }

    QBean<UserSimpleDto> createUserSimpleQBean(){
        return QBeanBuilder.create(UserSimpleDto.class).appendQEntity(qUser).build();
    }

    @Override
    public List<UserFullListDto> getUsersByRoleName(String roleName) {
        return getUsersByRoleName(roleName, Collections.singletonList(EUnitType.Inner));
    }

    @Override
    public List<UserFullListDto> getUsersByRoleName(String roleName, List<EUnitType> userTypes) {
        Role role = roleService.getRoleByRoleName(roleName);
        if (role == null) {
            return new ArrayList<>();
        }
        List<UserFullListDto> users = DozerUtils.mapList(dozerMapper, role.getUserRoles(), UserFullListDto.class);

        return users.stream().filter(dto -> userTypes.contains(dto.getUserType()) && UserStatus.OnJob.equals(dto.getUserStatus())).collect(Collectors.toList());
    }

    @Override
    public List<UserFullListDto> getUsersByRoleName(String roleName, boolean filterByStation) {
        return getUsersByRoleName(roleName, Collections.singletonList(EUnitType.Inner));
    }

    @Override
    public List<UserFullListDto> getUsersByRoleName(String roleName, boolean filterByStation, List<EUnitType> userTypes) {
        JPAQuery<User> query = queryFactory.selectFrom(qUser).where(qUser.userRoles.any().roleName.eq(roleName).and(qUser.userType.in(userTypes)).and(qUser.userStatus.eq(UserStatus.OnJob)));
        if (filterByStation) {
            String[] idStrings = DataFilterContextHolder.getDataFilterId().split(",");
            List<Long> ids = Arrays.stream(idStrings).map(Long::parseLong).collect(Collectors.toList());
            query.where(qUser.organizationUnit.any().id.in(ids));
        }
        return DozerUtils.mapList(dozerMapper, query.fetch(), UserFullListDto.class);
    }

    @Override
    public String generateToken(Long userId) {
        return userService.generateToken(userId);
    }

    @Override
    public UserFullListDto getByEmailAddressOrPhoneNo(String input) {
        var user = queryFactory.selectFrom(qUser).where(qUser.emailAddress.eq(input).or(qUser.phoneNo.eq(input))).fetchFirst();
        return Objects.isNull(user) ? null : dozerMapper.map(user, UserFullListDto.class);
    }

    @Override
    public String generateTokenByUserName(String userName) {
        return userService.generateTokenByUserName(userName);
    }

    @Override
    public String generateExpireTokenByUserName(String userName, long expireTime, TimeUnit unit) {
        return userService.generateExpireTokenByUserName(userName,expireTime,unit);
    }

    @Override
    public List<UserFullListDto> getUsersByRoleIds(List<Long> roleIds) {
        List<Role> roles = roleService.getRoleByIds(roleIds);
        if (roles == null) {
            return new ArrayList<>();
        }

        List<User> users = new ArrayList<>();
        for (Role role : roles) {
            users.addAll(role.getUserRoles());
        }
        users = users.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(User::getId))), ArrayList::new));
        return DozerUtils.mapList(dozerMapper, users, UserFullListDto.class);
    }

    @Override
    public void deleteUser(Long id) {
        EntityDto dto = new EntityDto();
        dto.setId(id);
        userService.deleteUser(dto);
    }


    @Override
    public Long totalCount() {
        return repository.count();
    }


    //region private method
    void recursiveSaveOrganizationUnit(int sortIndex, OrganizationUnit unit, List<OrganizationUnit> persistOrgList) {
        unit.setSortIndex(sortIndex);
        organizationUnitService.save(unit);
        int childSortIndex = 1;
        for (OrganizationUnit child : persistOrgList) {
            if (child.getParentId() != null && child.getParentId().equals(unit.getId())) {
                recursiveSaveOrganizationUnit(childSortIndex++, child, persistOrgList);
            }
        }
    }
    //endregion
}
