package com.dusk.module.auth.setting;


/**
 * @author kefuming
 * @date 2020-05-20 16:17
 */
public interface ISettingManager {
    /**
     * <p>Gets current value of a setting.</p>
     * <p>It gets the setting value, overwritten by application, current tenant and station and current user if exists.</p>
     * @param name Unique name of the setting
     * @return Current value of the setting
     */
    String getSettingValue(String name);

    /**
     * Gets current value of a setting for the application level.
     * @param name Unique name of the setting
     * @return Current value of the setting for the application
     */
    String getSettingValueForApplication(String name);

    /**
     * <p>Gets current value of a setting for a tenant level.</p>
     * <p>It gets the setting value, overwritten by given tenant.</p>
     * @param name Unique name of the setting
     * @return Current value of the setting
     */
    String getSettingValueForTenant(String name);

    /**
     * <p>Gets current value of a setting for a station level.</p>
     * <p>It gets the setting value, overwritten by given tenant and station.</p>
     * @param name Unique name of the setting
     * @return Current value of the setting
     */
    String getSettingValueForStation(String name);

    /**
     * <p>Gets current value of a setting for a user level.</p>
     * <p>It gets the setting value, overwritten by given tenant and station and user.</p>
     * @param name Unique name of the setting
     * @return Current value of the setting for the user
     */
    String getSettingValueForUser(String name);

    /**
     * Changes setting for the application level.
     * @param name Unique name of the setting
     * @param value Value of the setting
     */
    void changeSettingForApplication(String name, String value);

    /**
     * Changes setting for a Tenant.
     * @param name Unique name of the setting
     * @param value Value of the setting
     */
    void changeSettingForTenant(String name, String value);

    /**
     * Changes setting for a station.
     * @param name Unique name of the setting
     * @param value Value of the setting
     */
    void changeSettingForStation(String name, String value);

    /**
     * Changes setting for a user.
     * @param name Unique name of the setting
     * @param value Value of the setting
     */
    void changeSettingForUser(String name, String value);
}
