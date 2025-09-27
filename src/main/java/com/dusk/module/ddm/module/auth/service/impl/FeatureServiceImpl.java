package com.dusk.module.ddm.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.feature.ui.TenantFeature;
import com.dusk.common.core.service.CruxBaseServiceImpl;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.feature.IFeatureCache;
import com.dusk.module.auth.dto.feature.FeatureValueInput;
import com.dusk.module.auth.entity.FeatureValue;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.IFeatureValueRepository;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.ITenantService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 租户特性分为默认特性和特殊特性，新建一个租户后默认具有的特性可以从json构造
 * 后期考虑从前代码或者数据库构造。
 * 当用户修改了特性值后，需要保存到数据库中
 *
 * @author kefuming
 * @date 2020/4/29 16:03
 */

@Service
@Transactional
public class FeatureServiceImpl extends CruxBaseServiceImpl implements IFeatureService {

    @Autowired
    private IFeatureValueRepository featureValueRespository;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private IFeatureCache featureCache;


    @Override
    public Map<String, Map<String, String>> getTenantFeatures() {
        Map<String, Map<String, String>> resultMap = new HashMap<>();
        List<TenantFeature> resultList;
        Long tenantId = TenantContextHolder.getTenantId();
        if (StringUtils.isEmpty(tenantId)) {
            return resultMap;
        }
        resultList = getTenantFeaturesForEdit(tenantId);
        resultList.forEach(tf -> {
            Map<String, String> temMap = new HashMap<>();
            temMap.put("value", tf.getFeatureValue());
            resultMap.put(tf.getName(), temMap);
        });
        return resultMap;
    }

    @Override
    public List<TenantFeature> getTenantFeaturesForEdit(Long tenantId) {
        List<TenantFeature> result = new ArrayList<>();
        if (StringUtils.isEmpty(tenantId)) {
            return result;
        }
        Tenant t = tenantService.findById(tenantId).orElseThrow(() -> new BusinessException("未找到租户"));
        List<FeatureValue> tflist = new ArrayList<>(featureValueRespository.findAllByTenantId(tenantId));
        List<TenantFeature> dflist = featureCache.getDefaultFeatureList();
        if (!StringUtils.isEmpty(t.getEditionId())) {
            List<FeatureValue> eflist = new ArrayList<>(featureValueRespository.findAllByEditionId(t.getEditionId()));
            //将版本特性追加到默认特性
            dflist = mergeFeatureDefaultValue(featureCache.getDefaultFeatureList(), eflist);
        }
        result = mergeFeatureValueToList(dflist, tflist);
        return result;
    }

    @Override
    public void setEditionFeatures(Long editionId, List<FeatureValueInput> featureList) {
        setEditionFeaturesList(editionId, featureList);
    }

    @Override
    public void setEditionFeaturesList(Long editionId, List<FeatureValueInput> featureValues) {
        List<FeatureValue> flist = featureValueRespository.findAllByEditionId(editionId);
        featureValues.forEach(f -> featureCache.getDefaultFeatureList().forEach(fd -> {
            if (f.getName().equals(fd.getName()) && f.getValue().equals(fd.getDefaultValue())) {
                //修改后的特性值与默认特性值相同时，删除数据库的原有的记录
                flist.forEach(fl -> {
                    if (fl.getName().equals(f.getName())) {
                        featureValueRespository.delete(fl);
                    }
                });
            } else if (f.getName().equals(fd.getName()) && !f.getValue().equals(fd.getDefaultValue())) {
                //修改后的特性值与默认特性值不同时，插入或者修改数据库的原有的记录
                final FeatureValue[] fv = {null};   //为了能在lambda表达式里面赋值，这里要定义为数组
                flist.forEach(fl -> {
                    if (fl.getName().equals(f.getName())) {
                        fv[0] = fl;
                    }
                });
                if (null == fv[0]) {
                    fv[0] = new FeatureValue();
                    dozerMapper.map(f, fv[0]);
                    fv[0].setEditionId(editionId);
                } else {
                    fv[0].setValue(f.getValue());
                }
                featureValueRespository.save(fv[0]);
            }
        }));
    }

