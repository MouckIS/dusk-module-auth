package com.dusk.module.auth.controller;

import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.module.auth.dto.todo.EnumOutputDto;
import com.dusk.module.auth.dto.todo.GetTodosInput;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.service.IToDoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * @author kefuming
 * @date 2020-08-05 9:52
 */
@RestController
@RequestMapping("/todo")
@Api(tags = "ToDo", description = "待办")
public class TodoController extends CruxBaseController {
    @Autowired
    IToDoService toDoService;
    @Autowired
    Mapper dozerMapper;

    @PostMapping("/getTodos")
    @ApiOperation(value = "查询待办清单")
    public PagedResultDto<TodoInfoDto> getTodos(@RequestBody GetTodosInput input) {
        Page<TodoInfoDto> todos = toDoService.getTodos(input);
        return new PagedResultDto<>(todos.getTotalElements(),todos.getContent());
    }

    @PostMapping("/ignore/{id}")
    @ApiOperation(value = "忽略待办")
    public void ignore(@PathVariable Long id) {
        toDoService.ignoreTodo(id);
    }

    @ApiOperation("获取代办的枚举类型")
    @GetMapping("getTodoEnums")
    public EnumOutputDto getDeviceEnums() {
        return new EnumOutputDto();
    }

    @PostMapping("/read/{id}")
    @ApiOperation(value = "打开待办")
    public void read(@PathVariable Long id){
        toDoService.read(id);
    }

}
