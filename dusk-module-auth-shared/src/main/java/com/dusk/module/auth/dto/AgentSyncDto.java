package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author pengjian
 * @date 2024/10/25 12:33
 */
@Getter
@Setter
public class AgentSyncDto extends AgentBaseDto {
    @Schema(description = "委托人账号")
    private String userName;

    @Schema(description = "被委托人账号")
    private String agentUserName;
}
