package org.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.imooc.bilibili.domain.auth.AuthRole;

@Mapper
public interface AuthRoleDao {
    AuthRole getRoleByCode(String code);
}
