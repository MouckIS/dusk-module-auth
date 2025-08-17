package com.dusk.module.auth.service;


import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.syscode.SysCodeValueDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValuePagedReqDto;
import com.dusk.module.auth.entity.SysCodeValue;
import com.dusk.module.auth.repository.ISysCodeValueRepository;

import java.util.List;


public interface ISysCodeValueService extends IBaseService<SysCodeValue, ISysCodeValueRepository> {
    /**
     * 增加/修改配置常量
     */
    SysCodeValueDto add(SysCodeValueDto sysCodeValueDto);

    /**
     * 删除配置常量
     */
    void deleteByIds(List<Long> ids);

    /**
     * 通过typeCode查询配置常量
     */
    PagedResultDto<SysCodeValueDto> list(SysCodeValuePagedReqDto inputDto);


    List<SysCodeValueDto> list(List<String> typeCode);

    void saveAllSysCode(List<SysCodeValueDto> inputList);

    SysCodeValueDto findFirstByTypeCodeAndCode(String typeCode, String code);


    List<SysCodeValueDto> list(String typeCode);

}
