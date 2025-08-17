package com.dusk.module.auth.common.datafilter;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-06-17 8:38
 */
public interface IDataFilterDefinitionContext {
    Map<String, List<Long>> getDataFilterDefinition();
    //刷新
    void refresh();
}
