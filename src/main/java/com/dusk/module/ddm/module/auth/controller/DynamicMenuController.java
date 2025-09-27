package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.module.auth.dto.dynamicmenu.DynamicLinkRoles;
import com.dusk.common.module.auth.dto.dynamicmenu.PublishDynamicMenuInput;
import com.dusk.common.module.auth.dto.dynamicmenu.UnPublishDynamicMenuInput;
import com.dusk.module.auth.authorization.DynamicMenuAuthProvider;
import com.dusk.module.auth.dto.dynamicmenu.DynamicMenuRolesDto;
import com.dusk.module.auth.dto.dynamicmenu.GetDynamicMenuInput;
import com.dusk.module.auth.service.IDynamicMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author kefuming
 * @date 2022-08-29 16:17
 */
@RestController
@RequestMapping("/dynamic-menu")
@Api(description = "动态菜单", tags = "DynamicMenu")
@authorize(DynamicMenuAuthProvider.PAGES_DYNAMIC_MENU)
@Validated
public class DynamicMenuController extends CruxBaseController {

    @Autowired
    IDynamicMenuService service;

    @PostMapping("list")
    @ApiOperation("获取列表")
    public PagedResultDto<DynamicMenuRolesDto> getList(@Valid @RequestBody GetDynamicMenuInput input) {
        return service.getList(input);
    }

    //发布菜单
    @PostMapping("publish")
    @ApiOperation("发布")
    @authorize(DynamicMenuAuthProvider.PAGES_DYNAMIC_MENU_ADD)
    public void publish(@Valid @RequestBody PublishDynamicMenuInput input) {
        service.publish(input);
    }

    //发布菜单
    @PostMapping("editPublish")
    @ApiOperation("更改发布")
    @authorize(DynamicMenuAuthProvider.PAGES_DYNAMIC_MENU_ADD)
    public void editPublish(@Valid @RequestBody PublishDynamicMenuInput input) {
        service.editPublish(input);
    }

    //取消发布
    @PostMapping("unpublish")
    @ApiOperation("取消发布")
    @authorize(DynamicMenuAuthProvider.PAGES_DYNAMIC_MENU_Delete)
    public void unpublish(@Valid @RequestBody UnPublishDynamicMenuInput input) {
        service.unpublish(input);
    }


    @GetMapping("publish-roles")
    @ApiOperation("获取发布关联的角色清单")
    public List<DynamicLinkRoles> getPublishLinkRoles(@Valid @NotEmpty(message = "业务标识不能为空") @RequestParam String businessKey) {
        return service.getPublishRoles(businessKey);
    }
}
