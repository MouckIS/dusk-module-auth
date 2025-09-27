package com.dusk.module.ddm.module.auth.dto.todo;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;

/**
 * @author kefuming
 * @date 2020-10-19 8:34
 */
@Data
@AllArgsConstructor
public class ToDoMQTTContent {
    private TodoInfoDto todo;
    private ToDoMQTTTypeEnum mqttType;
}
