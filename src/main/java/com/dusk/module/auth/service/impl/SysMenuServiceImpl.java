package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.framework.utils.UtBeanUtils;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListSearchDto;
import com.dusk.common.module.auth.dto.sysmenu.SysMenuInputDto;
import com.dusk.common.module.auth.enums.MenuTypeEnum;
import com.dusk.module.auth.entity.SysMenu;
import com.dusk.module.auth.repository.ISysMenuRepository;
import com.dusk.module.auth.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Transactional
public class SysMenuServiceImpl extends BaseService <SysMenu, ISysMenuRepository> implements ISysMenuService {

    @Autowired
    Mapper dozerMapper;


    @Override
    public void save(List<SysMenuInputDto> input) {
        List<SysMenu> menuList = findAll();
        List<SysMenu> saveList = new ArrayList<>();
        for (SysMenuInputDto dto : input) {
            if(dto.getType() == null) {
                dto.setType(MenuTypeEnum.System);
            }
            if(dto.getId()==null){
                SysMenu menu = dozerMapper.map(dto,SysMenu.class);
                saveList.add(menu);
                continue;
            }
            Iterator<SysMenu> iterator = menuList.iterator();
            while (iterator.hasNext()){
                var next = iterator.next();
                if(next.getId().equals(dto.getId())){
                    UtBeanUtils.copyNotNullProperties(dto,next);
                    iterator.remove();;
                    saveList.add(next);
                    break;
                }
            }
        }
        saveAll(saveList);
        for (int i = 0; i < input.size(); i++) {
            SysMenuInputDto dto = input.get(i);
            SysMenu menu = saveList.get(i);
            dto.setId(menu.getId());
        }
    }

    @Override
    public Long save(SysMenuInputDto input) {
        return saveAndFlush(dozerMapper.map(input, SysMenu.class)).getId();
    }

    @Override
    public void delete(Long id) {
        SysMenu menu = findById(id).orElseThrow(() -> new BusinessException("菜单不存在或已被删除"));
        List<SysMenu> menuList = findAll();
        List<SysMenu> deleteList = new ArrayList<>();
        findChildren(menu, menuList, deleteList);
        deleteInBatch(deleteList);
    }

    @Override
    public void deleteCurrent(Long id) {
        SysMenu menu = findById(id).orElseThrow(() -> new BusinessException("菜单不存在或已被删除"));
        delete(menu);
    }

    void findChildren(SysMenu parent, List<SysMenu> menuList, List<SysMenu> deleteList){
        deleteList.add(parent);
        menuList.forEach(child->{
            if(StringUtils.equals(child.getParentRouteName(),parent.getRouteName())){
                findChildren(child,menuList,deleteList);
            }
        });
    }


    @Override
    public PagedResultDto<GetSysMenuListDto> list(GetSysMenuListSearchDto input) {
        Specification<SysMenu> query = Specifications.where(e -> {
            e.contains(StringUtils.isNotBlank(input.getTitle()), SysMenu.Fields.title, input.getTitle())
                    .contains(StringUtils.isNotBlank(input.getRouteName()), SysMenu.Fields.routeName, input.getRouteName())
                    .contains(StringUtils.isNotBlank(input.getParentRouteName()), SysMenu.Fields.parentRouteName, input.getParentRouteName());
        });
        return DozerUtils.mapToPagedResultDto(dozerMapper, findAll(query, input.getPageable()), GetSysMenuListDto.class);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        repository.deleteByIds(ids);
    }


}
