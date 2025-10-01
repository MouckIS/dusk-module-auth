package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.annotation.DisableTenantFilter;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IUserService;
import com.dusk.module.auth.service.IWxMaTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信小程序审核的后门类
 *
 * @author kefuming
 * @date 2021-02-24 8:27
 */
@Service
@ConditionalOnProperty(name = "wx.test.enabled", havingValue = "true")
@Slf4j
public class WxMaTestServiceImpl implements IWxMaTestService {

    //如果尚未配置 则用test作为默认值
    @Value("${wx.test.account:test}")
    String defaultAccount;

    @Autowired
    IUserRepository userRepository;

    @Autowired
    IUserService userService;

    @Autowired
    Mapper dozerMapper;

    @Autowired
    private TokenAuthManager tokenAuthManager;

    @Override
    @DisableTenantFilter
    public List<MobileUserDto> getValidTestUser() {
        List<MobileUserDto> list = new ArrayList<>();
        List<User> userList;
        if (TenantContextHolder.getTenantId() == null) {
            userList = userRepository.findByUserNameAndTenantIdIsNotNull(defaultAccount);
        } else {
            userList = userRepository.findByUserNameAndTenantId(defaultAccount, TenantContextHolder.getTenantId());
        }

        for (User temp : userList) {
            try {
                userService.checkUserValid(temp);
                UserContext context = LoginUtils.getUserContextByUser(temp);
                String token = tokenAuthManager.generateToken(context);
                MobileUserDto dto = dozerMapper.map(temp, MobileUserDto.class);
                dto.setToken(token);
                list.add(dto);

            } catch (Exception ex) {
                log.info("检测登陆用户异常：{}", ex.getMessage());
            }

        }
        return list;
    }
}
