package com.dusk.module.auth.dimension.filter;

import com.dusk.common.core.dimension.context.DataDimensionContextHolder;
import com.dusk.common.core.jpa.SpecificationWrapper;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 数据维度过滤工具类
 * <p>
 * 提供便捷方法，将当前用户的维度值权限转化为 JPA Specification 查询条件，
 * 从而实现数据行级过滤。
 * </p>
 *
 * <h3>使用方式一：注解 + 手动调用（推荐）</h3>
 * <pre>
 * // Controller 层标注注解，AOP 切面会自动加载维度权限到上下文
 * {@code @DataDimension("DEPARTMENT")}
 * {@code @GetMapping("/page")}
 * public PagedResultDto<OrderDto> getPage(OrderPagedInputDto input) {
 *     return orderService.getPage(input);
 * }
 *
 * // Service 层构建查询时使用 DataDimensionFilter
 * public PagedResultDto<OrderDto> getPage(OrderPagedInputDto input) {
 *     Page<Order> page = repository.findAll(
 *         Specifications.where(w -> {
 *             w.contains(StrUtil.isNotBlank(input.getName()), "name", input.getName());
 *             // 按部门维度过滤：实体的 dep 字段存储的是维度值编码
 *             DataDimensionFilter.applyByCode(w, "DEPARTMENT", "dep");
 *         }),
 *         input.getPageable()
 *     );
 *     // ...
 * }
 * </pre>
 *
 * <h3>使用方式二：纯代码方式（不使用注解）</h3>
 * <pre>
 * // 直接注入 IUserDimensionPermissionService 获取权限列表
 * {@code @Autowired}
 * private IUserDimensionPermissionService permissionService;
 *
 * public PagedResultDto<OrderDto> getPage(OrderPagedInputDto input) {
 *     Long userId = securityUtils.getCurrentUser().getId();
 *     List<String> accessibleCodes = permissionService.getAccessibleValueCodes(userId, "DEPARTMENT");
 *     Page<Order> page = repository.findAll(
 *         Specifications.where(w -> {
 *             DataDimensionFilter.applyWithValues(w, "dep", accessibleCodes);
 *         }),
 *         input.getPageable()
 *     );
 * }
 * </pre>
 *
 * <h3>使用方式三：获取 Specification 直接组合</h3>
 * <pre>
 * Specification<Order> dimSpec = DataDimensionFilter.specByCode("DEPARTMENT", "dep");
 * Specification<Order> otherSpec = Specifications.where(w -> { ... });
 * Page<Order> page = repository.findAll(dimSpec.and(otherSpec), pageable);
 * </pre>
 *
 * @author dusk
 */
public final class DataDimensionFilter {

    private DataDimensionFilter() {
    }

    /**
     * 根据维度编码和实体字段名，将维度过滤条件追加到 SpecificationWrapper 中。
     * <p>使用维度值编码匹配实体字段，适用于实体字段存储维度值编码的场景。</p>
     * <p>需先通过 {@link DataDimension} 注解或手动调用将维度信息加载到上下文中。</p>
     *
     * @param wrapper       JPA Specification 包装器
     * @param dimensionCode 维度编码（如 "DEPARTMENT"）
     * @param fieldName     实体字段名（如 "dep"）
     * @param <T>           实体类型
     */
    public static <T> void applyByCode(SpecificationWrapper<T> wrapper, String dimensionCode, String fieldName) {
        List<String> codes = DataDimensionContextHolder.getAccessibleValueCodes(dimensionCode);
        applyWithValues(wrapper, fieldName, codes);
    }

    /**
     * 根据维度编码和实体字段名，将维度过滤条件追加到 SpecificationWrapper 中。
     * <p>使用维度值名称匹配实体字段，适用于实体字段存储维度值名称的场景。</p>
     *
     * @param wrapper       JPA Specification 包装器
     * @param dimensionCode 维度编码（如 "DEPARTMENT"）
     * @param fieldName     实体字段名（如 "depName"）
     * @param <T>           实体类型
     */
    public static <T> void applyByName(SpecificationWrapper<T> wrapper, String dimensionCode, String fieldName) {
        List<String> names = DataDimensionContextHolder.getAccessibleValueNames(dimensionCode);
        applyWithValues(wrapper, fieldName, names);
    }

    /**
     * 根据维度编码和实体字段名，将维度过滤条件追加到 SpecificationWrapper 中。
     * <p>使用维度值ID匹配实体字段，适用于实体字段存储维度值ID的场景。</p>
     *
     * @param wrapper       JPA Specification 包装器
     * @param dimensionCode 维度编码（如 "DEPARTMENT"）
     * @param fieldName     实体字段名（如 "depId"）
     * @param <T>           实体类型
     */
    public static <T> void applyById(SpecificationWrapper<T> wrapper, String dimensionCode, String fieldName) {
        List<Long> ids = DataDimensionContextHolder.getAccessibleValueIds(dimensionCode);
        if (ids == null || ids.isEmpty()) {
            // 没有权限，添加一个永假条件确保查不到数据
            wrapper.eq(fieldName, Long.MIN_VALUE);
        } else {
            wrapper.in(fieldName, ids);
        }
    }

