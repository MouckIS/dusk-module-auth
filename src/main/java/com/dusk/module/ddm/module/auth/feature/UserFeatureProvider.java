package com.dusk.module.ddm.module.auth.feature;

import com.dusk.common.core.feature.IFeatureDefinitionContext;
import com.dusk.common.core.feature.impl.FeatureProvider;
import com.dusk.common.core.feature.ui.CheckBox;
import com.dusk.common.core.feature.ui.SingerLineString;
import com.dusk.common.core.feature.ui.TenantFeature;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020/5/29 14:19
 */
@Component
public class UserFeatureProvider extends FeatureProvider {
    public static final String APP_USER = "App.User";
    public static final String APP_USER_MAX_USERS = "App.User.MaxUsers";
    public static final String APP_USER_MAX_USERS_NUMBER = "App.User.MaxUsers.Number";
    public static final String APP_USER_LOCK_ON_LOGIN = "App.User.LockOnLogin";
    public static final String APP_USER_LOCK_ON_LOGIN_COUNT = "App.User.LockOnLogin.Count";
    public static final String APP_USER_ALLOW_MOBILE_LOGIN = "App.User.AllowMobileLogin";
    public static final String APP_USER_ALLOW_SCANNING_LOGIN = "App.User.AllowScanningLogin";
    public static final String APP_USER_ALLOW_FACE_LOGIN = "App.User.AllowFaceLogin";
    public static final String APP_USER_STRONG_PASSWORD = "App.User.StrongPassword";
    public static final String APP_USER_LOGIN_SHOW_DROPDOWN_LIST = "App.User.LoginShowDropdownList";
    public static final String APP_USER_ALLOW_KEEP_LOGIN = "App.User.AllowKeepLogin";


    /**
     * 允许匿名获取用户名称列表用于登陆
     */
    public static final String APP_USER_ALLOW_GET_USERS_FOR_LOGIN_BY_ANONYMOUS = "App.User.AllowGetUsersForLoginByAnonymous";

    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        //create方法创建或者定位到一个特性；createChildren创建子特性，必须跟在create方法后面
        context.create(APP_USER, "true", "用户特性");
        TenantFeature feature = context.createChildren(APP_USER_MAX_USERS, "true", "最大用户限制", "", new CheckBox());
        context.createChildren(feature, APP_USER_MAX_USERS_NUMBER, "100", "最大用户数", "", new SingerLineString());
        context.createChildren(APP_USER_LOCK_ON_LOGIN, "false", "登陆失败锁定用户", "", new CheckBox());
        context.createChildren(APP_USER_LOCK_ON_LOGIN_COUNT, "5", "允许登陆失败次数", "", new SingerLineString());
        TenantFeature loginContext = context.createChildren(APP_USER_LOGIN_SHOW_DROPDOWN_LIST,"false","登陆显示人员下拉列表","",new CheckBox());
        context.createChildren(loginContext, APP_USER_ALLOW_GET_USERS_FOR_LOGIN_BY_ANONYMOUS,"false","匿名获取用户名称列表(勾选) / 获取历史登录记录(不勾选)","",new CheckBox());
        context.createChildren(APP_USER_ALLOW_MOBILE_LOGIN, "false", "允许手机登录", "", new CheckBox());
        context.createChildren(APP_USER_ALLOW_SCANNING_LOGIN, "false", "允许扫码登录", "", new CheckBox());
        context.createChildren(APP_USER_ALLOW_FACE_LOGIN, "false", "允许刷脸登录", "", new CheckBox());
        context.createChildren(APP_USER_STRONG_PASSWORD, "true", "强密码验证", "", new CheckBox());
        context.createChildren(APP_USER_ALLOW_KEEP_LOGIN, "false", "允许用户设置保持登录", "", new CheckBox());
    }

}
