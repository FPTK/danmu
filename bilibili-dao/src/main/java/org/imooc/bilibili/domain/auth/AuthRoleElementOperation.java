package org.imooc.bilibili.domain.auth;
import java.util.Date;

public class AuthRoleElementOperation {
    private Long id;
    private Long roleId;
    private Long elementOperationId;
    private Date createTime;
    // 由于这个类只保存了关系, 所以添加一个实体类, 这样查数据库的时候不用查两次, 只用查一次, 把数据放到实体类中就行
    private AuthElementOperation authElementOperation;

    public AuthElementOperation getAuthElementOperation() {
        return authElementOperation;
    }

    public void setAuthElementOperation(AuthElementOperation authElementOperation) {
        this.authElementOperation = authElementOperation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getElementOperationId() {
        return elementOperationId;
    }

    public void setElementOperationId(Long elementOperationId) {
        this.elementOperationId = elementOperationId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
