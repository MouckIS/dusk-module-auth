package com.dusk.module.auth.registry.lifecycle;

import com.dusk.module.auth.registry.enums.ReleaseStatus;

import java.time.LocalDateTime;

/**
 * 一条 Release 状态迁移决定，见《权限优化方案-整理版》4.7。
 *
 * <p>由 {@link ReleaseLifecyclePlanner} 纯计算产出，上层负责落库。
 * {@code from == to} 表示状态不变，但可能需要更新防抖计时（{@link #zeroInstanceSince}）。</p>
 *
 * @param releaseVersion    发布版本号
 * @param from              迁移前状态
 * @param to                迁移后状态
 * @param zeroInstanceSince 需要持久化的「0 实例」起点；{@code null} 表示清除计时
 * @param reason            判定原因，用于审计与排查
 */
public record ReleaseTransition(
        String releaseVersion,
        ReleaseStatus from,
        ReleaseStatus to,
        LocalDateTime zeroInstanceSince,
        String reason) {

    /**
     * 状态是否真的发生了变化。
     */
    public boolean statusChanged() {
        return from != to;
    }

    @Override
    public String toString() {
        if (statusChanged()) {
            return releaseVersion + ": " + from + " -> " + to + "（" + reason + "）";
        }
        return releaseVersion + ": " + from + " 保持不变（" + reason + "）";
    }
}
