package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.ToDoDto;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.entity.Todo;
import org.mapstruct.Mapper;

@Mapper
public interface TodoMapper {
    TodoMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(TodoMapper.class);

    Todo toEntity(ToDoDto dto);

    TodoInfoDto toInfoDto(Todo todo);


}
