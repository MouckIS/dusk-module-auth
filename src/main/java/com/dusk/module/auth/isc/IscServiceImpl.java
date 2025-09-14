package com.dusk.module.auth.isc;

import cn.hutool.core.util.StrUtil;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.service.*;
import com.sgcc.isc.service.BizOrgRoleOfUserType;
import com.sgcc.isc.service.BizOrgType;
import com.sgcc.isc.service.ExtendType;
import com.sgcc.isc.service.UserType;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.common.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.common.module.auth.dto.UserEditDto;
import com.dusk.module.auth.dto.role.RoleCreateOrEditDto;
import com.dusk.module.auth.dto.user.CreateUserInput;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.jws.WebService;
import jakarta.xml.ws.Holder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-11-19 9:19
 */
@Component
@WebService(endpointInterface = "com.dusk.module.auth.isc.IscService", targetNamespace = "http://www.sgcc.com/isc/service/", serviceName = "IscService", portName = "iscPort")
@Slf4j
public class IscServiceImpl implements IscService {
    private static final String DELETED = "1";
    private static final String ISC_USER_ID = "ISC_USER_ID";


    @Autowired
    private ITenantService tenantService;
    @Autowired
    private IscSyncConfig config;
    @Autowired
    private IOrganizationUnitService organizationUnitService;
    @Autowired
    private IUserService userService;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IExtendFieldService extendFieldService;

    @Override
    public void syncBizRoleGroup(String bizrolegroupID, String bizrolegroupNAME, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        log.info("syncBizRoleGroup");
    }

    @Override
    public void syncBizOrganization(List<BizOrgType> orgList, Holder<String> result, Holder<String> time, Holder<String> description) {
        if(orgList.isEmpty()){
            result.value = "0";
            description.value = "同步组织机构成功！";
            time.value = getTime();
            return;
        }
        Long tenantId = getTenantId(orgList.get(0).getEXTENDS(), description);
        if(tenantId == null){
            result.value = "1";
            time.value = getTime();
            return;
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            //删除的业务组织机构id
            List<String> deleteBizOrgIds = orgList.stream().filter(e -> DELETED.equals(e.getISDELETED())).map(BizOrgType::getBIZORGID).collect(Collectors.toList());
            organizationUnitService.deleteByCodes(deleteBizOrgIds);

            List<BizOrgType> notDeleteList = orgList.stream().filter(e -> !DELETED.equals(e.getISDELETED())).collect(Collectors.toList());
            if(notDeleteList.isEmpty()){
                result.value = "0";
                description.value = "同步组织机构成功！";
                time.value = getTime();
                return;
            }
            List<OrganizationUnit> cruxOrgs = new ArrayList<>();
            Set<String> bizOrgIds = notDeleteList.stream().map(BizOrgType::getBIZORGID).collect(Collectors.toSet());
            bizOrgIds.addAll(notDeleteList.stream().filter(e -> e.getPARENTBIZORGID() != null).map(BizOrgType::getPARENTBIZORGID).collect(Collectors.toSet()));

            List<OrganizationUnit> existedList = organizationUnitService.findByCodes(new ArrayList<>(bizOrgIds));
            for (BizOrgType orgType : notDeleteList) {
                OrganizationUnit cruxOrg = findCruxOrg(existedList, orgType.getBIZORGID());
                OrganizationUnit parentCruxOrg = findCruxOrg(existedList, orgType.getPARENTBIZORGID());
                if(cruxOrg == null){
                    cruxOrg = new OrganizationUnit();
                }
                cruxOrg.setDisplayName(orgType.getBIZORGNAME());
                cruxOrg.setCode(orgType.getBIZORGID());
                cruxOrg.setParentId(parentCruxOrg == null ? null : parentCruxOrg.getId());
                int sortIndex = 0;
                try {
                    if(StrUtil.isNotBlank(orgType.getSORTNO())){
                        sortIndex = Integer.parseInt(orgType.getSORTNO());
                    }
                }catch (Exception ignore){
                }
                cruxOrg.setSortIndex(sortIndex);
                cruxOrgs.add(cruxOrg);
            }
            organizationUnitService.saveAll(cruxOrgs);
            result.value = "0";
            description.value = "同步组织机构成功！";
            time.value = getTime();
        }catch (Exception e){
            result.value = "1";
            description.value = "同步组织机构失败，数据处理异常！";
            time.value = getTime();
           log.error("同步组织机构失败！", e);
        }finally {
            TenantContextHolder.clear();
        }
        log.info(description.value);
    }

    @Override
    public void syncFunction(String funcID, String funcNAME, String funcURL, String parentFUNCID, String funcINFO, String sortNO, String funcSTATUS, String isLEAF, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //业务组织角色与功能授权关系
    }

