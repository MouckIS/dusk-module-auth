package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.annotation.LogicDelete;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.CreationEntity;
import com.dusk.module.auth.enums.PaymentPeriodType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-08 8:39
 */
@Entity
@Table(name = "sys_edition")
@Data
@FieldNameConstants
public class SubscribableEdition extends CreationEntity {

    /**
     * 名称
     */
    @Column(length = 32)
    private String name;

    /**
     * 显示的名称
     */
    @NotBlank(message = "显示的名称不能为空")
    @Column(length = 64)
    private String displayName;

    /**
     * The edition that will assigned after expire date
     */
    private Long expiringEditionId;

    /**
     * 每月的价格
     */
    private BigDecimal monthlyPrice;

    /**
     * 每年的价格
     */
    private BigDecimal annualPrice;

    /**
     *试用天数
     */
    private Integer trialDayCount;

    /**
     * The account will be taken an action (termination of tenant account) after the specified days when the subscription is expired.
     */
    private Integer waitingDayAfterExpire;

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

    public boolean isFree(){
        return (monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) == 0)
               && (annualPrice == null || annualPrice.compareTo(BigDecimal.ZERO) == 0);
    }

    public boolean hasTrial()
    {
        if (isFree())
        {
            return false;
        }
        return trialDayCount != null && trialDayCount > 0;
    }

    public BigDecimal getPaymentAmount(PaymentPeriodType paymentPeriodType) throws Exception {
        switch (paymentPeriodType){
            case Monthly -> {
                if (monthlyPrice == null)
                {
                    throw new Exception("No price information found for " + displayName + " edition!");
                }
                return monthlyPrice;
            }
            case Annual -> {
                if (annualPrice == null)
                {
                    throw new Exception("No price information found for " + displayName + " edition!");
                }
                return annualPrice;
            }
        }

        throw new Exception("Edition does not support payment type: " + paymentPeriodType);
    }

    public boolean hasSamePrice(SubscribableEdition edition)
    {
        return !isFree() && monthlyPrice.equals(edition.getMonthlyPrice()) && annualPrice.equals(edition.getAnnualPrice());
    }

}
