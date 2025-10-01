package com.dusk.module.auth.service.impl;

import com.dusk.commom.rpc.auth.service.ICommonFavoriteRpcService;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.CommonFavoriteDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Sequence;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.entity.CommonFavorite;
import com.dusk.module.auth.entity.QCommonFavorite;
import com.dusk.module.auth.repository.ICommonFavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Objects;
import java.util.Optional;

/**
 * @author chenzhi1
 * @date 2021/5/25 11:30
 */
@Service
@Transactional
public class CommonFavoriteServiceImpl extends BaseService<CommonFavorite, ICommonFavoriteRepository> implements ICommonFavoriteRpcService {
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    Sequence sequence;
    @Autowired
    Mapper dozer;

    QCommonFavorite qCommonFavorite = QCommonFavorite.commonFavorite;

    @SneakyThrows
    @Override
    public  CommonFavoriteDto save(@Valid CommonFavoriteDto dto) {
        CommonFavorite commonFavorite = null;
        if (dto.getId() == null) {
            long count = queryFactory.selectFrom(qCommonFavorite).where(qCommonFavorite.name.eq(dto.getName()).and(qCommonFavorite.type.eq(dto.getType()))).fetchCount();
            if (count > 0) {
                throw new BusinessException("该名称数据已存在");
            }
            dto.setId(sequence.nextId());
            commonFavorite = dozer.map(dto, CommonFavorite.class);
        } else {
            commonFavorite = findById(dto.getId()).orElseThrow(() -> new BusinessException("数据不存在或已被删除"));
            //判断修改操作的名字有没有重名
            if (!commonFavorite.getName().equals(dto.getName())) {
                long count = queryFactory.selectFrom(qCommonFavorite).where(qCommonFavorite.name.eq(dto.getName()).and(qCommonFavorite.type.eq(dto.getType()))).fetchCount();
                if (count > 0) {
                    throw new BusinessException("该名称数据已存在");
                }
            }
            dozer.map(dto, commonFavorite);
        }

        save(commonFavorite);
        return dto;
    }

    @Override
    public void deleteById(Long id, String type) {
        Optional<CommonFavorite> optional = findById(id);
        if(optional.isPresent()){
            CommonFavorite favorite = optional.get();
           if(!StringUtils.equals(type,favorite.getType())){
               throw new BusinessException("指定删除收藏数据类型不匹配");
           }
           if(Objects.nonNull(favorite.getCreateId()) && !Objects.equals(favorite.getCreateId(), LoginUserIdContextHolder.getUserId())){
               throw new BusinessException("不能删除他人的收藏");
           }
           delete(favorite);
        }
    }

    @Override
    public void setPublic(Long id, boolean isPublic) {
        Optional<CommonFavorite> optional = findById(id);
        if(optional.isEmpty()){
            throw new BusinessException("该收藏不存在或已被删除");
        }
        CommonFavorite favorite = optional.get();
        if(Objects.nonNull(favorite.getCreateId()) && !Objects.equals(favorite.getCreateId(), LoginUserIdContextHolder.getUserId())){
            throw new BusinessException("不能设置他人的收藏是否公开");
        }
        favorite.setIsPublic(isPublic);
        save(favorite);
    }

    @SneakyThrows
    @Override
    public CommonFavoriteDto get(Long id, String type) {
        QBean<CommonFavoriteDto> qBean = QBeanBuilder.create(CommonFavoriteDto.class).appendQEntity(qCommonFavorite).build();
        CommonFavoriteDto dto = queryFactory.select(qBean).from(qCommonFavorite).where(qCommonFavorite.id.eq(id).and(qCommonFavorite.type.eq(type))).fetchOne();
        if(Objects.isNull(dto)){
            throw new BusinessException("该收藏不存在或已被删除");
        }
        return dto;
    }

    @Override
    public PagedResultDto<CommonFavoriteDto> list(boolean onlyMine, PagedAndSortedInputDto pageDto, String... types) {
        QBean<CommonFavoriteDto> qBean = QBeanBuilder.create(CommonFavoriteDto.class).appendQEntity(qCommonFavorite).build();
        JPAQuery<CommonFavoriteDto> query = queryFactory.select(qBean).from(qCommonFavorite).where(qCommonFavorite.type.in(types));
        if(onlyMine){
            query.where(qCommonFavorite.createId.eq(LoginUserIdContextHolder.getUserId()));
        }else{
            query.where(qCommonFavorite.createId.eq(LoginUserIdContextHolder.getUserId()).or(qCommonFavorite.isPublic.eq(true)));
        }
        Page<CommonFavoriteDto> page = (Page<CommonFavoriteDto>) page(query,pageDto.getPageable());
        return new PagedResultDto<>(page.getTotalElements(),page.getContent());
    }
}
