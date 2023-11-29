package org.imooc.bilibili.dao;

import org.imooc.bilibili.domain.File;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileDao {

    Integer addFile(File file);

    File getFileByMD5(String md5);
}
