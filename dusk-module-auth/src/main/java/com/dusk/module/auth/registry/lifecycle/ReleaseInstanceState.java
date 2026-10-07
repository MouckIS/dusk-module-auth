package com.dusk.module.auth.registry.lifecycle;

import com.dusk.module.auth.registry.enums.ReleaseStatus;

import java.time.LocalDateTime;

/**
 * 单个 Release 的实例观测输入，见《权限优化方案-整理版》4.7。
 *
 * <p>数据来源是 Nacos 的实例状态（红线⑥：Nacos 只告诉你谁在线、在哪里、什么版本），
 * 权限事实仍由 Auth 持有。</p>
 *
 * @param releaseVersion    发布版本号
 * @param currentStatus     当前 Release 状态
 * @param instanceCount     当前运行实例数，可以为 0
 * @param zeroInstanceSince 本 Release 开始「0 实例」的时刻；有实例时为 {@code null}
 */
public record ReleaseInstanceState(
        String releaseVersion,
        ReleaseStatus currentStatus,
        int instanceCount,
        LocalDateTime zeroInstanceSince) {

    public ReleaseInstanceState {
        if (releaseVersion == null || releaseVersion.isBlank()) {
            throw new IllegalArgumentException("ReleaseInstanceState.releaseVersion 不能为空");
        }
        releaseVersion = releaseVersion.trim();
        if (currentStatus == null) {
            throw new IllegalArgumentException("ReleaseInstanceState.currentStatus 不能为空，版本：" + releaseVersion);
        }
        if (instanceCount < 0) {
            throw new IllegalArgumentException("ReleaseInstanceState.instanceCount 不能为负数，版本：" + releaseVersion);
        }
    }

    public boolean hasInstance() {
        return instanceCount > 0;
    }
}
