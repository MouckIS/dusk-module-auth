package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValueDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeValuePagedReqDto;
import com.dusk.module.auth.authorization.SysCodeAuthProvider;
import com.dusk.module.auth.service.ISysCodeValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统常量管理
 */
@RestController
@RequestMapping("/sysCodeValue")
@Api(tags = "sysCodeValue", description = "系统常量管理")
public class SysCodeValueController extends CruxBaseController {
    @Autowired
    ISysCodeValueService sysCodeValueService;

    @ApiOperation("增加/修改配置常量")
    @PostMapping("/add")
    @authorize({SysCodeAuthProvider.PAGES_SYS_CODE_VALUE_SAVE, "Page.Energy.Charge.Setting.Save"})
    public SysCodeValueDto add(@Valid @RequestBody SysCodeValueDto sysCodeValueDto) {
        return sysCodeValueService.add(sysCodeValueDto);
    }

    @ApiOperation("删除配置常量")
    @DeleteMapping("/delete")
    @authorize({SysCodeAuthProvider.PAGES_SYS_CODE_VALUE_DELETE, "Page.Energy.Charge.Setting.Save"})
    public void delete(@RequestBody List<Long> ids) {
        sysCodeValueService.deleteByIds(ids);
    }

    @ApiOperation("查询配置常量分页")
    @PostMapping("/list")
    public PagedResultDto<SysCodeValueDto> list(@RequestBody @Valid SysCodeValuePagedReqDto inputDto) {
        return sysCodeValueService.list(inputDto);
    }

    @ApiOperation("批量增加/修改配置常量")
    @PostMapping("/saveAll")
    @authorize(SysCodeAuthProvider.PAGES_SYS_CODE_VALUE_SAVE)
    public void saveAll(@Valid @RequestBody List<SysCodeValueDto> inputList) {
        sysCodeValueService.saveAllSysCode(inputList);
    }

}
