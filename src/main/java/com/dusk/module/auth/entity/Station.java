package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.TreeEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/9/21 9:23
 */
@Entity
@Table(name = "sys_station")
@Data
@FieldNameConstants
public class Station extends TreeEntity {

    /**
     * 厂站关联的用户
     */
    @ManyToMany
    @JoinTable(name = "sys_station_user", joinColumns = { @JoinColumn(name = "station_id") }, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private List<User> users = new ArrayList<>();
}
