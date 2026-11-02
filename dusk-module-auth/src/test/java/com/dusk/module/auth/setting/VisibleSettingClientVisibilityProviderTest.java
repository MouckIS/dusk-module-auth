package com.dusk.module.auth.setting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VisibleSettingClientVisibilityProvider} 单元测试：单例懒加载与可见性判定。
 */
class VisibleSettingClientVisibilityProviderTest {

    @Test
    @DisplayName("getInstance 首次创建后复用同一实例")
    void returnsSingleton() {
        VisibleSettingClientVisibilityProvider first = VisibleSettingClientVisibilityProvider.getInstance();
        VisibleSettingClientVisibilityProvider second = VisibleSettingClientVisibilityProvider.getInstance();

        assertThat(first).isNotNull().isSameAs(second);
    }

    @Test
    @DisplayName("checkVisible 恒为 true")
    void alwaysVisible() {
        assertThat(VisibleSettingClientVisibilityProvider.getInstance().checkVisible()).isTrue();
    }
}
