package com.dusk.module.ddm.module.auth.mapping;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.dusk.module.auth.dto.user.UserExcellDto;
import com.dusk.module.auth.dto.user.UserListDto;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-29 08:28
 */
@Component
public class UserExcellMappingBuilder extends BeanMappingBuilder {
    @Override
    protected void configure() {
      mapping(UserListDto.class,UserExcellDto.class).fields("userRoles","roleNames", FieldsMappingOptions.customConverter(UserExcellConverter.class));
    }
}
