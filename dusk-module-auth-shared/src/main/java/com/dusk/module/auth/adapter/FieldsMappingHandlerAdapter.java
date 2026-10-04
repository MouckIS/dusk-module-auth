package com.dusk.module.auth.adapter;

import com.dusk.common.core.port.FieldsMappingHandler;
import com.dusk.module.auth.dto.UserSimpleDto;
import com.dusk.module.auth.service.IUserRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : kefuming
 * @date : 2026/10/4 15:38
 */
@Service
public class FieldsMappingHandlerAdapter implements FieldsMappingHandler {
    @DubboReference
    private IUserRpcService userRpcService;

    @Override
    public Map<Long, String> userNameMapping(Set<Long> userIds) {
        return userRpcService.getUserSimpleDto(userIds)
                .stream()
                .filter(dto -> dto.getId() != null && dto.getName() != null)
                .collect(Collectors.toMap(UserSimpleDto::getId, UserSimpleDto::getName, (k1, k2) -> k1));
    }

    @Override
    public List<Long> getUserIdsByNameLike(String name) {
        return userRpcService.getUserIdsByNameLike(name);
    }
}
