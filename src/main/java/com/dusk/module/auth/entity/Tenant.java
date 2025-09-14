package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.annotation.LogicDelete;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.CreationEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.module.auth.enums.EditionPaymentType;
import com.dusk.module.auth.enums.PaymentPeriodType;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 租户表
 *
 * @author kefuming
 * @date 2020-04-28 9:52
 */
@Entity
@Table(name = "sys_tenant")
@Data
@FieldNameConstants
public class Tenant extends CreationEntity {
    private static final long serialVersionUID = -5505009888989512074L;

    @Column(name = EntityConstant.LAST_MODIFY_ID)
    private Long lastModifyId;

    @Column(name = EntityConstant.LAST_MODIFY_TIME)
    private LocalDateTime lastModifyTime;

    @Version
    @Column(name = EntityConstant.VERSION)
    private int version;

    @LogicDelete
    @Column(name = EntityConstant.DR)
    private int dr;

    /**
     * 是否公用数据库
     */
    private boolean useDefaultDb;

    /**
     * 租户代码
     */
    private String tenantName;

    /**
     * 租户名
     */
    private String name;

    /**
     * 连接url
     */
    private String connUrl;

    /**
     * 数据库帐户名
     */
    private String connUserName;

    /**
     * 数据库密码
     */
    private String connPassword;

    /**
     * 租户是否激活 未激活无法使用
     */
    private boolean active;

    /**
     * 删除者的id
     */
    private String deleterUserId;

    /**
     * 删除时间
     */
    private LocalDateTime deletionTime;

    /**
     * 版本
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "edition_id")
    private SubscribableEdition edition;


    /**
     * logo文件类型
     */
    private String logoFileType;

    /**
     * logo的唯一标识
     */
    private String logoId;

    /**
     * 订阅到期时间（UTC）
     */
    private LocalDateTime subscriptionEndDateUtc;

    /**
     * 描述
     */
    private String description;

    /**
     * app下载地址
     */
    private String appDownloadUrl;


    public boolean HasLogo()
    {
        return StringUtils.isNotBlank(logoId) &&  StringUtils.isNotBlank(logoFileType);
    }

    public void ClearLogo()
    {
        logoId = null;
        logoFileType = null;
    }

    /**
     * 租户是否可用
     * @return
     */
    public boolean enabled(){
        return active && (
                 subscriptionEndDateUtc == null  //无限订阅
                || subscriptionEndDateUtc.isAfter(LocalDateTime.now()) //在订阅时间内
         ) ;
    }

    public Long getEditionId(){
        return edition == null ? null : edition.getId();
    }

    public String getEditionDisplayName(){
        return edition == null ? "" : edition.getDisplayName();
    }

    public void updateSubscriptionDateForPayment(PaymentPeriodType paymentPeriodType, EditionPaymentType editionPaymentType)
    {
        switch (editionPaymentType)
        {
            case NewRegistration, BuyNow -> {
                subscriptionEndDateUtc = LocalDateTime.now().plusDays(paymentPeriodType.getDays());
            }
            case Extend -> {
                extendSubscriptionDate(paymentPeriodType);
            }
            case Upgrade -> {
                if (subscriptionEndDateUtc == null)
                {
                    subscriptionEndDateUtc = LocalDateTime.now().plusDays(paymentPeriodType.getDays());
                }
            }
            default -> {
                throw new IllegalArgumentException();
            }
        }
    }

    private void extendSubscriptionDate(PaymentPeriodType paymentPeriodType)
    {
        if (subscriptionEndDateUtc == null)
        {
            throw new BusinessException("无限订阅不需要续期!");
        }

        if (isSubscriptionEnded())
        {
            subscriptionEndDateUtc = LocalDateTime.now();
        }

        subscriptionEndDateUtc = subscriptionEndDateUtc.plusDays(paymentPeriodType.getDays());
    }

    private boolean isSubscriptionEnded()
    {
        return subscriptionEndDateUtc.isBefore(LocalDateTime.now());
    }

    public long calculateRemainingDayCount()
    {
        if(subscriptionEndDateUtc == null){
            return 0;
        }

        Duration duration = Duration.between(subscriptionEndDateUtc, LocalDateTime.now());
        long remainingDays = duration.toDays();
        return remainingDays > 0 ? remainingDays : 0;
    }

}
