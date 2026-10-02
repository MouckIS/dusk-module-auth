package com.dusk.module.auth.service;

/**
 * @author kefuming
 * @date 2023/2/10 14:27
 */
public interface ISmRpcUtil {
    /**
     * 国密4 加密
     *
     * @param data 被加密的String
     * @return 加密后的String 16进制
     */
    String sm4EncryptHex(String data);

    /**
     * 国密4 加密
     *
     * @param data 被加密的String
     * @return 加密后的String Base64
     */
    String sm4EncryptBase64(String data);

    /**
     * 国密4 解密Hex（16进制）或Base64表示的字符串，默认UTF-8编码
     *
     * @param data 被解密的String
     * @return 解密后的String
     */
    String sm4DecryptStr(String data);
}