    /**
     * 根据注解配置的 MatchMode 自动选择匹配方式
     *
     * @param wrapper       JPA Specification 包装器
     * @param dimensionCode 维度编码
     * @param fieldName     实体字段名
     * @param matchMode     匹配模式
     * @param <T>           实体类型
     */
    /*public static <T> void apply(SpecificationWrapper<T> wrapper, String dimensionCode, String fieldName,
                                 DataDimension.MatchMode matchMode) {
        switch (matchMode) {
            case VALUE_CODE -> applyByCode(wrapper, dimensionCode, fieldName);
            case VALUE_NAME -> applyByName(wrapper, dimensionCode, fieldName);
            case VALUE_ID -> applyById(wrapper, dimensionCode, fieldName);
        }
    }*/

    /**
     * 直接用给定的值列表在字段上添加 IN 条件。
     * <p>不依赖上下文，适合纯代码方式使用。</p>
     *
     * @param wrapper   JPA Specification 包装器
     * @param fieldName 实体字段名
     * @param values    允许的值列表
     * @param <T>       实体类型
     */
    public static <T> void applyWithValues(SpecificationWrapper<T> wrapper, String fieldName, List<String> values) {
        if (values == null || values.isEmpty()) {
            // 没有权限，添加一个永假条件确保查不到数据
            wrapper.eq(fieldName, "\u0000__NO_PERMISSION__");
        } else {
            wrapper.in(fieldName, values);
        }
    }

    // ==================== Specification 生成方法 ====================

    /**
     * 生成基于维度值编码的 Specification，可直接与其他 Specification 组合使用
     *
     * @param dimensionCode 维度编码
     * @param fieldName     实体字段名
     * @param <T>           实体类型
     * @return JPA Specification
     */
    public static <T> Specification<T> specByCode(String dimensionCode, String fieldName) {
        return (root, query, builder) -> {
            List<String> codes = DataDimensionContextHolder.getAccessibleValueCodes(dimensionCode);
            if (codes == null || codes.isEmpty()) {
                return builder.equal(root.get(fieldName), "\u0000__NO_PERMISSION__");
            }
            return root.get(fieldName).in(codes);
        };
    }

    /**
     * 生成基于维度值名称的 Specification
     *
     * @param dimensionCode 维度编码
     * @param fieldName     实体字段名
     * @param <T>           实体类型
     * @return JPA Specification
     */
    public static <T> Specification<T> specByName(String dimensionCode, String fieldName) {
        return (root, query, builder) -> {
            List<String> names = DataDimensionContextHolder.getAccessibleValueNames(dimensionCode);
            if (names == null || names.isEmpty()) {
                return builder.equal(root.get(fieldName), "\u0000__NO_PERMISSION__");
            }
            return root.get(fieldName).in(names);
        };
    }

    /**
     * 生成基于维度值ID的 Specification
     *
     * @param dimensionCode 维度编码
     * @param fieldName     实体字段名
     * @param <T>           实体类型
     * @return JPA Specification
     */
    public static <T> Specification<T> specById(String dimensionCode, String fieldName) {
        return (root, query, builder) -> {
            List<Long> ids = DataDimensionContextHolder.getAccessibleValueIds(dimensionCode);
            if (ids == null || ids.isEmpty()) {
                return builder.equal(root.get(fieldName), Long.MIN_VALUE);
            }
            return root.get(fieldName).in(ids);
        };
    }

    /**
     * 直接用给定值列表生成 Specification，不依赖上下文
     *
     * @param fieldName 实体字段名
     * @param values    允许的值列表
     * @param <T>       实体类型
     * @return JPA Specification
     */
    public static <T> Specification<T> specWithValues(String fieldName, List<String> values) {
        return (root, query, builder) -> {
            if (values == null || values.isEmpty()) {
                return builder.equal(root.get(fieldName), "\u0000__NO_PERMISSION__");
            }
            return root.get(fieldName).in(values);
        };
    }

    /**
     * 直接用给定ID列表生成 Specification，不依赖上下文
     *
     * @param fieldName 实体字段名
     * @param ids       允许的ID列表
     * @param <T>       实体类型
     * @return JPA Specification
     */
    public static <T> Specification<T> specWithIds(String fieldName, List<Long> ids) {
        return (root, query, builder) -> {
            if (ids == null || ids.isEmpty()) {
                return builder.equal(root.get(fieldName), Long.MIN_VALUE);
            }
            return root.get(fieldName).in(ids);
        };
    }
}
