package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListSearchDto;
import com.dusk.common.module.auth.dto.sysmenu.SysMenuInputDto;
import com.dusk.module.auth.authorization.SysMenuAuthProvider;
import com.dusk.module.auth.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sysMenu")
@Api(tags = "SysMenu", description = "系统菜单管理")
@authorize(SysMenuAuthProvider.PAGES_SYS_MENU)
public class SysMenuController extends CruxBaseController {
    @Autowired
    ISysMenuService sysMenuService;

    @PostMapping("/save")
    @ApiOperation("保存菜单数据")
    @authorize(SysMenuAuthProvider.PAGES_SYS_MENU_MANAGER)
    public List<SysMenuInputDto> save(@RequestBody List<SysMenuInputDto> input) {
        sysMenuService.save(input);
        return input;

    }
    @GetMapping("/list")
    @ApiOperation("查询菜单")
    public PagedResultDto<GetSysMenuListDto> list(GetSysMenuListSearchDto input){
        return sysMenuService.list(input);
    }

    @DeleteMapping("delete")
    @ApiOperation(("根据id删除(包括子节点)"))
    @authorize(SysMenuAuthProvider.PAGES_SYS_MENU_MANAGER)
    public void delete(Long id){
        sysMenuService.delete(id);
    }

    @DeleteMapping("deleteCurrent")
    @ApiOperation(("根据id删除(不包括子节点)"))
    @authorize(SysMenuAuthProvider.PAGES_SYS_MENU_MANAGER)
    public void deleteCurrent(Long id){
        sysMenuService.deleteCurrent(id);
    }

    @DeleteMapping("/deleteAll")
    @ApiOperation("删除全部")
    @authorize(SysMenuAuthProvider.PAGES_SYS_MENU_MANAGER)
    public void deleteAll(){
        sysMenuService.deleteAllInBatch();
    }
}
