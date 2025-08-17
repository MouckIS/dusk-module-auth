package com.dusk.module.auth.repository;


import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface IRoleRepository extends IBaseRepository<Role> {

    @Override
    @EntityGraph(value = "role.creater")
    Page<Role> findAll(Specification<Role> specification, Pageable pageable);

    @Override
    @EntityGraph(value = "role.creater")
    List<Role> findAll(@Nullable Specification<Role> spec);


    @EntityGraph(value = "role.permission")
    Optional<Role> findById(long id);

//    @EntityGraph(value = "role.all")
//    Optional<Role> findByIdEquals(String id);

    Optional<Role> findByRoleName(String name);

    List<Role> findAllByIdIn(List<Long> ids);
    
}


