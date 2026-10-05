package com.dusk.module.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Author: pengmengjiang
 * @Date: 2022/8/11 16:51
 */
@Getter
@Setter
public class OpenAppAuthResult implements Serializable {

    private Long tenantId;

    private Long orgId;

    private boolean success;

    private String message;

    public OpenAppAuthResult() {
    }

    public OpenAppAuthResult(String message) {
        this.message = message;
    }

    public OpenAppAuthResult(Long tenantId, Long orgId) {
        this.tenantId = tenantId;
        this.orgId = orgId;
    }



    public static OpenAppAuthResult success(Long tenantId, Long orgId){
        OpenAppAuthResult result = new OpenAppAuthResult(tenantId,orgId);
        result.setSuccess(true);
        return result;
    }

    public static OpenAppAuthResult fail(String message){
        return new OpenAppAuthResult(message);
    }
}
