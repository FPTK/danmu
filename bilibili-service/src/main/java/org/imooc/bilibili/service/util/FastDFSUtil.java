package org.imooc.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.mysql.cj.util.StringUtils;
import org.imooc.bilibili.domain.Exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

@Component
public class FastDFSUtil {

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${fdfs.http.storage-addr}")
    private String httpFdfsStorageAddr;

    public static final String DEFAULT_GROUP = "group1";
    private static final String PATH_KEY = "path-key:";
    private static final String UPLOADED_SIZE_KEY = "uploaded-size-key:";
    private static final String UPLOADED_NO_KEY = "uploaded-no-key:";
    private static final int SLICE_SIZE = 1024 * 1024 * 2; //2MB

    /**
     * 获取文件类型
     * @param file
     * @return
     */
    public String getFileType(MultipartFile file) {
        if (file == null) {
            throw new ConditionException("非法文件! ");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new ConditionException("非法文件! ");
        }
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index + 1);
    }

    // 上传
    public String uploadCommoFile(MultipartFile file) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();
        String fileType = this.getFileType(file);
        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(), file.getSize(), fileType, metaDataSet);
        return storePath.getPath();
    }

    // 删除
    public void deleteFile(String filePath) {
        fastFileStorageClient.deleteFile(filePath);
    }


    // 上传可以断点续传的文件
    public String uploadAppenderFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String fileType = this.getFileType(file);
        StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, file.getInputStream(), file.getSize(), fileType);
        return storePath.getPath();
    }

    // 断点续传处添加文件
    public void modifyAppenderFile(MultipartFile file, String filePath, long offset) throws Exception {
        appendFileStorageClient.modifyFile(DEFAULT_GROUP, filePath, file.getInputStream(), file.getSize(), offset);
    }



    // 通过分片数上传文件
    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if (file == null || sliceNo == null || totalSliceNo == null) {
            throw new ConditionException("参数异常! ");
        }
        String pathKey = PATH_KEY + fileMd5;
        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5;
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5; // 通过这个key判断文件上传到哪个分片和是否上传完毕
        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
        Long uploadedSize = 0L;
        if (!StringUtils.isNullOrEmpty(uploadedSizeStr)) {
            uploadedSize = Long.valueOf(uploadedSizeStr);
        }
        String fileType = this.getFileType(file);
        // 上传第一个文件
        if (sliceNo == 1) {
            String path = this.uploadAppenderFile(file);
            if (StringUtils.isNullOrEmpty(path)) {
                throw new ConditionException("上传失败!");
            }
            redisTemplate.opsForValue().set(pathKey, path);
            redisTemplate.opsForValue().set(uploadedNoKey, "1");
        } else {
            // 如果不是第一片分片
            String filePath = redisTemplate.opsForValue().get(pathKey);
            if (StringUtils.isNullOrEmpty(filePath)) {
                throw new ConditionException("上传失败!");
            }
            this.modifyAppenderFile(file, filePath, uploadedSize);
            // 将对应的序号进行+1操作
            redisTemplate.opsForValue().increment(uploadedNoKey);
        }
        // 修改历史上传分片文件大小
        uploadedSize += file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));
        // 判断上传过程是否可以结束, 比对现在上传序号和总的序号是否一致, 如果一致, 将redis数据删掉, 返回给前端上传好的文件路径即可
        String uploadedNoStr = redisTemplate.opsForValue().get(uploadedNoKey);
        if (uploadedNoStr == null) {
            throw new ConditionException("参数异常!");
        }
        Integer uploadedNo = Integer.valueOf(uploadedNoStr);
        String resultPath = "";
        if (uploadedNo.equals(totalSliceNo)) {
            resultPath = redisTemplate.opsForValue().get(pathKey);
            List<String> keyList = Arrays.asList(uploadedNoKey, uploadedSizeKey, pathKey);
            // redis有一个方法可以删除list中的key
            redisTemplate.delete(keyList);
        }
        return resultPath;
    }

    // 把文件分片
    public void convertFileToSlices(MultipartFile multipartFile) throws Exception {
        String fileName = multipartFile.getOriginalFilename();
        String fileType = this.getFileType(multipartFile);
        File file = this.multipartFileToFile(multipartFile);
        long fileLength = file.length();
        int count = 1;
        // 循环对文件进行分片, 每次i增加的大小事分片的大小
        for (int i = 0; i < fileLength; i+=SLICE_SIZE) {
            // RandomAccessFile类, 支持文件的随机访问, 可以从任意位置读写文件
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            // seek方法: 从哪个地方开始读写文件
            randomAccessFile.seek(i);
            byte[] bytes = new byte[SLICE_SIZE];
            int len = randomAccessFile.read(bytes);
            String path = "D:\\project\\files\\" + count + "." + fileType;
            File slice = new File(path);
//            System.out.println(path);
            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes, 0, len);
            fos.close();
            randomAccessFile.close();
            count++;
        }
        file.delete();
    }

    // 将multipartfile 转换为java自带的file类型
    public File multipartFileToFile(MultipartFile multipartFile) throws Exception {
        String originalFilename = multipartFile.getOriginalFilename();
        String[] fileName = originalFilename.split("\\.");
        File file = File.createTempFile(fileName[0], "." + fileName[1]);
        multipartFile.transferTo(file);
        return file;
    }

    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws Exception {
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        long totalFileSize = fileInfo.getFileSize();
        String url = httpFdfsStorageAddr + path;
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>();
        // 从request.header中取出浏览器请求的视频范围range
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }
        String rangeStr = request.getHeader("Range");
        String[] range;
        if (StringUtils.isNullOrEmpty(rangeStr)) {
            rangeStr = "bytes=0-" + (totalFileSize - 1);
        }
        range = rangeStr.split("bytes=|-");
//        Long.parseLong 的返回类型是 long，它返回一个基本数据类型 long 的值。
//        Long.valueOf 的返回类型是 Long，它返回一个 Long 包装对象。
        long begin = 0;
        // 如果range数组中只有2个元素, 说明range标头只有begin
        if (range.length >= 2) {
            begin = Long.parseLong(range[1]);
        }
        long end = totalFileSize - 1;
        if (range.length >= 3) {
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;
        // 接下来写response, 将range写入响应头
        String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Content-Type", "video/mp4");
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLength((int) len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        HttpUtil.get(url, headers, response);
    }
    // 下载
}
