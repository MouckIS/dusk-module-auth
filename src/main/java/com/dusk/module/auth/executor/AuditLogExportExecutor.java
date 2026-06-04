package com.dusk.module.auth.executor;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.utils.JpaQueryFactoryUtil;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.doc.service.SimpleExcelExportExcutor;
import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogExportDto;
import com.dusk.module.auth.dto.auditlog.ExportAuditLogsInput;
import com.dusk.module.auth.entity.QAuditLog;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.mapper.AuditLogMapper;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : kefuming
 * @date : 2026/5/31 23:18
 */
@Service
public class AuditLogExportExecutor extends SimpleExcelExportExcutor<ExportAuditLogsInput, AuditLogExportDto> {
    private final AuditLogMapper mapper = AuditLogMapper.INSTANCE;
    @Resource
    private JPAQueryFactory queryFactory;


    @Override
    protected String fileName() {
        return String.format("审计日志%s.xlsx", LocalDate.now());
    }



    @Override
    public List<AuditLogExportDto> getDataPage(ExportAuditLogsInput input) {
        QAuditLog qLog = QAuditLog.auditLog;
        QUser qUser = QUser.user;
        QBeanBuilder<AuditLogDetailDto> builder = QBeanBuilder.create(AuditLogDetailDto.class).appendQEntity(qLog, qUser);
        JPAQuery<AuditLogDetailDto> query = queryFactory.select(builder.build()).from(qLog).leftJoin(qUser).on(qLog.createId.eq(qUser.id));
        if (input.getStartDate() != null) {
            query.where(qLog.executionTime.goe(input.getStartDate()));
        }
        if (input.getEndDate() != null) {
            query.where(qLog.executionTime.loe(input.getEndDate()));
        }
        if (StringUtils.isNotBlank(input.getUserName())) {
            query.where(qUser.userName.contains(input.getUserName()));
        }
        if (StringUtils.isNotBlank(input.getServiceName())) {
            query.where(qLog.serviceName.contains(input.getServiceName()));
        }
        if (StringUtils.isNotBlank(input.getMethodName())) {
            query.where(qLog.methodName.contains(input.getMethodName()));
        }
        if (StringUtils.isNotBlank(input.getBrowserInfo())) {
            query.where(qLog.browserInfo.contains(input.getBrowserInfo()));
        }
        if (input.getHasException() != null) {
            query.where(BooleanUtils.isTrue(input.getHasException()) ? qLog.exception.isNotNull() : qLog.exception.isNull());
        }

        query.orderBy(qLog.executionTime.desc());

        Page<AuditLogDetailDto> page = (Page<AuditLogDetailDto>) JpaQueryFactoryUtil.page(query, input.getPageable(), AuditLogDetailDto.class);


        List<AuditLogDetailDto> detailList = page.getContent();

        return MapperUtil.mapList(detailList, mapper::detailDtoToExportDto, (s, t) -> {
            t.setTime(LocalDateTimeUtil.format(s.getExecutionTime(), "yyyy-MM-dd HH:mm:ss"));
        });
    }
}
