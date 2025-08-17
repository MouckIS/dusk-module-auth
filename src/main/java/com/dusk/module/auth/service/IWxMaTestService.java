package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-02-24 8:24
 */
public interface IWxMaTestService {
    List<MobileUserDto> getValidTestUser();
}
