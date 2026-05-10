package com.ruoyi.common.storage;

import com.ruoyi.common.storage.impl.LocalFileStorageStrategy;
import com.ruoyi.common.storage.impl.WebdavFileStorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class FileStorageService {

    @Value("${ruoyi.storage.type:local}")
    private String storageType;

    @Autowired(required = false)
    private LocalFileStorageStrategy localFileStorageStrategy;

    @Autowired(required = false)
    private WebdavFileStorageStrategy webdavFileStorageStrategy;

    private FileStorageStrategy getStrategy() {
        if ("webdav".equalsIgnoreCase(storageType)) {
            if (webdavFileStorageStrategy == null) {
                throw new IllegalStateException("WebDAV 存储策略未启用，请检查配置 ruoyi.storage.type");
            }
            return webdavFileStorageStrategy;
        }

        if (localFileStorageStrategy == null) {
            throw new IllegalStateException("本地文件存储策略不可用");
        }
        return localFileStorageStrategy;
    }

    /**
     * 上传文件
     */
    public String upload(MultipartFile file, String subPath) throws Exception {
        return getStrategy().upload(file, subPath);
    }

    /**
     * 下载文件
     */
    public InputStream download(String filePath) throws Exception {
        return getStrategy().download(filePath);
    }

    /**
     * 删除文件
     */
    public boolean delete(String filePath) throws Exception {
        return getStrategy().delete(filePath);
    }

    /**
     * 检查文件是否存在
     */
    public boolean exists(String filePath) throws Exception {
        return getStrategy().exists(filePath);
    }

    /**
     * 获取完整路径
     */
    public String getFullPath(String subPath) {
        return getStrategy().getFullPath(subPath);
    }

    /**
     * 获取存储根目录
     */
    public String getBaseDir() {
        return getStrategy().getBaseDir();
    }

    /**
     * 获取当前存储类型
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * 动态切换存储类型（仅在当前运行期有效）
     */
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    /**
     * 列出目录下的文件和子目录
     * @param dirPath 目录路径
     * @return 文件信息列表
     */
    public List<FileStorageStrategy.FileInfo> listFiles(String dirPath) throws Exception {
        List<FileStorageStrategy.FileInfo> result = getStrategy().listFiles(dirPath);
        return result;
    }
    
    /**
     * 分页列出目录下的文件和子目录
     * @param dirPath 目录路径
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 文件信息列表
     */
    public List<FileStorageStrategy.FileInfo> listFilesPaginated(String dirPath, int pageNum, int pageSize) throws Exception {
        return getStrategy().listFilesPaginated(dirPath, pageNum, pageSize);
    }
    
    /**
     * 统计目录下的文件和子目录数量
     * @param dirPath 目录路径
     * @return 文件数量
     */
    public int countFiles(String dirPath) throws Exception {
        return getStrategy().countFiles(dirPath);
    }
    
    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return true-创建成功，false-创建失败或已存在
     */
    public boolean mkdir(String dirPath) throws Exception {
        return getStrategy().mkdir(dirPath);
    }
    
    /**
     * 重命名文件或文件夹
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return true-重命名成功，false-重命名失败
     */
    public boolean rename(String oldPath, String newPath) throws Exception {
        return getStrategy().rename(oldPath, newPath);
    }
    
    /**
     * 检查路径是否是目录
     * @param path 路径
     * @return true-是目录，false-不是目录或不存在
     */
    public boolean isDirectory(String path) throws Exception {
        // 更准确地判断是否是目录
        // 方法：检查列出的内容，如果只有一个且路径匹配，说明是文件；否则是目录
        try {
            List<FileStorageStrategy.FileInfo> files = getStrategy().listFiles(path);
            
            if (files == null || files.isEmpty()) {
                // 空列表，可能是空目录
                boolean result = path.endsWith("/");
                return result;
            }
            
            // 如果只有一个文件/文件夹，需要进一步判断
            if (files.size() == 1) {
                FileStorageStrategy.FileInfo fileInfo = files.get(0);
                String fileName = fileInfo.getName();
                String pathFileName = path.substring(path.lastIndexOf('/') + 1);
                
                // 如果返回的项目明确标记为目录，则是目录
                if (fileInfo.isDirectory()) {
                    return true;
                }
                
                // 如果文件名匹配路径名，且没有明确标记为目录，则可能是文件
                if (fileName.equals(pathFileName)) {
                    return false;
                }
            }
            
            // 多个文件，或者单个文件但不匹配路径，说明是目录
            return true;
        } catch (Exception e) {
            // 列出失败，可能是文件或不存在
            // 检查是否存在
            boolean exists = exists(path);
            if (!exists) {
                return false;
            }
            // 存在但不能列出，说明是文件
            return false;
        }
    }
}
