package com.dusk.module.ddm.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.dusk.module.auth.dto.weixin.*;
import com.github.dozermapper.core.Mapper;
import com.qq.weixin.mp.aes.JsonParse;
import com.qq.weixin.mp.aes.WXBizJsonMsgCrypt;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.module.auth.dto.wx.WxCpTextCardMessage;
import com.dusk.common.module.auth.dto.wx.WxTextcardDTO;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.common.module.auth.service.IWxPcRpcService;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.weixin.*;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.repository.IUserWxRelationRepository;
import com.dusk.module.auth.service.IUserService;
import com.dusk.module.auth.service.IWxCpService;
import com.dusk.module.auth.utils.okhttp.Method;
import com.dusk.module.auth.utils.okhttp.OkHttpTask;
import com.dusk.module.auth.utils.okhttp.OkHttpTaskParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-03-16 11:46
 */
@Service
@Slf4j
@Transactional
public class WxCpServiceImpl implements IWxPcRpcService, IWxCpService {
    //自建应用accessTokenKey
    private static final String WX_ACCESS_TOKEN_KEY = "WX_ACCESS_TOKEN_KEY";
    //服务商凭证KEY
    private static final String WX_PROVIDER_ACCESS_TOKEN_KEY = "WX_PROVIDER_ACCESS_TOKEN_KEY";
    //推送suite_ticket
    private static final String WX_PROVIDER_SUITE_TICKET_KEY = "WX_PROVIDER_SUITE_TICKET_KEY";
    //代开发应用模板凭证
    private static final String WX_PROVIDER_SUITE_ACCESS_TICKET_KEY = "WX_PROVIDER_SUITE_ACCESS_TICKET_KEY";
    //代开发授权应用secret
    private static final String WX_PROVIDER_APP_ACCESS_TICKET_KEY = "WX_PROVIDER_APP_ACCESS_TICKET_KEY";

    //供应商企业Id
    @Value("${wx.cp.service.corpId}")
    private String serverCorpId;
    //服务商的secret
    @Value("${wx.cp.service.providerSecret}")
    private String providerSecret ;
    //应用模板EncodingAESKey
    @Value("${wx.cp.app.encodingAESKey}")
    private String serviceEncodingAESKey ;
    //应用模板Token
    @Value("${wx.cp.app.token}")
    private String serviceToken ;

    @Value("${wx.cp.app.templateId}")
    private String templateId ;

    //客户企业id
    @Value("${wx.cp.client.corpId}")
    private String clientCorpId;
    //客户应用id
    @Value("${wx.cp.client.agentId}")
    private String agentId ;
    //授权客户应用secret
    @Value("${wx.cp.client.appSecret}")
    private String secret ;


    @Autowired
    private OkHttpTask okHttpTask;
    @Autowired
    private IUserWxRelationRepository userWxRelationRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IUserService userService;
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private ITenantRepository tenantRepository;
    @Autowired(required = false)
    private RedisUtil<String> redisUtil;
    @Autowired
    private Mapper mapper;
    @Autowired
    private JPAQueryFactory queryFactory;

