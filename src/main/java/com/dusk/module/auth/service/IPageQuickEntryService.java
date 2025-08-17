package com.dusk.module.auth.service;

import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.dto.quickentry.GetQuickSetListDto;
import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.entity.quickentry.PageQuickEntry;
import com.dusk.module.auth.repository.pagequickentry.IPageQuickEntryRepository;

import java.util.List;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
public interface IPageQuickEntryService extends IBaseService<PageQuickEntry, IPageQuickEntryRepository> {

    /**
     *  快捷入口设置项的新增
     * @param input
     */
    void updateQuickSet(List<UpdatePageQuickSetDto> input);

    /**
     * 快捷入口设置项的列表信息
     * @param input
     * @return
     */
    PagedResultDto<QuickEntryListDto> getQuickSetList(GetQuickSetListDto input);
}