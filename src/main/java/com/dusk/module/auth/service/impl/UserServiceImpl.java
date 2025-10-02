package com.dusk.module.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.dusk.common.rpc.auth.dto.ChangePwdInput;
import com.dusk.common.rpc.auth.dto.CreateOrUpdateUserInput;
import com.dusk.common.rpc.auth.dto.UserEditDto;
import com.dusk.common.rpc.auth.dto.UserFullListDto;
import com.dusk.common.rpc.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.mqs.pusher.PushSMS;
import com.dusk.common.mqs.pusher.SmsPushConfig;
import com.dusk.common.mqs.pusher.SmsTemplateParam;
import com.dusk.common.mqs.utils.RabbitMQUtils;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.repository.*;
import com.dusk.module.auth.service.*;
import com.dusk.module.ddm.service.ISettingChecker;
import com.dusk.module.ddm.service.ISettingRpcService;
import com.github.dozermapper.core.Mapper;
import com.hankcs.hanlp.HanLP;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.annotation.DisableTenantFilter;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.UserLoginException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
//import com.dusk.common.module.face.service.IUserFaceRpcService;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.rabbitmq.RabbitConstant;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.station.AddUsersToStationInput;
import com.dusk.module.auth.dto.station.RemoveUserFromStationInput;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.enums.ELevel;
import com.dusk.module.auth.feature.LoginFeatureProvider;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.push.INotificationPushManager;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-04-28 10:05
 */
@Service
@Transactional
@Slf4j
public class UserServiceImpl extends BaseService<User, IUserRepository> implements IUserService {
    private final static String REDIS_KEY_RESET_PWD_CAPTCHA_KEY_PREFIX = "CRUX:PWD:RESET:";
    private final static String REDIS_KEY_RESET_PWD_CAPTCHA_SEND_KEY_PREFIX = "CRUX:PWD:RESET:SEND";
    private final static int RESET_PWD_CAPTCHA_TIME = 60 * 5;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IOrganizationUnitService organizationUnitService;
    @Autowired
    IGrantPermissionRepository grantPermissionRepository;
    @Autowired
    private IFeatureChecker featureChecker;
    @Autowired(required = false)
    private RedisUtil<Object> redisUtil;
    @Autowired
    IUserManage userManage;
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    ITenantRepository tenantRepository;
    @Autowired
    AppAuthConfig appAuthConfig;
    @Autowired
    SmsPushConfig smsPushConfig;
    @Autowired(required = false)
    INotificationPushManager pushManager;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    private IOrganizationManagerRepository organizationManagerRepository;
    //@Reference(timeout = 15000)
    //private IUserFaceRpcService userFaceRpcService;
    @Autowired
    private IStationService stationService;
    
    @Reference
    private ISettingRpcService settingRpcService;

    @Autowired
    private IUserWxRelationRepository userWxRelationRepository;

    @Resource
    private RabbitMQUtils rabbitMQUtils;

    //随机密码长度，可配置读取
    @Value("${app.setting.passwdLen}")
    private int passwdLen;

    @Value("${app.setting.mail.activeAddr}")
    private String activeAddr;

    //登陆异常代码
    public static final int CODE_USER_NOT_USE = 1000;   //用户未激活
    public static final int CODE_USER_NOT_ACTIVE = 1001;   //用户未激活
    public static final int CODE_USER_CHANGE_PASSWORD = 1002;  //首次登陆需要修改密码
    public static final int CODE_USER_LOCKED = 1003;   //账户已经被锁定
    public static final int CODE_USER_VALID = 1004;   //需要验证码
    public static final int CODE_USER_DIMISSION = 1005; // 用户已离职

    private static final int HOST_MAX_ERROR_COUNT = 10; //宿主登陆账号最大错误次数

    private static final int PASSWORD_ERROR_COUNT_VALID = 3;

    //登陆异常提醒
    private static final String ERROR_USER_NOT_FIND = "未找到用户!";
    private static final String ERROR_USER_PASSWORD_ERROR = "密码错误!";
    private static final String ERROR_USER_NOT_ACTIVE = "用户未激活!";
    private static final String ERROR_USER_DIMISSION = "用户已离职!";
    private static final String ERROR_USER_PHONE_NO = "手机号码格式错误!";
    private static final String ERROR_USER_CHANGE_PASSWORD = "首次登陆需要修改密码";
    private static final String ERROR_USER_LOCKED = "您的账户已经被锁定，请与管理员联系或者在24小时后重新登陆！";
    private static final String ERROR_ACCOUNT_OR_PASSWORD = "帐户名或密码错误！";
    private static final String ERROR_USER_NOT_USE = "账户不可用";

    //其他异常提示信息
    private static final String ERROR_USERS_LIMITED = "用户数超出当前限制！";
    public static final String ERROR_USERS_DUPLICATE = "用户名已被占用！";
    private static final String ERROR_MAIL_SEND_FAIL = "邮件发送失败！";
    private static final String ERROR_WORKNUMBER_DUPLICATE = "工号已被占用！";
    private static final String ERROR_ID_CARD_DUPLICATE = "身份证号码已被占用！";

    private static final String REDIS_KEY_ACTIVE = "CRUX:AUTH:USER:ACTIVE:";


    private static final String USER_IMAGE_TYPE_SIGNATURE = "signature";
    private static final String USER_IMAGE_TYPE_PROFILE = "profile";

    public static final String PERSONNEL_CONTROL_CONFIRM_ID_CARD = "Personnel.Control.Confirm.idCard";
    public static final String PERSONNEL_CONTROL_CONFIRM_PHONE = "Personnel.Control.Confirm.phone";

    private static final QOrganizationManager qOrganizationManager = QOrganizationManager.organizationManager;
    private static final QOrganizationUnit qOrganizationUnit = QOrganizationUnit.organizationUnit;

    //这里独立方法只有密码相关的校验，checkUserValid 是公共校验 方法是密码登陆入口
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User checkAndGetUser(String username, String password) {
        //active字段判断 是否需要激活才能访问，还有就是强制密码修改的问题 可以实现一下
        Long tenantId = TenantContextHolder.getTenantId();
        User user;
        Optional<User> user_optional;
        if (tenantId != null && featureChecker.isEnabled(LoginFeatureProvider.APP_LOGIN_IGNORE_CASE)) {
            user_optional = userRepository.findByUserNameIgnoreCase(username);
        } else {
            user_optional = userRepository.findByUserName(username);
        }
        if (user_optional.isPresent()) {
            user = user_optional.get();
        } else {
            log.info("帐户名或者密码输入异常，账户：{}，租户id：{},", username, tenantId);
            throw new UsernameNotFoundException(ERROR_ACCOUNT_OR_PASSWORD);
        }
        try {
            password = SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).decryptStr(password);
        } catch (Exception ex) {
            log.warn("国密4解密失败：{}", ex.getMessage());
        }

        //if (true && !passwordEncoder.matches(password, user.getPassword())) {
        //    //密码错误，根据特性进行账号锁定相关处理
        //    log.info("帐户名或者密码输入异常，账户：{}，租户id：{},", username, tenantId);
        //    if (tenantId != null && featureChecker.isEnabled(UserFeatureProvider.APP_USER_LOCK_ON_LOGIN)) {
        //        String maxCount = featureChecker.getValue(UserFeatureProvider.APP_USER_LOCK_ON_LOGIN_COUNT);
        //        String msg = checkAndLockUser(user, Integer.parseInt(maxCount));
        //        throw new BadCredentialsException(msg);
        //    } else if (tenantId == null) {
        //        String msg = checkAndLockUser(user, HOST_MAX_ERROR_COUNT);
        //        throw new BadCredentialsException(msg);
        //    }
        //
        //    throw new BadCredentialsException(ERROR_ACCOUNT_OR_PASSWORD);
        //}

