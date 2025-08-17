package com.dusk.module.auth.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.dusk.module.auth.dto.dashboard.*;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.utils.DateUtils;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;
import com.dusk.module.auth.entity.dashboard.DashboardZoneItemRef;
import com.dusk.module.auth.entity.dashboard.QDashboardModule;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleItemRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardZoneItemRefRepository;
import com.dusk.module.auth.service.IDashBoardModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
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
public class DashBoardModuleServiceImpl extends CreateOrUpdateService<DashboardModule, IDashBoardModuleRepository> implements IDashBoardModuleService {

    @Autowired
    private IDashBoardModuleRepository moduleRepository;
    @Autowired
    private IDashBoardModuleItemRepository moduleItemRepository;
    @Autowired
    private IDashBoardZoneItemRefRepository zoneItemRefRepository;
    @Autowired
    JPAQueryFactory queryFactory;

    @Override
    public DashboardModule saveModule(CreateOrUpdateModule input) {
        if(input.getCenterModule() == null) {
            input.setCenterModule(false);
        }
        DashboardModule module = moduleRepository.findByName(input.getName());
        if(module != null && (input.getId() == null || module.getId().longValue() != input.getId().longValue())) {
            throw new BusinessException("["+input.getName()+"]已存在!");
        }
        return createOrUpdate(input, input.getId(), DashboardModule.class);
    }

    @Override
    public void copyItem(CopyModuleItemInput input) {
        DashboardModuleItem sourceModuleItem = moduleItemRepository.findById(input.getSourceModuleItemId()).orElseThrow(()->new BusinessException("未找到id为[" + input.getSourceModuleItemId() + "]的统计项记录！"));
        DashboardModule targetModule = moduleRepository.findById(input.getTargetModuleId()).orElseThrow(()->new BusinessException("未找到id为[" + input.getTargetModuleId() + "]的模块记录！"));

        DashboardModuleItem moduleItem = new DashboardModuleItem();
        moduleItem.setChartType(sourceModuleItem.getChartType());
        moduleItem.setCode(sourceModuleItem.getCode());
        moduleItem.setDataSource(sourceModuleItem.getDataSource());
        moduleItem.setDetailPath(sourceModuleItem.getDetailPath());
        moduleItem.setModuleId(targetModule.getId());
        moduleItem.setModuleType(sourceModuleItem.getModuleType());
        moduleItem.setName(sourceModuleItem.getName());
        moduleItemRepository.save(moduleItem);
    }

    @Override
    public void copyModuleItems(CopyModuleItemsInput input) {
        List<DashboardModuleItem> sourceModuleItems = moduleItemRepository.findAllByModuleId(input.getSourceModuleId());
        List<DashboardModuleItem> targetModuleItems = sourceModuleItems.stream().map((item) -> {
            DashboardModuleItem moduleItem = new DashboardModuleItem();
            moduleItem.setChartType(item.getChartType());
            moduleItem.setCode(item.getCode());
            moduleItem.setDataSource(item.getDataSource());
            moduleItem.setDetailPath(item.getDetailPath());
            moduleItem.setModuleId(input.getTargetModuleId());
            moduleItem.setModuleType(item.getModuleType());
            moduleItem.setName(item.getName());
            return moduleItem;
        }).collect(Collectors.toList());
        moduleItemRepository.saveAll(targetModuleItems);
    }

    @Override
    public PagedResultDto<ModuleListDto> getModuleList(GetModuleInput input) {
        QDashboardModule qDashboardModule = QDashboardModule.dashboardModule;
        QBean<ModuleListDto> moduleListDtoQBean = QBeanBuilder.create(ModuleListDto.class).appendQEntity(qDashboardModule).build();
        var query = queryFactory.select(moduleListDtoQBean).from(qDashboardModule)
                .orderBy(qDashboardModule.createTime.desc());

        //过滤名称
        if (StringUtils.isNotBlank(input.getName())) {
            query = query.where(qDashboardModule.name.contains(input.getName()));
        }

        //过滤编号
        if (StringUtils.isNotBlank(input.getCode())) {
            query = query.where(qDashboardModule.code.contains(input.getCode()));
        }
        var modulePage = page(query, input.getPageable());
        List<ModuleListDto> datas = (List<ModuleListDto>) modulePage.getContent();

        return new PagedResultDto<>(modulePage.getTotalElements(), datas);
    }

