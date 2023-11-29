package org.imooc.bilibili.service;

import org.imooc.bilibili.domain.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.imooc.bilibili.domain.constant.AuthRoleConstant.ROLE_LV0;

@Service
public class UserAuthService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private AuthRoleService authRoleService;


    public UserAuthorities getUserAuthorities(Long userId) {
        // 通过用户id获取用户的角色, 可能有多个, 所以是列表
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        // 需要从list中查出当前用户拥有的角色id, 以便后续用角色id查询这个角色拥有的权限
        Set<Long> roleIdSet = userRoleList.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        // 权限id分为2种, 一种是页面访问权限, 一种是按钮操作权限
        List<AuthRoleElementOperation> roleElementOperationList =
                authRoleService.getRoleElementOperationsByRoleIds(roleIdSet);
        List<AuthRoleMenu> authRoleMenuList = authRoleService.getAuthRoleMenusByRoleIds(roleIdSet);
        UserAuthorities userAuthorities = new UserAuthorities();
        userAuthorities.setRoleElementOperationList(roleElementOperationList);
        userAuthorities.setRoleMenuList(authRoleMenuList);
        return userAuthorities;
    }

    // 不在userRoleService 或者authRoleService 中新建的原因是, 在UserAuthService中已经引入了这两个依赖, 如果在这两个依赖中引入
    // UserAuthService, 可能会导致循环引用的问题, 这是不好的设计模式
    public void addUserDefaultRole(Long id) {
        UserRole userRole = new UserRole();
        AuthRole role = authRoleService.getRoleByCode(ROLE_LV0);
        userRole.setUserId(id);
        userRole.setRoleId(role.getId());
        userRoleService.addUserRole(userRole);
    }
}
