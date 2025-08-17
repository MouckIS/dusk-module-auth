package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.SysMenu;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ISysMenuRepository extends IBaseRepository<SysMenu> {
    @Transactional
    @Modifying
    @Query("delete  from SysMenu s  where  s.id in (:ids) ")
    void deleteByIds(@Param("ids") List<Long> ids);
}