        checkUserValid(user);

        //登陆成功，失败次数清0
        user.setAccessFailedCount(0);
        userRepository.save(user);

        return user;
    }

    @Override
    public Page<User> getUsers(GetUsersInput input) {
        QRole q_role = QRole.role;
        Specification<User> spec = Specifications.where(e -> {
            if (StringUtils.isNotBlank(input.getFilter())) {
                e.or(e2 -> {
                    e2.contains(User.Fields.name, input.getFilter()).contains(User.Fields.userName,
                                    input.getFilter()).contains(User.Fields.phoneNo, input.getFilter())
                            .contains(User.Fields.workNumber, input.getFilter()).contains(User.Fields.emailAddress,
                                    input.getFilter());

                    List<Long> fetch =
                            queryFactory.select(q_role.id).from(q_role).where(q_role.roleName.contains(input.filter)).fetch();
                    if (fetch.size() > 0) {
                        e2.in(User.Fields.userRoles + "." + BaseEntity.Fields.id, fetch);
                    }
                });
            }
            if (input.getRoleId() != null) {
                e.eq(User.Fields.userRoles + "." + BaseEntity.Fields.id, input.getRoleId());
            }
            if (StringUtils.isNotEmpty(input.getRoleCode())) {
                List<Long> roleIds =
                        queryFactory.select(q_role.id).from(q_role).where(q_role.roleCode.contains(input.getRoleCode())).fetch();
                e.in(User.Fields.userRoles + "." + BaseEntity.Fields.id, roleIds);
            }
            if (StringUtils.isNotEmpty(input.getRoleName())) {
                List<Long> roleIds =
                        queryFactory.select(q_role.id).from(q_role).where(q_role.roleName.contains(input.getRoleName())).fetch();
                e.in(User.Fields.userRoles + "." + BaseEntity.Fields.id, roleIds);
            }
            if (StringUtils.isNotBlank(input.getPermission())) {
                List<Long> roleIds = grantPermissionRepository.findRoleIdsByPermissionName(input.getPermission());
                e.in(User.Fields.userRoles + "." + BaseEntity.Fields.id, roleIds);
            }
            if (input.getUserType() != null) {
                e.eq(User.Fields.userType, input.getUserType());
            }
            if (!input.isDisplayDimissionUsers()) {
                e.eq(User.Fields.userStatus, UserStatus.OnJob);
            }
            e.ge(input.isOnlyLockedUsers(), User.Fields.lockoutEndDateUtc, LocalDateTime.now());
            e.getQuery().distinct(true);
        });
        return findAll(spec, input.getPageable());
    }

    @Override
    public PagedResultDto<UserListDto> getUsersList(GetUsersInput getUserInput) {
        Page<User> page = getUsers(getUserInput);
        List<UserListDto> list = DozerUtils.mapList(dozerMapper, page.getContent(), UserListDto.class, (s, t) -> {
            if (s.getLockoutEndDateUtc() != null && s.getLockoutEndDateUtc().compareTo(LocalDateTime.now()) > 0) {
                t.setLock(true);
            }
            t.setActive(checkUserIsActive(s));
        });
        return new PagedResultDto<>(page.getTotalElements(), list);
    }

    @Override
    public Page<User> getOrgaUsers(GetOrgaUsersInput input) {
        OrganizationUnit orga = organizationUnitService.findById(input.getOrgaId()).orElseThrow(() -> new BusinessException("组织机构id不存在"));
        QUser qUser = QUser.user;
        JPAQuery<User> query = queryFactory.selectFrom(qUser).where(qUser.organizationUnit.contains(orga));
        if (StrUtil.isNotBlank(input.getFilter())) {
            query.where(qUser.name.contains(input.getFilter())
                    .or(qUser.userName.contains(input.getFilter()))
                    .or(qUser.phoneNo.contains(input.getFilter()))
                    .or(qUser.workNumber.contains(input.getFilter())));
        }
        Role role = null;
        //调整 by kefuming  优先基于id查询，如果id不存在 才基于name查询
        if (input.getRoleId() != null) {
            role = roleService.findById(input.getRoleId()).orElse(null);
        } else if (StrUtil.isNotBlank(input.getRoleName())) {
            role = roleService.getRoleByRoleName(input.getRoleName());
        }
        if (role != null) {
            query.where(qUser.userRoles.contains(role));
        }
        Page<User> page = (Page<User>) page(query, input.getPageable());
        return page;
    }

    @Override
    public Page<User> getUsersByRoleCodes(GetUsersByRoleCodesInput input) {
        QRole q_role = QRole.role;
        QUser q_user = QUser.user;
        List<Long> roleIds =
                queryFactory.select(q_role.id).from(q_role).where(q_role.roleCode.in(input.getRoleCodeList())).fetch();
        JPAQuery<User> query;
        if (input.isAllRoles()) {
            query = queryFactory.selectFrom(q_user).where(q_user.id.in(queryFactory.select(q_user.id).from(q_user).leftJoin(q_user.userRoles).where(q_user.userRoles.any().id.in(roleIds)).groupBy(q_user.id)
                    .having(q_user.id.count().goe(roleIds.size()))));
        } else {
            query = queryFactory.selectDistinct(q_user).from(q_user).where(q_user.userRoles.any().id.in(roleIds));
        }
        if (input.getUserType() != null) {
            query.where(q_user.userType.eq(input.getUserType()));
        }
        return (Page<User>) page(query, input.getPageable());
    }

    @Override
    public Page<User> getUsersByRoleName(GetUsersByRoleNameInput input) {
        QRole q_role = QRole.role;
        QUser q_user = QUser.user;
        JPAQuery<User> query = queryFactory.selectDistinct(q_user).from(q_user)
                .where(q_user.userRoles.any().id.in(queryFactory.select(q_role.id).distinct().from(q_role).where(q_role.roleName.in(input.getRoleNames())).fetch()));
        if (input.getUserType() != null) {
            query.where(q_user.userType.eq(input.getUserType()));
        }
        return (Page<User>) page(query, input.getPageable());
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getAllInnerUsers() {
        QUser qUser = QUser.user;
        return queryFactory.selectFrom(qUser).where(qUser.userType.eq(EUnitType.Inner)).fetch();
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
    }

    @Override
    public void getUsersToExcel(HttpServletResponse response) {
        String fileName = "userlist.xlsx";
        List<User> userList = userRepository.findAll(Specifications.where(e -> e.ne(User.Fields.userType, EUnitType.Visitor)), Sort.by(User.Fields.surName));
        Map<Long, List<String>> orgNamePathMap = getAllUserOrgMap();

        //先去掉Role里面关联字段，以免序列化时出错
//        List<UserListDto> userDtoList = DozerUtils.mapList(dozerMapper, userList, UserListDto.class);
        List<UserExcellDto> outUserList = DozerUtils.mapList(dozerMapper, userList, UserExcellDto.class, (s, t) -> {
            t.setRoleNames(s.getUserRoles().stream().map(Role::getRoleName).collect(Collectors.joining(",")));
            List<String> orgs = orgNamePathMap.get(s.getId());
            if (orgs != null && orgs.size() > 0) {
                t.setOrgNamePath(String.join(";", orgs));
            }
            t.setUserType(s.getUserType().getDisplayName());
        });
        //CruxExcelUtils.exportSimpleExcel(response, fileName, outUserList, UserExcellDto.class);
    }

    @Override
    public GetUserForEditOutput getUserForEdit(EntityDto entityDto) {
        //获取所有角色
        List<Role> allRoles = roleService.findAll();
        List<UserRoleDto> roles = DozerUtils.mapList(dozerMapper, allRoles, UserRoleDto.class);
        GetUserForEditOutput output = new GetUserForEditOutput();
        output.setRoles(roles);
        output.setMemberedOrganizationUnits(new ArrayList<>());
        if (entityDto.getId() == null) {
            //创建用户
            UserEditDto ue = new UserEditDto();
            ue.setActive(true);
            ue.setShouldChangePasswordOnNextLogin(true);
            // TODO: 2020/5/18 通过配置初始化用户登陆属性
            output.setUser(ue);
        } else {
            var user =
                    userRepository.findById(entityDto.getId()).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
            output.setUser(dozerMapper.map(user, UserEditDto.class));
            dozerMapper.map(user, output);
            // 厂站信息
            output.setStations(stationService.getStationsForFrontByUserId(user.getId()));
            for (UserRoleDto userRoleDto : roles) {
                for (Role r : user.getUserRoles()) {
                    if (userRoleDto.getRoleId().equals(r.getId())) {
                        userRoleDto.setAssigned(true);
                    }
                }
            }
            List<OrganizationUnit> organizationUnits =
                    organizationUnitService.getOrganizationUnitsByUser(new EntityDto(user.getId()));
            output.setUserOrgaDtoList(DozerUtils.mapList(dozerMapper, organizationUnits, UserOrgaDto.class));
            output.setMemberedOrganizationUnits(organizationUnits.stream().map(BaseEntity::getId).collect(Collectors.toList()));
            // 获取岗位管理信息
            List<OrganizationUnit> managerUnit = queryFactory.selectFrom(qOrganizationUnit)
                    .leftJoin(qOrganizationManager).on(qOrganizationUnit.id.eq(qOrganizationManager.orgId))
                    .where(qOrganizationManager.userId.eq(user.getId())).fetch();
            if (managerUnit.isEmpty()) {
                output.setLevel(ELevel.General);
            } else {
                output.setLevel(ELevel.Manager);
                List<UserOrgaDto> managerDto = DozerUtils.mapList(dozerMapper, managerUnit, UserOrgaDto.class);
                output.setManagerOrgDtos(managerDto);
            }

        }
        return output;
    }

    @Override
    public GetExternalUserEditOutput getExternalUserEditInfo(Long userId) {
        User user = findById(userId).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
        GetExternalUserEditOutput output = dozerMapper.map(user, GetExternalUserEditOutput.class);

        //获取所有角色
        List<Role> allRoles = roleService.findAll();
        List<UserRoleDto> roles = DozerUtils.mapList(dozerMapper, allRoles, UserRoleDto.class);
        for (UserRoleDto userRoleDto : roles) {
            for (Role r : user.getUserRoles()) {
                if (userRoleDto.getRoleId().equals(r.getId())) {
                    userRoleDto.setAssigned(true);
                }
            }
        }
        output.setRoles(roles);

        // 获取所属的组织
        List<OrganizationUnit> organizationUnits = organizationUnitService.getOrganizationUnitsByUser(new EntityDto(user.getId()));
        output.setUserOrgaDtoList(DozerUtils.mapList(dozerMapper, organizationUnits, UserOrgaDto.class));

        // 获取岗位管理信息
        List<OrganizationUnit> managerUnit = queryFactory.selectFrom(qOrganizationUnit)
                .leftJoin(qOrganizationManager).on(qOrganizationUnit.id.eq(qOrganizationManager.orgId))
                .where(qOrganizationManager.userId.eq(user.getId())).fetch();
        List<UserOrgaDto> managerDto = DozerUtils.mapList(dozerMapper, managerUnit, UserOrgaDto.class);
        output.setManagerOrgDtos(managerDto);

        return output;
    }

    @Override
    public GetUserInfoOutput getUserInfo(Long id) {
        var user = userRepository.findById(id).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
        BaseUserInfoDto baseUserInfoDto = dozerMapper.map(user, BaseUserInfoDto.class);
        AccountInfoDto accountInfoDto = dozerMapper.map(user, AccountInfoDto.class);
        GetUserInfoOutput output = new GetUserInfoOutput();
        output.setAccountInfoDto(accountInfoDto);
        baseUserInfoDto.setLevel(ELevel.General);
        output.setBaseUserInfoDto(baseUserInfoDto);

        //获取所有角色
        List<Role> allRoles = roleService.findAll();
        List<UserRoleDto> roles = DozerUtils.mapList(dozerMapper, allRoles, UserRoleDto.class);
        accountInfoDto.setRoles(roles);
        for (UserRoleDto userRoleDto : roles) {
            for (Role r : user.getUserRoles()) {
                if (userRoleDto.getRoleId().equals(r.getId())) {
                    userRoleDto.setAssigned(true);
                }
            }
        }
        List<OrganizationUnit> organizationUnits = organizationUnitService.getOrganizationUnitsByUser(new EntityDto(id));
        List<UserOrgaDto> userOrgaDtos = DozerUtils.mapList(dozerMapper, organizationUnits, UserOrgaDto.class);
        baseUserInfoDto.setOrgaDtos(userOrgaDtos);
        List<OrganizationUnit> organizationUnit = queryFactory.select(qOrganizationUnit).from(qOrganizationManager)
                .leftJoin(qOrganizationUnit).on(qOrganizationUnit.id.eq(qOrganizationManager.orgId))
                .where(qOrganizationManager.userId.eq(user.getId())).fetch();
        if (!organizationUnit.isEmpty()) {
            List<UserOrgaDto> managerDtos = DozerUtils.mapList(dozerMapper, organizationUnit, UserOrgaDto.class);
            baseUserInfoDto.setManagerDtos(managerDtos);
            baseUserInfoDto.setLevel(ELevel.Manager);
        }

        UserFullListDto superiorUser = getSuperiorUserFullById(id);
        baseUserInfoDto.setSuperior(superiorUser == null ? null : superiorUser.getUserName());

        return output;
    }

    @Override
    public UserFullListDto getSuperiorUserFullById(Long userId) {
        Long superiorId = getSuperiorId(userId);
        if (superiorId != null) {
            return dozerMapper.map(getUserById(superiorId), UserFullListDto.class);
        } else {
            return null;
        }
    }

    @Override
    public Long getSuperiorId(Long userId) {
        List<OrganizationUnit> units = organizationUnitService.getOrganizationUnitsByUser(new EntityDto(userId));
        if (CollectionUtil.isEmpty(units)) {
            return null;
        } else {
            return findSuperiorId(units.get(0));
        }
    }
    /**
     * 递归查询用户上级
     */
    private Long findSuperiorId(OrganizationUnit unit) {
        Optional<OrganizationManager> one = organizationManagerRepository.findOne(Specifications.where(e -> e.eq(OrganizationManager.Fields.orgId, unit.getId())));
        if (one.isPresent()) {
            return one.get().getUserId();
        }
        if (unit.getParentId() == null) {
            return null;
        }
        OrganizationUnit parentUnit = organizationUnitService.findById(unit.getParentId()).orElseThrow();
        return findSuperiorId(parentUnit);
    }

    @Override
    public void updatePersonalInfo(PersonalInfoInput input) {
        User user = getUserById(input.getId());
        dozerMapper.map(input, user);

        // 外单位用户的手机号码为必填
        checkMobilePhoneIfNeed(user.getId(), user.getPhoneNo(), user.getUserType());
        checkIdCardIfNeed(user.getId(), user.getIdCard(), user.getUserType());

        // 工号唯一性校验
        if (StrUtil.isNotBlank(input.getWorkNumber()) && !checkUnique(input.getId(), User.Fields.workNumber, input.getWorkNumber())) {
            throw new BusinessException("[" + input.getWorkNumber() + "]" + ERROR_WORKNUMBER_DUPLICATE);
        }
        updateOrganizationRelation(user, Collections.singletonList(input.getOrgaId()));
        List<Long> orgIds = input.getManagerOrgIds();
        organizationManagerRepository.deleteByUserId(input.getId());
        if (IterUtil.isNotEmpty(orgIds)) {
            existsOrganization(orgIds);
            var managers = new ArrayList<OrganizationManager>(orgIds.size());
            orgIds.forEach(orgId -> managers.add(new OrganizationManager(orgId, user.getId())));
            organizationManagerRepository.saveAll(managers);
        }
        save(user);
    }

    @Override
    public Long createOrUpdateUser(CreateOrUpdateUserInput createOrUpdateUserInput) {
        Long userId = createOrUpdateUserInput.getUser().getId();
        if (userId == null) {
            userId = createUser(createOrUpdateUserInput, createOrUpdateUserInput.getUser().getUserType());
        } else {
            updateUser(createOrUpdateUserInput, "password", "profilePictureId", "signaturePictureId");
        }
        return userId;
    }


    @Override
    public String generateToken(Long userId) {
        User user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在！"));
        return generateToken(user);
    }

    @Override
    public void createOrUpdateUserExistByUserName(CreateOrUpdateUserInput createOrUpdateUserInput) {
        Long userId = createOrUpdateUserInput.getUser().getId();
        if (userId == null) {
            createUser(createOrUpdateUserInput, EUnitType.Inner);
        } else {
            //第二个参数为null时默认根据id判断唯一
            updateUser(createOrUpdateUserInput, "userName", "password");
        }
    }

    @Override
    @Transactional
    public void createOrUpdateUserExistByUserName(CreateOrUpdateUserInfoInput input) {
        Long userId = input.getUser().getId();
        if (userId == null) {
            userId = createUser(input, EUnitType.Inner);
        } else {
            //第二个参数为null时默认根据id判断唯一
            updateUser(input, "userName", "password");
        }
        updateOrgManager(input.getLevel(), input.getManagerOrgIds(), userId);
        updateUserInfo(userId, input);

        // 设置厂站
        updateUserStationRel(userId, input.getStationIds());
    }

    @Override
    @Transactional
    public Long createExternalUser(CreateExternalUserInput input) {
        Long userId;
        User user = dozerMapper.map(input, User.class);
        user.setUserType(EUnitType.External);
        user.setUserStatus(UserStatus.OnJob);

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            //根据特性检查租户最大用户数
            if (featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)) {
                String maxUsers = featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER);
                int userCount = (int) userRepository.count();
                if (userCount >= Integer.parseInt(maxUsers == null ? "" : maxUsers)) {
                    throw new BusinessException(ERROR_USERS_LIMITED);
                }
            }
        }

        // 校验员工年龄
        // checkUserAge(input.getIdCard());
        checkIdCardIfNeed(null, input.getIdCard(), user.getUserType());
        checkMobilePhoneIfNeed(null, input.getPhoneNo(), user.getUserType());

        // 校验工号不能重复
        String workNumber = input.getWorkNumber();
        if (StrUtil.isNotBlank(workNumber) && !checkUnique(null, User.Fields.workNumber, workNumber)) {
            throw new BusinessException("[" + workNumber + "]" + ERROR_WORKNUMBER_DUPLICATE);
        }

        // 账号信息设置
        if (input.getDto() != null) {
            userId = saveUserAccount(input.getDto(), user);
        } else {
            userId = save(user).getId();
        }

        // 添加人脸信息
        //if (StrUtil.isNotBlank(input.getFacePicture()) && DubboCustomUtils.isValidRpcService(IUserFaceRpcService.class.getName())) {
        //    String[] imageParts = input.getFacePicture().split(",");
        //    if (imageParts.length != 2) {
        //        throw new BusinessException("上传的图片base64编码有误");
        //    }
        //    userFaceRpcService.addFace(userId, imageParts[1]);
        //}

        // 更新组织机构
        updateOrganizationRelation(user, Collections.singletonList(input.getOrgaId()));

        // 更新组织机构管理关系
        updateOrgManager(input.getLevel(), input.getManagerOrgIds(), userId);
        return userId;
    }

    private void checkUserAge(String idCard) {
        int birthYear = Integer.parseInt(idCard.substring(6, 10));
        int gender = Integer.parseInt(idCard.substring(16, 17));

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        if (gender % 2 == 0) {  // 女性
            if (currentYear - birthYear > 55) {
                throw new BusinessException("超过年龄限制:女55岁");
            }
        } else {  // 男性
            if (currentYear - birthYear > 60) {
                throw new BusinessException("超过年龄限制:男60岁");
            }
        }

    }

    @Override
    public Long updateExternalUserInfo(ExternalUserSettingDto dto) {
        User user = findById(dto.getId()).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
        return saveUserAccount(dto, user);
    }

    /**
     * 保存账号信息
     */
    private Long saveUserAccount(ExternalUserSettingDto input, User user) {
        String hasAccount = user.getUserName(); // 判断是否已经有账号，有账号就不设置密码
        dozerMapper.map(input, user);
        //设置密码
        if (user.getId() == null || StrUtil.isBlank(hasAccount)) {
            String decryptSm4 = decryptSm4(input.getPassword(), "密码解密失败,未经加密或错误的加密算法");
            user.setPassword(passwordEncoder.encode(decryptSm4));
            user.setSurName(HanLP.convertToPinyinString(user.getName(), "", false));
        }

        // 校验账号不能重复
        String userName = input.getUserName();
        if (!checkUnique(user.getId(), User.Fields.userName, userName)) {
            throw new BusinessException("[" + userName + "]" + ERROR_USERS_DUPLICATE);
        }

        // 设置角色
        List<Long> assignedRoleIds = input.getAssignedRoleIds();
        if (assignedRoleIds != null) {
            List<Role> roles = roleService.findAllById(assignedRoleIds);
            user.setUserRoles(roles);
        }
        Long userId = saveAndFlush(user).getId();
        updateUserStationRel(userId, input.getStations());
        return userId;
    }

    /**
     * 更新厂站
     */
    private void updateUserStationRel(Long userId, List<Long> newStationIds) {
        if(CollectionUtils.isEmpty(newStationIds)) {
            return;
        }
        List<Long> oldStationIds = stationService.getStationsForFrontByUserId(userId).stream().map(StationsOfLoginUserDto::getValue).collect(Collectors.toList());
        ArrayList<Long> retainList = new ArrayList<>();
        RemoveUserFromStationInput removeUserFromStationInput = new RemoveUserFromStationInput();
        AddUsersToStationInput addUsersToStationInput = new AddUsersToStationInput();
        removeUserFromStationInput.setUserId(userId);
        addUsersToStationInput.setUserIds(Collections.singletonList(userId));

        for (var oldStationId : oldStationIds) {
            for (var newStationId : newStationIds) {
                if (oldStationId.equals(newStationId)) {
                    retainList.add(oldStationId);
                }
            }
        }
        newStationIds.removeAll(retainList);
        oldStationIds.removeAll(retainList);

        oldStationIds.forEach(stationId -> {
            removeUserFromStationInput.setStationId(stationId);
            stationService.removeUserFromStation(removeUserFromStationInput);
        });
        newStationIds.forEach(stationId -> {
            addUsersToStationInput.setStationId(stationId);
            stationService.addUsersToStation(addUsersToStationInput);
        });
    }

    /**
     * 更新组织机构与用户关联关系
     * @param user 用户
     * @param organizationIds 组织机构id列表
     */
    private void updateOrganizationRelation(User user, List<Long> organizationIds) {
        if (organizationIds != null) {
            List<OrganizationUnit> units = organizationUnitService.findAllById(organizationIds);
            user.setOrganizationUnit(units);
            // 如果组织机构是厂站且唯一，则设置为默认厂站
            List<OrganizationUnit> stations = units.stream().filter(p -> Boolean.TRUE.equals(p.getStation())).collect(Collectors.toList());
            if (stations.size() == 1) {
                user.setDefaultStation(units.get(0).getId());
            } else {
                user.setDefaultStation(null);
            }
        }
    }

    @Transactional
    public Long createUser(CreateOrUpdateUserInput createOrUpdateUserInput, EUnitType type) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            //根据特性检查租户最大用户数
            if (featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)) {
                String maxUsers = featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER);
                int userCount = (int) userRepository.count();
                if (userCount >= Integer.parseInt(maxUsers == null ? "" : maxUsers)) {
                    throw new BusinessException(ERROR_USERS_LIMITED);
                }
            }
        }
        if (StrUtil.isBlank(createOrUpdateUserInput.getUser().getPassword())) {
            throw new BusinessException("密码不能为空.");
        }
        User user = dozerMapper.map(createOrUpdateUserInput.getUser(), User.class);
        user.setUserType(type);
        user.setUserStatus(UserStatus.OnJob);
        //仅在创建用户设置密码
        if (createOrUpdateUserInput.isSetRandomPassword()) {
            String randomPwd = generatePassword(passwdLen);
            user.setPassword(randomPwd);
            //发送邮件通知接收随机密码
            try {
                String title = "账号注册通知";
                String content = "您的随机密码是：" + randomPwd + ",请及时修改!";
                emailService.sendEmail(title, content, user.getEmailAddress());
            } catch (Exception e) {
                throw new BusinessException(ERROR_MAIL_SEND_FAIL);
            }
        } else {
            String decryptSm4 = decryptSm4(createOrUpdateUserInput.getUser().getPassword(), "密码解密失败,未经加密或错误的加密算法");
            user.setPassword(passwordEncoder.encode(decryptSm4));
        }
        return saveUser(createOrUpdateUserInput, user);
    }

    @Transactional
    public void updateUser(CreateOrUpdateUserInput createOrUpdateUserInput, String... ignoreFields) {
        User user = getUserById(createOrUpdateUserInput.getUser().getId());
        //编辑用户忽略帐户名密码
        BeanUtil.copyProperties(createOrUpdateUserInput.getUser(), user, ignoreFields);
        saveUser(createOrUpdateUserInput, user);
    }


    @Override
    public void deleteUser(EntityDto entityDto) {
        Long id = entityDto.getId();
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
        userRepository.delete(user);
        //if (DubboCustomUtils.isValidRpcService(IUserFaceRpcService.class.getName())) {
        //    userFaceRpcService.removeUser(id);
        //}
        organizationManagerRepository.deleteByUserId(id);

        //删除企业微信绑定
        userWxRelationRepository.deleteAllByUserId(id);
        //删除人脸
        //userFaceRpcService.removeFace(id);
        //删除门禁权限
        CancelAuthDto cancelAuthDto = new CancelAuthDto(TenantContextHolder.getTenantId(), Collections.singletonList(id));
        rabbitMQUtils.publishMsgAsync(RabbitConstant.CANCEL_USER_AUTH_FANOUT_EXCHANGE_V1,"", cancelAuthDto);
    }

    @Override
    public void deleteUsers(List<EntityDto> entityDtos) {
        if (entityDtos == null || entityDtos.isEmpty()) {
            throw new BusinessException("传入的用户id不能为空");
        }
        Long loginId = LoginUserIdContextHolder.getUserId();
        List<Long> ids = entityDtos.stream().map(EntityDto::getId).filter(id -> !loginId.equals(id)).collect(Collectors.toList());
        userRepository.deleteByIdIn(ids);
        organizationManagerRepository.deleteByUserIdIn(ids);
    }

    @Override
    public void unlockUser(EntityDto entityDto) {
        User user = getUserById(entityDto.getId());
        user.setAccessFailedCount(0);
        user.setLockoutEndDateUtc(null);
        userRepository.save(user);
    }

    @Override
    public void changePassword(ChangePasswordInput cpi, UserContext userContext) {
        User u = getUserById(userContext.getId());
        String newPwd = decryptSm4(cpi.getNewPasswd(), "新密码解密失败,未经加密或错误的加密算法");
        if (passwordEncoder.matches(newPwd, u.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        String oldPwd = cpi.getOldPasswd();
        oldPwd = decryptSm4(oldPwd, "旧密码解密失败,未经加密或错误的加密算法");
        if (passwordEncoder.matches(oldPwd, u.getPassword())) {
            u.setPassword(passwordEncoder.encode(newPwd));
            u.setShouldChangePasswordOnNextLogin(false);
            u.setAccessFailedCount(0);
        } else {
            throw new BusinessException(ERROR_USER_PASSWORD_ERROR);
        }
        userRepository.save(u);
    }

    @Override
    public void listChangePassword(ChangePwdInput input) {
        User user = getUserById(input.getUserId());
        String password = decryptSm4(input.getNewPwd(), "密码解密失败,未经加密或错误的加密算法");
        if (passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(password));
        //下次登陆必须修改密码
        user.setShouldChangePasswordOnNextLogin(true);
        save(user);
    }

    @Override
    public void changeStatus(ChangeStatusInput input) {
        List<Long> userIds = input.getUserIds();
        List<User> userList = findAllById(userIds);
        userList.forEach(user -> user.setUserStatus(input.getStatus()));
        saveAll(userList);
        if (input.getStatus().equals(UserStatus.Dimission)) {
            for(Long id : userIds) {
                //删除人脸
                //userFaceRpcService.removeFace(id);
            }
            CancelAuthDto cancelAuthDto = new CancelAuthDto(TenantContextHolder.getTenantId(), userIds);
            rabbitMQUtils.publishMsgAsync(RabbitConstant.CANCEL_USER_AUTH_FANOUT_EXCHANGE_V1,"", cancelAuthDto);
        }
    }

    @Override
    public List<UserListForLoginDto> getUsersForLogin(GetUsersForLoginInput getUsersForLoginInput) {
        Specification<User> spec = Specifications.where(e -> {
            if (StringUtils.isNotBlank(getUsersForLoginInput.getName())) {
                e.contains(User.Fields.name, getUsersForLoginInput.getName());
            }
            if (StringUtils.isNotBlank(getUsersForLoginInput.getSurName())) {
                e.contains(User.Fields.surName, getUsersForLoginInput.getSurName());
            }
            if (StringUtils.isNotBlank(getUsersForLoginInput.getUserName())) {
                e.contains(User.Fields.userName, getUsersForLoginInput.getUserName());
            }
        });
        List<User> userList = userRepository.findAll(spec, Sort.by(User.Fields.name, User.Fields.surName));

        return DozerUtils.mapList(dozerMapper, userList, UserListForLoginDto.class);
    }

    @Override
    public List<Long> getCurrentRoles(Long userId) {
        return userRepository.getOne(userId).getUserRoles().stream().map(BaseEntity::getId).collect(Collectors.toList());
    }

    @Override
    public void activeUser(Long userId, String key, String code) {
        if (redisUtil != null && redisUtil.getCache(key) != null && code.equals(redisUtil.getCache(key).toString())) {
            throw new BusinessException("验证失败！");
        }
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void setDefaultStation(SetDefaultStationInput setDefaultStationInput) {
        User u = getUserById(securityUtils.getCurrentUser().getId());
        stationService.findById(setDefaultStationInput.getStationId()).orElseThrow(() -> new BusinessException("厂站不存在."));
        u.setDefaultStation(setDefaultStationInput.getStationId());
        userRepository.save(u);
    }

    @Override
    public void checkUserValid(User user) {
        //用户锁定校验
        if (null != user.getLockoutEndDateUtc() && user.getLockoutEndDateUtc().isAfter(LocalDateTime.now())) {
            UserLoginException result = new UserLoginException(ERROR_USER_LOCKED);
            result.setCode(CODE_USER_LOCKED);
            throw result;
        }

        //用户激活校验
//        if (!user.isActive()) {
//            UserLoginException result = new UserLoginException(ERROR_USER_NOT_ACTIVE);
//            result.setCode(CODE_USER_NOT_ACTIVE);
//            throw result;
//        } else {
//            //如果存在激活开始和结束 则继续判断
//            LocalDateTime now = LocalDateTime.now();
//            if ((user.getActiveStartDate() != null && now.isBefore(user.getActiveStartDate().atStartOfDay())) || (user.getActiveEndDate() != null && !now.isBefore(user.getActiveEndDate().plusDays(1).atStartOfDay()))) {
//                UserLoginException result = new UserLoginException(ERROR_USER_NOT_ACTIVE);
//                result.setCode(CODE_USER_NOT_ACTIVE);
//                throw result;
//            }
//        }
        // 检查员工是否离职
        if (UserStatus.Dimission.equals(user.getUserStatus())) {
            UserLoginException loginException = new UserLoginException(ERROR_USER_DIMISSION);
            loginException.setCode(CODE_USER_DIMISSION);
            throw loginException;
        }

        if (!checkUserIsActive(user)) {
            UserLoginException result = new UserLoginException(ERROR_USER_NOT_ACTIVE);
            result.setCode(CODE_USER_NOT_ACTIVE);
            throw result;
        }

        //租户账号校验租户是否可用
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId()).orElseThrow(() -> new BusinessException(
                    "账户不可用"));
            user.setTenant(tenant);
            if (!tenant.enabled()) {
                UserLoginException result = new UserLoginException(ERROR_USER_NOT_USE);
                result.setCode(CODE_USER_NOT_USE);
                throw result;
            }
        }

        //检查厂站是否被禁用
        //--改功能移除
        /*List<OrganizationUnit> stationsByUserId = organizationUnitService.getAllStationsByUserId(user.getId());
        //当用户关联了厂站并且所有的厂站都被禁用的情况下 禁止登陆系统
        if (stationsByUserId.size() > 0 && stationsByUserId.stream().filter(p -> Boolean.FALSE.equals(p.getStationEnabled())).count() == stationsByUserId.size()) {
            UserLoginException result = new UserLoginException(ERROR_USER_NOT_USE);
            result.setCode(CODE_USER_NOT_USE);
            throw result;
        }*/
    }


    @Override
    public void saveUserPicture(Long userId, Long minioId, String type) {
        User user = getUserById(userId);
        if (USER_IMAGE_TYPE_SIGNATURE.equals(type)) {
            user.setSignaturePictureId(minioId);
        } else if (USER_IMAGE_TYPE_PROFILE.equals(type)) {
            user.setProfilePictureId(minioId);
        } else {
            throw new BusinessException("参数传递不正确");
        }
        userRepository.save(user);
    }

    @Override
    public void getForgetPwdCaptchaByMobile(String userName, String mobile) {
        if (pushManager == null) {
            throw new BusinessException("消息推送服务未启用");
        }
        if (TenantContextHolder.getTenantId() != null) {
            String futureForgetPwd = featureChecker.getValue(LoginFeatureProvider.APP_LOGIN_FORGET_PWD);
            if (!(StringUtils.equals("mobile", futureForgetPwd) || StringUtils.equals("mobileOrEmail",
                    futureForgetPwd))) {
                throw new BusinessException("当前系统不允许通过手机找回密码");
            }
        }
        User user =
                repository.findByUserName(userName).orElseThrow(() -> new UsernameNotFoundException(ERROR_ACCOUNT_OR_PASSWORD));
        checkUserValid(user);
        if (!StringUtils.equals(mobile, user.getPhoneNo())) {
            throw new BusinessException("输入手机号不匹配");
        }
        String redisKeySuffix = user.getTenantId() == null ? "HOST:" + userName : (user.getTenantId() + ":" + userName);
        if (redisUtil.getCache(REDIS_KEY_RESET_PWD_CAPTCHA_SEND_KEY_PREFIX + redisKeySuffix) != null) {
            throw new BusinessException("发送频率过快");
        }
        String captcha = RandomStringUtils.randomNumeric(6);
        sendCaptchaBySms(mobile, captcha);
        redisUtil.setCache(REDIS_KEY_RESET_PWD_CAPTCHA_KEY_PREFIX + redisKeySuffix, captcha, RESET_PWD_CAPTCHA_TIME); //5分钟内有效
        //缓存发送手机号 1分钟
        redisUtil.setCache(REDIS_KEY_RESET_PWD_CAPTCHA_SEND_KEY_PREFIX + redisKeySuffix, "1", 60);
    }


    @Override
    public void getForgetPwdCaptchaByEmail(String userName, String email) {
        User user =
                repository.findByUserName(userName).orElseThrow(() -> new UsernameNotFoundException(ERROR_ACCOUNT_OR_PASSWORD));
        checkUserValid(user);
        if (!StringUtils.equals(email, user.getEmailAddress())) {
            throw new BusinessException("输入邮箱地址不匹配");
        }
        if (TenantContextHolder.getTenantId() != null) {
            String futureForgetPwd = featureChecker.getValue(LoginFeatureProvider.APP_LOGIN_FORGET_PWD);
            if (!(StringUtils.equals("email", futureForgetPwd) || StringUtils.equals("mobileOrEmail",
                    futureForgetPwd))) {
                throw new BusinessException("当前系统不允许通过邮箱找回密码");
            }
        }
        String redisKeySuffix = user.getTenantId() == null ? "HOST:" + userName : (user.getTenantId() + ":" + userName);
        String captcha = RandomStringUtils.randomNumeric(6);
        sendCaptchaByEmail(email, captcha);
        redisUtil.setCache(REDIS_KEY_RESET_PWD_CAPTCHA_KEY_PREFIX + redisKeySuffix, captcha, RESET_PWD_CAPTCHA_TIME); //5分钟内有效
    }


    @Override
    public void resetPwdWithCaptcha(String userName, String pwd, String captcha) {
        User user =
                repository.findByUserName(userName).orElseThrow(() -> new UsernameNotFoundException(ERROR_ACCOUNT_OR_PASSWORD));
        String redisKeySuffix = user.getTenantId() == null ? "host:" + userName : (user.getTenantId() + ":" + userName);
        String cacheCaptcha = (String) redisUtil.getCache(REDIS_KEY_RESET_PWD_CAPTCHA_KEY_PREFIX + redisKeySuffix);
        if (StringUtils.equals(captcha, cacheCaptcha)) {
            String password = decryptSm4(pwd, "密码解密失败,未经加密的密码或未经认证的加密");
            user.setPassword(passwordEncoder.encode(password));
            user.setShouldChangePasswordOnNextLogin(false);
            user.setAccessFailedCount(0);
            save(user);
            redisUtil.deleteCache(REDIS_KEY_RESET_PWD_CAPTCHA_KEY_PREFIX + redisKeySuffix);
            redisUtil.deleteCache(REDIS_KEY_RESET_PWD_CAPTCHA_SEND_KEY_PREFIX + redisKeySuffix);
        } else {
            throw new BusinessException("验证码错误");
        }
    }

    @Override
    @DisableTenantFilter
    public void changeAdminPasswordByHost(Long tenantId, String newPwd) {
        User user =
                findOne(Specifications.where(u -> u.eq(User.Fields.admin, true).and(u2 -> u2.eq(FullAuditedEntity.Fields.tenantId, tenantId))))
                        .orElseThrow(() -> new BusinessException("未找到该租户管理员信息"));
        user.setPassword(passwordEncoder.encode(newPwd));
        user.setShouldChangePasswordOnNextLogin(true);
        user.setAccessFailedCount(0);
        saveAndFlush(user);
    }


    @Override
    public String generateTokenByUserName(String userName) {
        User user = repository.findByUserName(userName).orElseThrow(() -> new BusinessException("无法找到账户名" + userName));
        return generateToken(user);
    }

    @Override
    public void updateUserInfoBySelf(UpdateUserInfo input) {
        Long userId = LoginUserIdContextHolder.getUserId();
        checkMobilePhoneIfNeed(userId, input.getPhoneNo(), null);
        User user = getUserById(userId);
        user.setName(input.getName());
        user.setPhoneNo(input.getPhoneNo());
        user.setEmailAddress(input.getEmailAddress());
        save(user);
    }

    @Override
    public void updateInfoBySelf(UserInfoDto dto) {
        User user = getUserById(LoginUserIdContextHolder.getUserId());
        dozerMapper.map(dto, user);
        save(user);
    }

    @Override
    public boolean checkUserIsActive(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.isActive() && !((user.getActiveStartDate() != null && now.isBefore(user.getActiveStartDate().atStartOfDay())) || (user.getActiveEndDate() != null && !now.isBefore(user.getActiveEndDate().plusDays(1).atStartOfDay())))) {
            return true;
        }
        return false;
    }

    @Override
    public void deleteUserByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        queryFactory.delete(QUser.user).where(QUser.user.id.in(ids)).execute();
    }

    @Override
    public List<User> findByUserNames(List<String> userNames) {
        if (userNames == null || userNames.isEmpty()) {
            return new ArrayList<>();
        }
        return queryFactory.selectFrom(QUser.user).where(QUser.user.userName.in(userNames)).fetch();
    }

    @Override
    public void updateUserRoles(Long userId, List<Role> roles) {
        findById(userId).ifPresent(user -> {
            user.setUserRoles(roles);
            save(user);
        });

    }

    @Override
    public String generateExpireTokenByUserName(String userName, long expireTime, TimeUnit unit) {
        User user = repository.findByUserName(userName).orElseThrow(() -> new BusinessException("无法找到账户名" + userName));
        return generateExpireToken(user, expireTime, unit);
    }

    //region private method

    private void sendCaptchaByEmail(String email, String captcha) {
        String content = "您本次密码找回操作验证码为:" + captcha + ",该验证码" + RESET_PWD_CAPTCHA_TIME + "分钟内有效，请勿泄露给他人。";
        String subject = "移动电力安全管控平台【密码找回】";
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new BusinessException("租户不存在或已被删除"));
            subject = tenant.getName() + "【密码找回】";
        }
        try {
            emailService.sendEmail(subject, content, email);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            } else {
                throw new BusinessException(ERROR_MAIL_SEND_FAIL, e);
            }
        }
    }


    private String generateToken(User user) {
        this.checkUserValid(user);
        UserContext context = LoginUtils.getUserContextByUser(user);
        return tokenAuthManager.generateToken(context);
    }

    private String generateExpireToken(User user, long expireTime, TimeUnit unit) {
        this.checkUserValid(user);
        UserContext context = LoginUtils.getUserContextByUser(user);
        return tokenAuthManager.generateToken(context, expireTime, unit);
    }


    @SneakyThrows
    private void sendCaptchaBySms(String mobile, String captcha) {
        PushSMS sms = new PushSMS();
        sms.setPhoneNumbers(mobile);
        sms.setSignName(smsPushConfig.getSmsSignName());
        sms.setTemplateCode(smsPushConfig.getSmsVerificationCode());
        SmsTemplateParam[] params = new SmsTemplateParam[1];
        SmsTemplateParam param = new SmsTemplateParam();
        param.setCode("code");
        param.setProduct(captcha);
        params[0] = param;
        sms.setTemplateParams(params);
        pushManager.smsPushAsync(sms);
    }

    private void checkMobilePhone(String phoneNo) {
        // 2020/5/18 验证手机号码格式，唯一性
        String regex = "^1[0-9]\\d{9}$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(phoneNo);  //registrant_phone  ====  电话号码字段
        boolean isMatch = m.matches();
        if (!isMatch) {
            throw new BusinessException(ERROR_USER_PHONE_NO);
        }
    }

    private void checkMobilePhoneIfNeed(Long userId, String phoneNo, EUnitType type) {
        if (StrUtil.isNotBlank(phoneNo)) {
            if (!checkUnique(userId, User.Fields.phoneNo, phoneNo)) {
                throw new BusinessException("手机号码[" + phoneNo + "已存在");
            }
            // 校验手机号码格式
            checkMobilePhone(phoneNo);
        } else {
            String value = settingRpcService.getValue(PERSONNEL_CONTROL_CONFIRM_PHONE);
            if (EUnitType.External.equals(type) && Boolean.TRUE.toString().equals(value)) {
                throw new BusinessException("手机号码不能为空");
            }
        }
    }

    private void checkIdCardIfNeed(Long userId, String idCard, EUnitType type) {
        if (StrUtil.isNotBlank(idCard)) {
            if (!checkUnique(userId, User.Fields.idCard, idCard)) {
                throw new BusinessException("[" + idCard + "]" + ERROR_ID_CARD_DUPLICATE);
            }
            // 校验身份证号码格式
            checkIdCard(idCard);
        } else {
            String value = settingRpcService.getValue(PERSONNEL_CONTROL_CONFIRM_ID_CARD);
            if (EUnitType.External.equals(type) && Boolean.TRUE.toString().equals(value)) {
                throw new BusinessException("身份证号码不能为空");
            }
        }
    }

    private void checkIdCard(String idCard) {
        // 正则表达式校验
        String regex = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(idCard);
        boolean isMatch = m.matches();
        if (!isMatch) {
            throw new BusinessException("身份证号码格式不正确");
        }
    }

    /**
     * 随机生成密码
     *
     * @param length 密码的长度
     * @return 最终生成的密码
     */
    private String generatePassword(int length) {
        // 最终生成的密码
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // 随机生成0或1，用来确定是当前使用数字还是字母 (0则输出数字，1则输出字母)
            int charOrNum = random.nextInt(2);
            if (charOrNum == 1) {
                // 随机生成0或1，用来判断是大写字母还是小写字母 (0则输出小写字母，1则输出大写字母)
                int temp = random.nextInt(2) == 1 ? 65 : 97;
                password.append((char) (random.nextInt(26) + temp));
            } else {
                // 生成随机数字
                password.append(random.nextInt(10));
            }
        }
        return password.toString();
    }

    private String checkAndLockUser(User user, int maxCount) {
        String msg = ERROR_ACCOUNT_OR_PASSWORD;
        LocalDateTime lockDateTime = user.getLockoutEndDateUtc();
        //锁定日期为null，判定次数
        if (null == lockDateTime) {
            Integer count = user.getAccessFailedCount();
            count = null == count ? 0 : count;
            if (count < maxCount) {
                msg = "密码错误，您还有" + (maxCount - count++) + "次机会";
                user.setAccessFailedCount(count);
            } else {
                LocalDateTime localDateTime = LocalDateTime.now();
                user.setLockoutEndDateUtc(localDateTime.plusHours(24L));
                msg = ERROR_USER_LOCKED;
            }
            userRepository.save(user);
        }
        //锁定日期不为null并且早于当前（说明锁定时间已过，需要解锁用户）
        else if (!lockDateTime.isAfter(LocalDateTime.now())) {
            unlockUser(new EntityDto(user.getId()));
            msg = "密码错误，您还有" + maxCount + "次机会";
        }
        return msg;
    }

    private Long saveUser(CreateOrUpdateUserInput createOrUpdateUserInput, User user) {
        //验证手机号码格式
        checkMobilePhoneIfNeed(user.getId(), user.getPhoneNo(), user.getUserType());
        //校验用户名不能重复
        String userName = createOrUpdateUserInput.getUser().getUserName();
        if (!checkUnique(createOrUpdateUserInput.getUser().getId(), User.Fields.userName, userName)) {
            throw new BusinessException("[" + userName + "]" + ERROR_USERS_DUPLICATE);
        }
        //校验如果存在激活结束日期，结束日期必须大于开始日期
        if (!createOrUpdateUserInput.getUser().isActive()) {
            user.setActiveStartDate(null);
            user.setActiveEndDate(null);
        } else {
            if (user.getActiveStartDate() != null && user.getActiveEndDate() != null) {
                if (user.getActiveEndDate().compareTo(user.getActiveStartDate()) < 0) {
                    throw new BusinessException("账号激活结束日期必须大于起始日期");
                }
            }
        }
        //校验工号不能重复
        String workNumber = createOrUpdateUserInput.getUser().getWorkNumber();
        if (StringUtils.isNotBlank(workNumber)) {
            if (!checkUnique(createOrUpdateUserInput.getUser().getId(), User.Fields.workNumber, workNumber)) {
                throw new BusinessException("[" + workNumber + "]" + ERROR_WORKNUMBER_DUPLICATE);
            }
        }
        //中文用户名转换为拼英
        user.setSurName(HanLP.convertToPinyinString(user.getName(), "", false));

        //设置角色
        List<Long> roleList = createOrUpdateUserInput.getAssignedRoleIds();
        if (roleList != null) {
//            List<Long> defaultRoles = roleService.getDefaultRoleIds();
//            defaultRoles.addAll(roleList);
            List<Role> roles = roleService.findAllById(roleList);
            user.setUserRoles(roles);
        }

        //更新组织机构与用户关联关系
        updateOrganizationRelation(user, createOrUpdateUserInput.getOrganizationUnits());
        userRepository.save(user);
        if (createOrUpdateUserInput.isSendActivationEmail()) {
            // 2020/5/18 发送邮件激活。
            String url = activeAddr + "?userId=" + user.getId();
            if (redisUtil != null) {
                String key = REDIS_KEY_ACTIVE + UUID.randomUUID().toString();
                String code = UUID.randomUUID().toString();
                url += "&key=" + key + "&code=" + code;
                redisUtil.setCache(key, code, 15 * 60);
            }
            try {
                String title = "账号激活通知";
                emailService.sendEmail(title, url, user.getEmailAddress());
            } catch (Exception e) {
                throw new BusinessException(ERROR_MAIL_SEND_FAIL);
            }
        }
        return user.getId();
    }
    //endregion

    String decryptSm4(String cryptStr, String errorMsg) {
        try {
            return SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).decryptStr(cryptStr);
        } catch (Exception e) {
            log.warn(errorMsg, e);
            return cryptStr;
        }
    }

    /**
     * 获取所有用户的组织机构
     * @return
     */
    private Map<Long, List<String>> getAllUserOrgMap(){
        List<OrganizationUnit> orgList = organizationUnitService.findAll();
        Map<Long, String> orgNamePathMap = new HashMap<>();
        for (OrganizationUnit organizationUnit : orgList) {
            orgNamePathMap.put(organizationUnit.getId(), getOrgNamePath(organizationUnit, orgList));
        }

        GetOrganizationUnitUsersInput input = new GetOrganizationUnitUsersInput();
        input.getOrganizationUnitIds().add(-1L);
        input.setDeepQuery(true);
        input.setUnPage(true);
        List<OrganizationUnitUserListDto> orgUserList = organizationUnitService.getOrganizationUnitUsers(input).getContent();
        Map<Long, List<String>> result = new HashMap<>();
        for (OrganizationUnitUserListDto orgUser : orgUserList) {
            List<String> list = result.getOrDefault(orgUser.getId(), new ArrayList<>());
            if (orgUser.getOrganizationUnitId() != null && orgNamePathMap.get(orgUser.getOrganizationUnitId()) != null) {
                list.add(orgNamePathMap.get(orgUser.getOrganizationUnitId()));
            }
            result.put(orgUser.getId(), list);
        }
        return result;
    }

    private String getOrgNamePath(OrganizationUnit organizationUnit, List<OrganizationUnit> allOrgList) {
        OrganizationUnit parent = organizationUnit.getParentId() == null ? null : allOrgList.stream().filter(e -> e.getId().equals(organizationUnit.getParentId())).findFirst().orElse(null);
        if (parent == null) {
            return organizationUnit.getDisplayName();
        } else {
            return getOrgNamePath(parent, allOrgList) + "/" + organizationUnit.getDisplayName();
        }
    }

    /**
     * 更新组织管理层关系
     * @param level 员工级别
     * @param orgIds 组织id列表
     * @param userId user id
     */
    private void updateOrgManager(ELevel level, List<Long> orgIds, Long userId) {
        // User user = getUserById(userId);
        organizationManagerRepository.deleteByUserId(userId);
        if (level == ELevel.Manager && IterUtil.isNotEmpty(orgIds)) {
            existsOrganization(orgIds);
            ArrayList<OrganizationManager> managers = new ArrayList<>();
            orgIds.forEach(orgId -> managers.add(new OrganizationManager(orgId, userId)));
            organizationManagerRepository.saveAll(managers);
        }
    }

    /**
     * 更新员工的信息（工号、岗位、入厂日期）
     */
    private void updateUserInfo(Long userId, CreateOrUpdateUserInfoInput input) {
        User user = findById(userId).orElseThrow(() -> new BusinessException(ERROR_USER_NOT_FIND));
        dozerMapper.map(input, user);
        save(user);
    }

    private void existsOrganization(List<Long> orgIds) {
        List<OrganizationUnit> fetch = queryFactory.select(qOrganizationUnit).from(qOrganizationManager).leftJoin(qOrganizationUnit).on(qOrganizationManager.orgId.eq(qOrganizationUnit.id))
                .where(qOrganizationManager.orgId.in(orgIds))
                .fetch();
        if (!fetch.isEmpty()) {
            String collect = fetch.stream().map(OrganizationUnit::getDisplayName).collect(Collectors.joining(","));
            throw new BusinessException("组织[" + collect + "]已经存在管理人员");
        }
    }
}

