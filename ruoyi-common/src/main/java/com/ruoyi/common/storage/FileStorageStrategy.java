package com.ruoyi.common.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储策略接口
 */
public interface FileStorageStrategy {
    
    /**
     * 上传文件
     * @param file 上传的文件
     * @param subPath 子路径（相对于基础路径）
     * @return 文件完整路径
     */
    String upload(MultipartFile file, String subPath) throws Exception;
    
    /**
     * 下载文件
     * @param filePath 文件路径
     * @return 文件输入流
     */
    InputStream download(String filePath) throws Exception;
    
    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    boolean delete(String filePath) throws Exception;
    
    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return true-存在，false-不存在
     */
    boolean exists(String filePath) throws Exception;
    
    /**
     * 获取完整路径
     * @param subPath 子路径
     * @return 完整路径
     */
    String getFullPath(String subPath);

    /**
     * 获取存储根目录
     * @return 根目录路径
     */
    default String getBaseDir() { return null; }
    
    /**
     * 列出目录下的文件和子目录
     * @param dirPath 目录路径
     * @return 文件信息列表
     */
    List<FileInfo> listFiles(String dirPath) throws Exception;
    
    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return true-创建成功，false-创建失败或已存在
     */
    boolean mkdir(String dirPath) throws Exception;
    
    /**
     * 重命名文件或文件夹
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return true-重命名成功，false-重命名失败
     */
    boolean rename(String oldPath, String newPath) throws Exception;
    
    /**
     * 分页列出目录下的文件和子目录
     * @param dirPath 目录路径
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量
     * @return 文件信息列表
     */
    default List<FileInfo> listFilesPaginated(String dirPath, int pageNum, int pageSize) throws Exception {
        // 默认实现：读取全部后分页
        List<FileInfo> allFiles = listFiles(dirPath);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allFiles.size());
        if (start >= allFiles.size()) {
            return new ArrayList<>();
        }
        return allFiles.subList(start, end);
    }
    
    /**
     * 统计目录下的文件和子目录数量
     * @param dirPath 目录路径
     * @return 文件数量
     */
    default int countFiles(String dirPath) throws Exception {
        return listFiles(dirPath).size();
    }
    
    /**
     * 文件信息内部类
     */
    class FileInfo {
        private String name;
        private String path;
        @JsonProperty("directory")
        private boolean isDirectory;
        private long size;
        private long lastModified;
        
        public FileInfo() {}
        
        public FileInfo(String name, String path, boolean isDirectory, long size, long lastModified) {
            this.name = name;
            this.path = path;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isDirectory() { return isDirectory; }
        public void setDirectory(boolean directory) { isDirectory = directory; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}
