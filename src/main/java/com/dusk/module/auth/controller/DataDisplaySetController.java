package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.authorization.DataDisplaySetAuthProvider;
import com.dusk.module.auth.dto.datadisplay.DataDisplayItemDto;
import com.dusk.module.auth.dto.datadisplay.GetDisplaySetInputDto;
import com.dusk.module.auth.dto.datadisplay.UpdateDataDisplaySetDto;
import com.dusk.module.auth.service.IDataDisplaySetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@RestController("/dataDisplay")
@Api(tags = "DataDisplay", description = "数据展示设置")
@Authorize(DataDisplaySetAuthProvider.PAGES_DATA_DISPLAY_SET)
public class DataDisplaySetController extends CruxBaseController {

    @Autowired
    IDataDisplaySetService dataDisplaySetService;

    @PostMapping("/updateDisplaySetItem")
    @ApiOperation("数据展示设置的更新")
    @Authorize(DataDisplaySetAuthProvider.PAGES_DATA_DISPLAY_SET_SAVE)
    public void updateDisplaySetItem(@RequestBody List<UpdateDataDisplaySetDto> input){
        dataDisplaySetService.updateDisplaySetItem(input);
    }

    @GetMapping("/getDisplayList")
    @ApiOperation("获取数据展示设置的列表信息")
    public PagedResultDto<DataDisplayItemDto> getDisplayList(GetDisplaySetInputDto input) {
        return dataDisplaySetService.getList(input);
    }
}
