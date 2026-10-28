package com.dusk.module.auth.registry.web;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.security.ServiceSecretVerifier;
import com.dusk.module.auth.registry.service.RebindApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 换绑审批管理端点，见《权限优化方案-整理版》4.7 / 8.2。
 *
 * <p>审批只允许由可信管理入口调用：当前使用服务间密钥 + 操作者请求头的内网端点，
 * 后续接入管理后台时应替换为普通管理 API（{@code @Authorize} + 审计）。</p>
 */
@RestController
@RequestMapping("/internal/registry/rebinds")
@IgnoreResponseAdvice
@Tag(name = "InternalRebindApproval", description = "权限换绑审批（内网，服务间密钥鉴权）")
@Slf4j
public class RebindApprovalController {

    @Resource
    private RebindApprovalService rebindApprovalService;
    @Resource
    private ServiceSecretVerifier serviceSecretVerifier;
    @Resource
    private RegistryProperties registryProperties;

    private static String actor(String operator) {
        return operator == null || operator.isBlank() ? "internal-admin" : operator.trim();
    }

    private static String reason(DecisionInput input) {
        return input == null ? null : input.reason();
    }

    @Operation(summary = "批准换绑待办")
    @PostMapping("/{id}:approve")
    @AllowAnonymous
    public ResponseEntity<Object> approve(@PathVariable Long id,
                                          @RequestBody(required = false) DecisionInput input,
                                          @RequestHeader HttpHeaders headers,
                                          @RequestHeader(value = "X-Operator", required = false) String operator) {
        if (!authorized(headers)) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(rebindApprovalService.approve(id, actor(operator), reason(input)));
        } catch (BusinessException e) {
            log.error("换绑审批失败：rebindId={}, reason={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorBody("REBIND_APPROVAL_FAILED", e.getMessage()));
        }
    }

    @Operation(summary = "拒绝换绑待办")
    @PostMapping("/{id}:reject")
    @AllowAnonymous
    public ResponseEntity<Object> reject(@PathVariable Long id,
                                         @RequestBody(required = false) DecisionInput input,
                                         @RequestHeader HttpHeaders headers,
                                         @RequestHeader(value = "X-Operator", required = false) String operator) {
        if (!authorized(headers)) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(rebindApprovalService.reject(id, actor(operator), reason(input)));
        } catch (BusinessException e) {
            log.error("换绑拒绝失败：rebindId={}, reason={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorBody("REBIND_REJECTION_FAILED", e.getMessage()));
        }
    }

    private boolean authorized(HttpHeaders headers) {
        return serviceSecretVerifier.isAuthorized(headers.getFirst(registryProperties.getServiceSecretHeader()));
    }

    private ResponseEntity<Object> unauthorized() {
        log.error("换绑审批被拒绝：服务间密钥校验未通过（请求头 {}）", registryProperties.getServiceSecretHeader());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorBody("UNAUTHORIZED", "服务间密钥校验未通过"));
    }

    public record DecisionInput(String reason) {
    }

    public record ErrorBody(String error, String message) {
    }
}
