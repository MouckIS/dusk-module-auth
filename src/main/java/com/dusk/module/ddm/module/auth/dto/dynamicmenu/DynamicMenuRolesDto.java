package com.dusk.module.ddm.module.auth.dto.dynamicmenu;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.module.auth.dto.dynamicmenu.DynamicLinkRoles;
import com.dusk.common.module.auth.enums.DynamicMenuOpenType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author jianjianhong
 * @date 2023/3/14 下午4:54
 */
@Getter
@Setter
public class DynamicMenuRolesDto extends EntityDto {
    //菜单名
    @ApiModelProperty("菜单名")
    private String name;
    //类型 前端用于判断  常见 动态报表、低代码、其他 这边不定义枚举得原因是因为对于auth来说无意义
    @ApiModelProperty("类型")
    private String dynamicType;
    //标识 前端根据这个字段以及类型动态组装路由，第三方得应该直接跳转即可
    @ApiModelProperty("标识")
    private String identify;
    //来源key 唯一标识 用于区分菜单从哪里来的 这里搞个索引
    @ApiModelProperty("来源key")
    private String businessKey;
    //角色id
    @ApiModelProperty("角色列表")
    private List<DynamicLinkRoles> roleList;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("打开方式")
    @Enumerated(EnumType.STRING)
    private DynamicMenuOpenType type;
}
