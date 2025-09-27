package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Data
@ApiModel("邮箱")
public class EmailSettingsEditDto {
    @ApiModelProperty("默认发送邮箱地址")
    public String defaultFromAddress;

    @ApiModelProperty("默认发送人名字")
    public String defaultFromDisplayName;

    @ApiModelProperty("SMTP服务器")
    public String smtpHost;

    @ApiModelProperty(value = "SMTP端口", example = "25")
    public int smtpPort;

    @ApiModelProperty("用户名")
    public String smtpUserName;

    @ApiModelProperty("密码")
    public String smtpPassword;

    @ApiModelProperty("域名")
    public String smtpDomain;

    @ApiModelProperty("使用SSL")
    public boolean smtpEnableSsl;

    @ApiModelProperty("默认身份验证")
    public boolean smtpUseDefaultCredentials;
}
