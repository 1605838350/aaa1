package com.ruoyi.common.storage.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.storage.FileStorageStrategy;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service("localFileStorageStrategy")
@DependsOn("ruoYiConfig")
public class LocalFileStorageStrategy implements FileStorageStrategy {

    private final String baseDir;

    public LocalFileStorageStrategy() {
        String profile = RuoYiConfig.getProfile();
        //System.out.println(profile);
        if (profile == null) {
            String os = System.getProperty("os.name").toLowerCase();
            profile = os.contains("windows") ? "D:/home" : "/volume1";
        }
        this.baseDir = profile;
    }

    @Override
    public String getBaseDir() {
        return baseDir;
    }

    @Override
    public String upload(MultipartFile file, String subPath) throws Exception {
        // 规范化路径：去除开头的 /，确保路径格式一致
        String normalizedSubPath = subPath != null ? subPath.trim() : "";
        if (normalizedSubPath.startsWith("/")) {
            normalizedSubPath = normalizedSubPath.substring(1);
        }
        
        // 构建完整文件路径
        String fullPath = baseDir + "/" + normalizedSubPath;
        File destFile = new File(fullPath);
        
        // 确保父目录存在
        File parentDir = destFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 保存文件
        file.transferTo(destFile);
        
        return getFullPath(normalizedSubPath);
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        // 规范化路径
        String normalizedPath = filePath;
        
        // 如果是完整的 URL 或包含 http，需要转换为本地路径
        if (filePath != null && (filePath.startsWith("http") || filePath.contains("/profile"))) {
            normalizedPath = extractRelativePath(filePath);
        }
        
        // 构建完整路径
        Path fullPath;
        // 如果已经是绝对路径（包含盘符如 D:），直接使用
        // 支持 D:\ 或 D:/ 格式
        if (normalizedPath != null && normalizedPath.length() > 1 && normalizedPath.charAt(1) == ':') {
            fullPath = Paths.get(normalizedPath);
        } else {
            // 去除开头的 / 或 \
            if (normalizedPath != null && (normalizedPath.startsWith("/") || normalizedPath.startsWith("\\"))) {
                normalizedPath = normalizedPath.substring(1);
            }
            fullPath = Paths.get(baseDir, normalizedPath);
        }
        
        // 检查文件是否存在
        File file = fullPath.toFile();
 if (!file.exists()) {
            throw new IOException("文件不存在: " + fullPath);
        }
        
        return new FileInputStream(file);
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        // 如果是完整的 URL 或包含 http，需要转换为本地路径
        String normalizedPath = filePath;
        if (filePath != null && (filePath.startsWith("http") || filePath.contains("/profile"))) {
            normalizedPath = extractRelativePath(filePath);
        }
        
        // 去除开头的 /
        if (normalizedPath != null && normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        Path fullPath = Paths.get(baseDir, normalizedPath);
        File file = fullPath.toFile();
        
        if (!file.exists()) {
            return false;
        }
        
        // 递归删除文件或目录
        return deleteRecursively(file);
    }
    
    /**
     * 递归删除文件或目录
     * @param file 要删除的文件或目录
     * @return true-删除成功，false-删除失败
     */
    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            // 递归删除子文件和子目录
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        // 删除文件或空目录
        return file.delete();
    }

