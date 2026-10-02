package com.dusk.module.auth.dto.setting;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 站点的配置
 *
 * @Author 喻黎洋
 * @Date 2022/1/12 11:22
 */
@Getter
@Setter
public class EmailShareLinkHostUrlOutput implements Serializable {

    /**
     * 域名
     */
    private String domain;

    /**
     * 是否启用了https
     */
    private boolean httpsEnabled;

    /**
     * 获取完成的访问域名地址
     *
     * @return
     */
    public String getHostUrl() {
        String schema = this.httpsEnabled ? "https://" : "http://";
        return schema + this.domain;
    }
}
