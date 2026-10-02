package com.dusk.module.auth.service;

import cn.hutool.extra.mail.MailAccount;
import com.dusk.module.auth.dto.setting.EmailShareLinkHostUrlOutput;

/**
 * @author: pengmengjiang
 * @date: 2021/2/4 14:32
 */
public interface IEmailRpcService {
    /**
     * 发送普通文本邮件
     *
     * @param subject    邮件标题
     * @param content    邮件内容
     * @param recipients 邮件接收者
     */
    void sendEmail(String subject, String content, String... recipients);

    /**
     * 异步发送
     *
     * @param subject
     * @param content
     * @param recipients
     */
    void sendEmailAsync(String subject, String content, String... recipients);


    /**
     * 发送邮件，可选格式内容是否为html格式邮件
     *
     * @param subject    邮件标题
     * @param content    内容
     * @param html       是否html格式
     * @param recipients 接收者
     */
    void sendEmail(String subject, String content, boolean html, String... recipients);

    void sendEmailAsync(String subject, String content, boolean html, String... recipients);

    /**
     * 指定邮箱账户发送普通文本邮件
     *
     * @param account
     * @param subject
     * @param content
     * @param recipients
     */
    void sendEmail(MailAccount account, String subject, String content, String... recipients);

    void sendEmailAsync(MailAccount account, String subject, String content, String... recipients);

    /**
     * 指定邮箱账户发送邮件，可选是否以html格式发送
     *
     * @param account
     * @param subject
     * @param content
     * @param recipients
     */
    void sendEmail(MailAccount account, String subject, String content, boolean html, String... recipients);

    void sendEmailAsync(MailAccount account, String subject, String content, boolean html, String... recipients);

    /**
     * 获取邮件的分享链接的host地址
     *
     * @return
     */
    EmailShareLinkHostUrlOutput getEmailShareLinkHostUrl();

}
