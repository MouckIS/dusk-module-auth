package com.dusk.module.auth.controller;

import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.todo.EnumOutputDto;
import com.dusk.module.auth.dto.todo.GetTodosInput;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.service.IToDoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * @author kefuming
 * @date 2020-08-05 9:52
 */
@RestController
@RequestMapping("/todo")
@Tag(name = "ToDo", description = "待办")
public class TodoController extends CruxBaseController {
    @Resource
    private IToDoService toDoService;

    @PostMapping("/getTodos")
    @Operation(summary = "查询待办清单")
    public PagedResultDto<TodoInfoDto> getTodos(@RequestBody GetTodosInput input) {
        Page<TodoInfoDto> todos = toDoService.getTodos(input);
        return new PagedResultDto<>(todos.getTotalElements(),todos.getContent());
    }

    @PostMapping("/ignore/{id}")
    @Operation(summary = "忽略待办")
    public void ignore(@PathVariable Long id) {
        toDoService.ignoreTodo(id);
    }

    @Operation(summary = "获取代办的枚举类型")
    @GetMapping("getTodoEnums")
    public EnumOutputDto getDeviceEnums() {
        return new EnumOutputDto();
    }

    @PostMapping("/read/{id}")
    @Operation(summary = "打开待办")
    public void read(@PathVariable Long id){
        toDoService.read(id);
    }

}
