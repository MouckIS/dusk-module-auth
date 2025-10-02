package com.dusk.module.auth.service.impl;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.dto.loginlog.ListUserLoginLogInput;
import com.dusk.module.auth.dto.loginlog.UserLoginLogDto;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.entity.QUserLoginLog;
import com.dusk.module.auth.entity.UserLoginLog;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.repository.IUserLoginLogRepository;
import com.dusk.module.auth.service.IUserLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Service
public class UserLoginLogServiceImpl extends BaseService<UserLoginLog, IUserLoginLogRepository> implements IUserLoginLogService {
    @Autowired
    private JPAQueryFactory queryFactory;


    @Override
    public void saveLog(LogInOutEvent event) {
        repository.save(dozerMapper.map(event, UserLoginLog.class));
    }

    @Override
    public PagedResultDto<UserLoginLogDto> listLog(ListUserLoginLogInput input) {
        QUserLoginLog qLog = QUserLoginLog.userLoginLog;
        QUser qUser = QUser.user;
        QBeanBuilder<UserLoginLogDto> builder = QBeanBuilder.create(UserLoginLogDto.class).appendQEntity(qLog, qUser);
        JPAQuery<UserLoginLogDto> query = queryFactory.select(builder.build()).from(qLog).leftJoin(qUser).on(qLog.userName.eq(qUser.userName));
        if (StringUtils.isNotBlank(input.getUserName())) {
            query.where(qUser.userName.containsIgnoreCase(input.getUserName()));
        }
        if (input.getLogType() != null) {
            query.where(qLog.logType.eq(input.getLogType()));
        }
        if (input.getBeginTime() != null) {
            query.where(qLog.operationTime.after(input.getBeginTime()));
        }
        if (input.getEndTime() != null) {
            query.where(qLog.operationTime.before(input.getEndTime()));
        }
        Page<UserLoginLogDto> page = (Page<UserLoginLogDto>) page(query, input.getPageable());
        page.getContent().stream().forEach(e -> e.setLogTypeName(e.getLogType().getDisplayName()));
        return new PagedResultDto<>(page.getTotalElements(), page.getContent());
    }
}
