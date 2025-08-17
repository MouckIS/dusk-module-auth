package com.dusk.module.auth.repository;

import com.dusk.module.auth.entity.FeatureValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/4/29 16:05
 */
public interface IFeatureValueRepository extends JpaRepository<FeatureValue,String>, JpaSpecificationExecutor<FeatureValue> {

    public List<FeatureValue> findAllByTenantId(Long tenantId);

    public List<FeatureValue> findAllByEditionId(Long editionId);

    @Query("select fv from FeatureValue fv where fv.tenantId = :tenantId and fv.name = :name")
    public FeatureValue getFeatureValueByTenant(Long tenantId, String name);

    @Query("select fv from FeatureValue fv where fv.editionId = :editionId and fv.name = :name")
    public FeatureValue getFeatureValueByEdition(Long editionId, String name);

    @Override
    public FeatureValue save(FeatureValue f);

    @Transactional
    public List<FeatureValue> deleteByTenantId(Long tenantId);

    @Transactional
    public List<FeatureValue> deleteByEditionId(Long editionId);
}
