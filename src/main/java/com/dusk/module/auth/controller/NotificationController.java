package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.notification.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
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
@Tag(name = "Notification", description = "用户消息通知")
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
    @Operation(summary = "获取用户消息列表")
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
    @Operation(summary = "获取用户消息详情")
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
    @Operation(summary = "获取用户消息数量")
    @GetMapping("getCount")
    public Long getCount(GetNotificationListCountInput input) {
        return service.getCount(input);
    }

    /**
     * 设置用户消息的状态为已读
     *
     * @param input
     */
    @Operation(summary = "设置用户消息的状态为已读")
    @PutMapping("setNotificationAsRead")
    public void setNotificationAsRead(@RequestBody SetNotificationAsReadInput input) {
        service.setNotificationAsRead(input);
    }

    /**
     * 删除消息通知
     *
     * @param id
     */
    @Operation(summary = "删除消息通知")
    @DeleteMapping("deleteNotification")
    @Parameters(
            @Parameter(name = "id", description = "消息Id", required = true)
    )
    public void deleteNotification(@RequestParam Long id) {
        service.deleteNotification(id);
    }

    /**
     * 批量删除消息通知
     *
     * @param input
     */
    @Operation(summary = "批量删除消息通知")
    @DeleteMapping("batchDeleteNotification")
    public void batchDeleteNotification(@Valid @RequestBody BatchDeleteNotificationInput input) {
        service.batchDeleteNotification(input);
    }

}