    @Override
    public boolean exists(String filePath) throws Exception {
        // 如果是完整的 URL 或包含 http，需要转换为本地路径
        String normalizedPath = filePath;
        if (filePath != null && (filePath.startsWith("http") || filePath.contains("/profile"))) {
            normalizedPath = extractRelativePath(filePath);
        }
        
        // 去除开头的 /
        if (normalizedPath != null && normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        Path fullPath = Paths.get(baseDir, normalizedPath);
        return fullPath.toFile().exists();
    }

    @Override
    public String getFullPath(String subPath) {
        // 去除开头的 /
        String normalizedPath = subPath;
        if (normalizedPath != null && normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return Paths.get(baseDir, normalizedPath).toString();
    }

    /**
     * 从 URL 或路径中提取相对路径
     * 例如：/profile/upload/2024/01/01/test.png -> upload/2024/01/01/test.png
     */
    private String extractRelativePath(String filePath) {
        if (filePath == null) {
            return null;
        }

        // 移除可能的前缀
        String path = filePath;
        if (path.startsWith("http")) {
            // 提取路径部分
            int portIndex = path.indexOf(":8");
            if (portIndex > 0) {
                path = path.substring(path.indexOf("/", portIndex));
            } else {
                path = path.substring(path.indexOf("/"));
            }
        }

        // 移除 /profile 前缀
        if (path.contains("/profile/")) {
            path = path.substring(path.indexOf("/profile/") + 9);
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    @Override
    public List<FileInfo> listFiles(String dirPath) throws Exception {
        List<FileInfo> fileList = new ArrayList<>();
        
        // 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 构建完整目录路径
        String fullDirPath = baseDir + "/" + normalizedPath;
        //System.out.println("[DEBUG] listFiles: dirPath=" + dirPath + ", fullDirPath=" + fullDirPath);
        
        File dir = new File(fullDirPath);
        
        if (!dir.exists()) {
            //System.out.println("[DEBUG] Directory does not exist: " + fullDirPath);
            return fileList;
        }
        if (!dir.isDirectory()) {
            //System.out.println("[DEBUG] Not a directory: " + fullDirPath);
            return fileList;
        }
        
        File[] files = dir.listFiles();
        //System.out.println("[DEBUG] Found " + (files != null ? files.length : 0) + " files");
        
        if (files != null) {
            for (File file : files) {
                //System.out.println("[DEBUG] File: " + file.getName() + ", isDirectory=" + file.isDirectory() + ", isFile=" + file.isFile());
                FileInfo info = new FileInfo();
                info.setName(file.getName());
                info.setPath(dirPath + (dirPath.endsWith("/") ? "" : "/") + file.getName());
                info.setDirectory(file.isDirectory());
                info.setSize(file.length());
                info.setLastModified(file.lastModified());
                fileList.add(info);
            }
        }
        
        return fileList;
    }
    
    /**
     * 高效分页列出目录下的文件和子目录
     * 优化策略：快速遍历，只获取当前页数据，后端不做排序（由前端处理）
     */
    @Override
    public List<FileInfo> listFilesPaginated(String dirPath, int pageNum, int pageSize) throws Exception {
        List<FileInfo> fileList = new ArrayList<>();
        
        // 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 构建完整目录路径
        String fullDirPath = baseDir + "/" + normalizedPath;
        Path dir = Paths.get(fullDirPath);
        
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return fileList;
        }
        
        // 计算需要跳过的数量
        int skip = (pageNum - 1) * pageSize;
        int collected = 0;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (collected >= skip && collected < skip + pageSize) {
                    File file = path.toFile();
                    FileInfo info = new FileInfo();
                    info.setName(file.getName());
                    info.setPath(dirPath + (dirPath.endsWith("/") ? "" : "/") + file.getName());
                    info.setDirectory(file.isDirectory());
                    info.setSize(file.length());
                    info.setLastModified(file.lastModified());
                    fileList.add(info);
                }
                collected++;
                
                // 如果已经收集够当前页，提前退出
                if (collected >= skip + pageSize) {
                    break;
                }
            }
        }
        
        return fileList;
    }
    
    /**
     * 统计目录下的文件和子目录数量（高效实现）
     */
    @Override
    public int countFiles(String dirPath) throws Exception {
        // 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 构建完整目录路径
        String fullDirPath = baseDir + "/" + normalizedPath;
        Path dir = Paths.get(fullDirPath);
        
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return 0;
        }
        
        // 使用 stream 统计数量
        try (Stream<Path> stream = Files.list(dir)) {
            return (int) stream.count();
        }
    }
    
    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return true-创建成功，false-已存在或创建失败
     */
    @Override
    public boolean mkdir(String dirPath) throws Exception {
        // 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // 构建完整目录路径
        Path fullDirPath = Paths.get(baseDir, normalizedPath);
        File dir = fullDirPath.toFile();
        
        // 如果目录已存在，返回 false
        if (dir.exists()) {
            return false;
        }
        
        // 创建目录（包括父目录）
        return dir.mkdirs();
    }
    
    /**
     * 重命名文件或文件夹
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return true-重命名成功，false-重命名失败
     */
    @Override
    public boolean rename(String oldPath, String newPath) throws Exception {
        // 规范化路径
        String normalizedOldPath = oldPath.startsWith("/") ? oldPath.substring(1) : oldPath;
        String normalizedNewPath = newPath.startsWith("/") ? newPath.substring(1) : newPath;
        
        // 构建完整路径
        Path fullOldPath = Paths.get(baseDir, normalizedOldPath);
        Path fullNewPath = Paths.get(baseDir, normalizedNewPath);
        
        File oldFile = fullOldPath.toFile();
        File newFile = fullNewPath.toFile();
        
        // 检查原文件是否存在
        if (!oldFile.exists()) {
            return false;
        }
        
        // 检查新路径是否已存在
        if (newFile.exists()) {
            return false;
        }
        
        // 执行重命名
        return oldFile.renameTo(newFile);
    }
}
