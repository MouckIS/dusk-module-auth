package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.*;
import com.dusk.module.auth.repository.dashboard.*;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.module.auth.dto.RoleSimpleDto;
import com.dusk.common.module.auth.dto.UserFullListDto;
import com.dusk.common.module.auth.dto.UserRoleDto;
import com.dusk.common.module.auth.dto.role.RoleListDto;
import com.dusk.common.module.auth.service.IRoleRpcService;
import com.dusk.common.module.auth.service.IUserRpcService;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.*;
import com.dusk.module.auth.repository.dashboard.*;
import com.dusk.module.auth.service.IDashBoardModuleService;
import com.dusk.module.auth.service.IDashBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianjianhong
 * @date 2021-07-21
 */
@Service
@Transactional
@Slf4j
public class DashBoardServiceImpl extends CreateOrUpdateService<DashboardTheme, IDashBoardThemeRepository> implements IDashBoardService {

    @Autowired
    private IDashBoardClassifyRepository classifyRepository;
    @Autowired
    private IDashBoardModuleRepository moduleRepository;
    @Autowired
    private IDashBoardModuleItemRepository moduleItemRepository;
    @Autowired
    private IDashBoardModuleService moduleService;
    @Autowired
    private IDashBoardPermissionRepository permissionRepository;
    @Autowired
    private IDashBoardThemeRepository themeRepository;
    @Autowired
    private IDashBoardZoneRepository zoneRepository;
    @Autowired
    private IDashBoardZoneItemRefRepository zoneItemRefRepository;
    @Autowired
    JPAQueryFactory queryFactory;

    @Reference
    private IUserRpcService userRpcService;
    @Reference
    private IRoleRpcService roleRpcService;

    @Override
    public DashboardTheme saveTheme(CreateOrUpdateTheme input) {
        //检查主题名次是否重复
        List<DashboardTheme> themes = themeRepository.findAllByName(input.getName());
        themes.forEach(theme -> {
            if(!theme.getId().equals(input.getId())) {
                throw new BusinessException("已存在名称为["+input.getName()+"]的数据大屏!");
            }
        });

        return createOrUpdate(input, input.getId(), DashboardTheme.class);
    }

    @Override
    public PagedResultDto<ThemeListDto> getThemeList(GetThemeInput input) {
        QDashboardTheme qDashboardTheme = QDashboardTheme.dashboardTheme;
        QBean<ThemeListDto> themeListDtoQBean = QBeanBuilder.create(ThemeListDto.class).appendQEntity(qDashboardTheme).build();
        var query = queryFactory.select(themeListDtoQBean).from(qDashboardTheme)
                .orderBy(qDashboardTheme.createTime.desc());

        //过滤名称
        if (StringUtils.isNotBlank(input.getName())) {
            query = query.where(qDashboardTheme.name.contains(input.getName()));
        }

        //过滤标题
        if (StringUtils.isNotBlank(input.getTitle())) {
            query = query.where(qDashboardTheme.title.contains(input.getTitle()));
        }

        //过滤样式
        if (StringUtils.isNotBlank(input.getThemeType())) {
            query = query.where(qDashboardTheme.themeType.contains(input.getThemeType()));
        }
        var themePage = page(query, input.getPageable());
        List<ThemeListDto> datas = (List<ThemeListDto>)themePage.getContent();

        //获取主题栏目
        List<Long> themeIds = datas.stream().map(ThemeListDto::getId).collect(Collectors.toList());
        List<DashboardClassify> classifies = classifyRepository.findAllByThemeIdIn(themeIds);
        if(!classifies.isEmpty()) {
            Map<Long, List<DashboardClassify>> classifyMap = classifies.stream().collect(Collectors.groupingBy(DashboardClassify::getThemeId));
            datas.forEach((theme) -> {
                List<DashboardClassify> themeClassify = classifyMap.get(theme.getId());
                if(themeClassify != null) {
                    theme.setClassifies(DozerUtils.mapList(mapper, themeClassify, ClassifyDetailDto.class));
                }
            });
        }
        return new PagedResultDto<>(themePage.getTotalElements(), datas);
    }

    @Override
    public ThemeDetailDto themeDetail(Long id) {
        DashboardTheme theme = findT(id);
        ThemeDetailDto themeDto = mapper.map(theme, ThemeDetailDto.class);

        List<DashboardClassify> classifies = classifyRepository.findAllByThemeId(id);
        List<ClassifyDetailDto> classifyDetailDtos = DozerUtils.mapList(mapper, classifies, ClassifyDetailDto.class);
        List<ClassifyDetailDto> sortedClassifies = classifyDetailDtos.stream().sorted(Comparator.comparing(ClassifyDetailDto::getSeq)).collect(Collectors.toList());
        themeDto.setClassifyList(sortedClassifies);

        sortedClassifies.forEach((classify) -> {
            classify.setZones(findClassifyZones(classify));
        });
        return themeDto;
    }

