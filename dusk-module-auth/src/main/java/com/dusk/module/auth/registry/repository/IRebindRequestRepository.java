package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.RebindRequest;
import com.dusk.module.auth.registry.enums.RebindStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 换绑审批待办仓库，见《权限优化方案-整理版》4.7。
 */
public interface IRebindRequestRepository extends IBaseRepository<RebindRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RebindRequest r where r.id = :id")
    Optional<RebindRequest> findByIdForUpdate(@Param("id") Long id);

    List<RebindRequest> findByCandidateReleaseIdAndStatusIn(Long candidateReleaseId,
                                                            Collection<RebindStatus> statuses);

    List<RebindRequest> findByCandidateReleaseId(Long candidateReleaseId);

    Optional<RebindRequest> findByCandidateReleaseIdAndHttpMethodAndPathAndNewPermissionCode(
            Long candidateReleaseId, String httpMethod, String path, String newPermissionCode);

    List<RebindRequest> findByServiceIdAndStatus(String serviceId, RebindStatus status);
}
