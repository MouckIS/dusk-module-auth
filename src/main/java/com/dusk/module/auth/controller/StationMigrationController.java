package com.dusk.module.auth.controller;

import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.service.IStationMigrationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kefuming
 * @date 2023/2/14 15:32
 */
@RestController
@RequestMapping("stationMigration")
@Api(tags="StationMigration",description="厂站迁移")
public class StationMigrationController extends CruxBaseController {
    @Resource
    private IStationMigrationService migrationService;
    @Resource
    private SecurityUtils securityUtils;

    @PostMapping("migration")
    @ApiOperation("迁移厂站")
    public void migration() {
        if(securityUtils.getCurrentUser() == null || securityUtils.getCurrentUser().getIsAdmin()){
            throw new BusinessException("无权限执行此操作");
        }
        migrationService.migration();
    }
}
