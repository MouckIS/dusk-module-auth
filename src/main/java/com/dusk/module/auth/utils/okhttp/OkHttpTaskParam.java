package com.dusk.module.auth.utils.okhttp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jianjianhong
 * @date 2022/8/1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OkHttpTaskParam {
    private String url;
    private Method method;
    private Map<String, String> query;
    private String body;
    private Map<String, String> header = new HashMap<>();
    private MediaType type;
}

