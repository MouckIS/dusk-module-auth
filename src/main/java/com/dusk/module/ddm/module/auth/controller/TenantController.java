package com.dusk.module.ddm.module.auth.controller;

import com.dusk.module.auth.dto.tenant.*;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.*;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.module.auth.authorization.TenantAuthProvider;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.tenant.*;
import com.dusk.module.auth.dto.user.GetUsersInput;
import com.dusk.module.auth.dto.user.UserListDto;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.service.ITenantService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-04-30 9:39
 */
@RestController
@RequestMapping("tenant")
@Api(description = "租户", tags = "Tenant")
@authorize(TenantAuthProvider.PAGES_TENANTS)
public class TenantController extends CruxBaseController {
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    IUserService userService;
    @Autowired
    private TokenAuthManager tokenAuthManager;

    /**
     * 查询租户列表
     *
     * @param input
     * @return
     */
    @ApiOperation(value = "查询租户列表")
    @GetMapping("getTenants")
    public PagedResultDto<TenantListDto> getTenants(GetTenantsInput input) {
        Page<Tenant> page = tenantService.getTenants(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, TenantListDto.class);
    }

    /**
     * 创建租户
     *
     * @param input
     * @return
     */
    @PostMapping("createTenant")
    @ApiOperation(value = "创建租户")
    @authorize(TenantAuthProvider.PAGES_TENANTS_CREATE)
    public void createTenant(@Valid @RequestBody CreateTenantInput input) {
        tenantService.createTenantWithDefaultSettings(input);
    }

    /**
     * 根据租户id获取租户信息
     *
     * @param entityDto
     * @return
     */
    @GetMapping("getTenantForEdit")
    @ApiOperation(value = "获取租户信息进行编辑")
    @authorize(TenantAuthProvider.PAGES_TENANTS_EDIT)
    public TenantEditDto getTenantForEdit(EntityDto entityDto) {
        Tenant tenant = tenantService.findById(entityDto.getId()).orElseThrow(() -> new BusinessException("未找到相应的租户信息！"));
        return dozerMapper.map(tenant, TenantEditDto.class);
    }

    /**
     * 更新租户
     *
     * @param editDto
     * @return
     */
    @PutMapping("updateTenant")
    @ApiOperation(value = "更新租户")
    @authorize(TenantAuthProvider.PAGES_TENANTS_EDIT)
    public void updateTenant(@Valid @RequestBody TenantEditDto editDto) {
        tenantService.updateTenant(editDto);
    }

    /**
     * 根据id删除租户
     *
     * @param entityDto
     * @return
     */
    @DeleteMapping("deleteTenant")
    @ApiOperation(value = "删除租户")
    @authorize(TenantAuthProvider.PAGES_TENANTS_DELETE)
    public void deleteTenant(@RequestBody EntityDto entityDto) {
        tenantService.deleteTenant(entityDto.getId());
    }

    @GetMapping("isTenantAvailable")
    @ApiOperation(value = "租户是否可用")
    @AllowAnonymous
    public IsTenantAvailableOutput isTenantAvailable(IsTenantAvailableInput input) {
        return tenantService.isTenantAvailable(input);
    }

    @GetMapping("{tenantId}/user/list")
    @ApiOperation("获取指定租户用户列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "filter", value = "模糊查找[姓名、账号、电子邮箱、手机号、角色名]")})
    public PagedResultDto<UserListDto> getTenantUserList(@PathVariable Long tenantId, String filter, PagedAndSortedInputDto pageReq) {
        GetUsersInput input = dozerMapper.map(pageReq, GetUsersInput.class);
        input.setFilter(filter);
        TenantContextHolder.setTenantId(tenantId);
        try {
            Page<User> page = userService.getUsers(input);
            List<UserListDto> list = DozerUtils.mapList(dozerMapper, page.getContent(), UserListDto.class, (s, t) -> {
                if (s.getLockoutEndDateUtc() != null && s.getLockoutEndDateUtc().compareTo(LocalDateTime.now()) > 0) {
                    t.setLock(true);
                }
            });
            return new PagedResultDto<>(page.getTotalElements(), list);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @PostMapping("host/impersonation")
    @ApiOperation("宿主以租户用户登录")
    @authorize(TenantAuthProvider.PAGES_TENANTS_IMPERSONATION)
    public String hostTenantImpersonation(@ApiParam(value = "租户ID", required = true) @RequestParam @NotNull(message = "租户ID不能为空") Long tenantId,
                                          @ApiParam(value = "租户用户ID", required = true) @RequestParam @NotNull(message = "租户用户ID不能为空") Long userId) {
        Long hostUserId = LoginUserIdContextHolder.getUserId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            User user = userService.getUserById(userId);
            userService.checkUserValid(user);
            UserContext userContext = LoginUtils.getUserContextByUser(user);
            userContext.setHostUserId(hostUserId);
            return tokenAuthManager.generateToken(userContext);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @PostMapping("host/impersonation-other")
    @ApiOperation("宿主以租户用户登录时-切换至其他用户登录")
    public String hostTenantImpersonationOther(@ApiParam(value = "租户用户ID", required = true) @RequestParam @NotNull(message = "租户用户ID不能为空") Long userId) {
        UserContext context = getCurrentUser();
        if (context.isHostImpersonation()) {
            User user = userService.getUserById(userId);
            userService.checkUserValid(user);
            UserContext userContext = LoginUtils.getUserContextByUser(user);
            userContext.setHostUserId(context.getHostUserId());
            return tokenAuthManager.generateToken(userContext);
        } else {
            throw new BusinessException("当前不是宿主登录租户");
        }
    }


    @GetMapping("host/impersonation-back")
    @ApiOperation("宿主以租户用户登录后返回到宿主")
    public String backToHost() {
        UserContext context = getCurrentUser();
        if (context.isHostImpersonation()) {
            Long hostUserId = context.getHostUserId();
            TenantContextHolder.clear();    // 清除当前租户上下文
            User user = userService.getUserById(hostUserId);
            userService.checkUserValid(user);
            UserContext userContext = LoginUtils.getUserContextByUser(user);
            return tokenAuthManager.generateToken(userContext);
        } else {
            throw new BusinessException("当前不是宿主登录租户");
        }
    }

    @PostMapping("host/change-password")
    @ApiOperation("宿主修改租户管理员密码")
    @authorize(TenantAuthProvider.PAGES_TENANTS_CHANGEPASSWORD)
    public void changeTenantAdminPasswordByHost(@ApiParam(value = "租户ID", required = true) @RequestParam @NotNull(message = "租户ID不能为空") Long tenantId,
                                                @ApiParam(value = "租户管理员重置密码", required = true) @RequestParam @NotNull(message = "密码不能为空") String newPwd) {
        userService.changeAdminPasswordByHost(tenantId, newPwd);
    }
}
