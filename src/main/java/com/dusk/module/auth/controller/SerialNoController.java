package com.dusk.module.auth.controller;

import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.dto.SelectListOutputDto;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.framework.utils.EnumUtils;
import com.dusk.common.module.auth.enums.EnumResetType;
import com.dusk.module.auth.authorization.SerialNoAuthProvider;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoDto;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.service.ISerialNoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-11-24 14:30
 */
@RestController
@RequestMapping("/serialNo")
@Api(description = "序列号", tags = "SerialNo")
public class SerialNoController extends CruxBaseController {
    @Autowired
    ISerialNoService serialNoService;
    @Autowired
    Mapper dozerMapper;

    @GetMapping("/getPageData")
    @ApiOperation(value = "分页查询序列号（需要权限）")
    @Authorize(SerialNoAuthProvider.PAGES_SERIAL_NO)
    public PagedResultDto<SerialNoDto> getPageData(GetSerialNoInput input) {
        Page<SerialNo> pages = serialNoService.getSerialNos(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, pages, SerialNoDto.class);
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新序列号配置")
    @Authorize(SerialNoAuthProvider.PAGES_SERIAL_NO_EDIT)
    public void update(@RequestBody @Valid SerialNoEditInput input) {
        serialNoService.update(input);
    }

    @GetMapping("/testNo")
    @ApiOperation(value = "测试票号配置，获取当前配置的序列号结果")
    public String testNo(String noFormat, int serialLength) {
        return serialNoService.getCurrentNo(LocalDateTime.now(), noFormat, 1, serialLength);
    }


    @GetMapping("/testNextNo/{id}")
    @ApiOperation(value = "测试获取下一个票号（不占用序列号）")
    public String testNextNo(@PathVariable Long id) {
        SerialNo data = serialNoService.getOneById(id);
        return serialNoService.getCurrentNo(LocalDateTime.now(), data.getDateFormat(), data.getCurrentNo() + 1, data.getNoLength());
    }

    @GetMapping("/getEnum")
    @ApiOperation(value = "获取重置规则下拉数据源")
    public List<SelectListOutputDto> getEnum() {
        return EnumUtils.ConvertToList(EnumResetType.class);
    }
}
