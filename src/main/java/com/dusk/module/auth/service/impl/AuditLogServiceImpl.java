package com.dusk.module.auth.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.excel.EasyExcel;
import com.dusk.module.auth.dto.auditlog.*;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.module.auth.dto.auditlog.*;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.QAuditLog;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.repository.IAuditLogRepository;
import com.dusk.module.auth.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletOutputStream;
import java.util.List;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-05-15 12:02
 */
@Service
public class AuditLogServiceImpl extends BaseService<AuditLog, IAuditLogRepository> implements IAuditLogService {
    @Autowired
    private Mapper dozerMapper;

    @Autowired
    JPAQueryFactory queryFactory;

    @Override
    public Page<AuditLogListDto> findAuditLogs(GetAuditLogsInput input) {
        QAuditLog qLog = QAuditLog.auditLog;
        QUser qUser = QUser.user;
        QBeanBuilder<AuditLogListDto> builder = QBeanBuilder.create(AuditLogListDto.class).appendQEntity(qLog,qUser);
        JPAQuery<AuditLogListDto> query = queryFactory.select(builder.build()).from(qLog).leftJoin(qUser).on(qLog.createId.eq(qUser.id));
        if(input.getStartDate()!=null){
            query.where(qLog.executionTime.goe(input.getStartDate()));
        }
        if(input.getEndDate()!=null){
            query.where(qLog.executionTime.loe(input.getEndDate()));
        }
        if(StringUtils.isNotBlank(input.getUserName())){
            query.where(qUser.userName.contains(input.getUserName()));
        }
        if(StringUtils.isNotBlank(input.getServiceName())){
            query.where(qLog.serviceName.contains(input.getServiceName()));
        }
        if(StringUtils.isNotBlank(input.getMethodName())){
            query.where(qLog.methodName.contains(input.getMethodName()));
        }
        if(StringUtils.isNotBlank(input.getBrowserInfo())){
            query.where(qLog.browserInfo.contains(input.getBrowserInfo()));
        }
        if(input.getMinExecutionDuration()!=null){
            query.where(qLog.executionDuration.goe(input.getMinExecutionDuration()));
        }
        if(input.getMaxExecutionDuration()!=null){
            query.where(qLog.executionDuration.loe(input.getMaxExecutionDuration()));
        }
        if(input.getHasException()!=null){
            query.where(BooleanUtils.isTrue(input.getHasException())?qLog.exception.isNotNull():qLog.exception.isNull());
        }

        Pageable pageable = input.getPageable();
        return (Page<AuditLogListDto>) page(query,pageable);
    }

    @Override
    @Transactional
    public AuditLogDetailDto getAuditLogDetail(Long id) {
        QAuditLog qLog = QAuditLog.auditLog;
        QUser qUser = QUser.user;
        QBeanBuilder<AuditLogDetailDto> builder = QBeanBuilder.create(AuditLogDetailDto.class).appendQEntity(qLog,qUser);
        AuditLogDetailDto detailDto = queryFactory.select(builder.build()).from(qLog).leftJoin(qUser).on(qLog.createId.eq(qUser.id)).where(qLog.id.eq(id)).fetchFirst();
        return Optional.ofNullable(detailDto).orElseThrow(()->new BusinessException("审计日志不存在或已被删除"));
    }

    @Override
    public void exportLog(ExportAuditLogsInput input, ServletOutputStream outputStream) {
        QAuditLog qLog = QAuditLog.auditLog;
        QUser qUser = QUser.user;
        QBeanBuilder<AuditLogDetailDto> builder = QBeanBuilder.create(AuditLogDetailDto.class).appendQEntity(qLog,qUser);
        JPAQuery<AuditLogDetailDto> query = queryFactory.select(builder.build()).from(qLog).leftJoin(qUser).on(qLog.createId.eq(qUser.id));
        if(input.getStartDate()!=null){
            query.where(qLog.executionTime.goe(input.getStartDate()));
        }
        if(input.getEndDate()!=null){
            query.where(qLog.executionTime.loe(input.getEndDate()));
        }
        if(StringUtils.isNotBlank(input.getUserName())){
            query.where(qUser.userName.contains(input.getUserName()));
        }
        if(StringUtils.isNotBlank(input.getServiceName())){
            query.where(qLog.serviceName.contains(input.getServiceName()));
        }
        if(StringUtils.isNotBlank(input.getMethodName())){
            query.where(qLog.methodName.contains(input.getMethodName()));
        }
        if(StringUtils.isNotBlank(input.getBrowserInfo())){
            query.where(qLog.browserInfo.contains(input.getBrowserInfo()));
        }
        if(input.getHasException()!=null){
            query.where(BooleanUtils.isTrue(input.getHasException())?qLog.exception.isNotNull():qLog.exception.isNull());
        }

        query.orderBy(qLog.executionTime.desc());

        List<AuditLogDetailDto> detailList = query.fetch();

        List<AuditLogExportDto> list = DozerUtils.mapList(dozerMapper, detailList, AuditLogExportDto.class,  (s, t) -> {
            t.setTime(LocalDateTimeUtil.format(s.getExecutionTime(), "yyyy-MM-dd HH:mm:ss"));
        });

        EasyExcel.write(outputStream, AuditLogExportDto.class).sheet().doWrite(list);
    }

}
