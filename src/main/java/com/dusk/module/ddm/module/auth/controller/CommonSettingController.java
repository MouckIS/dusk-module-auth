package com.dusk.module.ddm.module.auth.controller;

import com.dusk.module.auth.dto.commonsetting.*;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.module.auth.dto.commonsetting.*;
import com.dusk.module.auth.entity.CommonSetting;
import com.dusk.module.auth.authorization.CommonSettingAuthProvider;
import com.dusk.module.auth.service.ICommonSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-18 10:54
 */
@RestController
@RequestMapping("commonSetting")
@Api(tags = "CommonSetting", description = "通用配置")
@authorize(CommonSettingAuthProvider.PAGES_COMMONSETTING)
public class CommonSettingController extends CruxBaseController {
    @Autowired
    private ICommonSettingService commonSettingService;
    @Autowired
    private Mapper dozerMapper;


    @PostMapping("createOrUpdateCommonSetting")
    @authorize(CommonSettingAuthProvider.PAGES_COMMONSETTING_CREATEOREDIT)
    @ApiOperation("创建或更新配置信息")
    public void createOrUpdateCommonSetting(@Valid @RequestBody CreateOrUpdateCommonSettingInput input){
        commonSettingService.createOrUpdateCommonSetting(input);
    }

    @DeleteMapping("deleteCommonSetting")
    @authorize(CommonSettingAuthProvider.PAGES_COMMONSETTING_DELETE)
    @ApiOperation("删除配置信息")
    public void deleteCommonSetting(@Valid @RequestBody EntityDto input){
        commonSettingService.deleteById(input.getId());
    }

    @GetMapping("getCommonSettings")
    @ApiOperation("获取配置信息列表")
    public PagedResultDto<CommonSettingDto> getCommonSettings(GetCommonSettingsInput input)
    {
        Page<CommonSetting> page = commonSettingService.getCommonSettings(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, CommonSettingDto.class);
    }

    @GetMapping("getGroupNameList")
    @ApiOperation("获取分组名称列表")
    public List<String> getGroupNameList(GetGroupNameListInput input)
    {
        return commonSettingService.getGroupNameList(input);
    }

    @GetMapping("getCommonSettingByKey")
    @ApiOperation("根据Key获取配置信息")
    public CommonSettingDto getCommonSettingByKey(GetCommonSettingByKeyInput input)
    {
        CommonSetting commonSetting = commonSettingService.getCommonSettingByKey(input);
        return dozerMapper.map(commonSetting, CommonSettingDto.class);
    }
}
