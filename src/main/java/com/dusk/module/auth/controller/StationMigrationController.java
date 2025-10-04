package com.dusk.module.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.SecurityUtils;
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
@Tag(name = "StationMigration", description = "厂站迁移")
public class StationMigrationController extends CruxBaseController {
    @Autowired
    private IStationMigrationService migrationService;
    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("migration")
    @Operation(summary = "迁移厂站")
    public void migration() {
        if(securityUtils.getCurrentUser() == null || securityUtils.getCurrentUser().getIsAdmin()){
            throw new BusinessException("无权限执行此操作");
        }
        migrationService.migration();
    }
}
