package com.dusk.module.auth.feature;

import com.dusk.module.auth.impl.FeatureProvider;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.ddm.dto.ui.CheckBox;
import com.dusk.module.ddm.dto.ui.ComboBox;
import com.dusk.module.ddm.dto.ui.Item;
import com.dusk.module.ddm.dto.ui.SingerLineString;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 登陆相关特性
 *
 * @author kefuming
 * @date 2020-12-16 8:10
 */
@Component
public class LoginFeatureProvider extends FeatureProvider {
    public static final String APP_LOGIN = "App.Login";
    public static final String APP_LOGIN_APP_AUTO_LOGIN = "App.Login.App.AutoLogin";
    public static final String APP_LOGIN_MAX_ERROR = "App.Login.Max.Error";
    public static final String APP_LOGIN_FORGET_PWD = "App.Login.ForgetPassword";
    public static final String APP_LOGIN_IGNORE_CASE = "App.Login.IgnoreCase";
    public static final String APP_LOGIN_SSO_LOGIN = "App.Login.SsoLogin";

    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        //create方法创建或者定位到一个特性；createChildren创建子特性，必须跟在create方法后面
        context.create(APP_LOGIN, "true", "登陆特性");
        context.createChildren(APP_LOGIN_MAX_ERROR, "3", "登陆需要验证码错误次数", "", new SingerLineString());
        context.createChildren(APP_LOGIN_APP_AUTO_LOGIN, "true", "APP自动登录", "", new CheckBox());
        context.createChildren(APP_LOGIN_SSO_LOGIN, "false", "登录页跳转第三方登录", "", new CheckBox());

        List<Item> forgetPwdOptions = new ArrayList<>();
        forgetPwdOptions.add(new Item("false", "禁用"));
        forgetPwdOptions.add(new Item("mobile", "通过手机号找回"));
        forgetPwdOptions.add(new Item("email", "通过邮箱找回"));
        forgetPwdOptions.add(new Item("mobileOrEmail", "通过手机号或邮箱找回"));
        context.createChildren(APP_LOGIN_FORGET_PWD, "false", "忘记密码", "", new ComboBox(forgetPwdOptions));

        context.createChildren(APP_LOGIN_IGNORE_CASE, "false", "登录名忽略大小写","",new CheckBox());
    }
}
