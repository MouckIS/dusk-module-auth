package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.dto.tenant.*;
import com.github.dozermapper.core.Mapper;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.CreationEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.tenant.*;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ISubscribableEditionService;
import com.dusk.module.auth.service.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * @author kefuming
 * @date 2020-04-28 10:06
 */
@Service
@Transactional
public class TenantServiceImpl extends BaseService<Tenant, ITenantRepository> implements ITenantService {
    @Autowired
    private IUserRepository iUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ISubscribableEditionService editionService;
    @Autowired
    IAuthPermissionManager permissionManager;

    @Override
    public Tenant createTenantWithDefaultSettings(CreateTenantInput input) {
        if (notUniqueTenantName(null, input.getTenantName())) {
            throw new BusinessException("租户代码[" + input.getName() + "]已存在，请重新输入！");
        }

        Tenant tenant = addTenant(input);
        User user = createAdminUser(input, tenant.getId());
        if (input.isSendActivationEmail()) {
            sendActivationEmail(user);
        }
        permissionManager.refreshAll();
        return tenant;
    }

    private void sendActivationEmail(User user) {
        //TODO: 发送激活邮件
    }

    @Override
    public void updateTenant(TenantEditDto tenantEditDto) {
        Tenant tenant = repository.findById(tenantEditDto.getId()).orElseThrow(() -> new BusinessException("找不到相应的租户信息！"));

        if (notUniqueTenantName(tenantEditDto.getId(), tenantEditDto.getTenantName())) {
            throw new BusinessException("租户代码[" + tenantEditDto.getName() + "]已存在，请重新输入！");
        }
        SubscribableEdition edition = checkEdition(tenantEditDto.getEditionId(), tenantEditDto.isInTrialPeriod());

        dozerMapper.map(tenantEditDto, tenant);
        tenant.setEdition(edition);
        permissionManager.refreshAll();
        repository.save(tenant);
    }

    @Override
    public Page<Tenant> getTenants(GetTenantsInput input) {
        Specification<Tenant> spec = Specifications.where(e -> {
            if (StringUtils.isNotBlank(input.getFilter())) {
                e.or(e2 -> e2.contains(Tenant.Fields.name, input.getFilter())
                        .contains(Tenant.Fields.tenantName, input.getFilter()));
            }
            e.ge(input.getCreationDateStart() != null, CreationEntity.Fields.createTime, input.getCreationDateStart())
                    .le(input.getCreationDateEnd() != null, CreationEntity.Fields.createTime, input.getCreationDateEnd())
                    .ge(input.getSubscriptionEndDateStart() != null, Tenant.Fields.subscriptionEndDateUtc, input.getSubscriptionEndDateStart())
                    .le(input.getSubscriptionEndDateEnd() != null, Tenant.Fields.subscriptionEndDateUtc, input.getSubscriptionEndDateEnd())
                    .eq(StringUtils.isNotBlank(input.getEditionId()), Tenant.Fields.edition + "." + BaseEntity.Fields.id, input.getEditionId());
        });
        return repository.findAll(spec, input.getPageable());
    }

    @Override
    public Optional<Tenant> findByTenantName(String name) {
        return repository.findByTenantName(name);
    }

    /**
     * 校验租户代码是否唯一
     *
     * @param id         租户id新增时为null
     * @param tenantName 租户代码
     * @return 名称不唯一时返回true
     */
    private boolean notUniqueTenantName(Long id, String tenantName) {
        Optional<Tenant> tenantOptional = findByTenantName(tenantName);
        if (tenantOptional.isEmpty()) {
            return false;
        }
        return !tenantOptional.get().getId().equals(id);
    }

    private Tenant addTenant(CreateTenantInput input) {
        Tenant tenant = new Tenant();
        dozerMapper.map(input, tenant);
        SubscribableEdition edition = checkEdition(input.getEditionId());
        tenant.setEdition(edition);
        return repository.save(tenant);
    }

    private User createAdminUser(CreateTenantInput input, Long tenantId) {
        String password = passwordEncoder.encode(StringUtils.isBlank(input.getAdminPassword()) ? randomPassword() : input.getAdminPassword());
        User adminUser = new User();
        adminUser.setTenantId(tenantId);
        adminUser.setActive(true);
        adminUser.setEmailAddress(input.getAdminEmailAddress());
        adminUser.setUserName(input.getAdminUserName());
        adminUser.setName(input.getAdminUserName());
        adminUser.setPassword(password);
        adminUser.setAdmin(true);
        adminUser.setShouldChangePasswordOnNextLogin(input.isShouldChangePasswordOnNextLogin());
        adminUser.setUserType(EUnitType.Inner);
        adminUser.setUserStatus(UserStatus.OnJob);
        iUserRepository.save(adminUser);
        return adminUser;
    }

    @Override
    public IsTenantAvailableOutput isTenantAvailable(IsTenantAvailableInput input) {
        Optional<Tenant> optional = repository.findByTenantName(input.getTenantName());
        if (optional.isEmpty()) {
            return new IsTenantAvailableOutput(TenantAvailabilityState.NotFound);
        }
        Tenant tenant = optional.get();

        if (!tenant.enabled()) {
            return new IsTenantAvailableOutput(TenantAvailabilityState.InActive);
        }

        return new IsTenantAvailableOutput(TenantAvailabilityState.Available, tenant.getId());
    }

    @Override
    public long countTenantsByEdition(Long editionId) {
        Specification<Tenant> spec = Specifications.where(e -> {
            e.eq(Tenant.Fields.edition + "." + BaseEntity.Fields.id, editionId);
        });

        return repository.count(spec);
    }

    @Override
    public void deleteTenant(Long id) {
        repository.deleteById(id);
        permissionManager.refreshAll();
    }

    private String randomPassword() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    private SubscribableEdition checkEdition(Long editionId, boolean isInTrialPeriod) {
        if (editionId == null) {
            return null;
        }

        SubscribableEdition edition = editionService.findById(editionId).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));

        if (isInTrialPeriod && edition.isFree()) {
            throw new BusinessException("免费版本不需要试用版");
        }
        return edition;
    }


    private SubscribableEdition checkEdition(Long editionId) {
        if (editionId == null) {
            return null;
        }
        SubscribableEdition edition = editionService.findById(editionId).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));
        return edition;
    }

}
