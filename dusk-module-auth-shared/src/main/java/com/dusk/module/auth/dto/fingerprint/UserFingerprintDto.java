package com.dusk.module.auth.dto.fingerprint;

import com.dusk.common.core.dto.VersionDto;
import com.dusk.module.auth.enums.FingerprintFromEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-15 8:43
 */
@Getter
@Setter
public class UserFingerprintDto extends VersionDto {
    //用户id
    private Long userId;
    //用户序列
    private Integer userSeq;
    //指纹名称n
    private String name;
    //指纹数据
    private String data;
    //指纹数据大小
    private Integer size;
    //指纹来源
    private FingerprintFromEnum fromEnum;
}