    @Override
    public ModuleDetailDto moduleDetail(Long id) {
        DashboardModule module = findT(id);
        ModuleDetailDto detailDto = mapper.map(module, ModuleDetailDto.class);

        List<DashboardModuleItem> items = moduleItemRepository.findAllByModuleIdOrderByCreateTime(module.getId());
        List<ModuleItemListDto> itemListDtos = DozerUtils.mapList(mapper, items, ModuleItemListDto.class);
        detailDto.setModuleItems(itemListDtos);
        return detailDto;
    }

    @Override
    public void deleteModule(Long id) {
        // 删除(分类-模块)关联 和 (模块-统计项)关联 或者 (模块-中心统计项分组)关联
        DashboardModule module = findT(id);
        List<DashboardZoneItemRef> zoneItemRefs = zoneItemRefRepository.findAllByModuleId(module.getId());
        if (zoneItemRefs.isEmpty()) {
            moduleItemRepository.deleteByModuleId(module.getId());
            moduleRepository.delete(module);
        } else {
            throw new BusinessException("该模块已经被使用,无法删除");
        }
    }

    @Override
    public DashboardModuleItem saveModuleItem(CreateOrUpdateModuleItem input) {
        //检查统计项名称是否重复
        List<DashboardModuleItem> items = moduleItemRepository.findAllByModuleIdAndName(input.getModuleId(), input.getName());
        items.forEach(item -> {
            if(!item.getId().equals(input.getId())) {
                throw new BusinessException("已存在名称为["+input.getName()+"]的统计项!");
            }
        });


        DashboardModuleItem moduleItem;
        if (input.getId() == null) {
            moduleItem = mapper.map(input, DashboardModuleItem.class);
        } else {
            moduleItem = moduleItemRepository.findById(input.getId()).orElseThrow(() -> new BusinessException("未找到id为[" + input.getId() + "]的记录！"));
            mapper.map(input, moduleItem);
        }
        moduleItemRepository.save(moduleItem);
        return moduleItem;
    }

    @Override
    public void removeModuleItem(Long id) {
        DashboardModuleItem item = moduleItemRepository.findById(id).orElseThrow(()->new BusinessException("未找到id为[" + id + "]的记录！"));
        List<DashboardZoneItemRef> zoneItemRefs = zoneItemRefRepository.findAllByModuleItemId(item.getId());
        if (zoneItemRefs.isEmpty()) {
            moduleItemRepository.delete(item);
        } else {
            throw new BusinessException("该统计项已经被使用,无法删除");
        }
    }

    private List<ModuleDetailDto> getModuleDetailList() {
        List<ModuleDetailDto> moduleDetailList = DozerUtils.mapList(dozerMapper, findAll(), ModuleDetailDto.class);
        moduleDetailList.forEach(module -> {
            List<DashboardModuleItem> items = moduleItemRepository.findAllByModuleIdOrderByCreateTime(module.getId());
            List<ModuleItemListDto> itemListDtos = DozerUtils.mapList(mapper, items, ModuleItemListDto.class);
            module.setModuleItems(itemListDtos);
        });
        return moduleDetailList;
    }

