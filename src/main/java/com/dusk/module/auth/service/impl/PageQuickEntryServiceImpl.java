package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.module.auth.dto.quickentry.GetQuickSetListDto;
import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.entity.quickentry.PageQuickEntry;
import com.dusk.module.auth.entity.quickentry.QPageQuickEntry;
import com.dusk.module.auth.mapper.PageQuickEntryMapper;
import com.dusk.module.auth.repository.pagequickentry.IPageQuickEntryRepository;
import com.dusk.module.auth.service.IPageQuickEntryService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

    private final PageQuickEntryMapper mapper = PageQuickEntryMapper.INSTANCE;
    @Resource
    private JPAQueryFactory queryFactory;

    @Override
    public void updateQuickSet(List<UpdatePageQuickSetDto> input) {

        QPageQuickEntry qPageQuickEntry = QPageQuickEntry.pageQuickEntry;
        //获取当前用户ID
        Long userId = LoginUserIdContextHolder.getUserId();

        //先删除之前的设置项
        queryFactory.delete(qPageQuickEntry).where(qPageQuickEntry.createId.eq(userId)).execute();
        //新增
        List<PageQuickEntry> pageQuickEntryList = MapperUtil.mapList(input, mapper::toEntity);
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
        Page<PageQuickEntry> pageResult = (Page<PageQuickEntry>) page(queryResult, input.getPageable());
        return MapperUtil.mapToPagedResultDto(pageResult, mapper::toListDto);

    }
}
