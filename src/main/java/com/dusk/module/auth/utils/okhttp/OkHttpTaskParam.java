package com.dusk.module.auth.utils.okhttp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jianjianhong
 * @date 2022/8/1
 */
@Getter
@Setter
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

