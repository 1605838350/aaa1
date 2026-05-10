package com.ruoyi.common.storage.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.storage.FileStorageStrategy;
import com.ruoyi.common.utils.webdav.WebDavClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service("webdavFileStorageStrategy")
public class WebdavFileStorageStrategy implements FileStorageStrategy {

    private final String webdavUrl;
    private final String username;
    private final String password;
    private final int connectTimeoutMs;
    private final int connectionRequestTimeoutMs;
    private final int responseTimeoutMs;
    private final int maxConnTotal;
    private final int maxConnPerRoute;
    private WebDavClient webDavClient;

    public WebdavFileStorageStrategy() {
        this.webdavUrl = RuoYiConfig.getWebdavUrl();
        this.username = RuoYiConfig.getWebdavUsername();
        this.password = RuoYiConfig.getWebdavPassword();
        this.connectTimeoutMs = RuoYiConfig.getWebdavConnectTimeoutMs() == null ? 10000 : RuoYiConfig.getWebdavConnectTimeoutMs();
        this.connectionRequestTimeoutMs = RuoYiConfig.getWebdavConnectionRequestTimeoutMs() == null ? 30000 : RuoYiConfig.getWebdavConnectionRequestTimeoutMs();
        this.responseTimeoutMs = RuoYiConfig.getWebdavResponseTimeoutMs() == null ? 600000 : RuoYiConfig.getWebdavResponseTimeoutMs();
        this.maxConnTotal = RuoYiConfig.getWebdavMaxConnTotal() == null ? 200 : RuoYiConfig.getWebdavMaxConnTotal();
        this.maxConnPerRoute = RuoYiConfig.getWebdavMaxConnPerRoute() == null ? 50 : RuoYiConfig.getWebdavMaxConnPerRoute();
        initWebDavClient();
    }

    private void initWebDavClient() {
        if (this.webdavUrl != null && !this.webdavUrl.isEmpty()
                && this.username != null && this.password != null) {
            this.webDavClient = new WebDavClient(
                    this.webdavUrl,
                    this.username,
                    this.password,
                    this.connectTimeoutMs,
                    this.connectionRequestTimeoutMs,
                    this.responseTimeoutMs,
                    this.maxConnTotal,
                    this.maxConnPerRoute
            );
        }
    }

    @Override
    public String upload(MultipartFile file, String subPath) throws Exception {
        ensureClientInitialized();
        
        // 规范化路径：去除开头的 /，确保路径格式一致
        String normalizedSubPath = subPath != null ? subPath.trim() : "";
        if (normalizedSubPath.startsWith("/")) {
            normalizedSubPath = normalizedSubPath.substring(1);
        }
        
        String fullPath = getFullPath(normalizedSubPath);

        // 先尝试直接上传（某些服务器支持 PUT 时自动创建目录）
        try {
            webDavClient.upload(normalizedSubPath, file.getInputStream(), file.getSize());
            return fullPath;
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            
            // 409 = 父目录不存在；405 = 某些服务器对不存在路径返回此方法不允许
            if (errorMsg != null && (errorMsg.contains("409") || errorMsg.contains("405"))) {
                // 提取目录部分（去掉文件名）
                String dirPath = extractDirectoryPath(normalizedSubPath);
                if (dirPath != null && !dirPath.isEmpty()) {
                    try {
                        webDavClient.mkdirs(dirPath);
                    } catch (Exception mkdirEx) {
                        // 即使创建目录失败，也尝试再上传一次（某些服务器不支持 MKCOL 但 PUT 能自动创建）
                    }
                    
                    // 重新尝试上传
                    webDavClient.upload(normalizedSubPath, file.getInputStream(), file.getSize());
                    return fullPath;
                }
            }
            // 其他错误直接抛出
            throw e;
        }
    }

    /**
     * 从文件路径中提取目录路径
     * @param filePath 文件路径（如 "docker/test/file.txt"）
     * @return 目录路径（如 "/docker/test"），如果没有目录则返回 null
     */
    private String extractDirectoryPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            // 返回目录部分，前面加上 / 以符合 WebDAV 路径格式
            return "/" + filePath.substring(0, lastSlashIndex);
        }
        
        // 没有目录部分（文件直接在根目录）
        return null;
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        ensureClientInitialized();
        // 提取相对路径（去掉 http:// 或 https:// 前缀的基础 URL）
        String relativePath = extractRelativePath(filePath);

        // 使用相对路径下载
        return webDavClient.download(relativePath);
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        ensureClientInitialized();
        String path = extractRelativePath(filePath);
        webDavClient.delete(path);
        return true;
    }

    @Override
    public boolean exists(String filePath) throws Exception {
        ensureClientInitialized();
        String path = extractRelativePath(filePath);
        return webDavClient.exists(path);
    }

    @Override
    public String getFullPath(String subPath) {
        String cleanPath = subPath != null && subPath.startsWith("/") ? subPath.substring(1) : subPath;
        return webdavUrl + "/" + cleanPath;
    }

    @Override
    public List<FileInfo> listFiles(String dirPath) throws Exception {
        ensureClientInitialized();
        String relativePath = extractRelativePath(dirPath);
        //System.out.println("[DEBUG] WebdavFileStorageStrategy.listFiles: dirPath=" + dirPath + ", relativePath=" + relativePath + ", webdavUrl=" + webdavUrl);
        return webDavClient.listFiles(relativePath);
    }

    /**
     * 从完整路径中提取相对于 WebDAV 基础 URL 的路径
     */
    private String extractRelativePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // 如果以 webdavUrl 开头，去除它
        if (filePath.startsWith(webdavUrl)) {
            return filePath.substring(webdavUrl.length()).replaceFirst("^/+", "");
        }
        
        // 去除开头的 /，WebDAV 路径不应该以 / 开头
        if (filePath.startsWith("/")) {
            return filePath.substring(1);
        }
        
        return filePath;
    }

    /**
     * 确保客户端已初始化
     */
    private void ensureClientInitialized() {
        if (webDavClient == null) {
            throw new IllegalStateException("WebDAV 客户端未初始化，请检查配置是否正确");
        }
    }
    
    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return true-创建成功，false-已存在或失败
     */
    @Override
    public boolean mkdir(String dirPath) throws Exception {
        ensureClientInitialized();
        
        // 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 检查目录是否已存在
        if (webDavClient.exists(normalizedPath)) {
            return false;
        }
        
        // 创建目录
        try {
            webDavClient.mkdirs(normalizedPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 重命名文件或文件夹
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return true-重命名成功，false-重命名失败
     */
    @Override
    public boolean rename(String oldPath, String newPath) throws Exception {
        ensureClientInitialized();
        
        // 规范化路径
        String normalizedOldPath = extractRelativePath(oldPath);
        String normalizedNewPath = extractRelativePath(newPath);
        
        // 检查原路径是否存在
        if (!webDavClient.exists(normalizedOldPath)) {
            return false;
        }
        
        // 检查新路径是否已存在
        if (webDavClient.exists(normalizedNewPath)) {
            return false;
        }
        
        // 执行重命名（MOVE 操作）
        try {
            webDavClient.move(normalizedOldPath, normalizedNewPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
