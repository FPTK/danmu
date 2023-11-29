package org.imooc.bilibili.api;

import org.imooc.bilibili.api.support.UserSupport;
import org.imooc.bilibili.domain.JsonResponse;
import org.imooc.bilibili.domain.UserMoment;
import org.imooc.bilibili.domain.annotation.ApiLimitedRole;
import org.imooc.bilibili.domain.annotation.DataLimited;
import org.imooc.bilibili.service.UserMomentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.imooc.bilibili.domain.constant.AuthRoleConstant.ROLE_LV0;

// 与用户动态相关的Api
@RestController
public class UserMomentsApi {

    @Autowired
    private UserMomentsService userMomentsService;

    @Autowired
    private UserSupport userSupport;

    // 添加用户发布动态接口, 再添加切面, 在注解中传入的值代表着相关角色的用户是不被允许调用相关接口的
//    @ApiLimitedRole(limitedRoleCodeList = {ROLE_LV0})
    @DataLimited
    @PostMapping("/user-moments")
    public JsonResponse<String> addUserMoments(@RequestBody UserMoment userMoment) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        userMoment.setUserId(userId);
        userMomentsService.addUserMoments(userMoment);
        return JsonResponse.success();
    }

    // 查询用户关注的动态
    @GetMapping("/user-subscribed-moments")
    public JsonResponse<List<UserMoment>> getUserSubscribedMoments() {
        Long userId = userSupport.getCurrentUserId();
        List<UserMoment> list = userMomentsService.getUserSubscribedMoments(userId);
        return new JsonResponse<>(list);
    }
}