    @Override
    public DashboardClassify saveClassify(CreateOrUpdateClassify input) {
        //保存栏目
        DashboardClassify dashboardClassify;
        boolean isAdd = input.getId() == null;
        if(isAdd) {
            dashboardClassify = mapper.map(input, DashboardClassify.class);
        }else {
            dashboardClassify = classifyRepository.findById(input.getId()).orElseThrow(() -> new BusinessException("未找到id为["+input.getId()+"]的栏目记录！"));
            mapper.map(input, dashboardClassify);
        }
        classifyRepository.save(dashboardClassify);

        //更新区域列表
        zoneRepository.deleteAllByClassifyId(dashboardClassify.getId());
        input.getZones().forEach((zone)-> {
            saveZone(zone, dashboardClassify);
        });

        return dashboardClassify;
    }

    private DashboardZone saveZone(CreateOrUpdateZone input, DashboardClassify dashboardClassify) {
        DashboardZone zone = mapper.map(input, DashboardZone.class);
        zone.setId(null);
        zone.setClassifyId(dashboardClassify.getId());
        zoneRepository.save(zone);

        //更新区域统计项
        zoneItemRefRepository.deleteAllByZoneId(input.getId());
        if(input.getZoneItems() != null) {
            input.getZoneItems().forEach((item)-> {
                saveZoneItemRef(item, zone);
            });
        }
        return zone;
    }

    private DashboardZoneItemRef saveZoneItemRef(CreateOrUpdateZoneItemRef input, DashboardZone zone) {
        DashboardZoneItemRef zoneItemRef = mapper.map(input, DashboardZoneItemRef.class);
        zoneItemRef.setZoneId(zone.getId());
        zoneItemRefRepository.save(zoneItemRef);
        return zoneItemRef;
    }

    @Override
    public ClassifyDetailDto classifyDetail(Long id) {
        DashboardClassify classify = classifyRepository.findById(id).orElseThrow(()->new BusinessException("未找到id为["+id+"]的栏目记录！"));
        ClassifyDetailDto classifyDto = mapper.map(classify, ClassifyDetailDto.class);
        classifyDto.setZones(findClassifyZones(classifyDto));
        return classifyDto;
    }

    private List<ZoneDetailDto> findClassifyZones(ClassifyDetailDto classifyDto) {
        List<DashboardZone> zones = zoneRepository.findAllByClassifyIdOrderByZonePosition(classifyDto.getId());
        List<ZoneDetailDto> zoneDtos = DozerUtils.mapList(mapper, zones, ZoneDetailDto.class);

        zoneDtos.forEach((zone) -> {
            zone.setZoneItems(findZoneItemDetail(zone));
        });
        return zoneDtos;
    }

    private List<ZoneItemDetailDto> findZoneItemDetail(ZoneDetailDto zone) {
        List<DashboardZoneItemRef> itemRefs = zoneItemRefRepository.findAllByZoneId(zone.getId());
        List<ZoneItemDetailDto> itemRefDtos = DozerUtils.mapList(mapper, itemRefs, ZoneItemDetailDto.class);
        itemRefDtos.forEach((ref)->{
            DashboardModule module = moduleRepository.findById(ref.getModuleId()).orElse(null);
            DashboardModuleItem item = moduleItemRepository.findById(ref.getModuleItemId()).orElse(null);
            ref.setModule(module);
            ref.setModuleItem(item);
        });
        return itemRefDtos;
    }

    @Override
    public void removeClassify(Long id) {
        DashboardClassify classify = classifyRepository.findById(id).orElseThrow(()->new BusinessException("未找到id为["+id+"]的栏目记录！"));
        //删除栏目关联的区域
        deleteClassifyZones(classify);
        //删除栏目
        classifyRepository.delete(classify);
    }

    private void deleteClassifyZones(DashboardClassify classify) {
        List<DashboardZone> zones = zoneRepository.findAllByClassifyIdOrderByZonePosition(classify.getId());
        List<Long> zoneIds = zones.stream().map(DashboardZone::getId).collect(Collectors.toList());
        //删除区域和统计项的关联
        zoneItemRefRepository.deleteAllByZoneIdIn(zoneIds);
        //删除区域
        zoneRepository.deleteAll(zones);
    }

