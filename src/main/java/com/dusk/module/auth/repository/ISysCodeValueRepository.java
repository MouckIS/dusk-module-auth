package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.SysCodeValue;

import java.util.List;

public interface ISysCodeValueRepository extends IBaseRepository<SysCodeValue> {
    List<SysCodeValue> findByTypeCode(String typeCode);

    List<SysCodeValue> findByTypeCodeIn(List<String> typeCodes);

    SysCodeValue findFirstByTypeCodeAndCode(String typeCode, String code);

}
