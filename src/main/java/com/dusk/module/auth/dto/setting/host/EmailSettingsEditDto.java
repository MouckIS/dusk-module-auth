package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Getter
@Setter
@Schema(description = "邮箱")
public class EmailSettingsEditDto {
    @Schema(description = "默认发送邮箱地址")
    public String defaultFromAddress;

    @Schema(description = "默认发送人名字")
    public String defaultFromDisplayName;

    @Schema(description = "SMTP服务器")
    public String smtpHost;

    @Schema(description = "SMTP端口", example = "25")
    public int smtpPort;

    @Schema(description = "用户名")
    public String smtpUserName;

    @Schema(description = "密码")
    public String smtpPassword;

    @Schema(description = "域名")
    public String smtpDomain;

    @Schema(description = "使用SSL")
    public boolean smtpEnableSsl;

    @Schema(description = "默认身份验证")
    public boolean smtpUseDefaultCredentials;
}
