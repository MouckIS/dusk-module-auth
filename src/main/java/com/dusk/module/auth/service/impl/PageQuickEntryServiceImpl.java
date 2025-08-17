package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.module.auth.dto.quickentry.GetQuickSetListDto;
import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.entity.quickentry.PageQuickEntry;
import com.dusk.module.auth.entity.quickentry.QPageQuickEntry;
import com.dusk.module.auth.repository.pagequickentry.IPageQuickEntryRepository;
import com.dusk.module.auth.service.IPageQuickEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Service
@Slf4j
@Transactional
public class PageQuickEntryServiceImpl extends BaseService<PageQuickEntry, IPageQuickEntryRepository> implements IPageQuickEntryService {

    @Autowired
    protected Mapper dozerMapper;

    @Autowired
    JPAQueryFactory queryFactory;

    @Override
    public void updateQuickSet(List<UpdatePageQuickSetDto> input) {

        QPageQuickEntry qPageQuickEntry = QPageQuickEntry.pageQuickEntry;
        //获取当前用户ID
        Long userId = LoginUserIdContextHolder.getUserId();

        //先删除之前的设置项
        queryFactory.delete(qPageQuickEntry).where(qPageQuickEntry.createId.eq(userId)).execute();
        //新增
        List<PageQuickEntry> pageQuickEntryList = DozerUtils.mapList(dozerMapper, input, PageQuickEntry.class);
        saveAll(pageQuickEntryList);

    }

    @Override
    public PagedResultDto<QuickEntryListDto> getQuickSetList(GetQuickSetListDto input) {
        QPageQuickEntry qPageQuickEntry = QPageQuickEntry.pageQuickEntry;
        //获取当前用户ID
        Long userId = LoginUserIdContextHolder.getUserId();
        var queryResult = queryFactory.selectFrom(qPageQuickEntry).where(qPageQuickEntry.createId.eq(userId));
        if (Objects.nonNull(input)) {
            //获取到查询条件routeName
            String routeName = input.getRouteName();
            if (StrUtil.isNotBlank(routeName)) {
                queryResult.where(qPageQuickEntry.routeName.containsIgnoreCase(routeName));
            }
        }
        Page pageResult = page(queryResult, input.getPageable());
        List content = pageResult.getContent();

        return new PagedResultDto<>(pageResult.getTotalElements(), DozerUtils.mapList(dozerMapper, content, QuickEntryListDto.class));

    }
}
