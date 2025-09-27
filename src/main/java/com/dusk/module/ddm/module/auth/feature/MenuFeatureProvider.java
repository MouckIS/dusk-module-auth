package com.dusk.module.ddm.module.auth.feature;

import com.dusk.common.core.feature.IFeatureDefinitionContext;
import com.dusk.common.core.feature.impl.FeatureProvider;
import com.dusk.common.core.feature.ui.CheckBox;
import com.dusk.common.core.feature.ui.ComboBox;
import com.dusk.common.core.feature.ui.Item;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态添加特性示例，仿ABP 不注入 用于查看测试代码
 *
 * @author kefuming
 * @date 2020/5/12 14:13
 */
@Component
public class MenuFeatureProvider extends FeatureProvider {
    public static final String APP_MENU = "App.Menu";
    public static final String APP_MENU_HOME_WEB = "App.Menu.Home.Web";
    public static final String APP_MENU_HOME_APP = "App.Menu.Home.App";
    public static final String APP_ZNYD_NB_LOCK = "App.Znyd.NBLock";
    public static final List<Item> HOME_ITEM_LIST = new ArrayList<>();
    public static final List<Item> HOME_APP_ITEM_LIST = new ArrayList<>();

    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        // 设置下拉框选项
        setItemList();

        context.create(APP_MENU, "true", "自定义首页");
        context.createChildren(APP_MENU_HOME_WEB, "default", "web首页配置", "", new ComboBox(HOME_ITEM_LIST));
        context.createChildren(APP_MENU_HOME_APP, "default", "app首页配置", "", new ComboBox(HOME_APP_ITEM_LIST));
        context.createChildren(APP_ZNYD_NB_LOCK, "true", "Znyd首页配置", "", new CheckBox());

        /*
        //create方法创建或者定位到一个特性；createChildren创建子特性，必须跟在create方法后面
        context.create("App.Menu","true","能否获取菜单");
        context.createChildren("App.Menu.Home","true","能否获取首页菜单","",new ComboBox(this.DEFAULT_ITEM_LIST));
        context.createChildren("App.Menu.Ticket","false","票业务","",new CheckBox());
        context.createChildren("App.Menu.Ticket2","false","票业务2","",new SingerLineString());

        //create方法创建或者重新定位到一个特性；自定义控件
        context.create("App.Fuc1","true","功能1");
        //自定义下拉框
        List<Item> defItemList = new ArrayList<>();
        defItemList.add(new Item("false", "显示在更多页面"));
        defItemList.add(new Item("true", "显示在底部菜单"));
        context.createChildren("App.Fuc1.Ticket","false","票业务","",new ComboBox(defItemList));*/
    }

    private void setItemList() {
        HOME_ITEM_LIST.add(new Item("default", "默认首页"));
        HOME_ITEM_LIST.add(new Item("patrol", "巡检首页"));
        HOME_ITEM_LIST.add(new Item("znyd", "智能用电首页"));
        HOME_ITEM_LIST.add(new Item("guandi", "官地首页"));
        HOME_ITEM_LIST.add(new Item("gridMenu", "宫格样菜单"));
        HOME_ITEM_LIST.add(new Item("utplace", "优特汇首页"));
        HOME_ITEM_LIST.add(new Item("generate", "发电首页"));
        HOME_ITEM_LIST.add(new Item("dhxt", "动环系统首页(优特汇)"));

        HOME_APP_ITEM_LIST.add(new Item("default", "默认首页"));
        HOME_APP_ITEM_LIST.add(new Item("znyd", "智能用电首页"));
        HOME_APP_ITEM_LIST.add(new Item("whj", "微环境首页"));
    }
}
