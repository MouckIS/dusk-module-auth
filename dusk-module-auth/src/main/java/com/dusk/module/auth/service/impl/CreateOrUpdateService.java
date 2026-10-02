package com.dusk.module.auth.service.impl;

import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.common.core.service.impl.BaseService;
import org.springframework.beans.BeanUtils;

import java.util.function.Consumer;

import static com.dusk.common.core.utils.UtBeanUtils.getNullPropertyNames;

/**
 * @author jianjianhong
 * @date 2020/12/17
 */
public class CreateOrUpdateService<T extends BaseEntity, K extends IBaseRepository<T>> extends BaseService<T, K> {


    /**
     * 新增或者更新对象
     *
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
        if (action != null) {
            action.accept(t);
        }
        return save(t);
    }

    /**
     * 获取更新后的对象
     *
     * @param object
     * @param id
     * @param tClass
     * @return
     */
    protected T getUpdatedT(Object object, Long id, Class<T> tClass) {
        T t;
        if (id == null) {
            try {
                t = tClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(object, t);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("创建目标对象失败", e);
            }
        } else {
            t = findT(id);
            BeanUtils.copyProperties(object, t, getNullPropertyNames(object));
        }
        return t;
    }

    /**
     * 根据ID获取对象
     *
     * @param id
     * @return
     */
    protected T findT(Long id) {
        return findById(id).orElseThrow(() -> new BusinessException("未找到相应的记录！"));
    }
}
