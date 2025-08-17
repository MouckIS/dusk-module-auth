package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.SubscribableEdition;

import java.util.Optional;

public interface ISubscribableEditionRepository extends IBaseRepository<SubscribableEdition> {
    Optional<SubscribableEdition> findByDisplayName(String name);
}
