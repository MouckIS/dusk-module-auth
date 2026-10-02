package com.dusk.module.auth.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DubboCustomUtils} 单元测试，目标：覆盖反射探测全部分支。
 *
 * <p>该类为纯反射工具（Lombok {@code @UtilityClass}，全部方法为静态），
 * 其公开入口 {@code isValidRpcService} 的行为依赖运行时 Dubbo 容器状态，
 * 在单元测试中无法构造真实 provider/invoker，因此对公开入口做「不抛异常 + 返回布尔值」的
 * 契约测试，对其私有探测逻辑通过反射直接驱动，以覆盖各分支。</p>
 */
class DubboCustomUtilsTest {

    /** 带公开 isAvailable() 且返回 true 的 invoker。 */
    public static class AvailableInvoker {
        public boolean isAvailable() {
            return true;
        }
    }

    /** isAvailable() 返回 false 的 invoker。 */
    public static class UnavailableInvoker {
        public boolean isAvailable() {
            return false;
        }
    }

    /** isAvailable() 返回非布尔值的 invoker，用于覆盖类型判断分支。 */
    public static class NonBooleanInvoker {
        public String isAvailable() {
            return "yes";
        }
    }

    /** isAvailable() 非 public，且不实现任何接口：getMethod 抛 NoSuchMethodException 后直接返回 false。 */
    public static class NonPublicAvailable {
        boolean isAvailable() {
            return true;
        }
    }

    /** isAvailable() 非 public，但实现了不含该方法的接口：覆盖接口回退循环中的异常分支。 */
    public static class NonPublicAvailableWithInterface implements Serializable {
        boolean isAvailable() {
            return true;
        }
    }

    /** 暴露无参、返回 Collection 的方法，用于驱动 ConsumerModel 探测。 */
    public static class ConsumerModelWithInvokers {
        public Collection<Object> getInvokers() {
            return List.of(new AvailableInvoker());
        }
    }

    /** 暴露无参但返回空集合的方法。 */
    public static class ConsumerModelWithEmptyInvokers {
        public Collection<Object> getInvokers() {
            return List.of();
        }
    }

    /** 暴露会抛异常的无参集合方法，覆盖 InvocationTargetException 分支。 */
    public static class ConsumerModelThrowing {
        public Collection<Object> getInvokers() {
            throw new IllegalStateException("boom");
        }
    }

    /** 无任何无参集合方法，覆盖“遍历所有方法后仍未命中”分支。 */
    public static class ConsumerModelWithoutInvokers {
        public String getServiceKey() {
            return "other-service";
        }
    }

    /** serviceKey 与目标名称一致。 */
    public static class MatchedByServiceKey {
        public String getServiceKey() {
            return "target-service";
        }
    }

    /** serviceKey 不一致，但 getName 一致，覆盖候选方法逐个尝试。 */
    public static class MatchedByName {
        public String getServiceKey() {
            return "other";
        }

        public String getName() {
            return "target-service";
        }
    }

    /** 所有候选方法都存在但都不匹配，覆盖循环结束分支。 */
    public static class NotMatched {
        public String getServiceKey() {
            return "a";
        }

        public String getServiceInterface() {
            return "b";
        }

        public String getServiceInterfaceName() {
            return "c";
        }

        public String getServiceName() {
            return "d";
        }

        public String getName() {
            return "e";
        }
    }

    /** 候选方法抛异常，覆盖匹配逻辑的异常捕获分支。 */
    public static class ThrowingServiceKey {
        public String getServiceKey() {
            throw new IllegalStateException("boom");
        }
    }

    // ---------------- 反射辅助 ----------------

    private static Method privateMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = DubboCustomUtils.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static boolean invokeBoolean(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = privateMethod(name, parameterTypes);
        return (boolean) method.invoke(null, args);
    }

    // ---------------- 公开入口 ----------------

    @Test
    @DisplayName("isValidRpcService：单元测试环境下无可用提供者，安全返回 false 且不抛异常")
    void isValidRpcServiceReturnsFalseWithoutProviders() {
        assertThat(DubboCustomUtils.isValidRpcService("dusk-module-auth")).isFalse();
        assertThat(DubboCustomUtils.isValidRpcService(null)).isFalse();
    }

    // ---------------- isInvokerCollectionAvailable ----------------

    @Test
    @DisplayName("isInvokerCollectionAvailable：非集合对象直接返回 false")
    void invokerCollectionRejectsNonCollection() throws Exception {
        assertThat(invokeBoolean("isInvokerCollectionAvailable",
                new Class<?>[]{Object.class}, "not-a-collection")).isFalse();
    }

    @Test
    @DisplayName("isInvokerCollectionAvailable：空集合返回 false")
    void invokerCollectionRejectsEmpty() throws Exception {
        assertThat(invokeBoolean("isInvokerCollectionAvailable",
                new Class<?>[]{Object.class}, List.of())).isFalse();
    }

