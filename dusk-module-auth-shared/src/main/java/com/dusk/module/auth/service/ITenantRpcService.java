package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.TenantInfoDto;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-07-24 14:58
 */
public interface ITenantRpcService {
    /**
     * 根据租户id查找租户信息 dto字段可拓展
     *
     * @param id
     * @return
     */
    TenantInfoDto findById(Long id);

    /**
     * 根据租户name查找租户信息 dto字段可拓展
     *
     * @param name
     * @return
     */
    TenantInfoDto findByTenantName(String name);

    /**
     * 查找所有租户信息
     *
     * @return
     */
    List<TenantInfoDto> findAll();
}
