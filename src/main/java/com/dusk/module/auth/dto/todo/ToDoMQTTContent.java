package com.dusk.module.auth.dto.todo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;

/**
 * @author kefuming
 * @date 2020-10-19 8:34
 */
@Getter
@Setter
@AllArgsConstructor
public class ToDoMQTTContent {
    private TodoInfoDto todo;
    private ToDoMQTTTypeEnum mqttType;
}
