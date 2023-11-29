package org.imooc.bilibili.api.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.imooc.bilibili.api.UserMomentsApi;
import org.imooc.bilibili.api.support.UserSupport;
import org.imooc.bilibili.domain.Exception.ConditionException;
import org.imooc.bilibili.domain.UserMoment;
import org.imooc.bilibili.domain.annotation.ApiLimitedRole;
import org.imooc.bilibili.domain.auth.UserRole;
import org.imooc.bilibili.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.imooc.bilibili.domain.constant.AuthRoleConstant.ROLE_LV0;
import static org.imooc.bilibili.domain.constant.AuthRoleConstant.ROLE_LV1;

@Order(1)
@Component
@Aspect
public class ApiLimitedRoleAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    // 定义切点, 在自定义注解标注的地方执行
    @Pointcut("@annotation(org.imooc.bilibili.domain.annotation.ApiLimitedRole)")
    public void check() {
    }

    @Before("check() && @annotation(apiLimitedRole)")
    public void doBefore(JoinPoint joinPoint, ApiLimitedRole apiLimitedRole) {
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        String[] limitedRoleCodeList = apiLimitedRole.limitedRoleCodeList();
        // 将userRoleList中的UserRole中的roleCode取出来, 和limitedRoleCodeList中规定的限制的roleCode做交集
        Set<String> limitedRoleCodeSet = Arrays.stream(limitedRoleCodeList).collect(Collectors.toSet());
        Set<String> roleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        // 对两个set取交集
        roleCodeSet.retainAll(limitedRoleCodeSet);
        // 如果取出的roleCode中有在limitedRoleCode, 就不能让用户做操作
        if (roleCodeSet.size() > 0) {
            throw new ConditionException("权限不足! ");
        }

    }

}