    @Test
    @DisplayName("isInvokerCollectionAvailable：存在可用 invoker 时返回 true")
    void invokerCollectionAcceptsAvailable() throws Exception {
        assertThat(invokeBoolean("isInvokerCollectionAvailable",
                new Class<?>[]{Object.class}, List.of(new UnavailableInvoker(), new AvailableInvoker())))
                .isTrue();
    }

    @Test
    @DisplayName("isInvokerCollectionAvailable：全部不可用时返回 false")
    void invokerCollectionRejectsAllUnavailable() throws Exception {
        assertThat(invokeBoolean("isInvokerCollectionAvailable",
                new Class<?>[]{Object.class}, List.of(new UnavailableInvoker(), new NonBooleanInvoker())))
                .isFalse();
    }

    // ---------------- invokeIsAvailable ----------------

    @Test
    @DisplayName("invokeIsAvailable：null 返回 false")
    void invokerNullReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable", new Class<?>[]{Object.class}, new Object[]{null}))
                .isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：isAvailable() 返回 true 时返回 true")
    void invokerAvailableReturnsTrue() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new AvailableInvoker())).isTrue();
    }

    @Test
    @DisplayName("invokeIsAvailable：isAvailable() 返回 false 时返回 false")
    void invokerUnavailableReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new UnavailableInvoker())).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：返回值非布尔类型时返回 false")
    void invokerNonBooleanReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new NonBooleanInvoker())).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：方法非 public 且无接口时返回 false")
    void invokerNonPublicWithoutInterfaceReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new NonPublicAvailable())).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：方法非 public 且接口无该方法时返回 false（覆盖接口回退）")
    void invokerNonPublicWithInterfaceReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new NonPublicAvailableWithInterface())).isFalse();
    }

    // ---------------- checkConsumerModelInvokers ----------------

    @Test
    @DisplayName("checkConsumerModelInvokers：null 返回 false")
    void consumerModelNullReturnsFalse() throws Exception {
        assertThat(invokeBoolean("checkConsumerModelInvokers",
                new Class<?>[]{Object.class}, new Object[]{null})).isFalse();
    }

    @Test
    @DisplayName("checkConsumerModelInvokers：存在可用 invoker 集合时返回 true")
    void consumerModelWithAvailableInvokerReturnsTrue() throws Exception {
        assertThat(invokeBoolean("checkConsumerModelInvokers",
                new Class<?>[]{Object.class}, new ConsumerModelWithInvokers())).isTrue();
    }

    @Test
    @DisplayName("checkConsumerModelInvokers：集合为空时返回 false")
    void consumerModelWithEmptyInvokerReturnsFalse() throws Exception {
        assertThat(invokeBoolean("checkConsumerModelInvokers",
                new Class<?>[]{Object.class}, new ConsumerModelWithEmptyInvokers())).isFalse();
    }

    @Test
    @DisplayName("checkConsumerModelInvokers：集合方法抛异常时被吞掉并返回 false")
    void consumerModelThrowingReturnsFalse() throws Exception {
        assertThat(invokeBoolean("checkConsumerModelInvokers",
                new Class<?>[]{Object.class}, new ConsumerModelThrowing())).isFalse();
    }

    @Test
    @DisplayName("checkConsumerModelInvokers：无无参集合方法时返回 false")
    void consumerModelWithoutInvokerMethodReturnsFalse() throws Exception {
        assertThat(invokeBoolean("checkConsumerModelInvokers",
                new Class<?>[]{Object.class}, new ConsumerModelWithoutInvokers())).isFalse();
    }

    // ---------------- matchesServiceUniqueName ----------------

    @Test
    @DisplayName("matchesServiceUniqueName：null 返回 false")
    void matchesNullReturnsFalse() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class}, new Object[]{null, "svc"})).isFalse();
    }

    @Test
    @DisplayName("matchesServiceUniqueName：serviceKey 命中时返回 true")
    void matchesByServiceKey() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new MatchedByServiceKey(), "target-service")).isTrue();
    }

    @Test
    @DisplayName("matchesServiceUniqueName：serviceKey 未命中但 name 命中时返回 true")
    void matchesByName() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new MatchedByName(), "target-service")).isTrue();
    }

    @Test
    @DisplayName("matchesServiceUniqueName：所有候选方法都不匹配时返回 false")
    void matchesNoneReturnsFalse() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new NotMatched(), "target-service")).isFalse();
    }

    @Test
    @DisplayName("matchesServiceUniqueName：候选方法抛异常时被吞掉并返回 false")
    void matchesThrowingReturnsFalse() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new ThrowingServiceKey(), "target-service")).isFalse();
    }
}
