package org.imooc.bilibili.api.support;

import org.imooc.bilibili.domain.Exception.ConditionException;
import org.imooc.bilibili.service.util.TokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserSupport {

    public Long getCurrentUserId(){
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        // 前端拿到的token是从localstorage中取出, 放在请求头中
        String token = requestAttributes.getRequest().getHeader("token");
        Long userId = TokenUtil.verifyToken(token);
        // userId在数据库中从1开始, 不会是负数
        if (userId < 0) {
            throw new ConditionException("非法用户! ");
        }
        return userId;
    }
}
