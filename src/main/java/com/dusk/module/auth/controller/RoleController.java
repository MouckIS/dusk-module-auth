package com.dusk.module.auth.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.dusk.common.rpc.auth.dto.BindRoleToUserInput;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.DozerUtils;
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
import com.dusk.module.auth.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/role")
@Api(description = "角色", tags = "Role")
@Authorize(RoleAuthProvider.PAGES_ROLES)
public class RoleController extends CruxBaseController {

    @Autowired
    private IRoleService roleService;

    @Autowired
    private Mapper dozerMapper;

    @GetMapping("/getAllRoles")
    @ApiOperation(value = "获取所有角色")
    public List<RoleDto> getAllRoles() {
        List<Role> list = roleService.getRoles();
        return DozerUtils.mapList(dozerMapper,list,RoleDto.class);
    }

    @GetMapping("/getRoles")
    @ApiOperation(value = "查询角色列表")
    @Authorize(RoleAuthProvider.PAGES_ROLES)
    public PagedResultDto<RoleDto> getRoles(GetRolesInput input) {
        Page<Role> pages = roleService.getRoles(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper,pages,RoleDto.class);
    }

    @ApiOperation(value = "查看某个角色详情")
    @GetMapping("/getRoleDetails")
    @Authorize(RoleAuthProvider.PAGES_ROLES)
    public RoleDto getRoleDetails(EntityDto dto) {
        return roleService.getRoleDetails(dto);
    }

    @ApiOperation(value = "创建或修改角色")
    @PostMapping("/createOrUpdateRole")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    public Long createOrUpdateRole(@Valid @RequestBody RoleCreateOrEditDto input) {
        Role entity = roleService.createOrUpdate(input);
        return entity.getId();
    }

    @ApiOperation(value = "修改角色下的权限")
    @PostMapping("/updateRolePermission")
    @Authorize(RoleAuthProvider.PAGES_ROLES_MANAGEPERMISSIONS)
    public Long UpdateRolePermission(@Valid @RequestBody UpdateRolePermissionDto input) {
        Role entity = roleService.updatePermission(input);
        return entity.getId();
    }

    @ApiOperation(value = "删除角色")
    @DeleteMapping("/deleteRole")
    @Authorize(RoleAuthProvider.PAGES_ROLES_DELETE)
    public Long deleteRole(EntityDto dto) throws BusinessException {
        roleService.deleteRole(dto);
        return dto.getId();
    }

    @ApiOperation(value = "导出角色")
    @GetMapping("export/{id}")
    public void exportRole(@ApiParam(value = "角色ID")  @PathVariable long id, HttpServletResponse response)throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("角色信息导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        roleService.exportRole(id,response.getOutputStream());
    }

    @ApiOperation(value = "导入角色")
    @PostMapping("import")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    public void importRole(@ApiParam(value = "Excel文件",required = true) MultipartFile file){
        InputStream stream = null;
        RoleDto roleDto = null;
        try{
            stream = file.getInputStream();
            RolePermissionImportListener listener = new RolePermissionImportListener();
            EasyExcel.read(stream, listener).autoCloseStream(true).extraRead(CellExtraTypeEnum.MERGE).sheet().headRowNumber(0).autoTrim(true).doReadSync();
            roleDto = listener.getRoleDto();
        }catch (Exception e){
            if(e instanceof BusinessException){
                throw (BusinessException)e;
            }else{
                throw new BusinessException("导入失败:"+e.getMessage(),e);
            }
        }finally {
            IOUtils.closeQuietly(stream);
        }
        String msg = validateRoleDto(roleDto);
        if(StringUtils.isNotBlank(msg)){
            throw new BusinessException(msg);
        }
        roleService.importRole(roleDto);
    }

    @ApiOperation(value = "批量导入角色")
    @PostMapping(value = "import/batch",headers = "Content-Type=multipart/form-data")
    @Authorize(RoleAuthProvider.PAGES_ROLES_CREATEOREDIT)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "form", name = "files", value = "Excel文件", allowMultiple = true, dataType = "__file")
    })
    public void batchImportRole(@RequestParam("files") MultipartFile[] files){
        List<RoleDto> roleDtoList = new ArrayList<>();
        InputStream stream = null;
        try{
            for (MultipartFile excel : files) {
                RolePermissionImportListener listener = new RolePermissionImportListener();
                stream = excel.getInputStream();
                EasyExcel.read(stream, listener).autoCloseStream(true).extraRead(CellExtraTypeEnum.MERGE).sheet().headRowNumber(0).autoTrim(true).doReadSync();
                stream.close();
                RoleDto roleDto = listener.getRoleDto();
                String msg = validateRoleDto(roleDto);
                if(StringUtils.isNotBlank(msg)){
                    throw new BusinessException(excel.getOriginalFilename()+" "+msg);
                }
                roleDtoList.add(roleDto);
            }
        }catch (Exception e){
            throw new BusinessException("Excel解析失败", e);
        }finally {
            IOUtils.closeQuietly(stream);
        }
        roleService.batchImportRole(roleDtoList);
    }

    String validateRoleDto(RoleDto roleDto){
        if (StringUtils.isBlank(roleDto.getRoleCode())) {
            return "角色代码不能为空";
        }
        if (StringUtils.isBlank(roleDto.getRoleName())) {
            return "角色名称不能为空";
        }
        return "";
    }

    @ApiOperation(value = "角色批量绑定用户")
    @PostMapping(value = "/bindRoleToUsers")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void bindRoleToUsers(@RequestBody BindRoleToUserInput roleToUserInput) {
        roleService.bindRoleToUsers(roleToUserInput);
    }

    @ApiOperation(value = "角色批量绑定组织中的用户")
    @PostMapping(value = "/bindRoleToOrgans")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void bindRoleToOrgans(@RequestBody @Valid BindRoleToOrgInput orgaUsersInput) {
        roleService.bindRoleToOrgans(orgaUsersInput);
    }

    @ApiOperation(value = "查找有某角色的用户列表")
    @GetMapping(value = "/getUserByRoleId")
    public PagedResultDto<UserListDto> getUsersByRoleId(@Valid GetUserByRoleDto getUserByRoleDto) {
        return DozerUtils.mapToPagedResultDto(dozerMapper, roleService.getUserByRoleId(getUserByRoleDto), UserListDto.class);
    }

    @ApiOperation(value = "取消用户的角色")
    @PostMapping(value = "/unbindRoleForUsers")
    @Authorize(RoleAuthProvider.PAGES_ADMINISTRATION_USERS_BIND_ROLE)
    public void unbindRoleForUsers(@RequestBody @Valid UnbindRoleForUserDto roleForUserDto) {
        roleService.unbindRoleToUser(roleForUserDto);
    }

}
