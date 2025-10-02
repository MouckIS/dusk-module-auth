package com.dusk.module.auth.syscode;

import com.dusk.module.ddm.context.ISysCodeDefinitionContext;
import com.dusk.module.ddm.provider.SysCodeProvider;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-08-19 10:05
 */
@Component
public class AuthSysCodeProvider extends SysCodeProvider {
    @Override
    public void setSysCodes(ISysCodeDefinitionContext context) {
        context.add("threeDimensionalJobManagement", "3D-3D作业管理");
        context.add("threeDimensionalInternalPersonnelManagement", "3D-3D内部人员管理");
    }
}
