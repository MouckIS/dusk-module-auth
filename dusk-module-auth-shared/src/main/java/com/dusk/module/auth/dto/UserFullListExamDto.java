package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class UserFullListExamDto extends UserFullListDto {

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "出生年月")
    private LocalDate birthDate;

    @Schema(description = "参加工作时间")
    private LocalDate startingWorkDate;

    @Schema(description = "最高学历")
    private String educationalBackground;

    @Schema(description = "职称")
    private String jobTitle;
}
