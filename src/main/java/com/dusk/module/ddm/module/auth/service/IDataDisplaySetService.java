package com.dusk.module.ddm.module.auth.service;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dto.datadisplay.DataDisplayItemDto;
import com.dusk.module.auth.dto.datadisplay.GetDisplaySetInputDto;
import com.dusk.module.auth.dto.datadisplay.UpdateDataDisplaySetDto;
import com.dusk.module.auth.entity.datadisplay.DataDisplaySet;
import com.dusk.module.auth.repository.datadisplay.IDataDisplaySetRepository;

import java.util.List;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
public interface IDataDisplaySetService extends IBaseService<DataDisplaySet, IDataDisplaySetRepository> {

    /**
     * 数据展示设置项的新增
     * @param input
     */
    void updateDisplaySetItem(List<UpdateDataDisplaySetDto>  input);

    /**
     * 获取到数据展示设置项的列表信息
     * @param input
     * @return
     */
    PagedResultDto<DataDisplayItemDto> getList(GetDisplaySetInputDto input);
}