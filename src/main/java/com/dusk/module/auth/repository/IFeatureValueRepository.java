package com.dusk.module.auth.repository;

import com.dusk.module.auth.entity.FeatureValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import jakarta.transaction.Transactional;
import java.util.List;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/4/29 16:05
 */
public interface IFeatureValueRepository extends JpaRepository<FeatureValue,String>, JpaSpecificationExecutor<FeatureValue> {

    List<FeatureValue> findAllByTenantId(Long tenantId);

    List<FeatureValue> findAllByEditionId(Long editionId);

    @Query("select fv from FeatureValue fv where fv.tenantId = :tenantId and fv.name = :name")
    FeatureValue getFeatureValueByTenant(Long tenantId, String name);

    @Query("select fv from FeatureValue fv where fv.editionId = :editionId and fv.name = :name")
    FeatureValue getFeatureValueByEdition(Long editionId, String name);

    @Override
    FeatureValue save(FeatureValue f);

    @Transactional
    List<FeatureValue> deleteByTenantId(Long tenantId);

    @Transactional
    List<FeatureValue> deleteByEditionId(Long editionId);
}
