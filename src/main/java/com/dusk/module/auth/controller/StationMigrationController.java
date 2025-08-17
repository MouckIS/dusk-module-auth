package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.module.auth.service.IStationMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private IStationMigrationService migrationService;
    @Autowired
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
