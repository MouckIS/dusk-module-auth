package com.dusk.module.auth.service.impl;

import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.repository.IBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CreateOrUpdateService} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class CreateOrUpdateServiceTest {

    interface TestRepository extends IBaseRepository<TestEntity> {
    }

    interface NoCtorRepository extends IBaseRepository<NoDefaultConstructorEntity> {
    }

    public static class TestEntity extends BaseEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class NoDefaultConstructorEntity extends BaseEntity {
        public NoDefaultConstructorEntity(String ignored) {
            // 故意只提供带参构造，用于触发反射创建失败的 catch 分支
        }
    }

    public static class TestInput {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Mock
    private TestRepository repository;
    @Mock
    private NoCtorRepository noCtorRepository;

    private CreateOrUpdateService<TestEntity, TestRepository> service;

    @BeforeEach
    void setUp() {
        service = new CreateOrUpdateService<>();
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @Test
    @DisplayName("id 为 null：反射创建新实体并保存")
    void createsNewEntityWhenIdIsNull() {
        TestInput input = new TestInput();
        input.setName("new-name");
        when(repository.save(any(TestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestEntity result = service.createOrUpdate(input, null, TestEntity.class, null);

        assertThat(result.getName()).isEqualTo("new-name");
        verify(repository).save(any(TestEntity.class));
    }

    @Test
    @DisplayName("id 非 null：读取已有实体、执行回调后保存")
    void updatesExistingEntityWhenIdIsNotNull() {
        TestEntity existing = new TestEntity();
        existing.setId(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.save(any(TestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<TestEntity> appliedTo = new AtomicReference<>();
        TestInput input = new TestInput();
        input.setName("updated-name");

        TestEntity result = service.createOrUpdate(input, 9L, TestEntity.class, appliedTo::set);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getName()).isEqualTo("updated-name");
        assertThat(appliedTo.get()).isSameAs(result);
    }

    @Test
    @DisplayName("id 为 null 且目标类型无默认构造：包装为运行时异常")
    void wrapsReflectiveInstantiationFailure() {
        CreateOrUpdateService<NoDefaultConstructorEntity, NoCtorRepository> noCtorService = new CreateOrUpdateService<>();
        ReflectionTestUtils.setField(noCtorService, "repository", noCtorRepository);

        assertThatThrownBy(() -> noCtorService.createOrUpdate(new TestInput(), null, NoDefaultConstructorEntity.class, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("创建目标对象失败");
    }
}
