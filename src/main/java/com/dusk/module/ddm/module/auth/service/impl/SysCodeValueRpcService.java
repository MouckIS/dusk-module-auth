package com.dusk.module.ddm.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.syscode.SysCodeDefinitionDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeSyncDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValueDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValuePagedReqDto;
import com.dusk.common.module.auth.service.ISysCodeValueRpcService;
import com.dusk.module.auth.common.syscode.ISysCodeCache;
import com.dusk.module.auth.service.ISysCodeTypeService;
import com.dusk.module.auth.service.ISysCodeValueService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-12-03 11:18
 */
@Service(retries = 0, timeout = 2000)
public class SysCodeValueRpcService implements ISysCodeValueRpcService {
    @Autowired
    ISysCodeValueService sysCodeValueService;
    @Autowired
    ISysCodeTypeService sysCodeTypeService;
    @Autowired
    Mapper dozerMapper;
    @Autowired
    ISysCodeCache sysCodeCache;

    @Override
    public void pushSysCode(String applicationName, Map<String, SysCodeDefinitionDto> definition) {
        sysCodeCache.pushSysCode(applicationName, definition);
    }

    @Override
    public SysCodeValueDto add(SysCodeValueDto sysCodeValueDto) {
        return sysCodeValueService.add(sysCodeValueDto);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        sysCodeValueService.deleteByIds(ids);
    }

    @Override
    public PagedResultDto<SysCodeValueDto> list(SysCodeValuePagedReqDto inputDto) {
        return sysCodeValueService.list(inputDto);
    }

    @Override
    public List<SysCodeValueDto> list(String typeCode) {
        return sysCodeValueService.list(typeCode);
    }

    @Override
    public SysCodeValueDto findFirstByTypeCodeAndCode(String typeCode, String code) {
        return sysCodeValueService.findFirstByTypeCodeAndCode(typeCode, code);
    }

    @Override
    public List<SysCodeSyncDto> getSysCodeForSync() {
        List<SysCodeSyncDto> result = new ArrayList<>();
        List<SysCodeTypeDto> list = sysCodeTypeService.list();
        List<SysCodeValueDto> values = sysCodeValueService.list(list.stream().map(SysCodeTypeDto::getTypeCode).collect(Collectors.toList()));
        list.forEach(p -> {
            SysCodeSyncDto dto = new SysCodeSyncDto();
            dto.setTypeCode(p.getTypeCode());
            dto.setTypeName(p.getTypeName());
            List<SysCodeValueDto> collect = values.stream().filter(o -> o.getTypeCode().equals(p.getTypeCode())).collect(Collectors.toList());
            dto.setDetails(collect);
            result.add(dto);
        });
        return result;
    }
}
