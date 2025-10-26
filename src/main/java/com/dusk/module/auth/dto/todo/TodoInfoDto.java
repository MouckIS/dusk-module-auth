package com.dusk.module.auth.dto.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-08-05 9:49
 */
@Getter
@Setter
@Schema
@FieldNameConstants
public class TodoInfoDto {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "类型编码")
    private String type;
    @Schema(description = "类型名称")
    private String typeName;
    //待办标题，不要超过255字符
    @Schema(description = "待办标题")
    private String title;

    //业务状态位
    @Schema(description = "业务状态")
    private String state;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 发起人名字
     */
    @Schema(description = "发起人")
    private String starter;

    /**
     * 上一提交人
     */
    @Schema(description = "上一提交人")
    private String preHandler;


    //关联业务id
    @Schema(description = "关联业务id")
    private String businessId;

    //拓展字段
    @Schema(description = "拓展字段")
    private String extensions;

    @Schema(description = "已读")
    private Boolean hasRead;

    @Schema(description = "子类型编码")
    private String subType;

    @Schema(description = "子类型业务id")
    private String subBusinessId;
}
