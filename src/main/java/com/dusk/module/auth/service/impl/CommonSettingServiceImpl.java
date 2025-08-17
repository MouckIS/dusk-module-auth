package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.module.auth.dto.commonsetting.CreateOrUpdateCommonSettingInput;
import com.dusk.module.auth.dto.commonsetting.GetCommonSettingByKeyInput;
import com.dusk.module.auth.dto.commonsetting.GetCommonSettingsInput;
import com.dusk.module.auth.dto.commonsetting.GetGroupNameListInput;
import com.dusk.module.auth.entity.CommonSetting;
import com.dusk.module.auth.repository.ICommonSettingRepository;
import com.dusk.module.auth.service.ICommonSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.dusk.common.framework.service.impl.BaseService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-18 10:53
 */
@Service
public class CommonSettingServiceImpl extends BaseService<CommonSetting, ICommonSettingRepository> implements ICommonSettingService {
    @Autowired
    private Mapper dozerMapper;

    @Override
    public void createOrUpdateCommonSetting(CreateOrUpdateCommonSettingInput input) {
        if(!checkUnique(input.getId(), CommonSetting.Fields.key, input.getKey())){
            throw new BusinessException("存在相同的key[" + input.getKey() + "]，请重新输入！");
        }

        if(input.getId()==null){
            create(input);
        }else{
            update(input);
        }
    }

    private void create(CreateOrUpdateCommonSettingInput input){
        CommonSetting commonSetting = dozerMapper.map(input, CommonSetting.class);
        save(commonSetting);
    }

    private void update(CreateOrUpdateCommonSettingInput input){
        CommonSetting commonSetting = findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的配置信息！"));
        dozerMapper.map(input, commonSetting);
        save(commonSetting);
    }

    @Override
    public Page<CommonSetting> getCommonSettings(GetCommonSettingsInput input) {
        Specification<CommonSetting> spec = Specifications.where(e -> {
            e.contains(StringUtils.isNotBlank(input.getKey()), CommonSetting.Fields.key, input.getKey())
            .contains(StringUtils.isNotBlank(input.getGroupName()), CommonSetting.Fields.key, input.getGroupName());
        });
        return repository.findAll(spec, input.getPageable());
    }

    @Override
    public List<String> getGroupNameList(GetGroupNameListInput input) {
        Specification<CommonSetting> spec = Specifications.where(e -> {
            e.contains(StringUtils.isNotBlank(input.getFilter()), CommonSetting.Fields.groupName, input.getFilter());
        });
        List<CommonSetting> list = repository.findAll(spec);
        return list.stream().map(CommonSetting::getGroupName).collect(Collectors.toList());
    }

    @Override
    public CommonSetting getCommonSettingByKey(GetCommonSettingByKeyInput input) {
        return repository.findByKey(input.getKey()).orElseThrow(() -> new BusinessException("key为[" + input.getKey() + "]的配置信息不存在"));
    }
}
