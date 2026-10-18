package com.dusk.module.auth.authz.event;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.mqs.core.ConsumeMode;
import com.dusk.common.mqs.core.MessageConsumer;
import com.dusk.common.mqs.core.MqConsumeContext;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 消费 {@code RESOURCE_SNAPSHOT_REGISTERED}：运行期快照增量注册，见《权限优化方案-整理版》8.6。
 *
 * <p><b>载荷是 {@link ResourceSnapshot} 本身</b>（与启动期 HTTP 同步注册完全相同的契约类型），
 * 传输元数据（{@code instanceId} / {@code occurredAt}）在 mqs 的消息头里，因此这里不引入任何
 * 「只为传输存在」的中间 DTO。</p>
 *
 * <p>与启动期的 HTTP 同步注册（3.5 方案 A）<b>共用同一个业务入口</b>
 * {@link IResourceRegistryService#register}，保证两者的 Diff、权限生命周期、读模型投影行为完全一致——
 * 若为 MQ 通道另写一套落库逻辑，两条路径的行为必然漂移。</p>
 *
 * <p><b>幂等</b>：{@code register()} 对同一 {@code resourceVersion} 本身即幂等（3.4 第 4 步），
 * 叠加 mqs 的幂等消费（{@code bizKey} = {@code serviceId@serviceVersion@resourceVersion}），
 * 至少一次投递不会造成重复写库。</p>
 *
 * <p><b>租户上下文</b>：本链路写入宿主级全局数据（{@code CreationEntity}，无 {@code @Tenant}），
 * 因此<b>刻意不设置</b> {@code TenantContextHolder}（对齐 5.3：不为不存在的语义引入污染）。</p>
 *
 * <p><b>失败语义</b>：{@link BusinessException} 表示确定性校验失败（如 resourceVersion 与内容不符），
 * 重试不会成功，故记录后<b>确认消费</b>（不重投）；其余异常向上抛，由 mqs 转为重投
 * （{@code maxReconsumeTimes} 默认 16）。</p>
 */
@Slf4j
public class ResourceSnapshotRegisteredConsumer implements MessageConsumer<ResourceSnapshot> {

    private final IResourceRegistryService registryService;
    private final AuthzMqProperties properties;

    public ResourceSnapshotRegisteredConsumer(IResourceRegistryService registryService, AuthzMqProperties properties) {
        this.registryService = registryService;
        this.properties = properties;
    }

    @Override
    public void process(ResourceSnapshot data, MqConsumeContext context) {
        String messageId = context == null ? null : context.getMessageId();
        if (data == null) {
            log.warn("收到空的快照注册消息，忽略: messageId={}", messageId);
            return;
        }
        try {
            RegistryRegistrationResponse response = registryService.register(data);
            log.info("MQ 快照注册完成: serviceId={}, serviceVersion={}, resourceVersion={}, unchanged={}, "
                            + "新增 {}、废弃 {}、换绑挂起 {}，权限生效 {}、孤立 {}（instanceId={}, messageId={}）",
                    response.serviceId(), response.serviceVersion(), response.resourceVersion(), response.unchanged(),
                    response.addedResources().size(), response.deprecatedResources().size(),
                    response.pendingRebindResources().size(), response.activatedPermissions().size(),
                    response.orphanedPermissions().size(), header(context, MqResourcePublisherHeaders.INSTANCE_ID), messageId);
        } catch (BusinessException e) {
            // 确定性拒绝：重投无意义，记录后确认（对齐内部 HTTP 端点把领域校验归为 4xx 的语义）。
            log.error("MQ 快照注册被拒绝（确定性校验失败，不再重投）: messageId={}, serviceId={}, reason={}",
                    messageId, data.serviceId(), e.getMessage());
        }
    }

    /**
     * 读取传输元数据头（SDK 侧写入，见 {@code MqResourcePublisher}）。
     */
    private static String header(MqConsumeContext context, String name) {
        if (context == null || context.getHeaders() == null) {
            return null;
        }
        Map<String, String> headers = context.getHeaders();
        return headers.get(name);
    }

    @Override
    public String getTopic() {
        return properties.getSnapshotRegisterTopic();
    }

    @Override
    public String getConsumerGroup() {
        return env() + "." + properties.getDomain() + ".auth-registry-snapshot";
    }

    @Override
    public String getDomain() {
        return properties.getDomain();
    }

    @Override
    public ConsumeMode getConsumeMode() {
        return ConsumeMode.CONCURRENTLY;
    }

    @Override
    public Class<?> getPayloadType() {
        return ResourceSnapshot.class;
    }

    /**
     * 开启幂等消费：注册链路会写库，重复投递虽有业务幂等兜底，但省去一次无谓的完整 Diff 更划算。
     */
    @Override
    public boolean isIdempotent() {
        return true;
    }

    /**
     * 注册是写库操作，2 个线程足以覆盖低频的运行期增量。
     */
    @Override
    public int getConsumeThreads() {
        return 2;
    }

    /**
     * 与 SDK 侧 {@code MqResourcePublisher} 的传输头常量镜像。
     *
     * <p>SDK 与 Auth 分属不同仓库/模块，Auth 不依赖 SDK（避免反向依赖，3.10），
     * 因此头名在此以常量镜像的方式重复声明。两侧必须一致——这是跨仓契约的一部分（8.4）。</p>
     */
    private static final class MqResourcePublisherHeaders {
        private static final String INSTANCE_ID = "sdk.instanceId";

        private MqResourcePublisherHeaders() {
        }
    }
}
