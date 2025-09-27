package com.dusk.module.ddm.module.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CancelAuthDto implements Serializable {
    private Long tenantId;
    private List<Long> userIds;
}
