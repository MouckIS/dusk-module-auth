package com.dusk.module.auth.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

    private static final String FAKE_APP_MODEL = "org.apache.dubbo.rpc.model.ApplicationModel";

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
    private static final String UTILS_CLASS = "com.dusk.module.auth.common.util.DubboCustomUtils";

    @Test
    @DisplayName("matchesServiceUniqueName：候选方法抛异常时被吞掉并返回 false")
    void matchesThrowingReturnsFalse() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new ThrowingServiceKey(), "target-service")).isFalse();
    }
    // ---------------- invokeIsAvailable：接口静态方法回退 ----------------
    //
    // 真实 Dubbo 里 invoker 的 isAvailable() 可能只声明在接口上（实现类由代理生成）。
    // 类上 getMethod 找不到时，实现会回退到 getInterfaces() 逐个查找。下面用「接口静态方法」
    // 构造这种形态：静态接口方法不会被实现类继承，因此类的 getMethod 必然抛 NoSuchMethodException，
    // 恰好驱动回退分支。
    private static final String FAKE_APP_MODEL_SOURCE = """
            package org.apache.dubbo.rpc.model;
            
            import java.util.Collection;
            
            public class ApplicationModel {
            
                public static Object instance;
                public static Object model;
                public static Object consumerModel;
                public static Object allConsumerModelsResult;
                public static Object getConsumerModelsResult;
            
                public static Object getInstance() {
                    return instance;
                }
            
                public static Object getModel() {
                    return model;
                }
            
                public static Object getConsumerModel(String name) {
                    return consumerModel;
                }
            
                public Object allConsumerModels() {
                    return allConsumerModelsResult;
                }
            
                public Object getConsumerModels() {
                    return getConsumerModelsResult;
                }
            
                public static class Consumer {
                    private final String key;
                    private final Collection<Object> invokers;
            
                    public Consumer(String key, Collection<Object> invokers) {
                        this.key = key;
                        this.invokers = invokers;
                    }
            
                    public String getServiceKey() {
                        return key;
                    }
            
                    public Collection<Object> getInvokers() {
                        return invokers;
                    }
                }
            
                public static class Invoker {
                    private final boolean available;
            
                    public Invoker(boolean available) {
                        this.available = available;
                    }
            
                    public boolean isAvailable() {
                        return available;
                    }
                }
            }
            """;

    private static ChildFirstLoader isolatedLoader() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "该用例需要 JDK 内置编译器");
        Path directory = Files.createTempDirectory("fake-dubbo-model");
        Path packageDirectory = directory.resolve("org/apache/dubbo/rpc/model");
        Files.createDirectories(packageDirectory);
        Path source = packageDirectory.resolve("ApplicationModel.java");
        Files.writeString(source, FAKE_APP_MODEL_SOURCE);
        int exitCode = compiler.run(null, null, null, "-d", directory.toString(), source.toString());
        assertThat(exitCode).isZero();

        ClassLoader parent = DubboCustomUtilsTest.class.getClassLoader();
        return new ChildFirstLoader(parent, new FakeModelLoader(new URL[]{directory.toUri().toURL()}, parent));
    }

    private static boolean invokeIsValid(ChildFirstLoader loader, String serviceName) throws Exception {
        Class<?> utils = loader.loadClass(UTILS_CLASS);
        return (boolean) utils.getMethod("isValidRpcService", String.class).invoke(null, serviceName);
    }

    private static Object invoker(Class<?> appModel, boolean available) throws Exception {
        return appModel.getClassLoader().loadClass(FAKE_APP_MODEL + "$Invoker")
                .getConstructor(boolean.class).newInstance(available);
    }

    private static Object consumer(Class<?> appModel, String key, List<Object> invokers) throws Exception {
        return appModel.getClassLoader().loadClass(FAKE_APP_MODEL + "$Consumer")
                .getConstructor(String.class, Collection.class).newInstance(key, invokers);
    }

    @Test
    @DisplayName("matchesServiceUniqueName：候选方法返回 null 时跳过且不抛异常")
    void matchesNullValueReturnsFalse() throws Exception {
        assertThat(invokeBoolean("matchesServiceUniqueName",
                new Class<?>[]{Object.class, String.class},
                new NullServiceKey(), "target-service")).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：类上没有 isAvailable() 时回退到接口，返回 true")
    void invokerFallsBackToInterfaceAndReturnsTrue() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new OnlyStaticTrue())).isTrue();
    }

    @Test
    @DisplayName("invokeIsAvailable：接口回退返回 false 时结果为 false")
    void invokerFallsBackToInterfaceAndReturnsFalse() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new OnlyStaticFalse())).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：接口回退返回非布尔值时结果为 false")
    void invokerFallsBackToInterfaceNonBoolean() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new OnlyStaticNonBoolean())).isFalse();
    }

    @Test
    @DisplayName("invokeIsAvailable：接口回退调用抛异常时被吞掉并返回 false")
    void invokerFallsBackToInterfaceThrowing() throws Exception {
        assertThat(invokeBoolean("invokeIsAvailable",
                new Class<?>[]{Object.class}, new OnlyStaticThrowing())).isFalse();
    }

    @Test
    @DisplayName("isValidRpcService：静态 getConsumerModel 返回可用 consumer 时判定为可用")
    void detectsAvailableConsumerThroughStaticLookup() throws Exception {
        ChildFirstLoader loader = isolatedLoader();
        Class<?> appModel = loader.loadClass(FAKE_APP_MODEL);
        Object available = invoker(appModel, true);
        appModel.getField("consumerModel").set(null, consumer(appModel, "dusk-module-auth", List.of(available)));

        assertThat(invokeIsValid(loader, "dusk-module-auth")).isTrue();
    }

    @Test
    @DisplayName("isValidRpcService：静态 consumer 不可用时继续遍历全量 consumer 集合，命中可用项")
    void detectsAvailableConsumerThroughModelCollection() throws Exception {
        ChildFirstLoader loader = isolatedLoader();
        Class<?> appModel = loader.loadClass(FAKE_APP_MODEL);
        Object unavailable = invoker(appModel, false);
        Object available = invoker(appModel, true);
        appModel.getField("consumerModel").set(null,
                consumer(appModel, "dusk-module-auth", List.of(unavailable)));
        appModel.getField("instance").set(null, appModel.getConstructor().newInstance());
        appModel.getField("allConsumerModelsResult").set(null,
                List.of(consumer(appModel, "dusk-module-auth", List.of(available))));

        assertThat(invokeIsValid(loader, "dusk-module-auth")).isTrue();
    }

    // ---------------- isValidRpcService：Dubbo 3 模型反射探测 ----------------
    //
    // isValidRpcService 通过 Class.forName("org.apache.dubbo.rpc.model.ApplicationModel")
    // 读取 Dubbo 的全局模型，真实实现里这些方法是否存在取决于 Dubbo 版本，且测试进程中没有
    // 任何 provider/consumer 注册。为了覆盖「静态候选方法 → 单例 consumer → 全量 consumer 集合」
    // 三条探测路径，这里在一个隔离的类加载器中定义一份可控的 ApplicationModel 假实现
    // （仅测试期由 javac 生成，不进入 src/main，也不影响其他测试的 Dubbo 类）。

    @Test
    @DisplayName("isValidRpcService：无匹配 consumer、返回值非集合、方法缺失时安全返回 false")
    void returnsFalseWhenNoUsableConsumer() throws Exception {
        ChildFirstLoader loader = isolatedLoader();
        Class<?> appModel = loader.loadClass(FAKE_APP_MODEL);
        Object unavailable = invoker(appModel, false);
        appModel.getField("consumerModel").set(null,
                consumer(appModel, "other-service", List.of(unavailable)));
        appModel.getField("instance").set(null, appModel.getConstructor().newInstance());
        appModel.getField("allConsumerModelsResult").set(null, List.of(
                consumer(appModel, "other-service", List.of(unavailable)),
                consumer(appModel, "dusk-module-auth", List.of(unavailable))));
        appModel.getField("getConsumerModelsResult").set(null, "not-a-collection");

        assertThat(invokeIsValid(loader, "dusk-module-auth")).isFalse();
    }

    @Test
    @DisplayName("isValidRpcService：首个静态候选返回 null 时继续尝试后续候选方法")
    void continuesWhenFirstStaticCandidateReturnsNull() throws Exception {
        ChildFirstLoader loader = isolatedLoader();
        Class<?> appModel = loader.loadClass(FAKE_APP_MODEL);
        appModel.getField("instance").set(null, null);
        appModel.getField("model").set(null, appModel.getConstructor().newInstance());
        appModel.getField("allConsumerModelsResult").set(null, List.of());

        assertThat(invokeIsValid(loader, "dusk-module-auth")).isFalse();
    }

    /**
     * 接口静态 isAvailable() 返回 true，实现类自身没有该实例方法。
     */
    public interface StaticAvailableTrue {
        static boolean isAvailable() {
            return true;
        }
    }

    /**
     * 接口静态 isAvailable() 返回 false。
     */
    public interface StaticAvailableFalse {
        static boolean isAvailable() {
            return false;
        }
    }

    /**
     * 接口静态 isAvailable() 返回非布尔值。
     */
    public interface StaticNonBooleanAvailable {
        static String isAvailable() {
            return "yes";
        }
    }

    /**
     * 接口静态 isAvailable() 抛异常。
     */
    public interface StaticThrowingAvailable {
        static boolean isAvailable() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * 候选方法返回 null，覆盖 {@code val != null} 的短路分支。
     */
    public static class NullServiceKey {
        public String getServiceKey() {
            return null;
        }

        public String getName() {
            return null;
        }
    }

    public static class OnlyStaticTrue implements StaticAvailableTrue {
    }

    public static class OnlyStaticFalse implements StaticAvailableFalse {
    }

    public static class OnlyStaticNonBoolean implements StaticNonBooleanAvailable {
    }

    public static class OnlyStaticThrowing implements StaticThrowingAvailable {
    }

    /**
     * 只从临时目录加载 ApplicationModel，避免 URLClassLoader 的父优先委派拿到真实 Dubbo 类。
     */
    private static final class FakeModelLoader extends URLClassLoader {

        FakeModelLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        Class<?> loadLocal(String name) throws ClassNotFoundException {
            Class<?> alreadyLoaded = findLoadedClass(name);
            return alreadyLoaded != null ? alreadyLoaded : findClass(name);
        }
    }

    /**
     * 子优先类加载器：{@code org.apache.dubbo.rpc.model.ApplicationModel} 取自假实现，
     * {@link DubboCustomUtils} 自身重新定义，其余类型全部委派父加载器。
     */
    private static final class ChildFirstLoader extends ClassLoader {

        private final FakeModelLoader fake;

        ChildFirstLoader(ClassLoader parent, FakeModelLoader fake) {
            super(parent);
            this.fake = fake;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    if (FAKE_APP_MODEL.equals(name)) {
                        loaded = fake.loadLocal(name);
                    } else if (UTILS_CLASS.equals(name)) {
                        loaded = defineFromParent(name);
                    } else {
                        loaded = getParent().loadClass(name);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> defineFromParent(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            URL location = getParent().getResource(resource);
            if (location == null) {
                throw new ClassNotFoundException(name);
            }
            try (InputStream in = location.openStream()) {
                byte[] bytes = in.readAllBytes();
                // 必须给副本一个带 CodeSource 的 ProtectionDomain：JaCoCo 的 Agent 会跳过
                // 「无源位置」的类（inclNoLocationClasses 默认 false），否则副本不会被插桩，
                // 覆盖数据也就不会计入本类。
                CodeSource codeSource = new CodeSource(location, (Certificate[]) null);
                ProtectionDomain domain = new ProtectionDomain(codeSource, null);
                return defineClass(name, bytes, 0, bytes.length, domain);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
