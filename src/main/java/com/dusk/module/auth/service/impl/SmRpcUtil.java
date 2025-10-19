package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import com.dusk.common.rpc.auth.service.ISmRpcUtil;
import com.dusk.module.auth.common.config.AppAuthConfig;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.Service;

/**
 * @author kefuming
 * @date 2023/2/10 14:29
 */
@Service
public class SmRpcUtil implements ISmRpcUtil {
    @Resource
    private AppAuthConfig appAuthConfig;

    @Override
    public String sm4EncryptHex(String data) {
        return SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).encryptHex(data);
    }

    @Override
    public String sm4EncryptBase64(String data) {
        return SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).encryptBase64(data);
    }

    @Override
    public String sm4DecryptStr(String data) {
        return SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).decryptStr(data);
    }
}
