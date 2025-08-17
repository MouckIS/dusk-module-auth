package com.dusk.module.auth.setting.provider;

import com.dusk.common.framework.feature.ui.CheckBox;
import com.dusk.common.framework.feature.ui.SingerLineString;
import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.setting.SettingProvider;
import com.dusk.common.framework.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/7/20 11:43
 */
@Component
public class SafetyOperationSettingProvider extends SettingProvider {
    public static final String SAFE_MANAGEMENT_OPERATION = "Safe.Management.Operaton";
    public static final String SAFE_MANAGEMENT_IS_OPERATON = "Safe.Management.Is.SafeOperaion";
    public static final String SAFE_MANAGEMENT_PREVIEW_TIME = "Safe.Management.PreviewTime";
    
    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        ArrayList<SettingDefinition> list = new ArrayList<>();
        
        list.add(new SettingDefinition.Builder(SAFE_MANAGEMENT_OPERATION, "").displayName("安全运行").scopes(SettingScopes.Tenant).inputType(null).build());
        list.add(new SettingDefinition.Builder(SAFE_MANAGEMENT_IS_OPERATON, "").displayName("是否启动安全运行时间").scopes(SettingScopes.Tenant).parent(SAFE_MANAGEMENT_OPERATION)
                .inputType(new CheckBox()).build());
        list.add(new SettingDefinition.Builder(SAFE_MANAGEMENT_PREVIEW_TIME, "2020/01/01 08:00").displayName("安全运行初始时间").scopes(SettingScopes.Tenant).parent(SAFE_MANAGEMENT_IS_OPERATON)
                .inputType(new SingerLineString()).build());
        return list;
    }
}
