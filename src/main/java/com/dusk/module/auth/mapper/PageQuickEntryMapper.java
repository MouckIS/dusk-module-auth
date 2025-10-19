package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.entity.quickentry.PageQuickEntry;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 21:47
 */
@Mapper
public interface PageQuickEntryMapper {
    PageQuickEntryMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(PageQuickEntryMapper.class);

    PageQuickEntry toEntity(UpdatePageQuickSetDto dto);

    QuickEntryListDto toListDto(PageQuickEntry pageQuickEntry);
}
