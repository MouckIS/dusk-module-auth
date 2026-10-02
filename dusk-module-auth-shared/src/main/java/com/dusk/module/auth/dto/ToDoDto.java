package com.dusk.module.auth.dto;

import com.dusk.common.mqs.enums.NoticationLevel;
import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.module.auth.enums.ToDoTargetType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * type+businessId作为一组唯一业务类型数据
 *
 * @author 王吉
 * @date 2020-08-04 14:24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ToDoDto implements Serializable {
    @NotEmpty(message = "待办类型编码不能为空")
    private String type;
    //类型 名称 用于前端显示
    @NotEmpty(message = "待办类型名称不能为空")
    private String typeName;
    //待办标题，不要超过255字符
    @NotEmpty(message = "标题不能为空")
    private String title;

    //业务状态位
    @Size(max = 200, message = "状态过长")
    private String state;

    @NotNull(message = "待办目标主体类型不能为空")
    private ToDoTargetType targetType;
    @NotNull(message = "待办目标主体数据不能为空")
    private String[] targetData;

    //关联业务id
    @NotEmpty(message = "待办业务id不能为空")
    private String businessId;

    //子业务类型
    private String subType;
    //子业务id
    private String subBusinessId;

    /**
     * 是否过滤场站，如果你的业务是根据场站过滤的，该字段设为true
     */
    private boolean filterStation;

    //拓展字段 2000长度
    @Size(max = 2000, message = "拓展字段长度过长")
    private String extensions;


    //以下是手机顶部推送配置
    /**
     * 是否自动推送手机顶部消息。默认不推送
     */
    private boolean autoAppPush = false;

    /**
     * 推送类型，默认是notice
     */
    private PushType pushType = PushType.NOTICE;

    /**
     * app推送标题，为空则用待办的类型
     */
    private String appTitle;

    /**
     * app推送正文，为空则用待办的title
     */
    private String appBody;

    /**
     * 推送级别 默认是info
     */
    private NoticationLevel noticationLevel = NoticationLevel.INFO;

    /**
     * 顶部推送导航跳转参数
     */
    private Navigation navigation;

    /**
     * 发起人名字 不填则默认填充登录人姓名
     */
    private String starter;

    /**
     * 上一提交人
     */
    private String preHandler;

    /**
     * 第三方应用appcode
     */
    private String appcode;



    public ToDoDto(@NotEmpty(message = "待办类型编码不能为空") String type, @NotEmpty(message = "待办类型名称不能为空") String typeName, @NotEmpty(message = "标题不能为空") @Size(max = 200, message = "标题过长") String title, @Size(max = 200, message = "状态过长") String state, @NotNull(message = "待办目标主体类型不能为空") ToDoTargetType targetType, @NotNull(message = "待办目标主体数据不能为空") String[] targetData, @NotEmpty(message = "待办业务id不能为空") String businessId, @Size(max = 2000, message = "拓展字段长度过长") String extensions) {
        this.type = type;
        this.typeName = typeName;
        this.title = title;
        this.state = state;
        this.targetType = targetType;
        this.targetData = targetData;
        this.businessId = businessId;
        this.extensions = extensions;
    }

    public ToDoDto(@NotEmpty(message = "待办类型编码不能为空") String type, @NotEmpty(message = "待办类型名称不能为空") String typeName, @NotEmpty(message = "标题不能为空") @Size(max = 200, message = "标题过长") String title, @Size(max = 200, message = "状态过长") String state, @NotNull(message = "待办目标主体类型不能为空") ToDoTargetType targetType, @NotNull(message = "待办目标主体数据不能为空") String[] targetData, @NotEmpty(message = "待办业务id不能为空") String businessId, boolean filterStation, @Size(max = 2000, message = "拓展字段长度过长") String extensions) {
        this.type = type;
        this.typeName = typeName;
        this.title = title;
        this.state = state;
        this.targetType = targetType;
        this.targetData = targetData;
        this.businessId = businessId;
        this.extensions = extensions;
        this.filterStation = filterStation;
    }

    public ToDoDto(@NotEmpty(message = "待办类型编码不能为空") String type, @NotEmpty(message = "待办类型名称不能为空") String typeName, @NotEmpty(message = "标题不能为空") @Size(max = 200, message = "标题过长") String title, @Size(max = 200, message = "状态过长") String state, @NotNull(message = "待办目标主体类型不能为空") ToDoTargetType targetType, @NotNull(message = "待办目标主体数据不能为空") String[] targetData, @NotEmpty(message = "待办业务id不能为空") String businessId, boolean filterStation, @Size(max = 2000, message = "拓展字段长度过长") String extensions, boolean autoAppPush) {
        this.type = type;
        this.typeName = typeName;
        this.title = title;
        this.state = state;
        this.targetType = targetType;
        this.targetData = targetData;
        this.businessId = businessId;
        this.extensions = extensions;
        this.filterStation = filterStation;
        this.autoAppPush = autoAppPush;
    }

    public ToDoDto(@NotEmpty(message = "待办类型编码不能为空") String type, @NotEmpty(message = "待办类型名称不能为空") String typeName, @NotEmpty(message = "标题不能为空") @Size(max = 200, message = "标题过长") String title,
                   @Size(max = 200, message = "状态过长") String state, @NotNull(message = "待办目标主体类型不能为空") ToDoTargetType targetType, @NotNull(message = "待办目标主体数据不能为空") String[] targetData,
                   @NotEmpty(message = "待办业务id不能为空") String businessId, boolean filterStation, @Size(max = 2000, message = "拓展字段长度过长") String extensions,
                   boolean autoAppPush,
                   PushType pushType,
                   String appTitle,
                   String appBody,
                   NoticationLevel noticationLevel,
                   Navigation navigation) {
        this.type = type;
        this.typeName = typeName;
        this.title = title;
        this.state = state;
        this.targetType = targetType;
        this.targetData = targetData;
        this.businessId = businessId;
        this.extensions = extensions;
        this.filterStation = filterStation;
        this.autoAppPush = autoAppPush;
        this.pushType = pushType;
        this.appTitle = appTitle;
        this.appBody = appBody;
        this.noticationLevel = noticationLevel;
        this.navigation = navigation;
    }
}