    @Override
    public void exportModule(HttpServletResponse response) {
        try {
            List<ModuleDetailDto> moduleDetailList = getModuleDetailList();
            moduleDetailList.forEach(module -> {
                module.setId(null);
                module.getModuleItems().forEach(item -> item.setId(null));
            });

            String templateJson = JSONUtil.toJsonStr(JSONUtil.parse(moduleDetailList));

            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(String.format("数据大屏主题模块配置_%s.json", DateUtils.localDateToString(LocalDate.now())), "UTF-8"));
            response.addHeader("Content-Length", "" + templateJson.getBytes().length);
            OutputStream output = response.getOutputStream();
            output.write(templateJson.getBytes());
            output.close();
        } catch (IOException e) {
            log.error("导出数据大屏模块配置异常", e);
            throw new BusinessException("导出模块配置失败:"+e.getMessage());
        }

    }

    @Override
    public void importModule(MultipartFile uploadFile, ModuleItemPermissionInput permissionInput) {
        try {
            File file = new File(uploadFile.getOriginalFilename());
            FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), file);
            String templateStr = FileUtil.readString(file, "utf-8");
            List<ModuleDetailDto> updateModuleDetails = JSONUtil.toList(templateStr, ModuleDetailDto.class);

            if(updateModuleDetails == null || updateModuleDetails.size() == 0) {
                throw new BusinessException("导入数据大屏模块配置失败:配置为空");
            }

            //已有的模块
            List<ModuleDetailDto> moduleDetailList = getModuleDetailList();
            Map<String, ModuleDetailDto> moduleMap = moduleDetailList.stream().collect(Collectors.toMap(ModuleDetailDto::getName, a->a, (k1, k2)->k1));

            //模块更新
            updateModuleDetails.forEach(module -> {
                if(StringUtils.isBlank(module.getCode())) {
                    throw new BusinessException("导入数据大屏模块配置失败:格式不正确");
                }
                boolean hasModule = moduleMap.containsKey(module.getName());

                //更新模块
                DashboardModule newModule;
                if(!hasModule){
                    newModule = dozerMapper.map(module, DashboardModule.class);
                }else {
                    newModule = moduleRepository.findByName(module.getName());
                    newModule.setCenterModule(module.getCenterModule());
                    newModule.setCode(module.getCode());
                }
                save(newModule);

                //更新模块统计项
                if(hasModule){
                    List<ModuleItemListDto> itemList = moduleMap.get(module.getName()).getModuleItems();
                    Map<String, ModuleItemListDto> itemMap = itemList.stream().collect(Collectors.toMap(ModuleItemListDto::getCode, a->a, (k1, k2)->k1));
                    module.getModuleItems().forEach(item -> {
                        if(itemMap.containsKey(item.getCode())) {
                            //更新统计项
                            CreateOrUpdateModuleItem updateItem = dozerMapper.map(item, CreateOrUpdateModuleItem.class);
                            updateItem.setId(itemMap.get(item.getCode()).getId());
                            updateItem.setModuleId(newModule.getId());
                            saveModuleItem(updateItem);
                        }else {
                            //判断新增统计项是否有权限
                            if(hasPermission(permissionInput, module, item)) {
                                CreateOrUpdateModuleItem addItem = dozerMapper.map(item, CreateOrUpdateModuleItem.class);
                                addItem.setId(null);
                                addItem.setModuleId(newModule.getId());
                                saveModuleItem(addItem);
                            }
                        }
                    });
                }else {
                    List<DashboardModuleItem> addItemList = new ArrayList<>();
                    module.getModuleItems().forEach(item -> {
                        if(hasPermission(permissionInput, module, item)) {
                            DashboardModuleItem addItem = mapper.map(item, DashboardModuleItem.class);
                            addItem.setId(null);
                            addItem.setModuleId(newModule.getId());
                            addItemList.add(addItem);
                        }
                    });
                    moduleItemRepository.saveAll(addItemList);
                }
            });

        } catch (IOException e) {
            log.error("导入数据大屏模块配置异常", e);
            throw new BusinessException("导入模块配置失败:"+e.getMessage());
        }
    }

    private boolean hasPermission(ModuleItemPermissionInput permissionInput, ModuleDetailDto module, ModuleItemListDto item) {
        List<String> centerItemPermissions = permissionInput.getCenterItemPermission();
        List<String> businessItemPermissions = permissionInput.getBusinessItemPermission();
        return (module.getCenterModule() && centerItemPermissions != null && centerItemPermissions.contains(item.getCode()))
                || (!module.getCenterModule() && businessItemPermissions!=null && businessItemPermissions.contains(item.getCode()));
    }
}
