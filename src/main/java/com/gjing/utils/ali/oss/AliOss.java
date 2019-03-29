package com.gjing.utils.ali.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.gjing.enums.HttpStatus;
import com.gjing.ex.OssException;
import com.gjing.ex.ParamException;
import com.gjing.utils.ParamUtil;
import com.gjing.utils.TimeUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Archine
 **/
public class AliOss {
    /**
     * 实例
     */
    private static OSSClient instance = null;

    /**
     * Oss 实例化
     *
     * @return 实例
     */
    private static OSSClient getOssClient(OssModel ossModel) {
        if (instance == null) {
            synchronized (AliOss.class) {
                if (instance == null) {
                    instance = new OSSClient(ossModel.getEndPoint(), ossModel.getAccessKeyId(), ossModel.getAccessKeySecret());
                }
            }
        }
        return instance;
    }

    /**
     * 当Bucket 不存在时候创建Bucket
     */
    private static void createBucket(OssModel ossModel) {
        try {
            if (!AliOss.getOssClient(ossModel).doesBucketExist(ossModel.getBucketName())) {
                AliOss.getOssClient(ossModel).createBucket(ossModel.getBucketName());
            }
        } catch (Exception e) {
            throw new OssException("创建Bucket失败,请核对Bucket名称(规则：只能包含小写字母、数字和短横线，必须以小写字母和数字开头和结尾，长度在3-63之间)");
        }
    }

    /**
     * 通过文件名判断并获取OSS服务文件上传时文件的contentType
     * @param fileName 文件名
     * @return 文件的contentType
     */
    private static String getContentType(String fileName){
        String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1);
        if("bmp".equalsIgnoreCase(fileExtension)) {
            return "image/bmp";
        }
        if("gif".equalsIgnoreCase(fileExtension)) {
            return "image/gif";
        }
        if("jpeg".equalsIgnoreCase(fileExtension) || "jpg".equalsIgnoreCase(fileExtension)  ) {
            return "image/jpeg";
        }
        if("png".equalsIgnoreCase(fileExtension)) {
            return "image/png";
        }
        if("html".equalsIgnoreCase(fileExtension)) {
            return "text/html";
        }
        if("txt".equalsIgnoreCase(fileExtension)) {
            return "text/plain";
        }
        if("vsd".equalsIgnoreCase(fileExtension)) {
            return "application/vnd.visio";
        }
        if("ppt".equalsIgnoreCase(fileExtension) || "pptx".equalsIgnoreCase(fileExtension)) {
            return "application/vnd.ms-powerpoint";
        }
        if("doc".equalsIgnoreCase(fileExtension) || "docx".equalsIgnoreCase(fileExtension)) {
            return "application/msword";
        }
        if("xml".equalsIgnoreCase(fileExtension)) {
            return "text/xml";
        }
        return "text/html";
    }

    /**
     * 上传OSS服务器 如果同名文件会覆盖服务器上的
     *
     * @param file 文件
     * @return 文件地址
     */
    private static String uploadFile(MultipartFile file, OssModel ossModel) {
        String fileName = String.format("%s.%s", UUID.randomUUID().toString(), FilenameUtils.getExtension(file.getOriginalFilename()));
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(inputStream.available());
            objectMetadata.setCacheControl("no-cache");
            objectMetadata.setHeader("Pragma", "no-cache");
            objectMetadata.setContentType(FilenameUtils.getExtension("." + file.getOriginalFilename()));
            objectMetadata.setContentDisposition("inline;filename=" + fileName);
            AliOss.getOssClient(ossModel).putObject(ossModel.getBucketName(), ossModel.getFileDir() + "/" + fileName, inputStream, objectMetadata);
            return ossModel.getFileDir() + "/" + fileName;
        } catch (OSSException oe) {
            throw new OssException(oe.getMessage());
        } catch (ClientException | IOException ce) {
            throw new OssException(ce.getMessage());
        }
    }


    /**
     * 获取文件路径
     *
     * @param fileUrl 文件地址
     * @return 路径
     */
    private static String getFileUrl(String fileUrl, OssModel ossModel) {
        if (ParamUtil.paramIsEmpty(fileUrl)) {
            throw new ParamException("文件地址为空!");
        }
        String[] split = ParamUtil.split(fileUrl, "/");
        if (ParamUtil.paramIsEmpty(split)) {
            throw new OssException(HttpStatus.INVALID_PARAMETER.getMsg());
        }
        URL url = AliOss.getOssClient(ossModel).generatePresignedUrl(ossModel.getBucketName(), ossModel.getFileDir() + "/" + split[split.length - 1],
                TimeUtil.addDay(new Date(), 365));
        if (url == null) {
            throw new OssException("获取OSS文件URL失败!");
        }
        return url.toString();
    }

    /**
     * 获取路径地址
     *
     * @param fileName 文件名
     * @return 地址
     */
    private static String getPathUrl(String fileName, OssModel ossModel) {
        return fileName.substring(fileName.indexOf(ossModel.getEndPoint()) + ossModel.getEndPoint().length() + 1);
    }


    /**
     * 文件删除
     *
     * @param urls 文件地址集合
     * @return 返回true为删除成功
     */
    public static boolean delete(List<String> urls, OssModel ossModel) {
        List<String> urlList = urls.stream().map(e -> AliOss.getPathUrl(e, ossModel)).collect(Collectors.toList());
        try {
            AliOss.getOssClient(ossModel).deleteObjects(new DeleteObjectsRequest(ossModel.getBucketName()).withKeys(urlList));
            return true;
        } catch (RuntimeException e) {
            throw new OssException(e.getMessage());
        }
    }

    /**
     * 文件简单上传（最大文件不能超过5G）
     *
     * @param file     文件
     * @param ossModel oss模型
     * @return string
     */
    public static String upload(MultipartFile file, OssModel ossModel) {
        if (ParamUtil.paramIsEmpty(ossModel.getFileDir())) {
            ossModel.setFileDir("");
        }
        if (file.getSize() > 5 * 1024 * 1024 * 1024) {
            throw new OssException("上传失败，图片大小不能超过5G");
        }
        AliOss.createBucket(ossModel);
        String fileName = AliOss.uploadFile(file, ossModel);
        String fileOssUrl = AliOss.getFileUrl(fileName,ossModel);
        //去掉URL中的?后的时间戳
        int firstChar = fileOssUrl.indexOf("?");
        if (firstChar > 0) {
            fileOssUrl = fileOssUrl.substring(0, firstChar);
        }
        return fileOssUrl;
    }

}
