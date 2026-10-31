package com.dusk.module.auth.authz.readmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link RedisVersionedWriter} 单元测试：Lua CAS 写入的返回值语义与参数编码。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisVersionedWriterTest {

    private static final String KEY = "auth:subject:TENANT:5:user:7";
    private static final String FIELD = "subjectVersion";

    @Mock
    private ObjectProvider<StringRedisTemplate> provider;

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisVersionedWriter writer;
    private List<Object> captured;

    @BeforeEach
    void setUp() {
        writer = new RedisVersionedWriter(provider);
        captured = new ArrayList<>();
    }

    private void stubLuaResult(Long result) {
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        doAnswer(invocation -> {
            captured.addAll(List.of(invocation.getArguments()));
            return result;
        }).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Redis 模板不可用：返回 false，不尝试写入")
    void shouldFailWhenRedisUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(writer.write(KEY, FIELD, 1L, "{}", Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("Lua 返回 1：写入成功，参数按 key/字段/版本/载荷/TTL 顺序传递")
    void shouldWriteAndEncodeArguments() {
        stubLuaResult(1L);

        assertThat(writer.write(KEY, FIELD, 7L, "{\"subjectVersion\":7}", Duration.ofMillis(1500))).isTrue();

        assertThat(captured).hasSize(6);
        assertThat(captured.get(1)).isEqualTo(List.of(KEY));
        assertThat(captured.get(2)).isEqualTo(FIELD);
        assertThat(captured.get(3)).isEqualTo("7");
        assertThat(captured.get(4)).isEqualTo("{\"subjectVersion\":7}");
        assertThat(captured.get(5)).isEqualTo("1500");
    }

    @Test
    @DisplayName("Lua 返回 0：已有更新版本，视为无需重试的成功")
    void shouldTreatOlderVersionAsSuccess() {
        stubLuaResult(0L);

        assertThat(writer.write(KEY, FIELD, 3L, "{}", Duration.ofSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("Lua 返回 -1：当前值无法解析，拒绝覆盖")
    void shouldRejectCorruptedValue() {
        stubLuaResult(-1L);

        assertThat(writer.write(KEY, FIELD, 3L, "{}", Duration.ofSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("Lua 返回 null：按失败处理，避免误判为已写入")
    void shouldFailOnNullResult() {
        stubLuaResult(null);

        assertThat(writer.write(KEY, FIELD, 3L, "{}", Duration.ofSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("Redis 执行异常：吞掉并返回 false，由 Outbox 退避重试")
    void shouldFailOnRedisException() {
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        doThrow(new IllegalStateException("connection refused"))
                .when(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());

        assertThat(writer.write(KEY, FIELD, 3L, "{}", Duration.ofSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("非正 TTL：向下钳制为 1ms，避免 PX 0 直接过期")
    void shouldClampNonPositiveTtl() {
        stubLuaResult(1L);

        writer.write(KEY, FIELD, 1L, "{}", Duration.ZERO);

        assertThat(captured.get(5)).isEqualTo("1");
        assertThat(captured.get(1)).isEqualTo(List.of(KEY));
    }
}
