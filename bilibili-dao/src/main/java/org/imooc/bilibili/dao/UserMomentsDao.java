package org.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.imooc.bilibili.domain.UserMoment;

@Mapper
public interface UserMomentsDao {
    Integer addUserMoments(UserMoment userMoment);
}
