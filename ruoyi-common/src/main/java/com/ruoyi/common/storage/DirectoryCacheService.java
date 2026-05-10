package com.ruoyi.common.storage;

import com.ruoyi.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 目录缓存服务
 * 用于缓存 NAS 目录内容，提升文件列表加载速度
 * 
 * 方案一：缓存时间延长至30分钟 + 预热机制
 * 方案二：增量更新机制（检查目录修改时间）
 */
@Service
public class DirectoryCacheService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryCacheService.class);

    /** 缓存 key 前缀 */
    private static final String CACHE_KEY_PREFIX = "dir_cache:";
    
    /** 用户级别缓存 key 前缀 */
    private static final String USER_CACHE_KEY_PREFIX = "user_dir_cache:";
    
    /** 【方案二】目录修改时间 key 前缀 */
    private static final String MTIME_KEY_PREFIX = "dir_mtime:";

    /** 【方案一】缓存过期时间（分钟）- 延长到30分钟，减少NAS访问频率 */
    private static final int CACHE_EXPIRE_MINUTES = 30;

    @Autowired(required = false)
    private RedisCache redisCache;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 获取目录下的文件列表（优先从 Redis 缓存获取）
     * @param dirPath 目录路径
     * @param forceRefresh 是否强制刷新缓存
     * @return 文件信息列表（已排序：目录在前，文件在后，按名称排序）
     */
    public List<FileStorageStrategy.FileInfo> getFiles(String dirPath, boolean forceRefresh) throws Exception {
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 尝试从 Redis 缓存获取
        if (redisCache != null && !forceRefresh) {
            try {
                String cacheKey = getCacheKey(normalizedPath);
                List<FileStorageStrategy.FileInfo> cached = redisCache.getCacheObject(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                // Redis 不可用时降级为直读
            }
        }

        try {
            // 从存储服务获取
            long startAt = System.currentTimeMillis();
            List<FileStorageStrategy.FileInfo> files = fileStorageService.listFiles(normalizedPath);
            long cost = System.currentTimeMillis() - startAt;
            if (cost > 500) {
                log.info("存储读取较慢: dirPath={}, cost={}ms, count={}", normalizedPath, cost, files != null ? files.size() : 0);
            }
            // 排序：目录在前，文件在后，按名称排序
            List<FileStorageStrategy.FileInfo> sorted = sortFiles(files);

            // 写入 Redis 缓存
            if (redisCache != null && sorted != null) {
                try {
                    String cacheKey = getCacheKey(normalizedPath);
                    redisCache.setCacheObject(cacheKey, sorted, CACHE_EXPIRE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
                } catch (Exception e) {
                    // 缓存写入失败不影响功能
                }
            }
            return sorted;
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("目录不存在，返回空列表: {}", dirPath);
                return Collections.emptyList();
            }
            throw e;
        }
    }
    
    /**
     * 【方案二】获取目录的最后修改时间
     * 通过扫描目录内容获取最新的修改时间
     */
    private long getDirectoryLastModified(String dirPath) throws Exception {
        try {
            List<FileStorageStrategy.FileInfo> items = fileStorageService.listFiles(dirPath);
            if (items == null || items.isEmpty()) {
                return 0;
            }
            return calculateDirectoryMtime(items);
        } catch (java.io.IOException e) {
            // 如果是 404 错误（目录不存在），返回 0
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return 0;
            }
            throw e;
        }
    }
    
    /**
     * 【方案二】计算目录的修改时间（取所有子项的最新修改时间）
     */
    private long calculateDirectoryMtime(List<FileStorageStrategy.FileInfo> files) {
        if (files == null || files.isEmpty()) {
            return System.currentTimeMillis();
        }
        long maxMtime = 0;
        for (FileStorageStrategy.FileInfo file : files) {
            if (file.getLastModified() > maxMtime) {
                maxMtime = file.getLastModified();
            }
        }
        return maxMtime > 0 ? maxMtime : System.currentTimeMillis();
    }

    /**
     * 分页获取目录下的文件列表（优先从缓存获取）
     * @param dirPath 目录路径
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public PageResult getFilesPaginated(String dirPath, int pageNum, int pageSize) throws Exception {
        return getFilesPaginated(dirPath, pageNum, pageSize, false);
    }

    /**
     * 分页获取目录下的文件列表（优先从缓存获取）
     * @param dirPath 目录路径
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量
     * @param forceRefresh 是否强制刷新缓存
     * @return 分页结果
     */
    public PageResult getFilesPaginated(String dirPath, int pageNum, int pageSize, boolean forceRefresh) throws Exception {
        List<FileStorageStrategy.FileInfo> allFiles = getFiles(dirPath, forceRefresh);

        int total = allFiles.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<FileStorageStrategy.FileInfo> pageFiles;
        if (start >= total) {
            pageFiles = Collections.emptyList();
        } else {
            pageFiles = new ArrayList<>(allFiles.subList(start, end));
        }

        return new PageResult(pageFiles, total, pageNum, pageSize);
    }

    /**
     * 用户级别的分页获取（禁用缓存，实时获取）
     * 权限模型：只检查第一层目录权限，子文件夹需过滤
     */
    public PageResult getFilesPaginatedForUser(String dirPath, Long userId, int pageNum, int pageSize, 
            boolean forceRefresh,
            java.util.function.Function<List<String>, List<String>> batchDirPermissionChecker) throws Exception {
        
        // 获取原始文件列表
        List<FileStorageStrategy.FileInfo> allFiles = getFiles(dirPath, forceRefresh);
        
        // 分离文件夹和文件，过滤系统隐藏目录
        List<FileStorageStrategy.FileInfo> dirs = new ArrayList<>();
        List<FileStorageStrategy.FileInfo> files = new ArrayList<>();
        for (FileStorageStrategy.FileInfo file : allFiles) {
            if (file.isDirectory()) {
                // 跳过系统隐藏目录（如 #recycle 回收站）
                if (!isSystemHiddenDir(file.getName())) {
                    dirs.add(file);
                }
            } else {
                files.add(file);
            }
        }
        
        // 批量检查子文件夹权限
        List<String> dirPaths = new ArrayList<>();
        for (FileStorageStrategy.FileInfo dir : dirs) {
            dirPaths.add(dir.getPath());
        }
        List<String> allowedDirPaths = batchDirPermissionChecker.apply(dirPaths);
        Set<String> allowedDirPathSet = new HashSet<>(allowedDirPaths);
        
        // 组装结果：有权限的文件夹 + 所有文件（文件继承父目录权限）
        List<FileStorageStrategy.FileInfo> filteredFiles = new ArrayList<>();
        for (FileStorageStrategy.FileInfo dir : dirs) {
            if (allowedDirPathSet.contains(dir.getPath())) {
                filteredFiles.add(dir);
            }
        }
        filteredFiles.addAll(files);  // 文件不过滤
        
        return performPaging(filteredFiles, pageNum, pageSize);
    }
    
    /**
     * 判断是否为系统隐藏目录
     */
    private boolean isSystemHiddenDir(String name) {
        if (name == null) return false;
        // NAS 回收站目录
        if ("#recycle".equalsIgnoreCase(name)) return true;
        // 其他以 # 开头的系统目录
        if (name.startsWith("#")) return true;
        return false;
    }
    
    /**
     * 从文件路径提取目录路径
     */
    private String extractDirPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "/";
        }
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return filePath.substring(0, lastSlash);
    }
    
    private PageResult performPaging(List<FileStorageStrategy.FileInfo> files, int pageNum, int pageSize) {
        int total = files.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<FileStorageStrategy.FileInfo> pageFiles = start >= total ? Collections.emptyList() : new ArrayList<>(files.subList(start, end));
        return new PageResult(pageFiles, total, pageNum, pageSize);
    }
    
    public void invalidateUserCache(Long userId, String dirPath) {
        if (redisCache == null || userId == null) return;
        redisCache.deleteObject(getUserCacheKey(userId, dirPath));
    }
    
    /**
     * 清除指定目录所有用户的缓存
     */
    public void invalidateAllUserCacheForDir(String dirPath) {
        if (redisCache == null) return;
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        // 匹配所有用户对该目录的缓存
        String pattern = USER_CACHE_KEY_PREFIX + "*:" + normalizedPath;
        Collection<String> keys = redisCache.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisCache.deleteObject(keys);
            log.info("已清除目录所有用户缓存: dirPath={}, 共 {} 个", dirPath, keys.size());
        }
    }

    /**
     * 清除指定目录的缓存
     * @param dirPath 目录路径
     */
    public void invalidateCache(String dirPath) {
        if (redisCache == null) {
            return;
        }

        String cacheKey = getCacheKey(dirPath);
        String mtimeKey = getMtimeKey(dirPath);
        redisCache.deleteObject(cacheKey);
        redisCache.deleteObject(mtimeKey);
        log.info("已清除目录缓存: {}", dirPath);
    }

    /**
     * 清除所有目录缓存
     */
    public void invalidateAllCache() {
        if (redisCache == null) {
            return;
        }

        // 清除目录内容缓存
        String cachePattern = CACHE_KEY_PREFIX + "*";
        Collection<String> cacheKeys = redisCache.keys(cachePattern);
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            redisCache.deleteObject(cacheKeys);
            log.info("已清除所有目录缓存，共 {} 个", cacheKeys.size());
        }
        
        // 【方案二】清除修改时间缓存
        String mtimePattern = MTIME_KEY_PREFIX + "*";
        Collection<String> mtimeKeys = redisCache.keys(mtimePattern);
        if (mtimeKeys != null && !mtimeKeys.isEmpty()) {
            redisCache.deleteObject(mtimeKeys);
            log.info("已清除所有修改时间缓存，共 {} 个", mtimeKeys.size());
        }
    }

    /**
     * 【方案一】预热目录缓存（后台异步加载）
     * 用于系统启动时或用户首次访问前预加载常用目录
     * @param dirPaths 要预热的目录路径列表
     */
    private volatile ExecutorService warmupExecutor;

    private ExecutorService getWarmupExecutor() {
        if (warmupExecutor == null) {
            synchronized (this) {
                if (warmupExecutor == null) {
                    warmupExecutor = Executors.newFixedThreadPool(4, r -> {
                        Thread t = new Thread(r, "dir-cache-warmer");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return warmupExecutor;
    }

    public void warmupCache(List<String> dirPaths) {
        if (dirPaths == null || dirPaths.isEmpty() || redisCache == null) {
            return;
        }

        ExecutorService executor = getWarmupExecutor();
        for (String dirPath : dirPaths) {
            executor.submit(() -> {
                try {
                    String cacheKey = getCacheKey(dirPath);
                    if (redisCache.getCacheObject(cacheKey) != null) {
                        log.debug("目录缓存已存在，跳过预热: {}", dirPath);
                        return;
                    }
                    getFiles(dirPath, false);
                    log.info("目录预热完成: {}", dirPath);
                } catch (Exception e) {
                    log.warn("目录预热失败: {} - {}", dirPath, e.getMessage());
                }
            });
        }
    }

    /**
     * 启动时预热：根目录 + 第一层子目录
     */
    public void warmupRootAndChildren() {
        if (redisCache == null) {
            log.info("Redis 不可用，跳过目录预热");
            return;
        }
        try {
            // 1. 先预热根目录
            List<FileStorageStrategy.FileInfo> rootFiles = getFiles("/", false);
            log.info("根目录预热完成，共 {} 项", rootFiles != null ? rootFiles.size() : 0);

            // 2. 提取第一层子目录路径，异步预热
            if (rootFiles != null) {
                List<String> childDirs = new ArrayList<>();
                for (FileStorageStrategy.FileInfo f : rootFiles) {
                    if (f.isDirectory() && !isSystemHiddenDir(f.getName())) {
                        childDirs.add(f.getPath());
                    }
                }
                if (!childDirs.isEmpty()) {
                    log.info("开始预热 {} 个一级子目录", childDirs.size());
                    warmupCache(childDirs);
                }
            }
        } catch (Exception e) {
            log.warn("启动预热失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存 key
     */
    private String getCacheKey(String dirPath) {
        // 规范化路径：统一使用 / 开头
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return CACHE_KEY_PREFIX + normalizedPath;
    }
    
    /**
     * 获取用户级别缓存 key
     */
    private String getUserCacheKey(Long userId, String dirPath) {
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return USER_CACHE_KEY_PREFIX + userId + ":" + normalizedPath;
    }
    
    /**
     * 【方案二】获取修改时间缓存 key
     */
    private String getMtimeKey(String dirPath) {
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return MTIME_KEY_PREFIX + normalizedPath;
    }

    /**
     * 排序文件列表：目录在前，文件在后，按名称排序
     */
    private List<FileStorageStrategy.FileInfo> sortFiles(List<FileStorageStrategy.FileInfo> files) {
        List<FileStorageStrategy.FileInfo> sortedFiles = new ArrayList<>(files);
        sortedFiles.sort((a, b) -> {
            // 目录在前
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            // 按名称排序
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return sortedFiles;
    }

    /**
     * 分页结果类
     */
    public static class PageResult {
        private List<FileStorageStrategy.FileInfo> files;
        private int total;
        private int pageNum;
        private int pageSize;
        private boolean hasMore;

        public PageResult(List<FileStorageStrategy.FileInfo> files, int total, int pageNum, int pageSize) {
            this.files = files;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.hasMore = pageNum * pageSize < total;
        }

        public List<FileStorageStrategy.FileInfo> getFiles() {
            return files;
        }

        public int getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public boolean isHasMore() {
            return hasMore;
        }
    }
}
