package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.entity.BaseEntity;
import org.springframework.util.StringUtils;
import com.dusk.common.core.annotation.Tenant;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-04-29 15:38
 */
@Entity
@Table(name = "sys_audit_logs")
@Data
@FieldNameConstants
@NamedEntityGraph(name = AuditLog.NamedEntityGraph_createUser, attributeNodes = {
        @NamedAttributeNode("createUser")
})
public class AuditLog extends BaseEntity {
    public static final String NamedEntityGraph_createUser = "AuditLog.createUser";


    private static final long serialVersionUID = 6103792701342406983L;

    private String browserInfo;
    private String clientIpAddress;
    private String exception;
    private int executionDuration;
    private LocalDateTime executionTime;
    private String methodName;
    private String serviceName;
    @Column(name = EntityConstant.CREATE_ID)
    private Long createId;
    @Tenant
    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;

    private String parameters;

    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name= EntityConstant.CREATE_ID, referencedColumnName = BaseEntity.Fields.id, insertable = false, updatable = false)
    private User createUser;

    /**
     * 请求头内带的组织机构id，并非原始的，是经过网关转换的
     */
    private String orgId;

    //返回值
    private String result;


    public String toString() {
        String loggedUserId = createId!=null
                ? "user " + createId
                : "an anonymous user";

        String exceptionOrSuccessMessage = !StringUtils.isEmpty(exception)
                ? "exception: " + exception
                : "succeed";

        return String.format("AUDIT LOG: %s.%s is executed by %s in %s ms from %s IP address with %s.",serviceName,methodName,loggedUserId,executionDuration,clientIpAddress,exceptionOrSuccessMessage);
    }
}
