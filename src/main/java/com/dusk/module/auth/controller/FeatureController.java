package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.authorization.EditionAuthProvider;
import com.dusk.module.auth.authorization.TenantAuthProvider;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.dto.configuration.TenantFeatureInputDto;
import com.dusk.module.auth.dto.feature.EditionFeatureInputDto;
import com.dusk.module.auth.service.IFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/4/30 14:44
 */
@Tag(name = "特性", description = "Feature")
@RequestMapping("/feature")
@RestController
public class FeatureController extends CruxBaseController {

    @Resource
    private IFeatureService featureService;

    /**
     * 获取租户特性值，用于getAll接口
     *
     * @return Map<String, Map<String,String>>
     */
    @Operation(summary = "获取当前租户特性值")
    @RequestMapping(value = "/getTenantFeatures", method = RequestMethod.GET)
    public Map<String, Map<String, String>> getTenantFeatures() {
        return featureService.getTenantFeatures();
    }

    /**
     * 获取租户特性值，用于编辑界面
     *
     * @param entityDto 租户id
     */
    @Operation(summary = "通过租户id获取特性值")
    @RequestMapping(value = "/getFeaturesForEdit", method = RequestMethod.GET)
    public List<TenantFeature> getTenantFeaturesForEdit(@Parameter(description = "租户id") @Valid EntityDto entityDto) {
        return featureService.getTenantFeaturesForEdit(entityDto.getId());
    }

    /**
     * 获取版本特性值，用于编辑界面
     *
     * @param entityDto 版本id
     * @return List<TenantFeature>
     */
    @Operation(summary = "通过版本id获取特性值")
    @RequestMapping(value = "/getTenantFeaturesByEdition", method = RequestMethod.GET)
    public List<TenantFeature> getTenantFeaturesByEdition(@Parameter(description = "版本id") @Valid EntityDto entityDto) {
        return featureService.getTenantFeaturesByEdition(entityDto.getId());
    }

    /**
     * 获取默认租户特性
     *
     * @return List<TenantFeature>
     */
    @Operation(summary = "获取默认的所有特性列表")
    @RequestMapping(value = "/getDefaultFeatures", method = RequestMethod.GET)
    public List<TenantFeature> getDefaultFeatures() {

        return featureService.getDefaultFeatures();
    }

    /**
     * 生成版本特性
     *
     * @param feaInput 版本特性
     */
    @Operation(summary = "更新/保存版本特性")
    @RequestMapping(value = "/setEditionFeatures", method = RequestMethod.POST)
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_FEATURE)
    public void setEditionFeatures(
            @Parameter(description = "版本特性") @Valid @RequestBody EditionFeatureInputDto feaInput) {
        featureService.setEditionFeatures(feaInput.getEditionId(), feaInput.getFeatureList());
    }

    /**
     * 修改租户特性
     *
     * @param feaInput 租户特性
     */
    @Operation(summary = "更新/保存租户特性")
    @RequestMapping(value = "/updateTenantFeatures", method = RequestMethod.POST)
    @Authorize(TenantAuthProvider.PAGES_TENANTS_CHANGEFEATURES)
    public void updateTenantFeatures(
            @Parameter(description = "租户特性") @Valid @RequestBody TenantFeatureInputDto feaInput) {
        featureService.updateTenantFeatures(feaInput.getTenantId(), feaInput.getFeatureList());
    }

    /**
     * 重置租户特性
     *
     * @param entityDto 租户id
     */
    @Operation(summary = "重置租户特性")
    @RequestMapping(value = "/resetTenantSpecificFeatures", method = RequestMethod.POST)
    @Authorize(TenantAuthProvider.PAGES_TENANTS_CHANGEFEATURES)
    public void resetTenantSpecificFeatures(@Parameter(description = "租户id") @Valid @RequestBody EntityDto entityDto) {
        featureService.resetTenantSpecificFeatures(entityDto.getId());
    }

}
