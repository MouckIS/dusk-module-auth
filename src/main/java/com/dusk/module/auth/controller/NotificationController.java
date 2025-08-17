package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.notification.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.module.auth.dto.notification.*;
import com.dusk.module.auth.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户消息的controller
 *
 * @author yuliyang
 * @date 2020-12-24 15:17:08
 */
@RestController
@RequestMapping("notification")
@Api(tags = "Notification", description = "用户消息通知")
public class NotificationController extends CruxBaseController {

    private final INotificationService service;

    @Autowired
    public NotificationController(INotificationService service) {
        this.service = service;
    }


    /**
     * 获取用户消息列表
     *
     * @param input
     * @return
     */
    @ApiOperation("获取用户消息列表")
    @GetMapping("getNotificationList")
    public PagedResultDto<NotificationListOutput> getNotificationList(GetNotificationListInput input) {
        return service.getNotificationList(input);
    }

    /**
     * 获取用户消息详情
     *
     * @param input
     * @return
     */
    @ApiOperation("获取用户消息详情")
    @GetMapping("getNotification")
    public NotificationOutput getNotification(EntityDto input) {
        return service.getNotification(input);
    }

    /**
     * 获取用户消息数量
     *
     * @param input
     * @return
     */
    @ApiOperation("获取用户消息数量")
    @GetMapping("getCount")
    public Long getCount(GetNotificationListCountInput input) {
        return service.getCount(input);
    }

    /**
     * 设置用户消息的状态为已读
     *
     * @param input
     */
    @ApiOperation("设置用户消息的状态为已读")
    @PutMapping("setNotificationAsRead")
    public void setNotificationAsRead(@RequestBody SetNotificationAsReadInput input) {
        service.setNotificationAsRead(input);
    }

    /**
     * 删除消息通知
     *
     * @param id
     */
    @ApiOperation("删除消息通知")
    @DeleteMapping("deleteNotification")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", value = "消息Id", required = true)
    )
    public void deleteNotification(@RequestParam Long id) {
        service.deleteNotification(id);
    }

    /**
     * 批量删除消息通知
     *
     * @param input
     */
    @ApiOperation("批量删除消息通知")
    @DeleteMapping("batchDeleteNotification")
    public void batchDeleteNotification(@Valid @RequestBody BatchDeleteNotificationInput input) {
        service.batchDeleteNotification(input);
    }

}
