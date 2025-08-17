package com.dusk.module.auth.cache;

/**
 * @author kefuming
 * @date 2021-05-11 17:27
 */
public interface IUserFingerprintCacheService {

    Integer getUserSeq(Long userId);

    void saveUserSeq(Long userId, Integer userSeq);
}
