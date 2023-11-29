package org.imooc.bilibili.service;

import com.mysql.cj.util.StringUtils;
import org.imooc.bilibili.dao.FileDao;
import org.imooc.bilibili.domain.File;
import org.imooc.bilibili.service.util.FastDFSUtil;
import org.imooc.bilibili.service.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Service
public class FileService {

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private FileDao fileDao;

    public String uploadFileBySlices(MultipartFile slice, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        File dbFileMD5 = fileDao.getFileByMD5(fileMd5);
        if (dbFileMD5 != null) {
            return dbFileMD5.getUrl();
        }
        String url = fastDFSUtil.uploadFileBySlices(slice, fileMd5, sliceNo, totalSliceNo);
        if (!StringUtils.isNullOrEmpty(url)) {
            dbFileMD5 = new File();
            dbFileMD5.setCreateTime(new Date());
            dbFileMD5.setType(fastDFSUtil.getFileType(slice));
            dbFileMD5.setUrl(url);
            dbFileMD5.setMd5(fileMd5);
            fileDao.addFile(dbFileMD5);
        }
        return url;
    }

    public String getFileMD5(MultipartFile file) throws Exception {
        return MD5Util.getFileMD5(file);
    }

}
