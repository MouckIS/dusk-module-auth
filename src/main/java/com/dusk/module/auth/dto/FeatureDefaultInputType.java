package com.dusk.module.ddm.dto;

import com.dusk.module.ddm.dto.ui.Item;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 特性输入默认公共值
 *
 * @author kefuming
 * @date 2021-07-26 8:52
 */
@UtilityClass
public class FeatureDefaultInputType {
    public final static List<Item> DEFAULT_APP_ITEM_LIST = new ArrayList<>();

    static {
        DEFAULT_APP_ITEM_LIST.add(new Item("false", "显示在更多页面"));
        DEFAULT_APP_ITEM_LIST.add(new Item("true", "显示在底部菜单"));
    }
}
