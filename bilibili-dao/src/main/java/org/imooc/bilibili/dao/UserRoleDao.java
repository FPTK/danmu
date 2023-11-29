package org.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.imooc.bilibili.domain.auth.UserRole;

import java.util.List;

@Mapper
public interface UserRoleDao {
    List<UserRole> getUserRoleByUserId(Long userId);

    Integer addUserRole(UserRole userRole);

}