    @Override
    public void removeTheme(Long id) {

        List<DashboardClassify> classifies = classifyRepository.findAllByThemeId(id);
        List<Long> classifyIds = classifies.stream().map(DashboardClassify::getId).collect(Collectors.toList());

        List<DashboardZone> zones = zoneRepository.findAllByClassifyIdIn(classifyIds);
        List<Long> zoneIds = zones.stream().map(DashboardZone::getId).collect(Collectors.toList());

        //删除栏目区域
        zoneRepository.deleteAll(zones);

        //删除区域和统计项的关联
        zoneItemRefRepository.deleteAllByZoneIdIn(zoneIds);

        //删除栏目
        classifyRepository.deleteAllByThemeId(id);

        //删除主题
        deleteById(id);
    }

    @Override
    public void setDashBardPermission(CreateOrUpdateDashBoardPermission input) {
        findById(input.getThemeId()).orElseThrow(() -> new BusinessException("未找到id为["+input.getThemeId()+"]的主题记录！"));
        if(input.getRoleIds() != null) {
            input.getRoleIds().forEach(roleId -> {
                if(permissionRepository.findByThemeIdAndRoleId(input.getThemeId(), roleId) == null) {
                    DashboardPermission dashboardPermission = new DashboardPermission(input.getThemeId(), roleId);
                    permissionRepository.save(dashboardPermission);
                }
            });
        }
    }

    @Override
    public void removeDashBardPermission(RemoveDashBoardPermission input) {
        findById(input.getThemeId()).orElseThrow(() -> new BusinessException("未找到id为["+input.getThemeId()+"]的主题记录！"));
        permissionRepository.deleteByThemeIdAndRoleId(input.getThemeId(), input.getRoleId());
    }

    @Override
    public List<ThemeListDto> getDashBoardThemeByUserId(Long userId) {
        UserFullListDto user = userRpcService.getUserFullById(userId);
        if(user == null) {
            throw new BusinessException("id为["+userId+"]的用户不存在！");
        }
        if(user.getUserRoles() == null || user.getUserRoles().size() == 0) {
            return new ArrayList<>();
        }
        List<Long> roleIds = user.getUserRoles().stream().map(UserRoleDto::getId).collect(Collectors.toList());
        List<DashboardPermission> permissions = permissionRepository.findAllByRoleIdIn(roleIds);
        List<Long> themeIds = permissions.stream().map(DashboardPermission::getThemeId).distinct().collect(Collectors.toList());
        List<DashboardTheme> themes = themeRepository.findAllByIdIn(themeIds);
        return DozerUtils.mapList(mapper, themes, ThemeListDto.class);
    }

    @Override
    public List<RoleSimpleDto> getDashBoardThemeUser(Long themeId) {
        findById(themeId).orElseThrow(() -> new BusinessException("未找到id为["+themeId+"]的主题记录！"));
        List<DashboardPermission> permissions = permissionRepository.findAllByThemeId(themeId);
        if(permissions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = permissions.stream().filter((p)->p.getRoleId()!= null).map((p)->p.getRoleId()).collect(Collectors.toList());
        if(roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoleListDto> roleListDtos = roleRpcService.getRolesByIds(roleIds);
        return DozerUtils.mapList(mapper, roleListDtos, RoleSimpleDto.class);
    }

    @Override
    public UserMainDashBoardDto getUserMainDashBoard(Long userId) {
        DashboardTheme dashboardTheme = themeRepository.findFirstByMainPage(true);
        if(dashboardTheme == null) {
            return new UserMainDashBoardDto(null, false);
        }

        UserFullListDto user = userRpcService.getUserFullById(userId);
        if(user == null) {
            throw new BusinessException("id为["+userId+"]的用户不存在！");
        }
        if(user.getUserRoles() == null || user.getUserRoles().size() == 0) {
            return new UserMainDashBoardDto(null, false);
        }
        List<Long> roleIds = user.getUserRoles().stream().map(UserRoleDto::getId).collect(Collectors.toList());

        QDashboardPermission qDashboardPermission = QDashboardPermission.dashboardPermission;
        long count = queryFactory.selectFrom(qDashboardPermission)
                .where(qDashboardPermission.themeId.eq(dashboardTheme.getId())
                        .and(qDashboardPermission.roleId.in(roleIds))).fetchCount();
        return new UserMainDashBoardDto(dashboardTheme.getId(), count>0);
    }

    @Override
    public Boolean checkMainDashBoard(Long themeId) {
        DashboardTheme dashboardTheme = themeRepository.findFirstByMainPage(true);
        return dashboardTheme == null || dashboardTheme.getId().equals(themeId);
    }
}
