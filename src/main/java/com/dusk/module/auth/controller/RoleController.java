package com.dusk.module.auth.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.dto.BindRoleToUserInput;
import com.dusk.module.auth.authorization.RoleAuthProvider;
import com.dusk.module.auth.dto.orga.BindRoleToOrgInput;
import com.dusk.module.auth.dto.role.GetRolesInput;
import com.dusk.module.auth.dto.role.RoleCreateOrEditDto;
import com.dusk.module.auth.dto.role.RoleDto;
import com.dusk.module.auth.dto.role.UpdateRolePermissionDto;
import com.dusk.module.auth.dto.user.GetUserByRoleDto;
import com.dusk.module.auth.dto.user.UnbindRoleForUserDto;
import com.dusk.module.auth.dto.user.UserListDto;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.excel.RolePermissionImportListener;
import com.dusk.module.auth.mapper.RoleMapper;
import com.dusk.module.auth.mapper.UserMapper;
import com.dusk.module.auth.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/role")
@Tag(name = "角色", description = "Role")
@Authorize(RoleAuthProvider.PAGES_ROLES)
public class RoleController extends CruxBaseController {

    private final RoleMapper mapper = RoleMapper.INSTANCE;
    private final UserMapper userMapper = UserMapper.INSTANCE;
    @Resource
    private IRoleService roleService;

    @GetMapping("/getAllRoles")
    @Operation(summary = "获取所有角色")
    public List<RoleDto> getAllRoles() {
        List<Role> list = roleService.getRoles();
        return MapperUtil.mapList(list, mapper::toDto);
    }

    @GetMapping("/getRoles")
    @Operation(summary = "查询角色列表")
    @Authorize(RoleAuthProvider.PAGES_ROLES)
    public PagedResultDto<RoleDto> getRoles(GetRolesInput input) {
        Page<Role> pages = roleService.getRoles(input);
        return MapperUtil.mapToPagedResultDto(pages, mapper::toDto);
    }

    @Operation(summary = "查看某个角色详情")
    @GetMapping("/getRoleDetails")
    @Authorize(RoleAuthProvider.PAGES_ROLES)
    public RoleDto getRoleDetails(EntityDto dto) {
        return roleService.getRoleDetails(dto);
    }

    @Operation(summary = "创建或修改角色")
    @PostMapping("/createOrUpdateRole")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    public Long createOrUpdateRole(@Valid @RequestBody RoleCreateOrEditDto input) {
        Role entity = roleService.createOrUpdate(input);
        return entity.getId();
    }

    @Operation(summary = "修改角色下的权限")
    @PostMapping("/updateRolePermission")
    @Authorize(RoleAuthProvider.PAGES_ROLES_MANAGEPERMISSIONS)
    public Long UpdateRolePermission(@Valid @RequestBody UpdateRolePermissionDto input) {
        Role entity = roleService.updatePermission(input);
        return entity.getId();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/deleteRole")
    @Authorize(RoleAuthProvider.PAGES_ROLES_DELETE)
    public Long deleteRole(EntityDto dto) throws BusinessException {
        roleService.deleteRole(dto);
        return dto.getId();
    }

    @Operation(summary = "导出角色")
    @GetMapping("export/{id}")
    public void exportRole(@Parameter(description = "角色ID") @PathVariable long id, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("角色信息导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        roleService.exportRole(id, response.getOutputStream());
    }

    @Operation(summary = "导入角色")
    @PostMapping("import")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    public void importRole(@Parameter(description = "Excel文件", required = true) MultipartFile file) {
        InputStream stream = null;
        RoleDto roleDto = null;
        try {
            stream = file.getInputStream();
            RolePermissionImportListener listener = new RolePermissionImportListener();
            EasyExcel.read(stream, listener).autoCloseStream(true).extraRead(CellExtraTypeEnum.MERGE).sheet().headRowNumber(0).autoTrim(true).doReadSync();
            roleDto = listener.getRoleDto();
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            } else {
                throw new BusinessException("导入失败:" + e.getMessage(), e);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        String msg = validateRoleDto(roleDto);
        if (StringUtils.isNotBlank(msg)) {
            throw new BusinessException(msg);
        }
        roleService.importRole(roleDto);
    }

    @Operation(summary = "批量导入角色")
    @PostMapping(value = "import/batch", headers = "Content-Type=multipart/form-data")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    public void batchImportRole(@Parameter(description = "Excel文件", schema = @Schema(type = "string", format = "binary")) @RequestParam("files") MultipartFile[] files) {
        List<RoleDto> roleDtoList = new ArrayList<>();
        InputStream stream = null;
        try {
            for (MultipartFile excel : files) {
                RolePermissionImportListener listener = new RolePermissionImportListener();
                stream = excel.getInputStream();
                EasyExcel.read(stream, listener).autoCloseStream(true).extraRead(CellExtraTypeEnum.MERGE).sheet().headRowNumber(0).autoTrim(true).doReadSync();
                stream.close();
                RoleDto roleDto = listener.getRoleDto();
                String msg = validateRoleDto(roleDto);
                if (StringUtils.isNotBlank(msg)) {
                    throw new BusinessException(excel.getOriginalFilename() + " " + msg);
                }
                roleDtoList.add(roleDto);
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        roleService.batchImportRole(roleDtoList);
    }

    String validateRoleDto(RoleDto roleDto) {
        if (StringUtils.isBlank(roleDto.getRoleCode())) {
            return "角色代码不能为空";
        }
        if (StringUtils.isBlank(roleDto.getRoleName())) {
            return "角色名称不能为空";
        }
        return "";
    }

    @Operation(summary = "角色批量绑定用户")
    @PostMapping(value = "/bindRoleToUsers")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void bindRoleToUsers(@RequestBody BindRoleToUserInput roleToUserInput) {
        roleService.bindRoleToUsers(roleToUserInput);
    }

    @Operation(summary = "角色批量绑定组织中的用户")
    @PostMapping(value = "/bindRoleToOrgans")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void bindRoleToOrgans(@RequestBody @Valid BindRoleToOrgInput orgaUsersInput) {
        roleService.bindRoleToOrgans(orgaUsersInput);
    }

    @Operation(summary = "查找有某角色的用户列表")
    @GetMapping(value = "/getUserByRoleId")
    public PagedResultDto<UserListDto> getUsersByRoleId(@Valid GetUserByRoleDto getUserByRoleDto) {
        return MapperUtil.mapToPagedResultDto(roleService.getUserByRoleId(getUserByRoleDto), userMapper::toListDto);
    }

    @Operation(summary = "取消用户的角色")
    @PostMapping(value = "/unbindRoleForUsers")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void unbindRoleForUsers(@RequestBody @Valid UnbindRoleForUserDto roleForUserDto) {
        roleService.unbindRoleToUser(roleForUserDto);
    }

}
