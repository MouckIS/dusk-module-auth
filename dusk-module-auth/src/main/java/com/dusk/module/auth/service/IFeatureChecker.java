package com.dusk.module.auth.service;

/**
 * @author duanxiaokang
 * @date 2020/5/29 13:48
 */
public interface IFeatureChecker {
    /**
     * 获取指定的feature的值
     *
     * @param name
     * @return
     */
    String getValue(String name);

    /**
     * 根据租户id获取指定的feature的值，常见于没有上下文以及想获取其他上下文的特性的情况
     *
     * @param tenantId
     * @param name
     * @return
     */
    String getValue(Long tenantId, String name);

    /**
     * 判断指定的feature是否启用，不区分true大小写
     *
     * @param name
     * @return
     */
    boolean isEnabled(String name);


    /**
     * 根据租户id判断指定的feature是否启用，不区分true大小写，常见于没有上下文以及想获取其他上下文的特性的情况
     *
     * @param tenantId
     * @param name
     * @return
     */
    boolean isEnabled(Long tenantId, String name);


}
