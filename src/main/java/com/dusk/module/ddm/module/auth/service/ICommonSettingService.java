package com.dusk.module.ddm.module.auth.service;


import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dto.commonsetting.CreateOrUpdateCommonSettingInput;
import com.dusk.module.auth.dto.commonsetting.GetCommonSettingByKeyInput;
import com.dusk.module.auth.dto.commonsetting.GetCommonSettingsInput;
import com.dusk.module.auth.dto.commonsetting.GetGroupNameListInput;
import com.dusk.module.auth.entity.CommonSetting;
import com.dusk.module.auth.repository.ICommonSettingRepository;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-18 10:53
 */
public interface ICommonSettingService extends IBaseService<CommonSetting, ICommonSettingRepository> {
    /**
     * 创建或更新配置信息
     * @param input
     */
    void createOrUpdateCommonSetting(CreateOrUpdateCommonSettingInput input);

    /**
     * 获取配置信息列表
     * @param input
     * @return
     */
    Page<CommonSetting> getCommonSettings(GetCommonSettingsInput input);

    /**
     * 获取分组名称列表
     * @param input
     * @return
     */
    List<String> getGroupNameList(GetGroupNameListInput input);

    /**
     * 根据Key获取配置信息
     * @param input
     * @return
     */
    CommonSetting getCommonSettingByKey(GetCommonSettingByKeyInput input);
}