    /**
     * 获取服务商凭证
     * @return
     */
    private String getProviderAccessToken() {
        String providerAccessToken = redisUtil.getCache(WX_PROVIDER_ACCESS_TOKEN_KEY);
        if(StringUtils.isEmpty(providerAccessToken)) {
            String providerAccessTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/service/get_provider_token";
            OkHttpTaskParam accessTokenParam = new OkHttpTaskParam();
            accessTokenParam.setUrl(providerAccessTokenUrl);
            accessTokenParam.setMethod(Method.POST);
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("corpid", serverCorpId);
            paramMap.put("provider_secret", providerSecret);
            accessTokenParam.setBody(JSON.toJSONString(paramMap));

            log.info("providerAccessTokenUrl:"+providerAccessTokenUrl);
            try {
                String providerAccessTokenData = okHttpTask.dataTask(accessTokenParam);
                WxCpProviderAccessTokenInfo providerAccessTokenResult = JSON.parseObject(providerAccessTokenData, WxCpProviderAccessTokenInfo.class);
                providerAccessToken =  providerAccessTokenResult.getProvider_access_token();
                //缓存access token
                redisUtil.setCache(WX_PROVIDER_ACCESS_TOKEN_KEY, providerAccessToken, providerAccessTokenResult.getExpires_in(), TimeUnit.SECONDS);
            }catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException("获取服务商凭证失败："+e.getMessage());
            }
        }
        return providerAccessToken;
    }

    /**
     * 获取加密后的cropId
     * @return
     */
    private String getOpenCorpId() {
        String openCorpIdUrl = String.format("https://qyapi.weixin.qq.com/cgi-bin/service/corpid_to_opencorpid?provider_access_token=%s", getProviderAccessToken());
        OkHttpTaskParam openCorpIdParam = new OkHttpTaskParam();
        openCorpIdParam.setUrl(openCorpIdUrl);
        openCorpIdParam.setMethod(Method.POST);
        Map<String, String> openCorpIdMap = new HashMap<>();
        openCorpIdMap.put("corpid", clientCorpId);
        openCorpIdParam.setBody(JSON.toJSONString(openCorpIdMap));

        log.info("openCorpIdUrl:"+openCorpIdUrl);
        try {
            String data = okHttpTask.dataTask(openCorpIdParam);
            WxOpenCorpIdInfo result = JSON.parseObject(data, WxOpenCorpIdInfo.class);
            if(result.getErrcode() != 0) {
                throw new BusinessException("获取access_token："+result.getErrmsg());
            }else {
                return result.getOpen_corpid();
            }
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("获取服务商主体下的密文corpid失败："+e.getMessage());
        }
    }

    @Override
    public String serviceProviderUrlGet(String msg_signature, Integer timestamp, String nonce, String echostr) {
        try {
            log.info("企业微信加密签名: {},时间戳: {},随机数: {},加密的字符串: {}", msg_signature, timestamp, nonce, echostr);
            WXBizJsonMsgCrypt wxcpt = null;
            try {
                wxcpt = new WXBizJsonMsgCrypt(serviceToken, serviceEncodingAESKey, getOpenCorpId());
            } catch (Exception e) {
                wxcpt = new WXBizJsonMsgCrypt(serviceToken, serviceEncodingAESKey, serverCorpId);
            }

            return wxcpt.VerifyURL(msg_signature, String.valueOf(timestamp), nonce, echostr);
        } catch (Exception e) {
            log.error("企业微信服务商应用验证URL失败："+e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String serviceProviderUrlPost(String msg_signature, String timestamp, String nonce, String body) {
        try {
            log.info("=========参数解析开始=========");
            log.info("企业微信加密签名: {},时间戳: {},随机数: {}", msg_signature, timestamp, nonce);
            WXBizJsonMsgCrypt wxcpt = null;
            String sMsg = null;
            /**
             * 加解密库里，ReceiveId 在各个场景的含义不同：
             *
             * 企业应用的回调，表示corpid
             * 第三方事件的回调，表示suiteid
             * 个人主体的第三方应用的回调，ReceiveId是一个空字符串
             */
            try {
                wxcpt = new WXBizJsonMsgCrypt(serviceToken, serviceEncodingAESKey, templateId);
                sMsg = wxcpt.DecryptMsg(msg_signature, timestamp, nonce, body);
            } catch (Exception e) {
                try {
                    wxcpt = new WXBizJsonMsgCrypt(serviceToken, serviceEncodingAESKey, serverCorpId);
                    sMsg = wxcpt.DecryptMsg(msg_signature, timestamp, nonce, body);
                }catch (Exception e1){
                    wxcpt = new WXBizJsonMsgCrypt(serviceToken, serviceEncodingAESKey, getOpenCorpId());
                    sMsg = wxcpt.DecryptMsg(msg_signature, timestamp, nonce, body);
                }

            }

            Map<String, String> resultMap = new HashMap<String, String>(16);
            JsonParse.parseXmlToMap(sMsg, resultMap);
            log.info("decrypt密文转为map结果为{}", resultMap);
            log.info("=========参数解析结束=========");

            callbackAction(resultMap);
        } catch (Exception e) {
            log.error("密文参数解析失败，错误原因请查看异常:"+e.getMessage(), e);
        }
        return null;
    }

    private void callbackAction(Map<String, String> resultMap) {
        String infoType = resultMap.get("InfoType");
        if(infoType == null) {
            return;
        }
        switch (infoType) {
            case "suite_ticket" :
                log.info("获取推送suite_ticket");
                redisUtil.setCache(WX_PROVIDER_SUITE_TICKET_KEY, resultMap.get("SuiteTicket"), 30, TimeUnit.MINUTES);
                break;
            case "reset_permanent_code":
                log.info("代开发授权应用secret");
                getAppSecret(resultMap.get("AuthCode"), getSuiteAccessToken());
                break;
        }
    }

    /**
     * 获取代开发应用模板凭证
     * @return
     */
    private String getSuiteAccessToken() {
        String suiteAccessToken = redisUtil.getCache(WX_PROVIDER_SUITE_ACCESS_TICKET_KEY);
        if(StringUtils.isEmpty(suiteAccessToken)) {
            String providerAccessTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/service/get_suite_token";
            OkHttpTaskParam accessTokenParam = new OkHttpTaskParam();
            accessTokenParam.setUrl(providerAccessTokenUrl);
            accessTokenParam.setMethod(Method.POST);
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("suite_id", "dk67e8671a7d8d5dfa");
            paramMap.put("suite_secret", "2IT0-x6Iizsgc5J3xsvi3t_BwAJoOuX2X4UEqympJOg");
            paramMap.put("suite_ticket", redisUtil.getCache(WX_PROVIDER_SUITE_TICKET_KEY));
            accessTokenParam.setBody(JSON.toJSONString(paramMap));

            log.info("providerAccessTokenUrl:"+providerAccessTokenUrl);
            try {
                String providerAccessTokenData = okHttpTask.dataTask(accessTokenParam);
                WxCpSuiteAccessTokenInfo providerAccessTokenResult = JSON.parseObject(providerAccessTokenData, WxCpSuiteAccessTokenInfo.class);
                suiteAccessToken =  providerAccessTokenResult.getSuite_access_token();
                //缓存access token
                redisUtil.setCache(WX_PROVIDER_SUITE_ACCESS_TICKET_KEY, suiteAccessToken, providerAccessTokenResult.getExpires_in(), TimeUnit.SECONDS);
            }catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException("获取服务商凭证失败："+e.getMessage());
            }
        }
        return suiteAccessToken;
    }

    private String getAppSecret(String authCode, String suiteAccessToken) {
        String appAccessToken = redisUtil.getCache(WX_PROVIDER_APP_ACCESS_TICKET_KEY);
        if(StringUtils.isEmpty(appAccessToken)) {
            String accessTokenUrl = String.format("https://qyapi.weixin.qq.com/cgi-bin/service/get_permanent_code?suite_access_token=%s", suiteAccessToken);
            OkHttpTaskParam accessTokenParam = new OkHttpTaskParam();
            accessTokenParam.setUrl(accessTokenUrl);
            accessTokenParam.setMethod(Method.POST);
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("auth_code", authCode);
            accessTokenParam.setBody(JSON.toJSONString(paramMap));
            log.info("accessTokenUrl:"+accessTokenUrl);
            try {
                String data = okHttpTask.dataTask(accessTokenParam);
                log.info("accessTokenData:"+data);
                WxCpAppAccessTokenInfo result = JSON.parseObject(data, WxCpAppAccessTokenInfo.class);
                if(result.getErrcode() != null) {
                    throw new BusinessException("代开发授权应用secret失败："+result.getErrmsg());
                }else {
                    appAccessToken =  result.getPermanent_code();
                    log.info("代开发授权应用secret:"+appAccessToken);
                    //缓存access token
                    redisUtil.setCache(WX_PROVIDER_APP_ACCESS_TICKET_KEY, appAccessToken, 60, TimeUnit.SECONDS);
                }
            }catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException("获取access 代开发授权应用secret失败："+e.getMessage());
            }
        }
        return appAccessToken;
    }

    /**
     * 获取代开发授权应用access_token
     * @return
     */
    private String getAccessToken() {
        //从缓存获取access token
        String accessToken = redisUtil.getCache(WX_ACCESS_TOKEN_KEY);
        if(StringUtils.isEmpty(accessToken)) {
            String accessTokenUrl = String.format("https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s", getOpenCorpId(), secret);
            OkHttpTaskParam accessTokenParam = new OkHttpTaskParam();
            accessTokenParam.setUrl(accessTokenUrl);
            accessTokenParam.setMethod(Method.GET);
            log.info("accessTokenUrl:"+accessTokenUrl);
            try {
                String data = okHttpTask.dataTask(accessTokenParam);
                log.info("accessTokenData:"+data);
                WxCpAccessTokenInfo result = JSON.parseObject(data, WxCpAccessTokenInfo.class);
                if(result.getErrcode() != 0) {
                    throw new BusinessException("获取access_token："+result.getErrmsg());
                }else {
                    accessToken =  result.getAccess_token();
                    //缓存access token
                    redisUtil.setCache(WX_ACCESS_TOKEN_KEY, accessToken, result.getExpires_in(), TimeUnit.SECONDS);
                }
            }catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException("获取access token失败："+e.getMessage());
            }
        }
        return accessToken;
    }

    @Override
    public WxCpUserAuthorizationResult authorization(String code, String state, String n) {
        //获取用户微信Id
        return getWxCpUser(getAccessToken(), code, n);

    }

    /**
     * 获取访问用户身份
     * @param accessToken
     * @param code
     * @param tenantName
     * @return
     */
    private WxCpUserAuthorizationResult getWxCpUser(String accessToken, String code, String tenantName) {
        log.info("=========获取访问用户身份==============");
        code = code.replaceAll("d,", "");
        log.info("code:"+code);
        String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=%s&code=%s&debug=1", accessToken, code);
        OkHttpTaskParam param = new OkHttpTaskParam();
        param.setUrl(url);
        param.setMethod(Method.GET);
        log.info("url:"+url);
        try {

            String data = okHttpTask.dataTask(param);
            log.info("data:{}",data);
            WxCpUserIdInfo result = JSON.parseObject(data, WxCpUserIdInfo.class);
            if(result.getErrcode() != 0) {
                throw new BusinessException("微信用户身份验证失败："+result.getErrmsg());
            }else {
                Tenant tenant = tenantRepository.findByTenantName(tenantName).orElse(null);
                if(tenant != null) {
                    TenantContextHolder.setTenantId(tenant.getId());
                }else {
                    throw new BusinessException("租户["+tenantName+"]不存在");
                }

                String wxUserId = result.getUserid();
                String token = null;
                UserWxRelation userWxRelation = userWxRelationRepository.findByAppIdAndOpenId(agentId, wxUserId);

                //如果没有绑定，获取企业微信用户敏感信息，通过手机号获取人员关联
                if(userWxRelation == null) {
                    userWxRelation = findRelationByUserSensitiveInfo(result, accessToken);
                }

                //如果没有绑定，获取微信用户通讯录信息，通过用户名获取人员关联
                if(userWxRelation == null) {
                    userWxRelation = findRelationByUserAddressBook(result, accessToken);
                }

                if(userWxRelation != null) {
                    //根据id签发token
                    Long cruxUserId = userWxRelation.getUserId();
                    User user = userRepository.findById(cruxUserId).orElse(null);
                    if(user != null) {
                        userService.checkUserValid(user);
                        UserContext context = LoginUtils.getUserContextByUser(user);
                        token = tokenAuthManager.generateToken(context);
                    }
                }
                return new WxCpUserAuthorizationResult(wxUserId, token);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("微信用户身份验证失败："+e.getMessage());
        }
    }

    /**
     * 从微信用户敏感信息，通过手机号自动关联
     * @param userIdInfo
     * @param accessToken
     * @return
     */
    private UserWxRelation findRelationByUserSensitiveInfo(WxCpUserIdInfo userIdInfo, String accessToken) {
        log.info("===========获取访问用户敏感信息=============");
        UserWxRelation userWxRelation = null;
        String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail?access_token=%s", accessToken);
        OkHttpTaskParam param = new OkHttpTaskParam();
        param.setUrl(url);
        param.setMethod(Method.POST);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("user_ticket", userIdInfo.getUser_ticket());
        String body = JSON.toJSONString(paramMap);
        param.setBody(body);
        log.info("url:{}， body:{}", url, body);
        try {

            String data = okHttpTask.dataTask(param);
            log.info("data:{}", data);
            WxCpUserDetail result = JSON.parseObject(data, WxCpUserDetail.class);
            if(result.getErrcode() != 0) {
                throw new BusinessException("获取访问用户敏感信息失败："+result.getErrmsg());
            }else {
                String phoneNo = result.getMobile();
                if(StringUtils.isEmpty(phoneNo)) {
                    log.error("企业微信用户{},手机号为空，无法和系统用户关联", result.getUserid());
                }else  {
                    QUser qUser = QUser.user;
                    List<Long> userIds = queryFactory.select(qUser.id).from(qUser).where(qUser.phoneNo.eq(phoneNo).and(qUser.userType.eq(EUnitType.Inner))).fetch();
                    if(CollectionUtils.isEmpty(userIds)) {
                        log.error("企业微信用户{},手机号{}，不存在系统用户中，无法关联", result.getUserid(), phoneNo);
                    }else if(userIds.size() > 1) {
                        log.error("企业微信用户{},手机号{}，存在多个系统用户，无法关联", result.getUserid(), phoneNo);
                    }else {
                        userWxRelation = userWxRelationRepository.findFirstByUserIdAndAppId(userIds.get(0), agentId);

                        if(userWxRelation == null) {
                            userWxRelation = new UserWxRelation();
                            userWxRelation.setUserId(userIds.get(0));
                            userWxRelation.setAppId(agentId);
                            userWxRelation.setOpenId(userIdInfo.getUserid());
                        }else {
                            userWxRelation.setOpenId(userIdInfo.getUserid());
                        }

                        userWxRelationRepository.save(userWxRelation);
                    }
                }

            }
            return userWxRelation;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("获取访问用户敏感信息失败："+e.getMessage());
        }
    }

    /**
     * 从微信用户通讯录信息，通过用户名自动关联
     * @param userIdInfo
     * @param accessToken
     * @return
     */
    private UserWxRelation findRelationByUserAddressBook(WxCpUserIdInfo userIdInfo, String accessToken) {
        log.info("===========获取读取成员通讯录信息=============");
        UserWxRelation userWxRelation = null;
        String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s", accessToken, userIdInfo.getUserid());
        OkHttpTaskParam param = new OkHttpTaskParam();
        param.setUrl(url);
        param.setMethod(Method.GET);
        log.info("url:{}", url);
        try {
            String data = okHttpTask.dataTask(param);
            log.info("data:{}", data);
            WxCpUserAddressBook result = JSON.parseObject(data, WxCpUserAddressBook.class);
            if(result.getErrcode() != 0) {
                throw new BusinessException("获取读取成员通讯录信息失败："+result.getErrmsg());
            }else {
                String name = result.getName();
                QUser qUser = QUser.user;
                List<Long> userIds = queryFactory.select(qUser.id).from(qUser).where(qUser.name.eq(name).and(qUser.userType.eq(EUnitType.Inner))).fetch();
                if(CollectionUtils.isEmpty(userIds)) {
                    log.error("企业微信用户{},用户名{}，不存在系统用户中，无法关联", result.getUserid(), name);
                }else if(userIds.size() > 1) {
                    log.error("企业微信用户{},用户名{}，存在多个系统用户，无法关联", result.getUserid(), name);
                }else {
                    userWxRelation = userWxRelationRepository.findFirstByUserIdAndAppId(userIds.get(0), agentId);

                    if(userWxRelation == null) {
                        userWxRelation = new UserWxRelation();
                        userWxRelation.setUserId(userIds.get(0));
                        userWxRelation.setAppId(agentId);
                        userWxRelation.setOpenId(userIdInfo.getUserid());
                    }else {
                        userWxRelation.setOpenId(userIdInfo.getUserid());
                    }
                    userWxRelationRepository.save(userWxRelation);
                }
            }
            return userWxRelation;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("获取读取成员通讯录信息失败："+e.getMessage());
        }
    }

    @Override
    public WxCpUserAuthorizationResult bind(String wxUserId, String userName, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userName, password);
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(token);
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("帐户名或者密码错误");
        }

        if (authResult.isAuthenticated()) {
            User user = userRepository.findByUserName(userName).orElseThrow(() -> new BusinessException("用户名["+userName+"]不存在"));
            //如果该微信用户已经绑定同样的账号，直接签发token
            if(userWxRelationRepository.countAllByUserIdAndOpenId(user.getId(), wxUserId) > 0) {
                userService.checkUserValid(user);
                UserContext context = LoginUtils.getUserContextByUser(user);
                return new WxCpUserAuthorizationResult(wxUserId, tokenAuthManager.generateToken(context));
            }else {
                if(userWxRelationRepository.countAllByUserId(user.getId())>0) {
                    throw new BusinessException("用户["+userName+"]已被绑定");
                }

                if(userWxRelationRepository.countAllByOpenId(wxUserId)>0) {
                    throw new BusinessException("企业用户["+wxUserId+"]已被绑定");
                }
                UserWxRelation userWxRelation = new UserWxRelation();
                userWxRelation.setUserId(user.getId());
                userWxRelation.setAppId(agentId);
                userWxRelation.setOpenId(wxUserId);
                userWxRelationRepository.save(userWxRelation);
                UserContext userContext = (UserContext) authResult.getPrincipal();
                return new WxCpUserAuthorizationResult(wxUserId, tokenAuthManager.generateToken(userContext));
            }

        } else {
            throw new BusinessException("帐户名或者密码错误");
        }
    }

    @Override
    public void sendCardMessage(WxCpTextCardMessage input) {
        if(StringUtils.isEmpty(input.getTouser())) {
            if (!CollectionUtils.isEmpty(input.getUserId())) {
                String toUser = userWxRelationRepository.findAllByUserIdIn(input.getUserId()).stream().map(UserWxRelation::getOpenId).collect(Collectors.joining("|"));
                input.setTouser(toUser);
            }else {
                log.error("访客申请企业微信消息发送失败：发送目标账号为空");
            }
        }
        if(StringUtils.isEmpty(input.getTouser())) {
            return;
        }

        String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s",getAccessToken());
        OkHttpTaskParam param = new OkHttpTaskParam();

        //适配推送格式：微信消息卡片模式未增加消息跳转url时，转为发送文本形式微信通知
        param.setUrl(url);
        param.setMethod(Method.POST);
        input.setAgentid(Integer.parseInt(agentId));
        WxTextcardDTO textcard = input.getTextcard();

        if(Objects.nonNull(textcard) && StrUtil.isNotBlank(textcard.getUrl())){
            input.setMsgtype("textcard");
            param.setBody(JSON.toJSONString(input));
        }else {
            WxTextDTO textDTO = new WxTextDTO();
            // 消息内容，最长不超过2048个字节
            textDTO.setContent(textcard.getDescription());
            input.setMsgtype("text");
            WxCpTextMessage textMessage = mapper.map(input, WxCpTextMessage.class);
            textMessage.setText(textDTO);
            param.setBody(JSON.toJSONString(textMessage));
        }
        try {
            log.info("发送企业微信消息请求："+JSON.toJSONString(param));
            String data = okHttpTask.dataTask(param);
            WxCpMessageResult result = JSON.parseObject(data, WxCpMessageResult.class);
            log.info("发送企业微信消息结果："+result.toString());
            Integer code = result.getErrcode();
            if(code == 0) {
                log.info("发送企业微信消息成功");
            } else if (code == 81013) {
                throw new BusinessException("发送企业微信消息失败：该审批人员没有绑定企业微信账号.\n1.请该审批人到微信工作台-访客管理进行账号绑定. \n2.该申请单已流转，请审批人到系统中审批");
            } else {
                throw new BusinessException("发送企业微信消息失败："+result.getErrmsg());
            }
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException("获取发送消息失败："+e.getMessage());
        }
    }

    @Override
    public String getOpenId(Long userId) {
        UserWxRelation userWxRelation = userWxRelationRepository.findFirstByUserIdAndAppId(userId, agentId);
        if(userWxRelation != null) {
            return userWxRelation.getOpenId();
        }else {
            return null;
        }
    }
}
