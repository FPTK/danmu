package org.imooc.bilibili.api;

import com.alibaba.fastjson.JSONObject;
import org.imooc.bilibili.api.support.UserSupport;
import org.imooc.bilibili.domain.JsonResponse;
import org.imooc.bilibili.domain.PageResult;
import org.imooc.bilibili.domain.User;
import org.imooc.bilibili.domain.UserInfo;
import org.imooc.bilibili.service.UserFollowingService;
import org.imooc.bilibili.service.UserService;
import org.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

@RestController
public class UserApi {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserFollowingService userFollowingService;

    @GetMapping("/users")
    public JsonResponse<User> getUserInfo() {
        Long userId = userSupport.getCurrentUserId();
        User user = userService.getUserInfo(userId);
        return new JsonResponse<>(user);
    }

    /**
     * pks: publics
     *
     * @return 获取RSA加密的公钥
     */
    @GetMapping("/rsa-pks")
    public JsonResponse<String> getRsaPublicKey() {
        String pk = RSAUtil.getPublicKeyStr();
        return new JsonResponse<>(pk);
    }

    /**
     * 新用户注册接口
     *
     * @param user
     * @return 返回添加用户成功
     */
    @PostMapping("/users")
    public JsonResponse<String> addUser(@RequestBody User user) {
        userService.addUser(user);
        return JsonResponse.success();
    }

    /**
     * 用户第一次登录, 实际上是请求一个登录令牌
     * 改进: 使用双token进行用户登录认证
     *
     * @param user
     * @return 返回token
     */
    @PostMapping("/user-token")
    public JsonResponse<String> login(@RequestBody User user) throws Exception {
        String token = userService.login(user);
        return new JsonResponse<>(token);
    }

    /**
     * 更新用户相关信息到user-info表中
     *
     * @param userInfo
     * @return
     */
    @PutMapping("/user-infos")
    public JsonResponse<String> updateUserInfos(@RequestBody UserInfo userInfo) {
        Long userId = userSupport.getCurrentUserId();
        userInfo.setUserId(userId);
        userService.updateUserInfos(userInfo);
        return JsonResponse.success();
    }

    /**
     * 更新用户信息到user表中
     *
     * @param user
     * @return
     * @throws Exception
     */
    @PutMapping("/users")
    public JsonResponse<String> updateUsers(@RequestBody User user) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        user.setId(userId);
        userService.updateUsers(user);
        return JsonResponse.success();
    }

    /**
     * 分页查询用户列表, 满足nick模糊查询
     *
     * @param no   第几页
     * @param size 每页显示几条数据
     * @param nick 想要查的用户模糊匹配
     */
    @GetMapping("/user-infos")
    public JsonResponse<PageResult<UserInfo>> PageListUserInfos(@RequestParam Integer no, @RequestParam Integer size, String nick) {
        Long userId = userSupport.getCurrentUserId();
        // 类似于map的一个类
        JSONObject params = new JSONObject();
        params.put("no", no);
        params.put("size", size);
        params.put("nick", nick);
        params.put("userId", userId);
        PageResult<UserInfo> result = userService.PageListUserInfos(params);
        // 接口是为了用户关注服务的, 判断查到的用户列表有没有被当前登录的用户关注, 如果没有进行关注
        if (result.getTotal() > 0) {
            // 返回的还是userInfo的列表, 但是会标注followed字段
            List<UserInfo> checkedUserInfoList = userFollowingService.checkFollowingStatus(result.getList(), userId);
            result.setList(checkedUserInfoList);
        }
        return new JsonResponse<>(result);
    }

    /**
     * 双token认证, access-token和refresh-token
     *
     * @param user
     * @return
     */
    @PostMapping("/user-dts")
    public JsonResponse<Map<String, Object>> loginForDts(@RequestBody User user) throws Exception {
        Map<String, Object> map = userService.loginForDts(user);
        return new JsonResponse<>(map);
    }

    /**
     * 登出方法, 需要删除refresh-token
     * @param request
     * @return
     */
    @DeleteMapping("/refresh-tokens")
    public JsonResponse<String> logout(HttpServletRequest request) {
        String refreshToken = request.getHeader("refreshToken");
        Long userId = userSupport.getCurrentUserId();
        userService.logout(refreshToken, userId);
        return JsonResponse.success();
    }

    /**
     * 刷新时返回一个新的access-token给前端
     */
    @PostMapping("/access-tokens")
    public JsonResponse<String> refreshAccessToken(HttpServletRequest request) throws Exception {
        String refreshToken = request.getHeader("refreshToken");
        String accessToken = userService.refreshAccessToken(refreshToken);
        return new JsonResponse<>(accessToken);
    }

}
