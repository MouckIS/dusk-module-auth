package com.dusk.module.ddm.module.auth.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.logging.log4j.util.Strings;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.setting.ISettingChecker;
import com.dusk.common.module.auth.dto.setting.EmailShareLinkHostUrlOutput;
import com.dusk.common.module.auth.service.IEmailRpcService;
import com.dusk.module.auth.service.IEmailService;
import com.dusk.module.auth.setting.provider.EmailSettingProvider;
import com.dusk.module.auth.setting.provider.HostSettingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;


/**
 * @author: pengmengjiang
 * @date: 2021/2/4 11:25
 */
@Slf4j
@Service
public class EmailServiceImpl implements IEmailRpcService, IEmailService {

    @Autowired
    private ISettingChecker settingChecker;


    @Override
    public void sendEmail(String subject, String content, String... recipients) {
        sendEmail(subject, content, false, recipients);
    }

    @Override
    @Async
    public void sendEmailAsync(String subject, String content, String... recipients) {
        sendEmail(subject, content, recipients);
    }

    @Override
    public void sendEmail(String subject, String content, boolean html, String... recipients) {
        sendEmail(getTenantMailAccount(), subject, content, html, recipients);
    }

    @Override
    @Async
    public void sendEmailAsync(String subject, String content, boolean html, String... recipients) {
        sendEmail(subject, content, html, recipients);
    }


    /**
     * 获取邮件的分享链接的host地址
     *
     * @return
     */
    @Override
    public EmailShareLinkHostUrlOutput getEmailShareLinkHostUrl() {
        EmailShareLinkHostUrlOutput output = new EmailShareLinkHostUrlOutput();

        String domain = settingChecker.getValue(HostSettingProvider.HOST_DOMAIN);
        if (Strings.isBlank(domain)) {
            log.warn("域名信息未配置");
        } else {
            output.setDomain(domain);
        }
        String isHttpsEnabled = settingChecker.getValue(HostSettingProvider.HOST_SCHEMA);
        output.setHttpsEnabled(Boolean.parseBoolean(isHttpsEnabled));

        return output;
    }

    MailAccount getTenantMailAccount() {
        String host = settingChecker.getValue(EmailSettingProvider.SMTP_HOST);
        Assert.isTrue(StringUtils.isNotBlank(host), "未设置邮件服务器");

        String portStr = settingChecker.getValue(EmailSettingProvider.SMTP_PORT);
        Assert.isTrue(StringUtils.isNotBlank(portStr), "未设置邮箱服务器端口号");

        String userName = settingChecker.getValue(EmailSettingProvider.SMTP_USER_NAME);
        Assert.isTrue(StringUtils.isNotBlank(userName), "未设置邮箱用户名");

        String pass = settingChecker.getValue(EmailSettingProvider.SMTP_PASSWORD);
        Assert.isTrue(StringUtils.isNotBlank(pass), "未设置邮箱密码");

        MailAccount mailAccount = new MailAccount();
        mailAccount.setHost(host);
        try {
            Integer port = Integer.parseInt(portStr);
            mailAccount.setPort(port);
        } catch (NumberFormatException e) {
            throw new BusinessException("邮箱服务器端口号不是数字");
        }

        String displayName = settingChecker.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_DISPLAY_NAME);
        String fromAddress = settingChecker.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_ADDRESS);
        if (StringUtils.isNotBlank(displayName)) {
            mailAccount.setFrom(displayName + " <" + fromAddress + ">");
        } else {
            mailAccount.setFrom(fromAddress);
        }
        mailAccount.setUser(userName);
        mailAccount.setPass(pass);
        mailAccount.setSslEnable(Boolean.parseBoolean(settingChecker.getValue(EmailSettingProvider.SMTP_ENABLE_SSL)));
        mailAccount.setAuth(Boolean.parseBoolean(settingChecker.getValue(EmailSettingProvider.SMTP_USE_DEFAULT_CREDENTIALS)));

        return mailAccount;
    }

    @Override
    public void sendEmail(MailAccount account, String subject, String content, String... recipients) {
        sendEmail(account, subject, content, false, recipients);
    }

    @Override
    @Async
    public void sendEmailAsync(MailAccount account, String subject, String content, String... recipients) {
        sendEmail(account, subject, content, recipients);
    }

    @Override
    public void sendEmail(MailAccount account, String subject, String content, boolean html, String... recipients) {
        MailUtil.send(account, StringUtils.join(recipients, ";"), subject, content, html);
    }

    @Override
    @Async
    public void sendEmailAsync(MailAccount account, String subject, String content, boolean html, String... recipients) {
        sendEmail(account, subject, content, html, recipients);
    }
}
