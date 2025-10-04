package com.dusk.module.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.authorization.PageQuickEntryAuthProvider;
import com.dusk.module.auth.dto.quickentry.GetQuickSetListDto;
import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.service.IPageQuickEntryService;
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
 * @create 2022/2/9
 * @since 1.0.0
 */
@RestController("/quickEntry")
@Tag(name = "PageQuickEntry", description = "页面快捷入口")
@Authorize(PageQuickEntryAuthProvider.PAGES_QUICK_ENTRY)
public class PageQuickEntryController extends CruxBaseController {

    @Autowired
    IPageQuickEntryService quickEntryService;

    @PostMapping("/updateQuickSet")
    @Operation(summary = "更新设置项")
    @Authorize(PageQuickEntryAuthProvider.PAGES_QUICK_ENTRY_SAVE)
    public void updateQuickSet(@RequestBody List<UpdatePageQuickSetDto> input) {
        quickEntryService.updateQuickSet(input);
    }

    @GetMapping("/getQuickSetList")
    @Operation(summary = "获取设置项列表")
    public PagedResultDto<QuickEntryListDto> getQuickSetList(GetQuickSetListDto input) {
        return quickEntryService.getQuickSetList(input);
    }
}
