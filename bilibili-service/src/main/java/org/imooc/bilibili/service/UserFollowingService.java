package org.imooc.bilibili.service;

import org.imooc.bilibili.dao.FollowingGroupDao;
import org.imooc.bilibili.dao.UserFollowingDao;
import org.imooc.bilibili.domain.*;
import org.imooc.bilibili.domain.Exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.imooc.bilibili.domain.constant.UserConstant.*;

@Service
public class UserFollowingService {

    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FollowingGroupService followingGroupService;

    @Autowired
    private UserService userService;


    /**
     * 添加用户关注
     * @param userFollowing
     */
    @Transactional
    public void addUserFollowings(UserFollowing userFollowing) {
        Long groupId = userFollowing.getGroupId();
        if (groupId == null) {
            // 如果用户没有指定groupId, 则分至默认分组
            FollowingGroup followingGroup = followingGroupService.getByType(USER_FOLLOWING_GROUP_TYPE_DEFAULT);
            userFollowing.setGroupId(followingGroup.getId());
        } else {
            // 如果用户指定groupId
            FollowingGroup followingGroup = followingGroupService.getById(groupId);
            if (followingGroup == null) {
                throw new ConditionException("分组不存在！");
            }
        }
        // 看看关注的用户存不存在
        Long followingId = userFollowing.getFollowingId();
        // 一般是service和service进行交互
        User user = userService.getUserById(followingId);
        if (user == null) {
            throw new ConditionException("关注用户不存在！");
        }
        // 作关联关系更新时先删除对应的关联关系, 涉及到删除和更新操作, 最好加事务
        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(), userFollowing.getFollowingId());
        userFollowing.setCreateTime(new Date());
        userFollowingDao.addUserFollowing(userFollowing);
    }

    // 1.获取关注的用户列表
    // 2.根据关注用户的id查询关注用户的基本信息
    // 3.将关注用户按关注分组进行分类
    public List<FollowingGroup> getUserFollowings(Long userId) {
        List<UserFollowing> list = userFollowingDao.getUserFollowings(userId);
        // 提取UserFollowing中的followingId
        Set<Long> followingIdSet = list.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        if (followingIdSet.size() > 0) {
            userInfoList = userService.getUserInfoByUserIds(followingIdSet);
        }
        // 第二步, 将查询出来的基本信息放入userFollowing中
        for (UserFollowing userFollowing : list) {
            for (UserInfo userInfo : userInfoList) {
                if (userFollowing.getFollowingId().equals(userInfo.getUserId())) {
                    userFollowing.setUserInfo(userInfo);
                }
            }
        }

        // 根据userId 查询用户有几个关注分组, 分别是什么
        List<FollowingGroup> groupList = followingGroupService.getByUserId(userId);
        FollowingGroup allGroup = new FollowingGroup();
        // 创建一个用户的全部关注的分组, 并放入所有的关注用户信息
        allGroup.setName(USER_FOLLOWING_GROUP_ALL_NAME);
        allGroup.setFollowingUserInfoList(userInfoList);
        // 添加一个返回的分组list
        List<FollowingGroup> result = new ArrayList<>();
        result.add(allGroup);
        // 第三步
        for (FollowingGroup group : groupList) {
            List<UserInfo> infoList = new ArrayList<>();
            for (UserFollowing userFollowing : list) {
                if (group.getId().equals(userFollowing.getGroupId())) {
                    infoList.add(userFollowing.getUserInfo());
                }
            }
            group.setFollowingUserInfoList(infoList);
            result.add(group);
        }
        return result;
    }

    // 1.获取当前用户的粉丝列表
    // 2.根据粉丝的用户id查询粉丝的基本信息
    // 3.查询当前用户是否已经关注该粉丝(是否互粉)
    public List<UserFollowing> getUserFans(Long userId) {
        List<UserFollowing> fanList = userFollowingDao.getUserFans(userId);
        // 提取UserFollowing中的followingId
        Set<Long> fanIdSet = fanList.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        // 查询相关粉丝信息
        if (fanIdSet.size() > 0) {
            userInfoList = userService.getUserInfoByUserIds(fanIdSet);
        }

        // 查询当前用户的关注列表, 与粉丝关注进行比对, 查询当前用户是否已经关注该粉丝(是否互粉)
        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId);
        for (UserFollowing fan : fanList) {
            for (UserInfo userInfo : userInfoList) {
                // 首先对userInfo中的followed进行初始化
                if (fan.getUserId().equals(userInfo.getUserId())) {
                    userInfo.setFollowed(false);
                    fan.setUserInfo(userInfo);
                }
            }
            for (UserFollowing following : followingList) {
                // 如果当前用户的关注id与粉丝的用户id相等, 那么他们就是互粉的状态
                if (following.getFollowingId().equals(fan.getUserId())) {
                    fan.getUserInfo().setFollowed(true);
                }
            }
        }
        return fanList;
    }

    /**
     * 新建用户关注分组
     * @param followingGroup
     * @return
     */
    public Long addUserFollowingGroups(FollowingGroup followingGroup) {
        followingGroup.setCreateTime(new Date());
        followingGroup.setType(USER_FOLLOWING_GROUP_TYPE_USER);
        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();
    }

    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getUserFollowingGroups(userId);
    }

    public List<UserInfo> checkFollowingStatus(List<UserInfo> userInfoList, Long userId) {
        List<UserFollowing> userFollowingList = userFollowingDao.getUserFollowings(userId);
        for (UserInfo userInfo : userInfoList) {
            userInfo.setFollowed(false);
            for (UserFollowing userFollowing : userFollowingList) {
                if (userFollowing.getFollowingId().equals(userInfo.getUserId())) {
                    userInfo.setFollowed(true);
                }
            }
        }
        return userInfoList;
    }
}
