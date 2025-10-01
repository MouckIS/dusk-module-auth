package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.module.auth.dto.datadisplay.DataDisplayItemDto;
import com.dusk.module.auth.dto.datadisplay.GetDisplaySetInputDto;
import com.dusk.module.auth.dto.datadisplay.UpdateDataDisplaySetDto;
import com.dusk.module.auth.entity.datadisplay.DataDisplaySet;
import com.dusk.module.auth.entity.datadisplay.QDataDisplaySet;
import com.dusk.module.auth.repository.datadisplay.IDataDisplaySetRepository;
import com.dusk.module.auth.service.IDataDisplaySetService;
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
 * @create 2022/2/8
 * @since 1.0.0
 */
@Service
@Slf4j
@Transactional
public class DataDisplaySetServiceImpl extends BaseService<DataDisplaySet, IDataDisplaySetRepository> implements IDataDisplaySetService {

    @Autowired
    JPAQueryFactory queryFactory;

    @Autowired
    protected Mapper dozerMapper;

    @Override
    public void updateDisplaySetItem(List<UpdateDataDisplaySetDto> input) {
        QDataDisplaySet qDataDisplaySet = QDataDisplaySet.dataDisplaySet;

        //获取当前用户ID
        Long userId = LoginUserIdContextHolder.getUserId();
        //删除当前用户所有的数据设置项
        queryFactory.delete(qDataDisplaySet).where(qDataDisplaySet.createId.eq(userId)).execute();
        //删除之后，新增
        List<DataDisplaySet> addDataDisplaySetList = DozerUtils.mapList(dozerMapper, input, DataDisplaySet.class);
        saveAll(addDataDisplaySetList);

    }

    @Override
    public PagedResultDto<DataDisplayItemDto> getList(GetDisplaySetInputDto input) {
        QDataDisplaySet qDataDisplaySet = QDataDisplaySet.dataDisplaySet;

        //获取当前用户ID
        Long userId = LoginUserIdContextHolder.getUserId();
        var query = queryFactory.selectFrom(qDataDisplaySet).where(qDataDisplaySet.createId.eq(userId));

        if (Objects.nonNull(input)) {
            if (Objects.nonNull(input.getDisplayType())) {
                query.where(qDataDisplaySet.displayType.eq(input.getDisplayType()));
            }
        }

        Page pageResult = page(query, input.getPageable());
        List<DataDisplayItemDto> content = pageResult.getContent();
        return new PagedResultDto<>(pageResult.getTotalElements(), DozerUtils.mapList(dozerMapper, content, DataDisplayItemDto.class));

    }
}
