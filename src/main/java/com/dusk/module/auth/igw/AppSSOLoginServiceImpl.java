package com.dusk.module.auth.igw;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kefuming
 * @date 2021-11-22 10:06
 */
@Service
@ConditionalOnProperty(name = "igwsso.enable", havingValue = "true")
@Slf4j
public class AppSSOLoginServiceImpl implements IAppSSOLoginService {

    @Value("${igwsso.iscAppId}")
    private String iscAppId;

    @Value("${igwsso.iscSecret}")
    private String iscSecret;

    @Value("${igwsso.service}")
    private String service;

    @Value("${igwsso.address}")
    private String address;


    // http请求超时时长  3秒超时
    private final static int TIME_OUT_MILLISECONDS = 3000;

    // i国网获取token url
    private final static String ACCESSTOKEN_URL = "zuul/sgid-provider-console/res/iscMincroService/getAccessToken";

    // i国网转换ticket
    private final static String TICKET_URL = "zuul/sgid-frontmv/identity/getUserInfoByTicket";

    @Autowired
    IUserService userService;


    /**
     * 从i国网获取accesstoken
     *
     * @return
     * @throws Exception
     */
    private String getAccessToken() throws Exception {
        String url = StrUtil.format("{}{}", address, ACCESSTOKEN_URL);
        JSONObject iscInfo = new JSONObject();
        iscInfo.set("appId", iscAppId);
        iscInfo.set("clientSecret", iscSecret);
        String digest = SmUtil.sm3(iscInfo.toJSONString(0));
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Acloud-Data-Sign", digest);
        headers.put("X-Clientid", iscAppId);
        HttpRequest httpRequest = HttpUtil.createPost(url)
                .contentType("application/json;charset=utf8")
                .addHeaders(headers).timeout(TIME_OUT_MILLISECONDS);
        httpRequest.body(iscInfo.toJSONString(0));
        HttpResponse response = httpRequest.execute();
        JSONObject d = new JSONObject(response.body());
        if ("100001".equals(d.get("code").toString())) {
            JSONObject data = d.getJSONObject("data");
            return "Client " + data.get("accessToken").toString();
        } else {
            throw new Exception(StrUtil.format("获取i国网acessToken失败：{}", d.get("message").toString()));
        }


    }

    @Override
    public String login(String ticket) {
        try {
            String accessToken = getAccessToken();
            Map<String, String> headers = new HashMap<>();
            headers.put("X-ISC-AccessToken", accessToken);
            headers.put("X-Clientid", iscAppId);
            JSONObject dataSign = new JSONObject();
            dataSign.set("appId", iscAppId);
            dataSign.set("service", service);
            dataSign.set("ticket", ticket);
            //对body进行Sm3加密
            String digest = SmUtil.sm3(dataSign.toJSONString(0));
            headers.put("X-Acloud-Data-Sign", digest);
            String url = StrUtil.format("{}{}", address, TICKET_URL);
            HttpRequest request = HttpUtil.createPost(url)
                    .contentType("application/json;charset=utf8")
                    .addHeaders(headers).timeout(TIME_OUT_MILLISECONDS);
            request.body(dataSign.toJSONString(0));
            HttpResponse response = request.execute();
            JSONObject d = new JSONObject(response.body());
            if ("100001".equals(d.get("code").toString())) {
                JSONObject data = d.getJSONObject("data");
                String account = data.get("account").toString();
                return userService.generateTokenByUserName(account);
            } else {
                throw new Exception(StrUtil.format("解析i国网票据失败：{}", d.get("message").toString()));
            }
        } catch (BusinessException e) {
            throw new BusinessException("用户不存在，请在统一权限平台授权");
        } catch (Exception e) {
            log.error("token获取失败:{}", e.toString());
            throw new BusinessException("token获取失败:" + e.getMessage());
        }
    }
}
