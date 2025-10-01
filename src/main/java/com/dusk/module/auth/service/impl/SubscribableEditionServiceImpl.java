package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.GetEditionInput;
import com.dusk.module.auth.dto.feature.FeatureValueInput;
import com.dusk.module.auth.dto.permission.EditionPermissionInputDto;
import com.dusk.module.auth.entity.QTenantPermission;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.excel.EditionExcelExportWriter;
import com.dusk.module.auth.repository.IFeatureValueRepository;
import com.dusk.module.auth.repository.ISubscribableEditionRepository;
import com.dusk.module.auth.repository.ITenantPermissionRepository;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.ISubscribableEditionService;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.dusk.module.auth.service.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-08 10:40
 */
@Service
@Transactional
public class SubscribableEditionServiceImpl extends BaseService<SubscribableEdition, ISubscribableEditionRepository> implements ISubscribableEditionService {
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    IFeatureValueRepository featureValueRepository;
    @Autowired
    ITenantPermissionRepository tenantPermissionRepository;

    @Autowired
    IAuthPermissionManager permissionManager;

    @Autowired
    IFeatureService featureService;

    @Autowired
    ITenantPermissionService tenantPermissionService;

    @Autowired
    JPAQueryFactory queryFactory;

    @Override
    public SubscribableEdition createEdition(EditionEditDto input) {
        var edition = dozerMapper.map(input, SubscribableEdition.class);

        if (notUniqueDisplayName(null, edition.getDisplayName())) {
            throw new BusinessException("名称[" + input.getDisplayName() + "]已存在，请重新输入！");
        }

        if (edition.getExpiringEditionId() != null) {
            var expiringEdition = findById(edition.getExpiringEditionId()).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));
            if (!expiringEdition.isFree()) {
                throw new BusinessException("到期后强制转为免费版");
            }
        }
        repository.save(edition);
        return edition;
    }

    @Override
    public void updateEdition(EditionEditDto input) {
        if (input.getId() != null) {
            var existingSubscribableEdition = findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));
            if (notUniqueDisplayName(input.getId(), input.getDisplayName())) {
                throw new BusinessException("名称[" + input.getDisplayName() + "]已存在，请重新输入！");
            }

            var updatingSubscribableEdition = dozerMapper.map(input, SubscribableEdition.class);
            if (existingSubscribableEdition.isFree() &&
                    !updatingSubscribableEdition.isFree() &&
                    repository.findOne(Specifications.where(e -> {
                        e.eq(SubscribableEdition.Fields.expiringEditionId, existingSubscribableEdition.getId());
                    })).isPresent()) {
                throw new BusinessException(("此版本用作其他版本订阅到期后版本。如果你想让这个版本付费，你应该先从其他版本中删除它。"));
            }

            dozerMapper.map(input, existingSubscribableEdition);
            repository.save(existingSubscribableEdition);
        }
    }

    /**
     * 校验名称是否唯一
     *
     * @param id
     * @param displayName
     * @return
     */
    private boolean notUniqueDisplayName(Long id, String displayName) {
        var optional = findByDisplayName(displayName);
        if (optional.isEmpty()) {
            return false;
        }
        return !optional.get().getId().equals(id);
    }

    @Override
    public Page<SubscribableEdition> getEditions(GetEditionInput input) {
        Specification<SubscribableEdition> spec = Specifications.where(e -> {
            e.contains(StringUtils.isNotBlank(input.getFilter()), SubscribableEdition.Fields.displayName, input.getFilter());
        });
        return repository.findAll(spec, input.getPageable());
    }

    @Override
    public Optional<SubscribableEdition> findByDisplayName(String name) {
        return repository.findByDisplayName(name);
    }


    @Override
    public void deleteEdition(Long editionId) {
        repository.findById(editionId).orElseThrow(() -> new BusinessException("数据不存在"));
        long count = tenantService.countTenantsByEdition(editionId);
        if (count > 0) {
            throw new BusinessException("当前有" + count + "个租户关联到该版本，不能删除！");
        }
        deleteById(editionId);
        featureValueRepository.deleteByEditionId(editionId);
        tenantPermissionRepository.deleteByEditionId(editionId);
    }

    @Override
    public Workbook export(Long id) {
        SubscribableEdition edition = findById(id).orElseThrow(()->new BusinessException("版本不存在或已被删除"));
        EditionExcelExportWriter writer = new EditionExcelExportWriter(edition);

        List<String> editionPermissions = findEditionPermission(id);
        List<Permission> permissions = permissionManager.getDefinitionPermissionTree(true);
        writer.setEditionPermissions(editionPermissions);
        writer.setDefinitionPermissions(permissions);
        writer.setEditionFeatures(featureService.getTenantFeaturesByEdition(id));
        writer.write();
        return writer.getWorkbook();
    }

    @Override
    public void importEdition(InputStream in) {
        List<Map<Integer,String>> list =  null;
        try {
            list = EasyExcel.read(in).sheet().headRowNumber(0).doReadSync();
        }catch (Exception e){
            throw new BusinessException("无法识别的Excel文件",e);
        }
        if(list.size()<2){
            throw  new BusinessException("读取到的数据行数少于两行,不满足导入条件");
        }

        SubscribableEdition edition = null;
        List<FeatureValueInput> features = new ArrayList<>();
        List<Permission> definitionPermissions = permissionManager.getDefinitionPermissionTree(true);
        Set<String> finalGrantedPermission = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Map<Integer,String> map = list.get(i);
            if(i==0){
                String displayName = map.get(2);
                if(StringUtils.isBlank(displayName)){
                    throw new BusinessException(StrUtil.format("第{}行版本名称不能为空",i+1));
                }
                Optional<SubscribableEdition> optional = findByDisplayName(displayName);
                if(optional.isPresent()){
                    edition = optional.get();
                }else {
                    edition = new SubscribableEdition();
                    edition.setDisplayName(displayName);
                }
                continue;
            }
            if(i==1){
                continue;
            }
            String permission = map.get(0);
            if(StringUtils.isNotBlank(permission)){
                String granted = map.get(2);
                if("是".equals(granted)){
                    recursionAddGrantedPermission(permission,definitionPermissions,finalGrantedPermission);
                }
            }
            String featureName = map.get(EditionExcelExportWriter.FEATURE_START_INDEX);
            if(StringUtils.isBlank(featureName)){
                continue;
            }
            String value = map.get(EditionExcelExportWriter.FEATURE_START_INDEX+2);
            value = StringUtils.substringBefore(value,EditionExcelExportWriter.SEPARATOR);
            if(Objects.isNull(value)){
                value = StringUtils.EMPTY;
            }
            FeatureValueInput feature = new FeatureValueInput();
            feature.setName(featureName);
            feature.setValue(value);
            features.add(feature);
        }
        if(edition.getId()==null){
            edition = save(edition);
        }

        EditionPermissionInputDto input = new EditionPermissionInputDto();
        input.setId(edition.getId());
        input.setPermissions(finalGrantedPermission.stream().collect(Collectors.toList()));
        tenantPermissionService.setEditionPermissions(input);

        featureService.setEditionFeatures(edition.getId(),features);
    }

    void recursionAddGrantedPermission(String granted,List<Permission> definitionPermissions,Set<String> finalGrantedPermission){
        for (Permission definitionPermission : definitionPermissions) {
            if(StringUtils.equals(definitionPermission.getName(),granted)){
                finalGrantedPermission.add(granted);
                if(definitionPermission.getParent()!=null && StringUtils.isNotBlank(definitionPermission.getParent().getName())){
                    recursionAddGrantedPermission(definitionPermission.getParent().getName(),definitionPermissions,finalGrantedPermission);
                }
                break;
            }
        }
    }

    List<String> findEditionPermission(Long editionId){
        QTenantPermission tenantPermission = QTenantPermission.tenantPermission;
        return queryFactory.select(tenantPermission.name).from(tenantPermission).where(tenantPermission.editionId.eq(editionId)).fetch();
    }


}
