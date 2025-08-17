package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.common.framework.service.impl.BaseService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

/**
 * @author jianjianhong
 * @date 2020/12/17
 */
public class CreateOrUpdateService<T extends BaseEntity, K extends IBaseRepository<T>> extends BaseService<T, K> {

    @Autowired
    protected Mapper mapper;

    /**
     * 新增或者更新对象
     * @param object
     * @param id
     * @param tClass
     * @return
     */
    protected T createOrUpdate(Object object, Long id, Class<T> tClass) {
        return createOrUpdate(object, id, tClass, null);
    }

    protected T createOrUpdate(Object object, Long id, Class<T> tClass, Consumer<? super T> action) {
        T t = getUpdatedT(object, id, tClass);
        if(action != null) {
            action.accept(t);
        }
        return save(t);
    }

    /**
     * 获取更新后的对象
     * @param object
     * @param id
     * @param tClass
     * @return
     */
    protected T getUpdatedT(Object object, Long id, Class<T> tClass) {
        T t;
        if(id == null) {
            t = mapper.map(object, tClass);
        }else {
            t = findT(id);
            mapper.map(object, t);
        }
        return t;
    }

    /**
     * 根据ID获取对象
     * @param id
     * @return
     */
    protected T findT(Long id){
        return findById(id).orElseThrow(() -> new BusinessException("未找到相应的记录！"));
    }
}
