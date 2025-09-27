package com.dusk.module.ddm.module.auth.dto.fingerprint;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2021-05-12 8:19
 */
@Data
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class RegisterFingerprintStartPayload implements Serializable {
    public int UserId;
}
