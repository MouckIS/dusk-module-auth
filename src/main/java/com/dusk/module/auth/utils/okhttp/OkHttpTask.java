package com.dusk.module.auth.utils.okhttp;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import com.dusk.common.framework.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author jianjianhong
 * @date 2022/8/1
 */
@Component
@Slf4j
public class OkHttpTask {
    private final static OkHttpClient client = new OkHttpClient();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String dataTask(OkHttpTaskParam param) throws IOException {
        Request request = requestOf(param);
        Response response = client.newCall(request).execute();
        validateResponse(response);
        return response.body().string();
    }

    public String formTask(OkHttpTaskParam param) throws IOException {
        // 构建表单数据
        MultipartBody.Builder builder = new MultipartBody.Builder();
        if(param.getQuery()!=null) {
            param.getQuery().forEach(builder::addFormDataPart);
        }
        MultipartBody requestBody = builder.setType(MultipartBody.FORM).build();
        Request request = new Request.Builder()
                .url(param.getUrl())
                .headers(Headers.of(param.getHeader()))
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        validateResponse(response);
        return response.body().string();
    }

    private Request requestOf(OkHttpTaskParam param) {
        return switch (param.getMethod()) {
            case GET -> getRequest(param);
            case POST -> postRequest(param);
            default -> throw new BusinessException("请求方式不能为空");
        };
    }

    private Request getRequest(OkHttpTaskParam param) {
        StringBuilder urlBuilder = new StringBuilder(param.getUrl());
        Map<String, String> query = param.getQuery();
        Map<String, String> header = param.getHeader();
        if (query != null && !query.isEmpty()) {
            if (!param.getUrl().contains("?")) {
                urlBuilder.append("?");
            }
            query.forEach((key, value) -> {
                urlBuilder.append(key);
                urlBuilder.append("=");
                urlBuilder.append(value);
                urlBuilder.append("&");
            });
        }
        return new Request.Builder()
                .url(urlBuilder.toString())
                .headers(Headers.of(header))
                .build();
    }


    private Request postRequest(OkHttpTaskParam param) {
        Map<String, String> header = param.getHeader();

        RequestBody requestBody = getRequestBody(param);
        return new Request.Builder()
                .url(param.getUrl())
                .headers(Headers.of(header))
                .post(requestBody)
                .build();
    }

    private RequestBody getRequestBody(OkHttpTaskParam param) {
        if (!StringUtils.isEmpty(param.getBody())) {
            return RequestBody.create(JSON, param.getBody());
        }else {
            FormBody.Builder builder = new FormBody.Builder();
            if(param.getQuery()!=null) {
                param.getQuery().forEach((key, value) -> builder.add(key, value));
            }
            return builder.build();
        }
    }

    @SneakyThrows
    private static void validateResponse(Response response) {
        if (response.isSuccessful()) {
            return;
        }

        int httpStatus = response.code();
        if (200 != httpStatus) {

            String msg = null;
            try {
                msg = response.body().string();
            } catch (IOException e) {
                log.error("Http请求异常", e);
                throw new BusinessException("http请求调用异常");
            }

            log.error(msg);
            throw new BusinessException(msg);
        }
    }
}
