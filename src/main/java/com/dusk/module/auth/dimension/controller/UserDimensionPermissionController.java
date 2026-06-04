package com.dusk.module.auth.dimension.controller;

import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dimension.dto.BatchUserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionGrantDto;
import com.dusk.module.auth.dimension.service.IUserDimensionPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户维度权限管理控制器
 *
 * @author dusk
 */
@RestController
@RequestMapping("/api/dimension-permissions")
@Tag(name = "维度权限管理", description = "用户维度权限的授权、撤销、查询等操作")
public class UserDimensionPermissionController extends CruxBaseController {

    @Autowired
    private IUserDimensionPermissionService permissionService;

    @PostMapping("/grant")
    @Operation(summary = "授权用户维度值")
    public void grant(@Valid @RequestBody UserDimensionPermissionGrantDto dto) {
        permissionService.grant(dto);
    }

    @DeleteMapping("/revoke")
    @Operation(summary = "撤销用户维度值权限")
    public void revoke(@RequestParam Long userId, @RequestParam Long dimensionValueId) {
        permissionService.revoke(userId, dimensionValueId);
    }

    @DeleteMapping("/revoke/dimension")
    @Operation(summary = "撤销用户在某维度下的所有权限")
    public void revokeByDimension(@RequestParam Long userId, @RequestParam Long dimensionId) {
        permissionService.revokeByDimension(userId, dimensionId);
    }

    @PostMapping("/batch/grant")
    @Operation(summary = "批量授权用户维度值")
    public void batchGrant(@Valid @RequestBody BatchUserDimensionPermissionDto dto) {
        permissionService.batchGrant(dto);
    }

    @PostMapping("/batch/revoke")
    @Operation(summary = "批量撤销用户维度值权限")
    public void batchRevoke(@Valid @RequestBody BatchUserDimensionPermissionDto dto) {
        permissionService.batchRevoke(dto);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户的所有维度权限")
    public List<UserDimensionPermissionDto> getByUserId(@PathVariable Long userId) {
        return permissionService.getByUserId(userId);
    }

    @GetMapping("/user/{userId}/dimension/{dimensionId}")
    @Operation(summary = "查询用户在某维度下的权限")
    public List<UserDimensionPermissionDto> getByUserIdAndDimensionId(
            @PathVariable Long userId, @PathVariable Long dimensionId) {
        return permissionService.getByUserIdAndDimensionId(userId, dimensionId);
    }

    @GetMapping("/check")
    @Operation(summary = "检查用户是否有某维度值的权限")
    public boolean hasPermission(@RequestParam Long userId, @RequestParam Long dimensionValueId) {
        return permissionService.hasPermission(userId, dimensionValueId);
    }

    @GetMapping("/accessible-value-ids")
    @Operation(summary = "获取用户有权限的维度值ID列表")
    public List<Long> getAccessibleValueIds(@RequestParam Long userId, @RequestParam String dimensionCode) {
        return permissionService.getAccessibleValueIds(userId, dimensionCode);
    }
}
