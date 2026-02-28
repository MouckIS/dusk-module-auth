package com.dusk.module.auth.util;

import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.entity.UserRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试数据构建器 - 提供规范化的测试数据生成方法
 *
 * @author kefuming
 * @date 2026-02-28
 */
public class TestDataBuilder {

    /**
     * 构建用户实体 - 带默认值
     */
    public static User buildUser() {
        return buildUser(1L, "testUser", "test@example.com", true);
    }

    /**
     * 构建用户实体 - 自定义ID和名称
     */
    public static User buildUser(Long id, String name, String email, boolean isAdmin) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setUsername(email);
        user.setPassword("$2a$10$test.hashed.password");
        user.setTenantId(1L);
        user.setAdmin(isAdmin);
        user.setEnabled(true);
        user.setCreatedDate(LocalDateTime.now());
        user.setUserRoles(new ArrayList<>());
        return user;
    }

    /**
     * 构建角色实体
     */
    public static Role buildRole() {
        return buildRole(1L, "ADMIN", "Admin Role");
    }

    /**
     * 构建角色实体 - 自定义ID、编码和名称
     */
    public static Role buildRole(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setTenantId(1L);
        role.setEnabled(true);
        role.setCreatedDate(LocalDateTime.now());
        return role;
    }

    /**
     * 构建用户角色关联
     */
    public static UserRole buildUserRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        return userRole;
    }

    /**
     * 构建序列号实体
     */
    public static SerialNo buildSerialNo() {
        return buildSerialNo("TEST", 1L, "yyyy-MM-dd", 6, 1L);
    }

    /**
     * 构建序列号实体 - 自定义参数
     */
    public static SerialNo buildSerialNo(String billType, Long currentNo, String dateFormat,
                                         int serialLength, Long tenantId) {
        SerialNo serialNo = new SerialNo();
        serialNo.setId(1L);
        serialNo.setBillType(billType);
        serialNo.setCurrentNo(currentNo);
        serialNo.setDateFormat(dateFormat);
        serialNo.setSerialLength(serialLength);
        serialNo.setTenantId(tenantId);
        serialNo.setCreatedDate(LocalDateTime.now());
        return serialNo;
    }

    /**
     * 构建序列号编辑输入DTO
     */
    public static SerialNoEditInput buildSerialNoEditInput() {
        SerialNoEditInput input = new SerialNoEditInput();
        input.setId(1L);
        input.setBillType("TEST");
        input.setCurrentNo(1L);
        input.setDateFormat("yyyy-MM-dd");
        input.setSerialLength(6);
        return input;
    }

    /**
     * 构建验证码输入DTO
     */
    public static CaptchaInputDto buildCaptchaInputDto() {
        return buildCaptchaInputDto("test-key-id", "12345");
    }

    /**
     * 构建验证码输入DTO - 自定义key和验证码
     */
    public static CaptchaInputDto buildCaptchaInputDto(String key, String captcha) {
        CaptchaInputDto dto = new CaptchaInputDto();
        dto.setKey(key);
        dto.setCaptcha(captcha);
        return dto;
    }

    /**
     * 构建验证码输出DTO
     */
    public static CaptchaOutDto buildCaptchaOutDto() {
        CaptchaOutDto dto = new CaptchaOutDto();
        dto.setKey("test-captcha-key");
        dto.setImageBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+P+/HgAFhAJ/wlseKgAAAABJRU5ErkJggg==");
        return dto;
    }

    /**
     * 构建Token签名DTO
     */
    public static TokenSign buildTokenSign() {
        TokenSign tokenSign = new TokenSign();
        tokenSign.setUserId(1L);
        tokenSign.setTokenType("Bearer");
        tokenSign.setTokenValue("test-token-value");
        return tokenSign;
    }

    /**
     * 构建用户列表
     */
    public static List<User> buildUserList(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(buildUser((long) i, "user" + i, "user" + i + "@example.com", false));
        }
        return users;
    }

    /**
     * 构建角色列表
     */
    public static List<Role> buildRoleList(int count) {
        List<Role> roles = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            roles.add(buildRole((long) i, "ROLE_" + i, "Role " + i));
        }
        return roles;
    }

    /**
     * 构建权限实体
     */
    public static com.dusk.module.auth.entity.GrantPermission buildPermission() {
        return buildPermission(1L, "user:view", 1L);
    }

    /**
     * 构建权限实体 - 自定义参数
     */
    public static com.dusk.module.auth.entity.GrantPermission buildPermission(Long id, String name, Long roleId) {
        com.dusk.module.auth.entity.GrantPermission permission = new com.dusk.module.auth.entity.GrantPermission();
        permission.setId(id);
        permission.setName(name);
        permission.setTenantId(1L);
        if (roleId != null) {
            Role role = buildRole(roleId, "ROLE_" + roleId, "Role " + roleId);
            permission.setRole(role);
        }
        return permission;
    }

    /**
     * 构建通知实体
     */
    public static com.dusk.module.auth.entity.Notification buildNotification() {
        return buildNotification(1L, "Test Notification", "This is a test notification");
    }

    /**
     * 构建通知实体 - 自定义参数
     */
    public static com.dusk.module.auth.entity.Notification buildNotification(Long id, String title, String content) {
        com.dusk.module.auth.entity.Notification notification = new com.dusk.module.auth.entity.Notification();
        notification.setId(id);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTenantId(1L);
        notification.setCreatedDate(LocalDateTime.now());
        return notification;
    }

    /**
     * 构建用户通知实体
     */
    public static com.dusk.module.auth.entity.UserNotification buildUserNotification() {
        return buildUserNotification(1L, 1L, 1L);
    }

    /**
     * 构建用户通知实体 - 自定义参数
     */
    public static com.dusk.module.auth.entity.UserNotification buildUserNotification(Long id, Long userId, Long notificationId) {
        com.dusk.module.auth.entity.UserNotification userNotification = new com.dusk.module.auth.entity.UserNotification();
        userNotification.setId(id);
        userNotification.setUserId(userId);
        userNotification.setTenantId(1L);
        userNotification.setCreatedDate(LocalDateTime.now());
        return userNotification;
    }

    /**
     * 构建权限列表
     */
    public static List<com.dusk.module.auth.entity.GrantPermission> buildPermissionList(int count) {
        List<com.dusk.module.auth.entity.GrantPermission> permissions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            permissions.add(buildPermission((long) i, "permission:" + i, (long) i));
        }
        return permissions;
    }

    /**
     * 构建通知列表
     */
    public static List<com.dusk.module.auth.entity.Notification> buildNotificationList(int count) {
        List<com.dusk.module.auth.entity.Notification> notifications = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            notifications.add(buildNotification((long) i, "Notification " + i, "Content " + i));
        }
        return notifications;
    }

    /**
     * 构建用户与多个角色的关联
     */
    public static User buildUserWithRoles(Long userId, String username, Role... roles) {
        User user = buildUser(userId, username, username + "@example.com", false);
        for (Role role : roles) {
            user.getUserRoles().add(buildUserRole(user, role));
        }
        return user;
    }

    /**
     * 构建用户列表 - 已有角色
     */
    public static List<User> buildUserListWithRoles(int count, Role role) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(buildUserWithRoles((long) i, "user" + i, role));
        }
        return users;
    }
}
