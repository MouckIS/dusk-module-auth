package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.annotation.DisableTenantFilter;
import com.dusk.common.framework.entity.CreationEntity;
import com.dusk.common.framework.entity.TreeEntity;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.common.framework.utils.SpringContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IStationMigrationService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.auth.service.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2023/2/14 15:22
 */
@Slf4j
@Service
@Transactional
public class StationMigrationService implements IStationMigrationService {
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private IOrganizationUnitService organizationUnitService;
    @Autowired
    private IStationService stationService;
    @Autowired
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Autowired
    private SpringContextUtils springContextUtils;
    @Autowired
    private JPAQueryFactory queryFactory;


    @PostConstruct
    public void autoMigration(){
        boolean needMigration = SpringContextUtils.getBean(StationMigrationService.class).needMigration();//忽略租户过滤
        if(needMigration){ //新的厂站表没有数据的时候执行一次迁移
            migration();
        }
    }

    @DisableTenantFilter
    public boolean needMigration(){
        return stationService.count() == 0;
    }

    @Override
    public void migration() {
        List<Tenant> all = tenantService.findAll();
        for (Tenant tenant : all) {
            TenantContextHolder.setTenantId(tenant.getId());
            log.info(StrUtil.format("==============同步厂站数据，tenant={}=========",  tenant.getName()));
            try {
                syncStations();
            }catch (Exception e){
                log.info("同步厂站数据出错",e);
            }finally {
                TenantContextHolder.clear();
            }
        }
        dataFilterDefinitionContext.refresh();
        log.info("==============迁移厂站完成===========");
    }

    private void syncStations(){
        List<OrganizationUnit> list = organizationUnitService.findAll(Sort.by(Sort.Order.asc(TreeEntity.Fields.sortIndex), Sort.Order.desc(CreationEntity.Fields.createTime)));
        List<OrganizationUnit> rootList = list.stream().filter(e -> e.getParentId() == null).collect(Collectors.toList());
        for (OrganizationUnit unit : rootList) {
            if(unit.getStation()){
                Station station = syncStation(unit, null);
                syncStation(unit, list, station);
            }else{
                syncStation(unit, list, null);
            }
        }
    }

    /**
     * 从当前组织机构中同步厂站信息到厂站表
     * @param parent 父组织机构
     * @param all 所有组织机构
     * @param preStation 前一个创建的厂站，用作后续创建厂站的父级
     */
    private void syncStation(OrganizationUnit parent, List<OrganizationUnit> all, Station preStation){
        List<OrganizationUnit> children = getChildren(parent, all);
        for (OrganizationUnit child : children) {
            if(child.getStation()){
                Station station = syncStation(child, preStation);
                syncStation(child, all, station);
            }else{
                syncStation(child, all, preStation);
            }

        }
    }

    /**
     * 同步厂站信息
     * @param organizationUnit 当前组织机构
     * @param parentStation 前一个厂站，
     * @return
     */
    private Station syncStation(OrganizationUnit organizationUnit, Station parentStation){
        Station station = stationService.findById(organizationUnit.getId()).orElse(null);
        if(station == null){
            station = new Station();
            station.setId(organizationUnit.getId());
            station.setDisplayName(organizationUnit.getDisplayName());
            station.setSortIndex(organizationUnit.getSortIndex());
            station.setUsers(getOrgUsers(organizationUnit));
            if(parentStation != null){
                station.setParentId(parentStation.getId());
            }
            station = stationService.save(station);
        }
        log.info(StrUtil.format("同步厂站：id={}, parentId={}, name={}, path={}, parentStationId={}, parentStationName={},parentStationPath={}", organizationUnit.getId(), organizationUnit.getParentId(), organizationUnit.getDisplayName(), organizationUnit.getPath()
                , parentStation==null ? "" : parentStation.getId(), parentStation==null ? "" : parentStation.getDisplayName(), parentStation==null ? "" : parentStation.getPath()));
        return station;
    }

    private List<User> getOrgUsers(OrganizationUnit org){
        QUser qUser = QUser.user;
        return queryFactory.select(qUser).from(qUser).where(qUser.organizationUnit.any().id.eq(org.getId())).fetch();
    }


    private List<OrganizationUnit> getChildren(OrganizationUnit parent, List<OrganizationUnit> all){
        return all.stream().filter(e -> parent.getId().equals(e.getParentId())).collect(Collectors.toList());
    }

}
