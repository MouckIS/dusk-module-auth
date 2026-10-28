package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 账号状态 Redis 投影：{@code auth:subject:{scope}:user:{userId}}，见 5.2 / 5.3。
 *
 * <p>该缓存 TTL 应明显短于路由缓存，因为它是「禁用账号/改角色」的快速生效通道；
 * 失效事件只是加速，TTL 与周期对账负责兜底。</p>
 */
@Component
@Slf4j
public class SubjectReadModelProjector {

    public static final String SUBJECT_KEY_PREFIX = "auth:subject:";
    public static final String SUBJECT_VERSION_FIELD = "subjectVersion";

    private final AuthorizationReadModelResolver resolver;
    private final RedisVersionedWriter writer;
    private final AuthzMqProperties properties;
    private final ObjectMapper objectMapper;

    public SubjectReadModelProjector(AuthorizationReadModelResolver resolver,
                                     RedisVersionedWriter writer,
                                     AuthzMqProperties properties,
                                     ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.writer = writer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean project(String tenantScope, Long userId) {
        if (userId == null || tenantScope == null || tenantScope.isBlank()) {
            return false;
        }
        try {
            Optional<SubjectSnapshot> snapshot = resolver.resolveSubject(tenantScope, userId);
            if (snapshot.isEmpty()) {
                log.warn("账号状态读模型跳过：用户不存在或作用域不匹配，scope={}, userId={}", tenantScope, userId);
                return false;
            }
            SubjectSnapshot subject = snapshot.get();
            String payload = objectMapper.writeValueAsString(new SubjectPayload(
                    subject.tenantScope(), subject.userId(), subject.subjectVersion(),
                    subject.enabled(), List.copyOf(subject.roleIds())));
            return writer.write(key(subject.tenantScope(), subject.userId()), SUBJECT_VERSION_FIELD,
                    subject.subjectVersion(), payload, properties.getSubjectReadModelTtl());
        } catch (Exception e) {
            log.error("账号状态读模型投影失败：scope={}, userId={}", tenantScope, userId, e);
            return false;
        }
    }

    private String key(String scope, Long userId) {
        return SUBJECT_KEY_PREFIX + scope + ":user:" + userId;
    }

    public record SubjectPayload(String tenantScope,
                                 Long userId,
                                 long subjectVersion,
                                 boolean enabled,
                                 List<Long> roleIds) {
    }
}
