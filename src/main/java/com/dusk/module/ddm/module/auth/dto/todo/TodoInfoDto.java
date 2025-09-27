package com.dusk.module.ddm.module.auth.dto.todo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.Column;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-08-05 9:49
 */
@Getter
@Setter
@ApiModel
@FieldNameConstants
public class TodoInfoDto {

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("类型编码")
    private String type;
    @ApiModelProperty("类型名称")
    private String typeName;
    //待办标题，不要超过255字符
    @ApiModelProperty("待办标题")
    private String title;

    //业务状态位
    @ApiModelProperty("业务状态")
    private String state;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 发起人名字
     */
    @ApiModelProperty("发起人")
    private String starter;

    /**
     * 上一提交人
     */
    @ApiModelProperty("上一提交人")
    private String preHandler;


    //关联业务id
    @ApiModelProperty("关联业务id")
    private String businessId;

    //拓展字段
    @ApiModelProperty("拓展字段")
    private String extensions;

    @ApiModelProperty("已读")
    private Boolean hasRead;

    @ApiModelProperty("子类型编码")
    private String subType;

    @ApiModelProperty("子类型业务id")
    private String subBusinessId;
}
