package com.dusk.module.ddm.module.auth.syscode.treefix;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.common.core.service.ITreeService;
import com.dusk.common.tree.TreeFix;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.service.IOrganizationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-09-29 15:24
 */
@Component
public class OrganizationUnitFix extends TreeFix<OrganizationUnit> {
    @Autowired
    private IOrganizationUnitService organizationUnitService;

    @Override
    protected <K extends IBaseRepository<OrganizationUnit>> ITreeService<OrganizationUnit, K> getTreeService() {
        return (ITreeService<OrganizationUnit, K>) organizationUnitService;
    }
}