    @Override
    public void updateTenantFeaturesList(Long tenantId, List<FeatureValueInput> featureValues) {
        List<FeatureValue> flist = featureValueRespository.findAllByTenantId(tenantId);
        Tenant t = tenantService.findById(tenantId).orElseThrow(() -> new BusinessException("未找到租户"));
        if (StringUtils.isEmpty(t.getEdition())) {
            throw new BusinessException("该租户未定义版本!");
        }
        List<FeatureValue> elist = featureValueRespository.findAllByEditionId(t.getEditionId());
        List<TenantFeature> editionDefaultFeature = mergeFeatureDefaultValue(featureCache.getDefaultFeatureList(), elist);
        featureValues.forEach(f -> editionDefaultFeature.forEach(fd -> {
            if (f.getName().equals(fd.getName()) && f.getValue().equals(fd.getDefaultValue())) {
                //修改后的特性值与当前版本默认特性值相同时，删除数据库的原有的记录
                flist.forEach(fl -> {
                    if (fl.getName().equals(f.getName())) {
                        featureValueRespository.delete(fl);
                    }
                });
            } else if (f.getName().equals(fd.getName()) && !f.getValue().equals(fd.getDefaultValue())) {
                //修改后的特性值与当前版本默认特性值不同时，插入或者修改数据库的原有的记录
                final FeatureValue[] fv = {null};   //为了能在lambda表达式里面赋值，这里要定义为数组
                flist.forEach(fl -> {
                    if (fl.getName().equals(f.getName())) {
                        fv[0] = fl;
                    }
                });
                if (null == fv[0]) {
                    fv[0] = new FeatureValue();
                    dozerMapper.map(f, fv[0]);
                    fv[0].setTenantId(tenantId);
                } else {
                    fv[0].setValue(f.getValue());
                }
                featureValueRespository.save(fv[0]);
            }
        }));
    }

    @Override
    public void updateTenantFeatures(Long tenantId, List<FeatureValueInput> featureList) {
        updateTenantFeaturesList(tenantId, featureList);
    }

    @Override
    public List<TenantFeature> getTenantFeaturesByEdition(Long editionId) {
        List<TenantFeature> result;
        List<FeatureValue> eflist = featureValueRespository.findAllByEditionId(editionId);
        List<TenantFeature> dflist = featureCache.getDefaultFeatureList();
        result = mergeFeatureValueToList(dflist, eflist);
        return result;
    }

    @Override
    public void resetTenantSpecificFeatures(Long tenantId) {
        featureValueRespository.deleteByTenantId(tenantId);  //该方法直接删除，不走逻辑删除
    }

    @Override
    public List<TenantFeature> getDefaultFeatures() {
        return featureCache.getDefaultFeatureList();
    }

    @Override
    @DisableGlobalFilter
    public String getFeatureValue(Long tenantId, String name) {
        String result = null;
        FeatureValue fv = featureValueRespository.getFeatureValueByTenant(tenantId, name);
        if (fv == null) {
            Optional<Tenant> option = tenantService.findById(tenantId);
            if (option.isPresent()) {
                Tenant tenant = option.get();
                fv = featureValueRespository.getFeatureValueByEdition(tenant.getEditionId(), name);
            }
        }
        if (fv != null) {
            result = fv.getValue();
        }
        return result;
    }

    //将版本特性合并到临时的初始默认特性
    private List<TenantFeature> mergeFeatureDefaultValue(List<TenantFeature> createFeatureList, List<FeatureValue> editionFeatureList) {
        List<TenantFeature> resultlist = new ArrayList<>();
        createFeatureList.forEach(cf -> {
            TenantFeature temF = new TenantFeature();
            BeanUtils.copyProperties(cf, temF);
            editionFeatureList.forEach(ef -> {
                if (temF.getName().equals(ef.getName())) {
                    temF.setDefaultValue(ef.getValue());
                }
            });
            resultlist.add(temF);
        });
        return resultlist;
    }

    //将特性值与默认特性合并到列表返回给前端
    private List<TenantFeature> mergeFeatureValueToList(List<TenantFeature> createFeatureList, List<FeatureValue> featureList) {
        List<TenantFeature> temFdList = new ArrayList<>();
//        List<TenantFeature> tflist = new ArrayList<>();
        createFeatureList.forEach(cf -> {
            TenantFeature temF = new TenantFeature();
            BeanUtils.copyProperties(cf, temF);
            featureList.forEach(ef -> {
                if (temF.getName().equals(ef.getName())) {
                    temF.setFeatureValue(ef.getValue());
                }
            });
            if (StringUtils.isEmpty(temF.getFeatureValue())) {
                temF.setFeatureValue(temF.getDefaultValue());
            }
            temFdList.add(temF);
        });

        return temFdList;
    }

}
