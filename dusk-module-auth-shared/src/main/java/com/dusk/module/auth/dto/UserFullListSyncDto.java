package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 同步的用户信息
 *
 * @Author 喻黎洋
 * @Date 2020/11/9 11:11
 */
@Getter
@Setter
public class UserFullListSyncDto extends UserFullListDto {

    /**
     * 厂站列表
     */
    @Schema(description = "厂站列表")
    private List<Long> orgIds;

}
