package com.dusk.module.auth.authz.readmodel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 带版本比较的 Redis 原子写入，见《权限优化方案-整理版》6.2。
 *
 * <p>投影内容与版本号必须在同一个原子结构中更新，且只允许更大的版本覆盖。
 * Lua 解码当前值为 JSON，比较指定字段后写入；解码失败视为「无法比较」，
 * 直接拒绝覆盖，避免损坏值被旧版本洗白。</p>
 */
@Component
@Slf4j
public class RedisVersionedWriter {

    private static final String CAS_WRITE_SCRIPT = """
            local field = ARGV[1]
            local incoming = tonumber(ARGV[2])
            local payload = ARGV[3]
            local ttl = tonumber(ARGV[4])
            local current = redis.call('GET', KEYS[1])
            if current then
              local ok, decoded = pcall(cjson.decode, current)
              if not ok then
                return -1
              end
              if decoded[field] and tonumber(decoded[field]) > incoming then
                return 0
              end
            end
            redis.call('SET', KEYS[1], payload, 'PX', ttl)
            return 1
            """;

    private static final RedisScript<Long> CAS_WRITE = new DefaultRedisScript<>(CAS_WRITE_SCRIPT, Long.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public RedisVersionedWriter(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 仅当 {@code version} 不小于当前值时写入。
     *
     * @return {@code true} 表示已写入或已有相同/更新版本（无需重试）；
     * {@code false} 表示 Redis 不可用、值损坏或写入失败
     */
    public boolean write(String key, String versionField, long version, String payload, Duration ttl) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.warn("Redis 模板不可用，无法写入读模型：key={}", key);
            return false;
        }
        try {
            Long result = redisTemplate.execute(CAS_WRITE, List.of(key), versionField,
                    String.valueOf(version), payload, String.valueOf(Math.max(1L, ttl.toMillis())));
            if (result == null || result < 0) {
                log.error("Redis 读模型当前值无法解析，拒绝覆盖：key={}, version={}", key, version);
                return false;
            }
            if (result == 0L) {
                log.debug("Redis 读模型已有更新版本，跳过写入：key={}, incoming={}", key, version);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis 读模型写入失败：key={}, version={}", key, version, e);
            return false;
        }
    }
}
