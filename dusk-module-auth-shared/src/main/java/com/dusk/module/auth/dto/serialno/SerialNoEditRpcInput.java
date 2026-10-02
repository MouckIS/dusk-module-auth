package com.dusk.module.auth.dto.serialno;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2022/12/05 8:37
 */
@Getter
@Setter
public class SerialNoEditRpcInput implements Serializable {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "当前序号")
    private long currentNo;

    @Schema(description = "最后一次的序列号")
    private String lastNo;
}
