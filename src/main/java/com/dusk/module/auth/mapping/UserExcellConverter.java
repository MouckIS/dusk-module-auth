package com.dusk.module.auth.mapping;

import com.github.dozermapper.core.CustomConverter;
import com.dusk.module.auth.dto.user.UserListRoleDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-29 08:30
 */
@Component
public class UserExcellConverter implements CustomConverter {
    @Override
    public Object convert(Object destination, Object source,Class destClass, Class sourceClass) {
        if (source == null) {
            return null;
        }
        List<String> dest = new ArrayList<>();
        if (source instanceof List) {
            for(Object r : (List<?>)source){
                dest.add(((UserListRoleDto)r).getRoleName());
            }
        }
        return String.join(",", dest);
    }
}
