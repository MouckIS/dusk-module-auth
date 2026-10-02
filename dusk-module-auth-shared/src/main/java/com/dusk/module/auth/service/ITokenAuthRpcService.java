package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.GenerateTokenForNonUserInput;
import jakarta.validation.Valid;

import java.util.concurrent.TimeUnit;

/**
 * @author : kefuming
 * @date : 2025/8/17 19:40
 */
public interface ITokenAuthRpcService {
    default String generateTokenForNonUser(@Valid GenerateTokenForNonUserInput input) {
        throw new UnsupportedOperationException("未实现该方法");
    }

    default String generateTokenForUser(Long userId, Long time, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    default String generateTokenByCurrentUser(Long time, TimeUnit unit) {
        throw new UnsupportedOperationException("未实现该方法");
    }

    default void removeToken(String identify) {
        throw new UnsupportedOperationException("未实现该方法");
    }
}
