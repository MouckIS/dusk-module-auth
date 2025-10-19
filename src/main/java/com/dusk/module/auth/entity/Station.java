package com.dusk.module.auth.entity;

import com.dusk.common.core.entity.TreeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/9/21 9:23
 */
@Entity
@Table(name = "sys_station")
@Getter
@Setter
@FieldNameConstants
public class Station extends TreeEntity {

    /**
     * 厂站关联的用户
     */
    @ManyToMany
    @JoinTable(name = "sys_station_user", joinColumns = { @JoinColumn(name = "station_id") }, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private List<User> users = new ArrayList<>();
}
