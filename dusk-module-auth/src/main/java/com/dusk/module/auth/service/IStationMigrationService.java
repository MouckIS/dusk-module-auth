package com.dusk.module.auth.service;

/**
 * @author kefuming
 * @date 2023/2/14 13:47
 */
public interface IStationMigrationService {
    /**
     * 迁移OrganizationUnit中的厂站到Station中
     */
    void migration();
}
