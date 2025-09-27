package com.dusk.module.ddm.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.module.auth.dto.dynamicmenu.DynamicLinkRoles;
import com.dusk.common.module.auth.dto.dynamicmenu.PublishDynamicMenuInput;
import com.dusk.common.module.auth.dto.dynamicmenu.UnPublishDynamicMenuInput;
import com.dusk.common.module.auth.enums.DynamicMenuOpenType;
import com.dusk.common.module.auth.service.IDynamicMenuRpcService;
import com.dusk.module.auth.dto.configuration.DynamicMenuDto;
import com.dusk.module.auth.dto.dynamicmenu.DynamicMenuRolesDto;
import com.dusk.module.auth.dto.dynamicmenu.GetDynamicMenuInput;
import com.dusk.module.auth.entity.DynamicMenu;
import com.dusk.module.auth.entity.QDynamicMenu;
import com.dusk.module.auth.entity.QRole;
import com.dusk.module.auth.repository.IDynamicMenuRepository;
import com.dusk.module.auth.service.IDynamicMenuService;
import com.dusk.module.auth.service.IGrantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022-08-29 16:41
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class DynamicMenuServiceImpl extends BaseService<DynamicMenu, IDynamicMenuRepository> implements IDynamicMenuRpcService, IDynamicMenuService {
    QDynamicMenu qDynamicMenu = QDynamicMenu.dynamicMenu;
    @Autowired
    JPAQueryFactory jpaQueryFactory;
    @Autowired
    Mapper dozerMapper;
    @Autowired
    IGrantPermissionService grantPermissionService;

    @Override
    public PagedResultDto<DynamicMenuRolesDto> getList(GetDynamicMenuInput input) {
        QBean<DynamicMenuRolesDto> dynamicMenuRolesDtoQBean = QBeanBuilder.create(DynamicMenuRolesDto.class).appendQEntity(qDynamicMenu).build();
        var query = jpaQueryFactory.select(dynamicMenuRolesDtoQBean).from(qDynamicMenu)
                .where(qDynamicMenu.id.in(repository.getDistinctDynamicModule()))
                .orderBy(qDynamicMenu.createTime.desc());
        if(StringUtils.isNotBlank(input.getDynamicType())) {
            query = query.where(qDynamicMenu.dynamicType.eq(input.getDynamicType()));
        }
        if(StringUtils.isNotBlank(input.getName())) {
            query = query.where(qDynamicMenu.name.contains(input.getName()));
        }

        var dynamicMenuPage = page(query, input.getPageable());
        List<DynamicMenuRolesDto> results = (List<DynamicMenuRolesDto>)dynamicMenuPage.getContent();

        //获取菜单发布的角色数据
        results.forEach(result -> {
            result.setRoleList(getPublishRoles(result.getBusinessKey()));
        });
        return new PagedResultDto<>(dynamicMenuPage.getTotalElements(), results);
    }

    /**
     * 检查是否存在菜单
     * @param businessKey
     */
    private Boolean hasDynamicMenu(String businessKey, Long roleId) {
        long count = jpaQueryFactory.selectFrom(qDynamicMenu).where(qDynamicMenu.businessKey.eq(businessKey).and(qDynamicMenu.roleId.eq(roleId))).fetchCount();
        return count>0;
    }

    @Override
    public void publish(PublishDynamicMenuInput input) {


        List<DynamicMenu> saveData = new ArrayList<>();
        for (Long roleId : input.getRoleIds()) {
            if(!hasDynamicMenu(input.getBusinessKey(), roleId)) {
                DynamicMenu menu = dozerMapper.map(input, DynamicMenu.class);
                menu.setRoleId(roleId);
                menu.setType(input.getType() != null ? input.getType() : DynamicMenuOpenType.INNER);
                saveData.add(menu);
            }
        }
        repository.saveAll(saveData);

        //推送权限
        grantPermissionService.addDynamicPermission(input.getRelatedAuth(), input.getRoleIds(), input.getBusinessKey());
    }

    @Override
    public void unpublish(UnPublishDynamicMenuInput input) {
        //移除动态菜单
        jpaQueryFactory.delete(qDynamicMenu).where(qDynamicMenu.businessKey.eq(input.getBusinessKey())).execute();
        grantPermissionService.deleteDynamicPermission(input.getBusinessKey());
    }

    @Override
    public List<DynamicMenuDto> getDynamicMenus(List<Long> roleIds) {
        QBean<DynamicMenuDto> query = QBeanBuilder.create(DynamicMenuDto.class).appendQEntity(qDynamicMenu).build();
        return jpaQueryFactory.selectDistinct(query).from(qDynamicMenu).where(qDynamicMenu.roleId.in(roleIds)).fetch();
    }

    @Override
    public List<DynamicLinkRoles> getPublishRoles(String businessKey) {
        JPAQuery<Long> where = jpaQueryFactory.select(qDynamicMenu.roleId).from(qDynamicMenu).where(qDynamicMenu.businessKey.eq(businessKey));
        QBean<DynamicLinkRoles> qBean = QBeanBuilder.create(DynamicLinkRoles.class).appendQEntity(QRole.role).appendField(QRole.role.id.as("roleId")).build();

        List<DynamicLinkRoles> fetch = jpaQueryFactory.select(qBean).from(QRole.role).where(QRole.role.id.in(where)).fetch();
        return fetch;
    }

    @Override
    public void editPublish(PublishDynamicMenuInput input) {
        UnPublishDynamicMenuInput unPublishDynamicMenuInput =new UnPublishDynamicMenuInput();
        unPublishDynamicMenuInput.setBusinessKey(input.getBusinessKey());
        unpublish(unPublishDynamicMenuInput);

        publish(input);
    }
}
