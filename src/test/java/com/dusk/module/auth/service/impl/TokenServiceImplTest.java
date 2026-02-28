package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.common.rpc.auth.service.ITokenAuthRpcService;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Token服务单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("Token服务测试")
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private ITokenAuthRpcService tokenAuthRpcService;

    @InjectMocks
    private TokenServiceImpl tokenService;

    @DisplayName("foreverTokenSign - 生成永久Token")
    @Test
    void testForeverTokenSign_Success() {
        // Arrange
        TokenSign tokenSign = TestDataBuilder.buildTokenSign();
        tokenSign.setUserId(123L);
        tokenSign.setTokenType("Bearer");
        tokenSign.setTokenValue("test-token");

        String expectedToken = "generated-token-value";
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn(expectedToken);

        // Act
        String result = tokenService.foreverTokenSign(tokenSign);

        // Assert
        assertThat(result).isEqualTo(expectedToken);

        // Verify the RPC call
        ArgumentCaptor<GenerateTokenForNonUserInput> captor = ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());

        GenerateTokenForNonUserInput capturedInput = captor.getValue();
        assertThat(capturedInput.getUserId()).isEqualTo(tokenSign.getUserId());
        assertThat(capturedInput.getTokenType()).isEqualTo(tokenSign.getTokenType());
        assertThat(capturedInput.getTokenValue()).isEqualTo(tokenSign.getTokenValue());
        assertThat(capturedInput.getUnit()).isEqualTo(TimeUnit.DAYS);
    }

    @DisplayName("foreverTokenSign - 处理null的TokenSign字段")
    @Test
    void testForeverTokenSign_WithNullFields() {
        // Arrange
        TokenSign tokenSign = new TokenSign();
        tokenSign.setUserId(null);
        tokenSign.setTokenType(null);
        tokenSign.setTokenValue(null);

        String expectedToken = "token-with-nulls";
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn(expectedToken);

        // Act
        String result = tokenService.foreverTokenSign(tokenSign);

        // Assert
        assertThat(result).isEqualTo(expectedToken);

        ArgumentCaptor<GenerateTokenForNonUserInput> captor = ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());

        GenerateTokenForNonUserInput capturedInput = captor.getValue();
        assertThat(capturedInput.getUserId()).isNull();
        assertThat(capturedInput.getTokenType()).isNull();
        assertThat(capturedInput.getTokenValue()).isNull();
    }

    @DisplayName("foreverTokenSign - RPC服务返回null")
    @Test
    void testForeverTokenSign_RpcReturnsNull() {
        // Arrange
        TokenSign tokenSign = TestDataBuilder.buildTokenSign();
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn(null);

        // Act
        String result = tokenService.foreverTokenSign(tokenSign);

        // Assert
        assertThat(result).isNull();
        verify(tokenAuthRpcService).generateTokenForNonUser(any(GenerateTokenForNonUserInput.class));
    }

    @DisplayName("foreverTokenSign - 处理特殊字符")
    @Test
    void testForeverTokenSign_SpecialCharacters() {
        // Arrange
        TokenSign tokenSign = TestDataBuilder.buildTokenSign();
        tokenSign.setTokenValue("token-with-特殊字符-@#$%");

        String expectedToken = "special-chars-token";
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn(expectedToken);

        // Act
        String result = tokenService.foreverTokenSign(tokenSign);

        // Assert
        assertThat(result).isEqualTo(expectedToken);

        ArgumentCaptor<GenerateTokenForNonUserInput> captor = ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());
        assertThat(captor.getValue().getTokenValue()).contains("特殊字符");
    }

    @DisplayName("foreverTokenSign - 大型Token值")
    @Test
    void testForeverTokenSign_LargeTokenValue() {
        // Arrange
        TokenSign tokenSign = TestDataBuilder.buildTokenSign();
        // 创建一个很长的Token值
        String largeToken = "x".repeat(10000);
        tokenSign.setTokenValue(largeToken);

        String expectedToken = "large-token-result";
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn(expectedToken);

        // Act
        String result = tokenService.foreverTokenSign(tokenSign);

        // Assert
        assertThat(result).isEqualTo(expectedToken);

        ArgumentCaptor<GenerateTokenForNonUserInput> captor = ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());
        assertThat(captor.getValue().getTokenValue()).hasSize(10000);
    }

    @DisplayName("foreverTokenSign - 验证TimeUnit.DAYS总是被设置")
    @Test
    void testForeverTokenSign_VerifyTimeUnitDays() {
        // Arrange
        TokenSign tokenSign = TestDataBuilder.buildTokenSign();
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn("token");

        // Act
        tokenService.foreverTokenSign(tokenSign);

        // Assert
        ArgumentCaptor<GenerateTokenForNonUserInput> captor = ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());
        assertThat(captor.getValue().getUnit()).isEqualTo(TimeUnit.DAYS);
    }
}

