package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.EditionListDto;
import com.dusk.module.auth.entity.SubscribableEdition;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 21:08
 */
@Mapper
public interface SubscribableEditionMapper {
    SubscribableEditionMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SubscribableEditionMapper.class);

    SubscribableEdition editDtoToEntity(EditionEditDto dto);

    EditionListDto toEditionListDto(SubscribableEdition entity);

    EditionEditDto toEditionEditDto(SubscribableEdition entity);
}
