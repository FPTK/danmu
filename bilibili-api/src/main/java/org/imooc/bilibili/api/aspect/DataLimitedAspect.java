package org.imooc.bilibili.api.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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
public class DataLimitedAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    // 定义切点, 在自定义注解标注的地方执行
    @Pointcut("@annotation(org.imooc.bilibili.domain.annotation.DataLimited)")
    public void check() {
    }


    // 这个通知是做参数控制用的, 限制登录用户当前角色有哪些可操作的行为, 比如type字段, lv0的用户只有视频投稿功能可以使用
    @Before("check()")
    public void doBefore(JoinPoint joinPoint) {
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        Set<String> roleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UserMoment) {
                UserMoment userMoment = (UserMoment) arg;
                String type = userMoment.getType();
                // 如果目前用户为lv0且用户发布的moment不是视频, 则拒绝访问相关接口
                if (roleCodeSet.contains(ROLE_LV0) && !"0".equals(type)) {
                    throw new ConditionException("拒绝访问! ");
                } else if (roleCodeSet.contains(ROLE_LV1) && "1".equals(type)) {
                    throw new ConditionException("拒绝访问! ");
                }
            }
        }
    }
}
