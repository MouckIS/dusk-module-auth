package com.dusk.module.auth.igw;

/**
 * @author kefuming
 * @date 2021-11-22 10:06
 */
public interface IAppSSOLoginService {
    String login(String ticket);
}
