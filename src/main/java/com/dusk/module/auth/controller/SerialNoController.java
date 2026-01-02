package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.dto.SelectListOutputDto;
import com.dusk.common.core.utils.EnumUtils;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.module.auth.authorization.SerialNoAuthProvider;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoDto;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.mapper.SerialNoMapper;
import com.dusk.module.auth.service.ISerialNoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-11-24 14:30
 */
@RestController
@RequestMapping("/serialNo")
@Tag(name = "SerialNo", description = "序列号")
public class SerialNoController extends CruxBaseController {
    private final SerialNoMapper mapper = SerialNoMapper.INSTANCE;
    @Resource
    private ISerialNoService serialNoService;

    @GetMapping("/getPageData")
    @Operation(summary = "分页查询序列号（需要权限）")
    @Authorize(SerialNoAuthProvider.PAGES_SERIAL_NO)
    public PagedResultDto<SerialNoDto> getPageData(GetSerialNoInput input) {
        Page<SerialNo> pages = serialNoService.getSerialNos(input);
        return MapperUtil.mapToPagedResultDto(pages, mapper::toDto);
    }

    @PostMapping("/update")
    @Operation(summary = "更新序列号配置")
    @Authorize(SerialNoAuthProvider.PAGES_SERIAL_NO_EDIT)
    public void update(@RequestBody @Valid SerialNoEditInput input) {
        serialNoService.update(input);
    }

    @GetMapping("/testNo")
    @Operation(summary = "测试票号配置，获取当前配置的序列号结果")
    public String testNo(String noFormat, int serialLength) {
        return serialNoService.getCurrentNo(LocalDateTime.now(), noFormat, 1, serialLength);
    }


    @GetMapping("/testNextNo/{id}")
    @Operation(summary = "测试获取下一个票号（不占用序列号）")
    public String testNextNo(@PathVariable Long id) {
        SerialNo data = serialNoService.getOneById(id);
        return serialNoService.getCurrentNo(LocalDateTime.now(), data.getDateFormat(), data.getCurrentNo() + 1, data.getNoLength());
    }

    @GetMapping("/getEnum")
    @Operation(summary = "获取重置规则下拉数据源")
    public List<SelectListOutputDto> getEnum() {
        return EnumUtils.ConvertToList(EnumResetType.class);
    }
}
