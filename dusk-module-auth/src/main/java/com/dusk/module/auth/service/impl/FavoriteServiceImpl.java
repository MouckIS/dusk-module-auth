package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.*;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.service.IFavoriteService;
import com.dusk.module.auth.service.ICommonFavoriteRpcService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author: pengmengjiang
 * @date: 2022/1/23 9:26
 */
@Service
@Slf4j
public class FavoriteServiceImpl<T extends AbstractBaseFavoriteDto> implements IFavoriteService<T> {
    @DubboReference
    ICommonFavoriteRpcService iCommonFavoriteRpcService;
    @Resource
    ObjectMapper jackson;

    @Override
    public T save(T dto) {
        if (Objects.isNull(dto.getIsPublic())) {
            dto.setIsPublic(true);
        }
        CommonFavoriteDto saveDto = BeanUtils.instantiateClass(CommonFavoriteDto.class);
        BeanUtils.copyProperties(dto, saveDto);
        saveDto.setContent(getSerializeContent(dto));
        saveDto = iCommonFavoriteRpcService.save(saveDto);

        dto.setId(saveDto.getId());
        dto.setIsMine(true);
        return dto;
    }

    @Override
    public void deleteById(Long id, String type) {
        iCommonFavoriteRpcService.deleteById(id, type);
    }

    @Override
    public void setPublic(Long id, boolean isPublic) {
        iCommonFavoriteRpcService.setPublic(id, isPublic);
    }

    @Override
    public T get(Long id, String type, Class<T> favoriteClazz) {
        CommonFavoriteDto favoriteDto = iCommonFavoriteRpcService.get(id, type);
        T dto = BeanUtils.instantiateClass(favoriteClazz);
        BeanUtils.copyProperties(favoriteDto, dto);
        dto.setContentData(getDeSerializeContent(favoriteDto, favoriteClazz));
        return dto;
    }

    @Override
    public List<T> list(String type, boolean onlyMine, Class<T> favoriteClazz) {
        PagedAndSortedInputDto input = new PagedAndSortedInputDto();
        input.setUnPage(true);
        PagedResultDto<CommonFavoriteDto> page = iCommonFavoriteRpcService.list(onlyMine, input, type);
        List<T> list = new ArrayList<>();
        for (CommonFavoriteDto item : page.getItems()) {
            T t = BeanUtils.instantiateClass(favoriteClazz);
            BeanUtils.copyProperties(item, t);
            t.setContentData(getDeSerializeContent(item, favoriteClazz));
            list.add(t);
        }
        return list;
    }

    String getSerializeContent(T dto) {
        try {
            return jackson.writeValueAsString(dto.getContentData());
        } catch (Exception e) {
            throw new BusinessException("序列化收藏内容失败", e);
        }
    }

    public AbstractFavoriteContent getDeSerializeContent(CommonFavoriteDto dto, Class<T> clazz) {
        try {
            ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
            Class contentClass = (Class) parameterizedType.getActualTypeArguments()[0];
            return (AbstractFavoriteContent) jackson.readValue(dto.getContent(), contentClass);
        } catch (Exception e) {
            throw new BusinessException("反序列化收藏内容失败", e);
        }
    }


}
