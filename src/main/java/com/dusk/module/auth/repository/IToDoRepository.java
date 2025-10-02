package com.dusk.module.auth.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-04 13:36
 */
public interface IToDoRepository extends IBaseRepository<Todo> {
    @Override
    @Query("select distinct t.id,t.businessId,t.extensions,t.title,t.state,t.type,t.typeName from Todo t left join TodoPermission t1 on t.id=t1.todoId")
    Page<Todo> findAll(Specification<Todo> spec, Pageable pageable);

    @EntityGraph(value = "todo.permissions")
    List<Todo> findByTypeAndBusinessIdAndFinish(String type , String businessId,boolean finish);
}
