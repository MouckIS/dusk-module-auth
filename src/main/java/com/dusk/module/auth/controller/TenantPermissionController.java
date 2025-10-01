package com.dusk.module.auth.controller;

import com.dusk.commom.rpc.auth.dto.role.RolePermissionDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.authorization.EditionAuthProvider;
import com.dusk.module.auth.dto.permission.EditionPermissionInputDto;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-11 10:41
 */
@RestController
@RequestMapping("/permission")
@Api(description = "租户权限", tags = "TenantPermission")
public class TenantPermissionController extends CruxBaseController {
    @Autowired
    ITenantPermissionService tenantPermissionService;

    @ApiOperation("获取指定版本下的权限清单")
    @GetMapping("getEditionPermissionsById")
    public List<RolePermissionDto> getEditionPermissionsById(EntityDto input) {
        return tenantPermissionService.getEditionPermissions(input.getId());
    }

    @ApiOperation("获取指定租户下的权限清单")
    @GetMapping("getTenantPermissions")
    public List<RolePermissionDto> getTenantPermissions(EntityDto input) {
        return tenantPermissionService.getTenantPermissions(input.getId());
    }

    @ApiOperation("设置版本的权限清单")
    @PostMapping("setEditionPermissions")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_PERMISSION)
    public void setEditionPermissions(@Valid @RequestBody EditionPermissionInputDto input) {
        tenantPermissionService.setEditionPermissions(input);
    }

}
