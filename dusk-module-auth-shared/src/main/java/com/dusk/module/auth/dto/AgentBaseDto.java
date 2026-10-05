package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author pengjian
 * @date 2024/10/25 12:33
 */
@Getter
@Setter
public class AgentBaseDto extends EntityDto {
    @Schema(description = "委托人id")
    private Long userId;
    @Schema(description = "被委托人id")
    private Long agentUserId;
    @Schema(description = "委托说明")
    private String agentExplain;
    @Schema(description = "委托开始时间")
    private LocalDateTime startTime;
    @Schema(description = "委托结束时间")
    private LocalDateTime endTime;
    @Schema(description = "代理开启状态")
    private Boolean agentOpen;
}
