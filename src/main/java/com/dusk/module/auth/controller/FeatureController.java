package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.module.auth.authorization.EditionAuthProvider;
import com.dusk.module.auth.authorization.TenantAuthProvider;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.dto.configuration.TenantFeatureInputDto;
import com.dusk.module.auth.dto.feature.EditionFeatureInputDto;
import com.dusk.module.auth.service.IFeatureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/4/30 14:44
 */
@Api(description = "特性", tags = "Feature")
@RequestMapping("/feature")
@RestController
public class FeatureController extends CruxBaseController {

    @Autowired
    IFeatureService featureService;

    /**
     * 获取租户特性值，用于getAll接口
     * @return Map<String, Map<String,String>>
     */
    @ApiOperation(value="获取当前租户特性值")
    @RequestMapping(value="/getTenantFeatures",method = RequestMethod.GET)
    public Map<String, Map<String,String>> getTenantFeatures(){
        return featureService.getTenantFeatures();
    }

    /**
     * 获取租户特性值，用于编辑界面
     * @param entityDto 租户id
     */
    @ApiOperation(value="通过租户id获取特性值")
    @RequestMapping(value="/getFeaturesForEdit",method = RequestMethod.GET)
    public List<TenantFeature> getTenantFeaturesForEdit(@ApiParam(value = "租户id") @Valid EntityDto entityDto){
        return featureService.getTenantFeaturesForEdit(entityDto.getId());
    }

    /**
     * 获取版本特性值，用于编辑界面
     * @param entityDto 版本id
     * @return List<TenantFeature>
     */
    @ApiOperation(value="通过版本id获取特性值")
    @RequestMapping(value="/getTenantFeaturesByEdition",method = RequestMethod.GET)
    public List<TenantFeature> getTenantFeaturesByEdition(@ApiParam(value = "版本id") @Valid EntityDto entityDto){
        return featureService.getTenantFeaturesByEdition(entityDto.getId());
    }

    /**
     * 获取默认租户特性
     * @return List<TenantFeature>
     */
    @ApiOperation(value="获取默认的所有特性列表")
    @RequestMapping(value="/getDefaultFeatures",method = RequestMethod.GET)
    public List<TenantFeature> getDefaultFeatures(){

        return featureService.getDefaultFeatures();
    }

    /**
     * 生成版本特性
     * @param feaInput 版本特性
     */
    @ApiOperation(value="更新/保存版本特性")
    @RequestMapping(value="/setEditionFeatures",method = RequestMethod.POST)
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_FEATURE)
    public void setEditionFeatures(
            @ApiParam(value = "版本特性") @Valid @RequestBody EditionFeatureInputDto feaInput){
        featureService.setEditionFeatures(feaInput.getEditionId(),feaInput.getFeatureList());
    }

    /**
     * 修改租户特性
     * @param feaInput 租户特性
     */
    @ApiOperation(value="更新/保存租户特性")
    @RequestMapping(value="/updateTenantFeatures",method = RequestMethod.POST)
    @Authorize(TenantAuthProvider.PAGES_TENANTS_CHANGEFEATURES)
    public void updateTenantFeatures(
            @ApiParam(value = "租户特性") @Valid @RequestBody TenantFeatureInputDto feaInput){
        featureService.updateTenantFeatures(feaInput.getTenantId(),feaInput.getFeatureList());
    }

    /**
     * 重置租户特性
     * @param entityDto 租户id
     */
    @ApiOperation(value="重置租户特性")
    @RequestMapping(value="/resetTenantSpecificFeatures",method = RequestMethod.POST)
    @Authorize(TenantAuthProvider.PAGES_TENANTS_CHANGEFEATURES)
    public void resetTenantSpecificFeatures(@ApiParam(value = "租户id") @Valid @RequestBody EntityDto entityDto){
        featureService.resetTenantSpecificFeatures(entityDto.getId());
    }

}
