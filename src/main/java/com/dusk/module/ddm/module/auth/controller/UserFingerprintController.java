package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.module.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.module.auth.authorization.UserFingerprintAuthProvider;
import com.dusk.module.auth.dto.fingerprint.IdentifyInputDto;
import com.dusk.module.auth.dto.fingerprint.RegisterFingerprintInputDto;
import com.dusk.module.auth.dto.fingerprint.SaveFingerprintInputDto;
import com.dusk.module.auth.service.IUserFingerprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author panyanlin1
 * @date 2021-05-11 17:10:02
 */
@RestController
@RequestMapping("userFingerprint")
@Api(tags = "UserFingerprint", description = "用户指纹管理")
@authorize(UserFingerprintAuthProvider.PAGES_FINGERPRINT)
public class UserFingerprintController extends CruxBaseController {
    @Autowired
    IUserFingerprintService userFingerprintService;

    @ApiOperation(value = "发送注册指纹指令给指纹采集器")
    @PostMapping(value = "/registerFingerprint")
    public void registerFingerprint(RegisterFingerprintInputDto inputDto) {
        userFingerprintService.registerFingerprint(inputDto);
    }

    @ApiOperation(value = "保存/更新指纹信息")
    @PostMapping(value = "/saveFingerprint")
    @authorize(UserFingerprintAuthProvider.PAGES_FINGERPRINT_SAVE)
    public Long saveFingerprint(@Valid SaveFingerprintInputDto inputDto) {
        return userFingerprintService.saveFingerprint(inputDto);
    }

    @ApiOperation(value = "删除指纹记录")
    @PostMapping(value = "/delete")
    @authorize(UserFingerprintAuthProvider.PAGES_FINGERPRINT_DELETE)
    public void delete(@RequestBody List<Long> ids) {
        userFingerprintService.deleteByIds(ids);
    }

    @ApiOperation(value = "查询指纹记录")
    @GetMapping(value = "/getAll")
    public List<UserFingerprintDto> getAll(GetAllInputDto inputDto) {
        return userFingerprintService.getAll(inputDto);
    }

    @ApiOperation(value = "发送验证用户指纹指令给指纹仪")
    @PostMapping(value = "/identify")
    public void identify(IdentifyInputDto inputDto) {
        userFingerprintService.identify(inputDto);
    }

    @ApiOperation(value = "保存/更新个人指纹信息")
    @PostMapping(value = "/saveFingerprintPrivate")
    @authorize(UserFingerprintAuthProvider.PAGES_FINGERPRINT_SAVE_PRIVATE)
    public Long saveFingerprintPrivate(SaveFingerprintInputDto inputDto) {
        return userFingerprintService.saveFingerprintPrivate(inputDto);
    }

    @ApiOperation(value = "删除个人指纹记录")
    @PostMapping(value = "/deletePrivate")
    @authorize(UserFingerprintAuthProvider.PAGES_FINGERPRINT_DELETE_PRIVATE)
    public void deletePrivate(@RequestBody List<Long> ids) {
        userFingerprintService.deleteByIdsPrivate(ids);
    }
}
