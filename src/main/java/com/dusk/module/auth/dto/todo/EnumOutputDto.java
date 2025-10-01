package com.dusk.module.auth.dto.todo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import com.dusk.common.core.dto.SelectListOutputDto;
import com.dusk.common.core.utils.EnumUtils;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-11 11:12
 */
@Getter
public class EnumOutputDto {
    @ApiModelProperty("代办MQTT消息类型")
    private final List<SelectListOutputDto> toDoMQTTTypeEnumList = EnumUtils.ConvertToList(ToDoMQTTTypeEnum.class);


}