    @Override
    public void syncUser(List<UserType> userList, Holder<String> result, Holder<String> time, Holder<String> description) {
        if(userList.isEmpty()){
            result.value = "0";
            description.value = "同步用户成功！";
            time.value = getTime();
            return;
        }
        Long tenantId = getTenantId(userList.get(0).getEXTENDS(), description);
        if(tenantId == null){
            result.value = "1";
            time.value = getTime();
            return;
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            //删除用户 删除的数据ISC只传id信息
            List<String> deleteIscUserIds = userList.stream().filter(e -> DELETED.equals(e.getISDELETED())).map(UserType::getUSERID).collect(Collectors.toList());
            List<Long> deleteCruxUserIds = extendFieldService.getEntityIds(User.class.getName(), QUser.user, ISC_USER_ID, deleteIscUserIds);
            userService.deleteUserByIds(deleteCruxUserIds);

            List<UserType> notDeleteList = userList.stream().filter(e -> !DELETED.equals(e.getISDELETED())).collect(Collectors.toList());
            if(notDeleteList.isEmpty()){
                result.value = "0";
                description.value = "同步用户成功！";
                time.value = getTime();
                return;
            }

            Set<String> orgCodes = new HashSet<>();
            for (UserType userType : notDeleteList) {
                orgCodes.addAll(userType.getBIZORGID());
            }
            List<OrganizationUnit> orgs = organizationUnitService.findByCodes(new ArrayList<>(orgCodes));

            //创建或更新用户
            List<String> userNames = notDeleteList.stream().map(UserType::getLOGINNAME).collect(Collectors.toList());
            Map<String, User> userMap = userService.findByUserNames(userNames).stream().collect(Collectors.toMap(User::getUserName, user -> user));
            String defaultPassword = getDefaultPassword(notDeleteList.get(0).getEXTENDS());
            List<Long> defaultRoles = roleService.getDefaultRoleIds();

            for (UserType userType : notDeleteList) {
                CreateOrUpdateUserInput userInput = new CreateOrUpdateUserInput();
                User user = userMap.getOrDefault(userType.getLOGINNAME(),  null );
                UserEditDto userEditDto = new UserEditDto();
                userInput.setUser(userEditDto);
                if (user == null) {
                    userEditDto.setPassword(defaultPassword);
                    userEditDto.setShouldChangePasswordOnNextLogin(true);
                    userInput.setAssignedRoleIds(defaultRoles);
                } else {
                    userEditDto.setId(user.getId());
                }
                userEditDto.setActive("1".equals(userType.getUSERSTATUS()));
                userEditDto.setPhoneNo(userType.getMOBILE());
                userEditDto.setEmailAddress(userType.getEMAIL());
                userEditDto.setWorkNumber(userType.getEMPLOYNO());
                userEditDto.setUserName(userType.getLOGINNAME());
                userEditDto.setName(userType.getREALNAME());
                //设置组织机构， 以bizOrgId关联crux中org的code
                userInput.setOrganizationUnits(orgs.stream().filter(org -> userType.getBIZORGID().contains(org.getCode())).map(OrganizationUnit::getId).collect(Collectors.toList()));
                Long userId = userService.createOrUpdateUser(userInput);
                extendFieldService.addOrUpdateField(userId, User.class.getName(), ISC_USER_ID, userType.getUSERID());

                result.value = "0";
                description.value = "同步用户成功！";
                time.value = getTime();
            }
        }catch (Exception e){
            result.value = "1";
            description.value = "同步用户失败，数据处理异常！";
            time.value = getTime();
            log.error("同步用户失败！", e);
        }finally {
            TenantContextHolder.clear();
        }
        log.info(description.value);
    }

    @Override
    //TODO:需要实现同步
    public void syncBizRole(String bizroleID, String bizroleNAME, String bizrolegroupID, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        Long tenantId = getTenantId(_extends, description);
        if(tenantId == null){
            result.value = "1";
            time.value = getTime();
            return;
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            Role role = roleService.getRoleByRoleName(bizroleNAME);
            if(DELETED.equals(isDELETED)){//删除的时候接口只传id过来
                roleService.deleteByCode(bizroleID);
            }else{
                RoleCreateOrEditDto roleCreateInput = new RoleCreateOrEditDto();
                roleCreateInput.setRoleName(bizroleNAME);
                roleCreateInput.setRoleCode(bizroleID);//使用id作为code
                if(role != null){
                    roleCreateInput.setId(role.getId());
                }
                roleService.createOrUpdate(roleCreateInput);
            }
            result.value = "0";
            description.value = "同步角色成功！";
            time.value = getTime();
        }catch (Exception e){
            result.value = "1";
            description.value = "同步角色失败，数据处理异常！";
            time.value = getTime();
            log.error("同步角色失败！", e);
        }finally {
            TenantContextHolder.clear();
        }
        log.info(description.value);
    }

    @Override
    public void syncDataSet(String datasetID, String datasetNAME, String datasetCODE, String datatypeID, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //业务组织角色与数据集授权关系
    }

