package org.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.imooc.bilibili.domain.UserFollowing;

import java.util.List;

@Mapper
public interface UserFollowingDao {
    Integer deleteUserFollowing(Long userId, Long followingId);

    Integer addUserFollowing(UserFollowing userFollowing);

    List<UserFollowing> getUserFollowings(Long userId);

    List<UserFollowing> getUserFans(Long userId);

}
