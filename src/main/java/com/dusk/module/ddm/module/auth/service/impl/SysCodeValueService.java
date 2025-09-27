package com.dusk.module.ddm.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.seata.common.util.StringUtils;
import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.annotation.DisableTenantFilter;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.core.utils.UtBeanUtils;
import com.dusk.common.module.auth.dto.syscode.SysCodeValueDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValuePagedReqDto;
import com.dusk.module.auth.common.syscode.ISysCodeCache;
import com.dusk.module.auth.entity.QSysCodeValue;
import com.dusk.module.auth.entity.SysCodeValue;
import com.dusk.module.auth.repository.ISysCodeValueRepository;
import com.dusk.module.auth.service.ISysCodeValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SysCodeValueService extends BaseService<SysCodeValue, ISysCodeValueRepository> implements ISysCodeValueService {
    @Autowired
    private Mapper objectMapper;
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    ISysCodeCache sysCodeCache;

    QSysCodeValue qSysCodeValue = QSysCodeValue.sysCodeValue;

    @Override
    @DisableTenantFilter
    public SysCodeValueDto add(SysCodeValueDto sysCodeValueDto) {
        boolean isEdit = sysCodeValueDto.getId() != null;
        Long tenantId = TenantContextHolder.getTenantId();
        boolean extendOwner = isExtendOwner(sysCodeValueDto.getTypeCode());
        //校验 是否允许新增
        // 当租户编辑的时候需要校验是否允许新增
        if (tenantId != null && !isEdit && extendOwner) {
            throw new BusinessException("无法新增数据，仅允许编辑！");
        }
        SysCodeValue sysCodeValue;
        if (isEdit) {
            sysCodeValue = repository.findById(sysCodeValueDto.getId()).orElseThrow(() -> new BusinessException(StrUtil.format("ID为[{}]的配置常量不存在", sysCodeValueDto.getId())));
            //当编辑的租户id数据不一致时候 调整为新增模式
            if (!Objects.equals(sysCodeValue.getTenantId(), tenantId)) {
                sysCodeValue = objectMapper.map(sysCodeValueDto, SysCodeValue.class);
                sysCodeValue.setId(null);
            } else {
                UtBeanUtils.copyNotNullProperties(sysCodeValueDto, sysCodeValue);
            }
        } else {
            BooleanExpression expression = qSysCodeValue.typeCode.eq(sysCodeValueDto.getTypeCode()).and(qSysCodeValue.code.eq(sysCodeValueDto.getCode()).or(qSysCodeValue.value.eq(sysCodeValueDto.getValue())));
            if (tenantId != null) {
                expression = expression.and(qSysCodeValue.tenantId.eq(tenantId));
            } else {
                expression = expression.and(qSysCodeValue.tenantId.isNull());
            }
            List<SysCodeValue> sysCodeValueListTemp = queryFactory.select(qSysCodeValue).from(qSysCodeValue).where(expression).fetch();
            if (sysCodeValueListTemp.size() > 0) {
                throw new BusinessException("该类型配置常量代码/值重复");
            }
            sysCodeValue = objectMapper.map(sysCodeValueDto, SysCodeValue.class);
        }
        SysCodeValue sysCodeValueResp = repository.save(sysCodeValue);
        return objectMapper.map(sysCodeValueResp, sysCodeValueDto.getClass());
    }

    @Override
    @DisableTenantFilter
    public void deleteByIds(List<Long> ids) {
        if (ids.size() == 0) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        List<SysCodeValue> fetch = queryFactory.selectFrom(qSysCodeValue).where(qSysCodeValue.id.in(ids)).fetch();
        for (SysCodeValue codeValue : fetch) {
            if (!Objects.equals(codeValue.getTenantId(), tenantId)) {
                throw new BusinessException("无法删除！");
            }
        }
        queryFactory.delete(qSysCodeValue).where(qSysCodeValue.id.in(ids)).execute();
    }

    @Override
    //禁止使用数据全局过滤条件
    @DisableGlobalFilter
    public PagedResultDto<SysCodeValueDto> list(SysCodeValuePagedReqDto inputDto) {
        Page<?> page = page(queryFactory.selectFrom(qSysCodeValue).where(getQueryExpression(inputDto)), inputDto.getPageable());
        return DozerUtils.mapToPagedResultDto(objectMapper, page, SysCodeValueDto.class);
    }


    @Override
    public void saveAllSysCode(List<SysCodeValueDto> inputList) {
        long count = inputList.stream().map(e -> e.getTypeCode() + e.getCode()).distinct().count();

        if (inputList.size() > count) {
            throw new BusinessException("保存数据中存在重复的常量");
        }

        for (SysCodeValueDto sysCode : inputList) {
            Optional<SysCodeValue> optional = findOne(Specifications.where(e -> {
                e.ne(sysCode.getId() != null, BaseEntity.Fields.id, sysCode.getId());
                e.eq(SysCodeValue.Fields.typeCode, sysCode.getTypeCode());
                e.eq(SysCodeValue.Fields.code, sysCode.getCode());
            }));

            if (optional.isPresent()) {
                throw new BusinessException(StrUtil.format("常量类型代码[{}]的常量[{}]与现有常量重复", sysCode.getTypeCode(), sysCode.getCode()));
            }
        }

        saveAll(DozerUtils.mapList(objectMapper, inputList, SysCodeValue.class));
    }

    @Override
    public SysCodeValueDto findFirstByTypeCodeAndCode(String typeCode, String code) {
        SysCodeValuePagedReqDto inputDto = new SysCodeValuePagedReqDto();
        inputDto.setTypeCodeList(Collections.singletonList(typeCode));
        inputDto.setCode(code);
        inputDto.setSorting(SysCodeValue.Fields.sortIndex);
        SysCodeValue fetch = queryFactory.selectFrom(qSysCodeValue).where(getQueryExpression(inputDto)).fetchFirst();
        if (fetch != null) {
            return objectMapper.map(fetch, SysCodeValueDto.class);
        }
        return null;
    }

    @Override
    @DisableTenantFilter
    public List<SysCodeValueDto> list(List<String> typeCode) {
        SysCodeValuePagedReqDto inputDto = new SysCodeValuePagedReqDto();
        inputDto.setTypeCodeList(typeCode);
        List<SysCodeValue> fetch = queryFactory.selectFrom(qSysCodeValue).where(getQueryExpression(inputDto)).orderBy(qSysCodeValue.sortIndex.asc()).fetch();
        return DozerUtils.mapList(objectMapper, fetch, SysCodeValueDto.class);
    }


    @Override
    @DisableTenantFilter
    public List<SysCodeValueDto> list(String typeCode) {
        return list(Collections.singletonList(typeCode));
    }


    //region private method


    private boolean isExtendOwner(String typeCode) {
        return sysCodeCache.getSysCodeType().stream().anyMatch(p -> p.getTypeCode().equalsIgnoreCase(typeCode) && p.isExtendOwner() && TenantContextHolder.getTenantId() != null);
    }


    private BooleanExpression getQueryExpression(SysCodeValuePagedReqDto input) {
        Long tenantId = TenantContextHolder.getTenantId();
        BooleanExpression expression;
        BooleanExpression orExpression = null;
        List<String> extendOwnerTypeCode = input.getTypeCodeList().stream().filter(this::isExtendOwner).collect(Collectors.toList());
        List<String> commonTypeCode = input.getTypeCodeList().stream().filter(p -> !isExtendOwner(p)).collect(Collectors.toList());


        if (tenantId != null) {
            expression = qSysCodeValue.tenantId.eq(tenantId);
        } else {
            expression = qSysCodeValue.tenantId.isNull();
        }
        //查询模式
        switch (input.getListMode()) {
            case ENABLED:
                expression = expression.and(qSysCodeValue.dr.eq(0));
                break;
            case DISABLED:
                expression = expression.and(qSysCodeValue.dr.eq(1));
                break;
            default:
                break;
        }
        expression = expression.and(qSysCodeValue.typeCode.in(commonTypeCode));
        if (StringUtils.isNotBlank(input.getFilter())) {
            expression = expression.and(qSysCodeValue.code.contains(input.getFilter()).or(qSysCodeValue.value.contains(input.getFilter())));
        }
        if (StringUtils.isNotBlank(input.getCode())) {
            expression = expression.and(qSysCodeValue.code.eq(input.getCode()));
        }
        if (extendOwnerTypeCode.size() > 0) {
            BooleanExpression tempExpression = qSysCodeValue.typeCode.in(extendOwnerTypeCode);
            //查询模式
            switch (input.getListMode()) {
                case ENABLED:
                    tempExpression = tempExpression.and(qSysCodeValue.dr.eq(0));
                    break;
                case DISABLED:
                    tempExpression = tempExpression.and(qSysCodeValue.dr.eq(1));
                    break;
                default:
                    break;
            }
            if (StringUtils.isNotBlank(input.getFilter())) {
                tempExpression = tempExpression.and(qSysCodeValue.code.contains(input.getFilter()).or(qSysCodeValue.value.contains(input.getFilter())));
            }
            if (StringUtils.isNotBlank(input.getCode())) {
                tempExpression = tempExpression.and(qSysCodeValue.code.eq(input.getCode()));
            }
            JPAQuery<String> subQuery = queryFactory.selectDistinct(qSysCodeValue.typeCode.concat("&&").concat(qSysCodeValue.code)).from(qSysCodeValue)
                    .where(tempExpression.and(qSysCodeValue.tenantId.eq(tenantId)));
            orExpression = tempExpression.and(qSysCodeValue.tenantId.eq(tenantId).or(qSysCodeValue.tenantId.isNull().and(qSysCodeValue.typeCode.concat("&&").concat(qSysCodeValue.code).notIn(subQuery))));
        }
        if (orExpression != null) {
            return expression.or(orExpression);
        }
        return expression;

    }
    //endregion


}