    @Override
    public void syncResOfBizRole(String bizroleID, List<String> resID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //业务角色与权限对象授权关系
    }

    @Override
    public void syncResource(String resID, String resNAME, String resCODE, String funcID, String resINFO, String sortNO, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        log.info("syncResource");
        result.value = "0";
        time.value = getTime();
        description.value = "同步用户数据成功";
    }

    @Override
    public void syncBizOrgRole(String bizorgroleID, String bizorgroleNAME, String bizroleID, String bizorgID, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //用户与业务组织角色关系
        //不实现，但是不让他报错
        result.value = "0";
        time.value = getTime();
        description.value = "同步组织角色数据成功";
    }

    @Override
    public void syncBizOrgRoleOfUser(List<BizOrgRoleOfUserType> orgRoleOfUserList, String userID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //用户与业务组织角色关系
        result.value = "0";
        time.value = getTime();
        description.value = "同步成功";
    }

    @Override
    public void syncFunctionOfOrgRole(String bizorgroleID, List<String> funcID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //业务组织角色与功能授权关系
    }

    @Override
    public void syncDataSetOfOrgRole(String bizorgroleID, List<String> datasetID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //业务组织角色与数据集授权关系
    }

    @Override
    public void syncResOfOrgRole(String bizorgroleID, List<String> resID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        log.info("syncResOfOrgRole");
        result.value = "0";
        time.value = getTime();
        description.value = "同步用户数据成功";
    }

    @Override
    public void syncDataType(String datatypeID, String datatypeNAME, String datatypeCODE, String updateDATE, String isDELETED, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //数据类型
    }

    @Override
    public void syncFunctionOfBizRole(String bizroleID, List<String> funcID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        //同步业务角色与功能授权关系
    }

    @Override
    public void syncBizRoleOfUser(String userID, List<String> bizroleID, String updateDATE, List<ExtendType> _extends, Holder<String> result, Holder<String> time, Holder<String> description) {
        log.info("syncBizRoleOfUser");
        log.info(StrUtil.format("userId={}, roleIds=[{}]", userID, String.join(",", bizroleID)));

        Long tenantId = getTenantId(_extends, description);
        if(tenantId == null){
            result.value = "1";
            time.value = getTime();
            return;
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            Long userId = extendFieldService.getEntityId(User.class.getName(), QUser.user, ISC_USER_ID, userID);
            if(userId != null){
                List<Role> roles = roleService.findByCodes(bizroleID);
                userService.updateUserRoles(userId, roles);
            }
            result.value = "0";
            description.value = "同步用户与业务角色关系成功！";
            time.value = getTime();
        }catch (Exception e){
            result.value = "1";
            description.value = "同步用户与业务角色关系失败，数据处理异常！";
            time.value = getTime();
            log.error("同步用户与业务角色关系失败！", e);
        }finally {
            TenantContextHolder.clear();
        }
        log.info(description.value);
    }

    /**
     * 获取租户id
     * @param extendTypes
     * @return
     */
    private Long getTenantId(List<ExtendType> extendTypes, Holder<String> description){
        IscSyncConfig.Application application = getApplication(extendTypes);
        if(application == null){
            description.value = "未找到相应的app配置";
            return null;
        }

        Tenant tenant = tenantService.findByTenantName(application.getTenantName()).orElse(null);
        if(tenant == null){
            description.value = StrUtil.format("tenantName={}, 租户不存在", application.getTenantName());
            log.error(description.value);
            return null;
        }
        return tenant.getId();
    }

    private String getTime(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private OrganizationUnit findCruxOrg(List<OrganizationUnit> cruxOrgs, String bizOrgId){
        if(StrUtil.isBlank(bizOrgId)){
            return null;
        }
        return cruxOrgs.stream().filter(e -> bizOrgId.equals(e.getCode())).findFirst().orElse(null);
    }

    /**
     * 获取默认密码
     * @param extendTypes
     * @return
     */
    private String getDefaultPassword(List<ExtendType> extendTypes){
        IscSyncConfig.Application application = getApplication(extendTypes);
        if(application == null || StrUtil.isBlank(application.getDefaultPassword())){
            return config.getDefaultPassword();
        }else{
            return application.getDefaultPassword();
        }
    }

    /**
     * 获取应用配置
     * @param extendTypes
     * @return
     */
    private IscSyncConfig.Application getApplication(List<ExtendType> extendTypes){
        if(extendTypes == null || extendTypes.isEmpty()){
            return null;
        }
        String appid = "";
        for (ExtendType extendType : extendTypes) {
            if("APP_ID".equals(extendType.getKEY())){
                appid = extendType.getVALUE();
                break;
            }
        }
        if(StrUtil.isBlank(appid)){
            return null;
        }
        String finalAppid = appid;
        return config.getApplications().stream().filter(e -> finalAppid.equals(e.getAppid())).findFirst().orElse(null);
    }

}
