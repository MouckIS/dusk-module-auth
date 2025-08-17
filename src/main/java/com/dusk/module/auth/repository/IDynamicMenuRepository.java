package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.DynamicMenu;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author kefuming
 * @date 2022-08-29 16:16
 */
public interface IDynamicMenuRepository extends IBaseRepository<DynamicMenu> {
    @Query(nativeQuery = true, value = "SELECT DISTINCT ON ( business_key ) id FROM sys_dynamic_menu where dynamic_type='ThirdParty'")
    List<Long> getDistinctDynamicModule();
}
