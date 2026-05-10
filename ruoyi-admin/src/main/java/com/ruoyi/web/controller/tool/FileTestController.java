package com.ruoyi.web.controller.tool;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.storage.DirectoryCacheService;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.storage.FileStorageStrategy;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.service.IFileDirPermissionService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.service.FullTextSearchService;
import com.ruoyi.common.utils.file.DocumentContentExtractor;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文件测试控制器
 */
@RestController
@RequestMapping("/file")
public class FileTestController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(FileTestController.class);
    private static final String UPLOAD_PERF_LOG_KEY = "upload.perf.log.enabled";
    private static final String UPLOAD_ASYNC_TRANSFER_KEY = "upload.async.transfer.enabled";

    private static final ConcurrentMap<String, ChunkUploadSession> CHUNK_UPLOAD_SESSION_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, AsyncTransferTask> ASYNC_TRANSFER_TASK_MAP = new ConcurrentHashMap<>();
    private static final long CHUNK_SESSION_EXPIRE_MILLIS = 24 * 60 * 60 * 1000L;
    private static final long ASYNC_TASK_EXPIRE_MILLIS = 24 * 60 * 60 * 1000L;
    private static final ExecutorService ASYNC_TRANSFER_EXECUTOR = Executors.newFixedThreadPool(2);
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private IFileDirPermissionService fileDirPermissionService;
    
    @Autowired
    private DirectoryCacheService directoryCacheService;

    @Autowired
    private ISysConfigService configService;
    
    @Autowired
    private FullTextSearchService fullTextSearchService;
    
    /**
     * 上传单个文件（使用默认路径）
     * 权限检查：使用默认路径，仅限管理员
     */
    @Log(title = "文件上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        
        // 指定上传的子路径（会自动创建日期目录）
        String subPath = "test/" + DateUtils.datePath() + "/" + file.getOriginalFilename();

        InputStream contentStream = file.getInputStream();
        String fullPath = fileStorageService.upload(file, subPath);

        fullTextSearchService.indexFileAsync(fullPath, file.getOriginalFilename(), contentStream);

        return AjaxResult.success("上传成功", fullPath);
    }

    /**
     * 上传单个文件到指定路径（支持自定义上传位置）
     * 权限检查：基于文件目录权限系统
     * @param file 上传的文件
     * @param path 自定义上传路径，如 "docker/test/file.txt" 或 "docker/test/"（自动使用原文件名）
     */
    @Log(title = "文件上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload-to")
    public AjaxResult uploadTo(@RequestParam("file") MultipartFile file,
                               @RequestParam("path") String path) throws Exception {
        if (file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        if (StringUtils.isEmpty(path)) {
            return AjaxResult.error("上传路径不能为空");
        }

        // 构建完整的子路径
        String subPath;
        if (path.endsWith("/")) {
            // 如果路径以 / 结尾，自动拼接原文件名
            subPath = path + file.getOriginalFilename();
        } else {
            // 否则使用指定的完整路径（包含文件名）
            subPath = path;
        }

        // 检查目录上传权限
        String dirPath = extractDirPath(subPath);
        
        // 检查是否为根目录上传：只有管理员才能直接上传到根目录
        if ("/".equals(dirPath) && !SecurityUtils.isAdmin()) {
            return AjaxResult.error("只有管理员才能直接上传文件到根目录");
        }
        
        if (!fileDirPermissionService.hasUploadPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权在该目录上传文件");
        }

        // 上传前获取输入流（复用内存中的文件内容，避免索引时再读磁盘）
        InputStream contentStream = file.getInputStream();
        String fullPath = fileStorageService.upload(file, subPath);

        // 异步添加到全文索引
        fullTextSearchService.indexFileAsync(fullPath, file.getOriginalFilename(), contentStream);

        // 清除该目录的缓存
        directoryCacheService.invalidateCache(dirPath);
        
        return AjaxResult.success("上传成功", fullPath);
    }
    
    /**
     * 上传多个文件（使用默认路径）
     * 权限检查：使用默认路径，仅限管理员
     */
    @Log(title = "文件批量上传", businessType = BusinessType.INSERT)
    @PostMapping("/uploads")
    public AjaxResult uploads(@RequestParam("files") List<MultipartFile> files) throws Exception {
        if (files == null || files.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        
        List<String> fullPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String subPath = "batch/" + DateUtils.datePath() + "/" + file.getOriginalFilename();
                InputStream contentStream = file.getInputStream();
                String fullPath = fileStorageService.upload(file, subPath);
                fullPaths.add(fullPath);

                fullTextSearchService.indexFileAsync(fullPath, file.getOriginalFilename(), contentStream);
            }
        }
        
        return AjaxResult.success("批量上传成功", fullPaths);
    }

    /**
     * 上传多个文件到指定目录（支持自定义上传位置）
     * 权限检查：基于文件目录权限系统
     * @param files 上传的文件列表
     * @param dir 目标目录，如 "docker/test/"（必须以 / 结尾）
     */
    @Log(title = "文件批量上传", businessType = BusinessType.INSERT)
    @PostMapping("/uploads-to")
    public AjaxResult uploadsTo(@RequestParam("files") List<MultipartFile> files,
                                @RequestParam("dir") String dir) throws Exception {
        if (files == null || files.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        if (StringUtils.isEmpty(dir)) {
            return AjaxResult.error("目标目录不能为空");
        }
        if (!dir.endsWith("/")) {
            return AjaxResult.error("目标目录必须以 / 结尾");
        }

        // 检查目录上传权限
        String dirPath = dir.substring(0, dir.length() - 1); // 去掉末尾的 /
        
        // 检查是否为根目录上传：只有管理员才能直接上传到根目录
        if ("/".equals(dirPath) && !SecurityUtils.isAdmin()) {
            return AjaxResult.error("只有管理员才能直接上传文件到根目录");
        }
        
        if (!fileDirPermissionService.hasUploadPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权在该目录上传文件");
        }

        List<String> fullPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String subPath = dir + file.getOriginalFilename();
                InputStream contentStream = file.getInputStream();
                String fullPath = fileStorageService.upload(file, subPath);
                fullPaths.add(fullPath);

                fullTextSearchService.indexFileAsync(fullPath, file.getOriginalFilename(), contentStream);
            }
        }

        // 清除该目录的缓存
        directoryCacheService.invalidateCache(dirPath);

        return AjaxResult.success("批量上传成功", fullPaths);
    }

    @Log(title = "分片上传初始化", businessType = BusinessType.INSERT)
    @PostMapping("/chunk/init")
    public AjaxResult initChunkUpload(@RequestBody ChunkInitRequest request) throws Exception {
        cleanupExpiredChunkSessions();

        if (request == null || StringUtils.isEmpty(request.getFileName()) || StringUtils.isEmpty(request.getPath())) {
            return AjaxResult.error("初始化参数不完整");
        }
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            return AjaxResult.error("文件大小不合法");
        }
        if (request.getChunkSize() == null || request.getChunkSize() <= 0) {
            return AjaxResult.error("分片大小不合法");
        }
        if (request.getTotalChunks() == null || request.getTotalChunks() <= 0) {
            return AjaxResult.error("分片总数不合法");
        }

        String targetPath = buildTargetUploadPath(request.getPath(), request.getFileName());
        String dirPath = extractDirPath(targetPath);
        Long userId = SecurityUtils.getUserId();

        // 检查是否为根目录上传：只有管理员才能直接上传到根目录
        if ("/".equals(dirPath) && !SecurityUtils.isAdmin()) {
            return AjaxResult.error("只有管理员才能直接上传文件到根目录");
        }

        if (!fileDirPermissionService.hasUploadPermission(userId, dirPath)) {
            return AjaxResult.error("无权在该目录上传文件");
        }

        String uploadId = buildUploadId(userId, targetPath, request.getFileHash(), request.getFileSize());
        File tempDir = getChunkTempDir(uploadId);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return AjaxResult.error("创建分片临时目录失败");
        }

        Set<Integer> uploadedChunks = scanUploadedChunkIndexes(tempDir);
        ChunkUploadSession session = new ChunkUploadSession();
        session.setUploadId(uploadId);
        session.setUserId(userId);
        session.setDirPath(dirPath);
        session.setTargetPath(targetPath);
        session.setFileName(request.getFileName());
        session.setTotalChunks(request.getTotalChunks());
        session.setTempDir(tempDir);
        session.getUploadedChunks().addAll(uploadedChunks);
        session.setLastUpdatedAt(System.currentTimeMillis());
        CHUNK_UPLOAD_SESSION_MAP.put(uploadId, session);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("uploadId", uploadId);
        resultData.put("totalChunks", session.getTotalChunks());
        resultData.put("uploadedChunks", new ArrayList<>(session.getUploadedChunks()).stream().sorted().collect(Collectors.toList()));

        return AjaxResult.success("初始化成功", resultData);
    }

    @Log(title = "分片上传", businessType = BusinessType.INSERT)
    @PostMapping("/chunk/upload")
    public AjaxResult uploadChunk(@RequestParam("uploadId") String uploadId,
                                  @RequestParam("chunkIndex") Integer chunkIndex,
                                  @RequestParam("chunk") MultipartFile chunk) throws Exception {
        if (StringUtils.isEmpty(uploadId) || chunkIndex == null || chunk == null || chunk.isEmpty()) {
            return AjaxResult.error("分片参数不完整");
        }

        ChunkUploadSession session = CHUNK_UPLOAD_SESSION_MAP.get(uploadId);
        if (session == null) {
            return AjaxResult.error("上传会话不存在或已过期");
        }
        if (!session.getUserId().equals(SecurityUtils.getUserId())) {
            return AjaxResult.error("无权操作该上传会话");
        }
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            return AjaxResult.error("分片索引越界");
        }

        File partFile = new File(session.getTempDir(), chunkIndex + ".part");
        if (!partFile.exists() || partFile.length() == 0) {
            chunk.transferTo(partFile);
        }

        session.getUploadedChunks().add(chunkIndex);
        session.setLastUpdatedAt(System.currentTimeMillis());

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("uploadedCount", session.getUploadedChunks().size());
        resultData.put("totalChunks", session.getTotalChunks());
        return AjaxResult.success("分片上传成功", resultData);
    }

    @GetMapping("/chunk/status")
    public AjaxResult getChunkUploadStatus(@RequestParam("uploadId") String uploadId) {
        if (StringUtils.isEmpty(uploadId)) {
            return AjaxResult.error("uploadId 不能为空");
        }
        ChunkUploadSession session = CHUNK_UPLOAD_SESSION_MAP.get(uploadId);
        if (session == null) {
            return AjaxResult.error("上传会话不存在或已过期");
        }
        if (!session.getUserId().equals(SecurityUtils.getUserId())) {
            return AjaxResult.error("无权查看该上传会话");
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("uploadId", uploadId);
        resultData.put("totalChunks", session.getTotalChunks());
        resultData.put("uploadedChunks", new ArrayList<>(session.getUploadedChunks()).stream().sorted().collect(Collectors.toList()));
        resultData.put("completed", session.getUploadedChunks().size() == session.getTotalChunks());
        return AjaxResult.success("获取成功", resultData);
    }

    @Log(title = "分片上传合并", businessType = BusinessType.INSERT)
    @PostMapping("/chunk/complete")
    public AjaxResult completeChunkUpload(@RequestBody ChunkCompleteRequest request) throws Exception {
        cleanupExpiredAsyncTasks();
        long completeStartAt = System.currentTimeMillis();
        if (request == null || StringUtils.isEmpty(request.getUploadId())) {
            return AjaxResult.error("uploadId 不能为空");
        }
        ChunkUploadSession session = CHUNK_UPLOAD_SESSION_MAP.get(request.getUploadId());
        if (session == null) {
            return AjaxResult.error("上传会话不存在或已过期");
        }
        if (!session.getUserId().equals(SecurityUtils.getUserId())) {
            return AjaxResult.error("无权操作该上传会话");
        }

        List<Integer> missingIndexes = getMissingChunkIndexes(session);
        if (!missingIndexes.isEmpty()) {
            return AjaxResult.error("分片未上传完整，缺少分片: " + missingIndexes);
        }

        File mergedFile = new File(session.getTempDir(), "merged.bin");
    long mergeStartAt = System.currentTimeMillis();
        mergeChunks(session, mergedFile);
    long mergeCost = System.currentTimeMillis() - mergeStartAt;

        if (isAsyncTransferEnabled()) {
            String taskId = UUID.randomUUID().toString().replace("-", "");
            File stagedFile = moveMergedFileToAsyncStaging(taskId, mergedFile);

            AsyncTransferTask task = new AsyncTransferTask();
            task.setTaskId(taskId);
            task.setUploadId(session.getUploadId());
            task.setUserId(session.getUserId());
            task.setDirPath(session.getDirPath());
            task.setTargetPath(session.getTargetPath());
            task.setFileName(session.getFileName());
            task.setStagingFile(stagedFile);
            task.setStatus(AsyncTransferStatus.QUEUED.name());
            task.setLastUpdatedAt(System.currentTimeMillis());
            ASYNC_TRANSFER_TASK_MAP.put(taskId, task);

            ASYNC_TRANSFER_EXECUTOR.submit(() -> processAsyncTransfer(taskId));

            long totalCost = System.currentTimeMillis() - completeStartAt;
            if (isUploadPerfLogEnabled()) {
                log.info("chunk complete queued uploadId={}, file={}, merge={}ms, queue={}ms", session.getUploadId(), session.getFileName(), mergeCost, totalCost);
            }

            cleanupChunkSession(session.getUploadId());

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("async", true);
            resultData.put("taskId", taskId);
            resultData.put("status", task.getStatus());
            resultData.put("path", session.getTargetPath());
            return AjaxResult.success("文件已入队，后台转存中", resultData);
        }

        MultipartFile mergedMultipart = new LocalTempMultipartFile(session.getFileName(), mergedFile);
        InputStream contentStream = mergedMultipart.getInputStream();
        long storageStartAt = System.currentTimeMillis();
        String fullPath = fileStorageService.upload(mergedMultipart, session.getTargetPath());
        long storageCost = System.currentTimeMillis() - storageStartAt;
        long totalCost = System.currentTimeMillis() - completeStartAt;
        if (isUploadPerfLogEnabled()) {
            log.info("chunk complete timing uploadId={}, file={}, merge={}ms, storage={}ms, total={}ms", session.getUploadId(), session.getFileName(), mergeCost, storageCost, totalCost);
        }

        fullTextSearchService.indexFileAsync(fullPath, session.getFileName(), contentStream);

        directoryCacheService.invalidateCache(session.getDirPath());
        directoryCacheService.invalidateAllUserCacheForDir(session.getDirPath());

        cleanupChunkSession(session.getUploadId());
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("async", false);
        resultData.put("status", AsyncTransferStatus.SUCCESS.name());
        resultData.put("path", fullPath);
        return AjaxResult.success("上传成功", resultData);
    }

    @GetMapping("/chunk/transfer-status")
    public AjaxResult getChunkTransferStatus(@RequestParam("taskId") String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            return AjaxResult.error("taskId 不能为空");
        }
        AsyncTransferTask task = ASYNC_TRANSFER_TASK_MAP.get(taskId);
        if (task == null) {
            return AjaxResult.error("转存任务不存在或已过期");
        }
        if (!task.getUserId().equals(SecurityUtils.getUserId())) {
            return AjaxResult.error("无权查看该转存任务");
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("taskId", task.getTaskId());
        resultData.put("uploadId", task.getUploadId());
        resultData.put("status", task.getStatus());
        resultData.put("path", task.getFinalPath());
        resultData.put("error", task.getErrorMessage());
        return AjaxResult.success("获取成功", resultData);
    }

    @DeleteMapping("/chunk/cancel")
    public AjaxResult cancelChunkUpload(@RequestParam("uploadId") String uploadId) {
        if (StringUtils.isEmpty(uploadId)) {
            return AjaxResult.error("uploadId 不能为空");
        }
        ChunkUploadSession session = CHUNK_UPLOAD_SESSION_MAP.get(uploadId);
        if (session == null) {
            return AjaxResult.success("会话已不存在");
        }
        if (!session.getUserId().equals(SecurityUtils.getUserId())) {
            return AjaxResult.error("无权操作该上传会话");
        }
        cleanupChunkSession(uploadId);
        return AjaxResult.success("已取消上传");
    }
    
    /**
     * 新建文件夹
     * 权限检查：基于文件目录权限系统，需要父目录的上传权限
     * 权限分配：继承父目录的所有角色权限
     * @param parentPath 父目录路径
     * @param folderName 新文件夹名称
     */
    @Log(title = "新建文件夹", businessType = BusinessType.INSERT)
    @PostMapping("/mkdir")
    public AjaxResult mkdir(@RequestParam("parentPath") String parentPath,
                            @RequestParam("folderName") String folderName) throws Exception {
        if (StringUtils.isEmpty(folderName)) {
            return AjaxResult.error("文件夹名称不能为空");
        }
        
        // 检查文件夹名称是否合法
        if (folderName.matches(".*[\\\\/:*?\"<>|].*")) {
            return AjaxResult.error("文件夹名称包含非法字符");
        }
        
        // 规范化父目录路径
        String normalizedParentPath = StringUtils.isEmpty(parentPath) ? "/" : 
            (parentPath.startsWith("/") ? parentPath : "/" + parentPath);
        
        // 检查是否为根目录创建：只有管理员才能在根目录下创建文件夹
        if ("/".equals(normalizedParentPath) && !SecurityUtils.isAdmin()) {
            return AjaxResult.error("只有管理员才能在根目录下创建文件夹");
        }
        
        // 检查父目录上传权限
        Long userId = SecurityUtils.getUserId();
        if (!fileDirPermissionService.hasUploadPermission(userId, normalizedParentPath)) {
            return AjaxResult.error("无权在该目录创建文件夹");
        }
        
        // 1. 构建新目录路径
        String newDirPath = normalizedParentPath.endsWith("/") 
            ? normalizedParentPath + folderName 
            : normalizedParentPath + "/" + folderName;
        
        // 2. 先在文件系统创建目录
        boolean created = fileStorageService.mkdir(newDirPath);
        if (!created) {
            // 如果目录已存在，检查是否真的有这个目录
            if (!fileStorageService.exists(newDirPath)) {
                return AjaxResult.error("文件夹创建失败");
            }
            // 目录已存在，继续创建权限记录（幂等性）
        }
        
        // 3. 只在根目录下创建文件夹时，才在数据库创建目录记录并分配权限
        // 子文件夹继承父目录权限，不需要单独创建权限记录
        if ("/".equals(normalizedParentPath)) {
            try {
                fileDirPermissionService.createFolderWithPermission(
                        normalizedParentPath, folderName, userId);
            } catch (Exception e) {
                // 如果权限创建失败，且目录是我们刚创建的，尝试删除（回滚）
                if (created) {
                    try {
                        fileStorageService.delete(newDirPath);
                    } catch (Exception ex) {
                        // 忽略回滚失败
                    }
                }
                // 优化错误提示：检查是否是重复名称
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("Duplicate") || errorMsg.contains("重复") || 
                        errorMsg.contains("exists") || errorMsg.contains("已存在"))) {
                    return AjaxResult.error("该目录下已存在同名文件夹");
                }
                return AjaxResult.error("权限分配失败: " + errorMsg);
            }
        }
        
        // 4. 异步清除缓存（不阻塞返回）
        new Thread(() -> {
            try {
                directoryCacheService.invalidateCache(normalizedParentPath);
                directoryCacheService.invalidateAllUserCacheForDir(normalizedParentPath);
            } catch (Exception ex) {
                // 忽略缓存清除失败
            }
        }).start();
        
        return AjaxResult.success("创建成功", newDirPath);
    }
    
    /**
     * 下载文件
     * 权限检查：基于文件目录权限系统
     */
    @Log(title = "文件下载", businessType = BusinessType.EXPORT)
    @GetMapping("/download")
    public void download(@RequestParam("path") String path, HttpServletResponse response) throws Exception {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        // 兼容绝对路径：如果是绝对路径，自动转为相对路径
        path = normalizeToRelativePath(path);
        
        // 检查目录下载权限
        String dirPath = extractDirPath(path);
        if (!fileDirPermissionService.hasDownloadPermission(SecurityUtils.getUserId(), dirPath)) {
            throw new IllegalArgumentException("无权下载该目录的文件");
        }
        
        // 获取文件名
        String fileName = FileUtils.getName(path);
        
        // 下载文件内容
        String storageType = fileStorageService.getStorageType();
        
        if ("local".equals(storageType)) {
            // 本地存储：直接流式传输，避免大文件撑爆内存
            File file = new File(fileStorageService.getFullPath(path));
            if (!file.exists()) {
                throw new IOException("文件不存在: " + path);
            }

            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");

            try (FileInputStream fis = new FileInputStream(file)) {
                IOUtils.copy(fis, response.getOutputStream());
            }
        } else {
            // WebDAV 存储：无法预先获取文件大小
            try (InputStream is = fileStorageService.download(path)) {
                // 重置响应并设置下载头（不设置Content-Length）
                response.reset();
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                response.setContentType("application/octet-stream; charset=UTF-8");
                
                // 写入文件内容
                IOUtils.copy(is, response.getOutputStream());
            }
        }
    }
    
    /**
     * 在线预览文件（图片等）
     * 权限检查：基于文件目录权限系统
     */
    @Log(title = "文件预览", businessType = BusinessType.OTHER)
    @GetMapping("/preview")
    public void preview(@RequestParam("path") String path, HttpServletResponse response) throws Exception {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        // 兼容绝对路径：如果是绝对路径，自动转为相对路径
        String originalPath = path;
        path = normalizeToRelativePath(path);
        logger.info("preview path: original={}, normalized={}", originalPath, path);
        
        // 检查目录查看权限
        String dirPath = extractDirPath(path);
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            throw new IllegalArgumentException("无权预览该目录的文件");
        }
        
        // 获取文件类型
        String fileName = FileUtils.getName(path);
        String extension = FilenameUtils.getExtension(fileName);
        
        // 根据扩展名设置 Content-Type
        String contentType = getContentType(extension);
        if (contentType != null) {
            response.setContentType(contentType);
        } else {
            response.setContentType("application/octet-stream");
        }
                // 设置 Content-Disposition，使用 UTF-8 编码文件名
                String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);
        
        // 输出文件内容
        try (InputStream is = fileStorageService.download(path)) {
            // 获取文件大小并设置 Content-Length
            File file = new File(fileStorageService.getFullPath(path));
            if (file.exists()) {
                response.setContentLengthLong(file.length());
            }
            IOUtils.copy(is, response.getOutputStream());
        }
    }

    /**
     * 提取文档纯文本内容（用于 .doc 等旧格式的预览）
     */
    @GetMapping("/preview-text")
    @ResponseBody
    public AjaxResult previewText(@RequestParam("path") String path) throws Exception {
        if (StringUtils.isEmpty(path)) {
            return AjaxResult.error("文件路径不能为空");
        }

        path = normalizeToRelativePath(path);

        String dirPath = extractDirPath(path);
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权访问该文件");
        }

        String fileName = FileUtils.getName(path);
        if (!DocumentContentExtractor.isSupportedDocument(fileName)) {
            return AjaxResult.error("该文件格式不支持文本提取");
        }

        try (InputStream is = fileStorageService.download(path)) {
            String text = DocumentContentExtractor.extractContent(is, fileName);
            int len = text != null ? text.length() : 0;
            boolean hasCJK = text != null && text.matches(".*[\\u4e00-\\u9fff].*");
            boolean hasHtml = text != null && text.contains("<");
            String midSample = "";
            if (len > 400) {
                int mid = len / 2;
                int start = Math.max(0, mid - 100);
                int end = Math.min(len, mid + 100);
                midSample = text.substring(start, end).replace("\n", "\\n").replace("\r", "\\r");
            }
            logger.info("preview-text: fileName={}, textLen={}, hasHtml={}, hasCJK={}, first200={}, mid200={}",
                    fileName, len, hasHtml, hasCJK,
                    text != null ? text.substring(0, Math.min(200, len)).replace("\n", "\\n") : "null",
                    midSample);
            return AjaxResult.success("提取成功", text);
        }
    }

    /**
     * 删除文件/目录
     * 权限检查：基于文件目录权限系统
     */
    @Log(title = "文件删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete")
    public AjaxResult delete(@RequestParam("path") String path,
                             @RequestParam(value = "isDirectory", required = false, defaultValue = "false") Boolean isDirectory) throws Exception {
        if (StringUtils.isEmpty(path)) {
            return AjaxResult.error("文件路径不能为空");
        }
        
        // 检查是否为根目录下的直接子项删除：只有管理员才能删除根目录下的文件夹/文件
        if (isDirectory && "/".equals(path)) {
            return AjaxResult.error("不能删除根目录");
        }
        // 检查路径是否为根目录下的直接子项（如 /xxx，但不包括 /xxx/yyy）
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (!"/".equals(normalizedPath)) {
            String pathWithoutRoot = normalizedPath.substring(1); // 去掉开头的 /
            // 如果路径中没有 /，说明是根目录下的直接子项
            if (!pathWithoutRoot.contains("/") && !SecurityUtils.isAdmin()) {
                return AjaxResult.error("只有管理员才能删除根目录下的文件夹或文件");
            }
        }
        
        // 检查目录删除权限
        String dirPath = isDirectory ? path : extractDirPath(path);
        if (!fileDirPermissionService.hasDeletePermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权删除该目录的文件");
        }
        
        // 删除文件/目录（先删文件系统，成功后再异步删权限）
        boolean result = fileStorageService.delete(path);
        
        // 从全文索引中删除
        if (result && !isDirectory) {
            log.info("删除文件，传入路径: {}", path);
            fullTextSearchService.removeFile(path);
        }
        
        // 如果是目录，异步删除数据库中的权限记录（不阻塞返回）
        if (isDirectory) {
            new Thread(() -> {
                try {
                    fileDirPermissionService.deleteDirWithPermission(path);
                } catch (Exception e) {
                    // 忽略权限删除失败，记录日志
                }
            }).start();
        }
        
        // 异步清除缓存（不阻塞返回）
        new Thread(() -> {
            try {
                directoryCacheService.invalidateCache(dirPath);
                directoryCacheService.invalidateAllUserCacheForDir(dirPath);
            } catch (Exception ex) {
                // 忽略缓存清除失败
            }
        }).start();
        
        return AjaxResult.success(result ? "删除成功" : "删除失败", result);
    }
    
    /**
     * 检查文件是否存在
     * 权限检查：无需特殊权限
     */
    @GetMapping("/exists")
    public AjaxResult exists(@RequestParam("path") String path) throws Exception {
        if (StringUtils.isEmpty(path)) {
            return AjaxResult.error("文件路径不能为空");
        }
        
        boolean exists = fileStorageService.exists(path);
        return AjaxResult.success(exists ? "文件存在" : "文件不存在", exists);
    }
    
    /**
     * 获取当前存储类型
     * 权限检查：无需特殊权限
     */
    @GetMapping("/storageType")
    public AjaxResult getStorageType() {
        String storageType = fileStorageService.getStorageType();
        return AjaxResult.success("当前存储类型：" + storageType, storageType);
    }

    /**
     * 浏览目录（列出目录下的文件和子目录）- 支持分页和缓存
     * 权限检查：基于文件目录权限系统，而非菜单权限
     * @param path 目录路径，如 "docker/test" 或空字符串表示根目录
     * @param pageNum 页码，从1开始，默认1
     * @param pageSize 每页数量，默认50
     * @param refresh 是否强制刷新缓存，默认false
     */
    @GetMapping("/list")
    public AjaxResult listFiles(
            @RequestParam(value = "path", required = false, defaultValue = "") String path,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
            @RequestParam(value = "refresh", required = false, defaultValue = "false") Boolean refresh) throws Exception {
        // 规范化路径
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        // 检查目录查看权限
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权查看该目录");
        }
        
        // 权限已在入口处检查，只需过滤子文件夹权限
        Long userId = SecurityUtils.getUserId();
        
        DirectoryCacheService.PageResult pageResult = directoryCacheService.getFilesPaginatedForUser(
                dirPath, userId, pageNum, pageSize, refresh,
                // 批量目录权限检查
                dirPaths -> fileDirPermissionService.batchCheckViewPermission(userId, dirPaths));
        List<FileStorageStrategy.FileInfo> filteredFiles = pageResult.getFiles();
        
        // 返回结果
        AjaxResult result = AjaxResult.success("获取成功", filteredFiles);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("total", pageResult.getTotal());
        result.put("hasMore", pageResult.isHasMore());
        
        return result;
    }
    
    /**
     * 搜索文件（按文件名）- 分层搜索版本
     * 权限检查：基于文件目录权限系统，只搜索有权限的目录
     * pageNum: 1=当前目录, 2=第1层子目录, 3=第2层子目录, 以此类推
     */
    @GetMapping("/search")
    public AjaxResult searchFiles(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "path", required = false, defaultValue = "") String path,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) throws Exception {
        
        if (StringUtils.isEmpty(keyword)) {
            return AjaxResult.error("搜索关键词不能为空");
        }
        
        // 规范化路径
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        // 检查目录查看权限
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权搜索该目录");
        }
        
        Long userId = SecurityUtils.getUserId();
        String searchKeyword = keyword.toLowerCase();
        
        // 根据 pageNum 决定搜索哪一层
        // pageNum=1: 只搜当前目录
        // pageNum=2: 搜第1层子目录
        // pageNum=3: 搜第2层子目录
        int targetDepth = pageNum - 1;
        
        List<FileStorageStrategy.FileInfo> results = new ArrayList<>();
        searchFilesAtDepth(dirPath, userId, searchKeyword, results, 0, targetDepth);
        
        AjaxResult result = AjaxResult.success("搜索成功", results);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("total", results.size());
        result.put("hasMore", pageNum < 5); // 最多5层
        
        return result;
    }
    
    /**
     * 快速搜文件（基于 Lucene 索引，纯查索引不遍历目录，毫秒级）
     */
    @GetMapping("/search/name")
    public AjaxResult searchByName(@RequestParam("keyword") String keyword,
                                    @RequestParam(value = "path", required = false, defaultValue = "") String path,
                                    @RequestParam(value = "maxResults", required = false, defaultValue = "100") int maxResults) throws Exception {
        if (StringUtils.isEmpty(keyword)) return AjaxResult.error("搜索关键词不能为空");
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath))
            return AjaxResult.error("无权搜索该目录");

        List<FullTextSearchService.SearchResult> luceneResults = fullTextSearchService.searchFileByName(keyword, maxResults);
        Long userId = SecurityUtils.getUserId();
        String prefix = dirPath.equals("/") ? "/" : (dirPath.endsWith("/") ? dirPath : dirPath + "/");
        List<FileStorageStrategy.FileInfo> results = new ArrayList<>();
        for (FullTextSearchService.SearchResult sr : luceneResults) {
            String p = sr.getPath();
            if (p == null) continue;
            if (!p.startsWith("/")) p = "/" + p;
            if (!p.startsWith(prefix)) continue;
            if (!fileDirPermissionService.hasViewPermission(userId, extractDirPath(p))) continue;
            FileStorageStrategy.FileInfo info = new FileStorageStrategy.FileInfo();
            info.setName(sr.getFileName());
            info.setPath(p);
            info.setLastModified(sr.getUpdateTime());
            results.add(info);
        }
        return AjaxResult.success("搜索成功", results);
    }

    /**
     * 穿透搜索（按文档内容）
     * 基于 Lucene 全文索引搜索文档内容
     */
    @GetMapping("/search/content")
    public AjaxResult searchContent(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "path", required = false, defaultValue = "") String path,
            @RequestParam(value = "maxResults", required = false, defaultValue = "50") Integer maxResults) throws Exception {
        
        if (StringUtils.isEmpty(keyword)) {
            return AjaxResult.error("搜索关键词不能为空");
        }
        
        // 规范化路径
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        // 检查目录查看权限
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权搜索该目录");
        }
        
        // 执行全文搜索
        List<FullTextSearchService.SearchResult> searchResults = fullTextSearchService.search(keyword, maxResults);
        
        // 过滤结果（只返回当前目录及子目录下的文件）
        List<FullTextSearchService.SearchResult> filteredResults = new ArrayList<>();
        for (FullTextSearchService.SearchResult result : searchResults) {
            String filePath = result.getPath();
            
            // 将绝对路径转换为相对路径进行匹配
            // 例如: D:/home/test/test001/xxx.docx -> /test/test001/xxx.docx
            String relativePath = filePath;
            String baseDir = RuoYiConfig.getProfile();
            // 统一使用正斜杠进行比较
            String normalizedFilePath = filePath.replace("\\", "/");
            String normalizedBaseDir = baseDir.replace("\\", "/");
            if (normalizedFilePath.startsWith(normalizedBaseDir)) {
                relativePath = normalizedFilePath.substring(normalizedBaseDir.length());
                // 确保路径以 / 开头
                if (!relativePath.startsWith("/")) {
                    relativePath = "/" + relativePath;
                }
            } else {
                relativePath = normalizedFilePath;
            }
            
            // 检查文件是否在指定目录下
            if (relativePath.startsWith(dirPath) || dirPath.equals("/")) {
                // 检查文件查看权限
                String fileDir = relativePath.substring(0, relativePath.lastIndexOf('/'));
                if (fileDir.isEmpty()) fileDir = "/";
                
                if (fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), fileDir)) {
                    filteredResults.add(result);
                }
            }
        }
        
        AjaxResult result = AjaxResult.success("搜索成功", filteredResults);
        result.put("total", filteredResults.size());
        result.put("keyword", keyword);
        
        return result;
    }
    
    /**
     * 重建全文索引
     * 遍历所有有权限的文档并建立索引
     */
    @Log(title = "重建搜索索引", businessType = BusinessType.OTHER)
    @PostMapping("/search/rebuild-index")
    public AjaxResult rebuildSearchIndex(@RequestParam(value = "path", required = false, defaultValue = "") String path) throws Exception {
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        // 检查权限
        Long userId = SecurityUtils.getUserId();
        if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
            return AjaxResult.error("无权访问该目录");
        }
        
        // 异步重建索引，将 userId 传入线程
        final Long currentUserId = userId;
        new Thread(() -> {
            try {
                // 先清空所有索引
                log.info("开始重建索引，先清空旧索引...");
                int beforeCount = fullTextSearchService.getIndexedDocumentCount();
                log.info("清空前索引数量: {}", beforeCount);
                fullTextSearchService.clearAllIndex();
                int afterCount = fullTextSearchService.getIndexedDocumentCount();
                log.info("清空后索引数量: {}", afterCount);
                
                // 重新构建索引
                rebuildIndexRecursive(dirPath, currentUserId, 0);
                int finalCount = fullTextSearchService.getIndexedDocumentCount();
                log.info("索引重建完成，最终索引数量: {}", finalCount);
            } catch (Exception e) {
                log.error("重建索引失败: {}", e.getMessage(), e);
            }
        }).start();
        
        return AjaxResult.success("索引重建任务已启动，请稍后尝试搜索");
    }
    
    /**
     * 递归重建索引
     */
    private void rebuildIndexRecursive(String dirPath, Long userId, int depth) throws Exception {
        if (depth > 10) return; // 限制深度
        
        if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
            return;
        }
        
        List<FileStorageStrategy.FileInfo> files;
        try {
            files = directoryCacheService.getFiles(dirPath, false);
        } catch (Exception e) {
            return;
        }
        
        for (FileStorageStrategy.FileInfo file : files) {
            String fileName = file.getName();
            String filePath = dirPath + (dirPath.endsWith("/") ? "" : "/") + fileName;
            
            log.debug("重建索引遍历: dirPath={}, fileName={}, filePath={}, isDirectory={}", 
                    dirPath, fileName, filePath, file.isDirectory());
            
            if (file.isDirectory()) {
                // 递归处理子目录
                rebuildIndexRecursive(filePath, userId, depth + 1);
            } else if (DocumentContentExtractor.isSupportedDocument(fileName)) {
                try (InputStream is = fileStorageService.download(filePath)) {
                    if (is != null) {
                        fullTextSearchService.indexFile(filePath, fileName, is);
                    }
                } catch (Exception e) {
                    log.error("索引文档失败: {}, 错误: {}", fileName, e.getMessage());
                }
            }
        }
    }
    
    /**
     * 获取索引状态
     */
    @GetMapping("/search/index-status")
    public AjaxResult getIndexStatus() throws Exception {
        int count = fullTextSearchService.getIndexedDocumentCount();
        return AjaxResult.success("获取成功", Map.of("indexedCount", count));
    }
    
    /**
     * 在指定深度搜索文件
     */
    private void searchFilesAtDepth(String dirPath, Long userId, String keyword, 
            List<FileStorageStrategy.FileInfo> results, int currentDepth, int targetDepth) throws Exception {
        // 检查当前目录的查看权限
        if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
            return;
        }
        
        // 获取当前目录的文件列表（使用缓存）
        List<FileStorageStrategy.FileInfo> files;
        try {
            files = directoryCacheService.getFiles(dirPath, false);
        } catch (Exception e) {
            return;
        }
        
        // 如果当前深度等于目标深度，搜索文件
        if (currentDepth == targetDepth) {
            for (FileStorageStrategy.FileInfo file : files) {
                String fileName = file.getName();
                if (fileName == null) continue;
                
                // 检查文件名是否匹配
                if (fileName.toLowerCase().contains(keyword)) {
                    file.setPath(dirPath + (dirPath.endsWith("/") ? "" : "/") + fileName);
                    results.add(file);
                }
            }
            return;
        }
        
        // 如果还没到目标深度，继续递归子目录
        if (currentDepth < targetDepth) {
            for (FileStorageStrategy.FileInfo file : files) {
                if (file.isDirectory()) {
                    String subDirPath = dirPath + (dirPath.endsWith("/") ? "" : "/") + file.getName();
                    searchFilesAtDepth(subDirPath, userId, keyword, results, currentDepth + 1, targetDepth);
                }
            }
        }
    }
    
    // 最大搜索目录数，防止超时
    private static final int MAX_SEARCH_DIRS = 100;
    private static final int MAX_SEARCH_DEPTH = 5;
    
    /**
     * 并行搜索文件 - 优化版本
     */
    private void searchFilesParallel(String dirPath, Long userId, String keyword, 
            List<FileStorageStrategy.FileInfo> results, int depth) throws Exception {
        // 限制搜索深度
        if (depth > MAX_SEARCH_DEPTH) {
            return;
        }
        
        // 检查当前目录的查看权限
        if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
            return;
        }
        
        // 获取当前目录的文件列表（使用缓存）
        List<FileStorageStrategy.FileInfo> files;
        try {
            files = directoryCacheService.getFiles(dirPath, false);
        } catch (Exception e) {
            // 如果获取失败，跳过此目录
            return;
        }
        
        // 收集子目录
        List<FileStorageStrategy.FileInfo> subDirs = new ArrayList<>();
        
        for (FileStorageStrategy.FileInfo file : files) {
            String fileName = file.getName();
            if (fileName == null) continue;
            
            // 检查文件名是否匹配
            if (fileName.toLowerCase().contains(keyword)) {
                // 添加完整路径信息
                file.setPath(dirPath + (dirPath.endsWith("/") ? "" : "/") + fileName);
                synchronized (results) {
                    results.add(file);
                }
            }
            
            // 收集子目录
            if (file.isDirectory()) {
                subDirs.add(file);
            }
        }
        
        // 限制总搜索目录数
        if (results.size() > MAX_SEARCH_DIRS) {
            return;
        }
        
        // 递归搜索子目录（改为单线程避免并发问题）
        if (!subDirs.isEmpty()) {
            for (FileStorageStrategy.FileInfo subDir : subDirs) {
                try {
                    String subDirPath = dirPath + (dirPath.endsWith("/") ? "" : "/") + subDir.getName();
                    searchFilesParallel(subDirPath, userId, keyword, results, depth + 1);
                } catch (Exception e) {
                    // 忽略子目录搜索错误
                }
            }
        }
    }
    
    /**
     * 流式搜索文件（SSE）- 实时返回搜索结果
     */
    @GetMapping("/search/stream")
    public SseEmitter searchFilesStream(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "path", required = false, defaultValue = "") String path) {
        
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        if (StringUtils.isEmpty(keyword)) {
            emitter.completeWithError(new RuntimeException("搜索关键词不能为空"));
            return emitter;
        }
        
        // 异步执行搜索
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
                Long userId = SecurityUtils.getUserId();
                
                // 检查权限
                if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
                    emitter.send(SseEmitter.event().name("error").data("无权搜索该目录"));
                    emitter.complete();
                    return;
                }
                
                String searchKeyword = keyword.toLowerCase();
                
                // 流式搜索
                searchFilesStreamRecursive(dirPath, userId, searchKeyword, emitter, 0);
                
                // 发送完成事件
                emitter.send(SseEmitter.event().name("complete").data("搜索完成"));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE search error: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * 流式递归搜索 - 实时推送结果
     */
    private void searchFilesStreamRecursive(String dirPath, Long userId, String keyword, 
            SseEmitter emitter, int depth) throws Exception {
        // 限制搜索深度
        if (depth > MAX_SEARCH_DEPTH) {
            return;
        }
        
        // 检查权限
        if (!fileDirPermissionService.hasViewPermission(userId, dirPath)) {
            return;
        }
        
        // 获取文件列表
        List<FileStorageStrategy.FileInfo> files;
        try {
            files = directoryCacheService.getFiles(dirPath, false);
        } catch (Exception e) {
            return;
        }
        
        // 收集子目录和匹配的文件
        List<FileStorageStrategy.FileInfo> subDirs = new ArrayList<>();
        List<FileStorageStrategy.FileInfo> matchedFiles = new ArrayList<>();
        
        for (FileStorageStrategy.FileInfo file : files) {
            String fileName = file.getName();
            if (fileName == null) continue;
            
            if (fileName.toLowerCase().contains(keyword)) {
                file.setPath(dirPath + (dirPath.endsWith("/") ? "" : "/") + fileName);
                matchedFiles.add(file);
            }
            
            if (file.isDirectory()) {
                subDirs.add(file);
            }
        }
        
        // 立即发送匹配的文件
        if (!matchedFiles.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("result").data(matchedFiles));
            } catch (IOException e) {
                // 客户端断开连接
                throw e;
            }
        }
        
        // 递归搜索子目录
        for (FileStorageStrategy.FileInfo subDir : subDirs) {
            String subDirPath = dirPath + (dirPath.endsWith("/") ? "" : "/") + subDir.getName();
            searchFilesStreamRecursive(subDirPath, userId, keyword, emitter, depth + 1);
        }
    }
    
    /**
     * 刷新目录缓存
     * 权限检查：基于文件目录权限系统
     * @param path 目录路径，如 "docker/test" 或空字符串表示根目录
     */
    @GetMapping("/refresh")
    public AjaxResult refreshCache(
            @RequestParam(value = "path", required = false, defaultValue = "") String path) throws Exception {
        // 规范化路径
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        // 检查目录查看权限
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权查看该目录");
        }
        
        // 强制刷新目录缓存
        directoryCacheService.getFiles(dirPath, true);
        // 同时清除该目录的所有用户缓存
        directoryCacheService.invalidateAllUserCacheForDir(dirPath);
        
        return AjaxResult.success("刷新成功");
    }
    
    /**
     * 获取目录文件数量（从缓存获取，更快）
     * 权限检查：基于文件目录权限系统
     */
    @GetMapping("/count")
    public AjaxResult countFiles(@RequestParam(value = "path", required = false, defaultValue = "") String path) throws Exception {
        String dirPath = StringUtils.isEmpty(path) ? "/" : (path.startsWith("/") ? path : "/" + path);
        
        if (!fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), dirPath)) {
            return AjaxResult.error("无权查看该目录");
        }
        
        // 从缓存获取总数
        DirectoryCacheService.PageResult pageResult = directoryCacheService.getFilesPaginated(dirPath, 1, 1);
        return AjaxResult.success("获取成功", pageResult.getTotal());
    }
    
    /**
     * 预热目录缓存（后台异步加载）
     * 用于用户登录时或首次访问前预加载常用目录
     * 权限检查：基于文件目录权限系统
     * @param paths 要预热的目录路径列表，逗号分隔
     */
    @GetMapping("/warmup")
    public AjaxResult warmupCache(
            @RequestParam(value = "paths", required = false, defaultValue = "/") String paths) throws Exception {
        // 解析路径列表
        String[] pathArray = paths.split(",");
        List<String> pathList = new ArrayList<>();
        for (String path : pathArray) {
            String normalizedPath = path.trim();
            if (!normalizedPath.isEmpty()) {
                if (!normalizedPath.startsWith("/")) {
                    normalizedPath = "/" + normalizedPath;
                }
                // 检查权限
                if (fileDirPermissionService.hasViewPermission(SecurityUtils.getUserId(), normalizedPath)) {
                    pathList.add(normalizedPath);
                }
            }
        }
        
        // 异步预热缓存
        directoryCacheService.warmupCache(pathList);
        
        return AjaxResult.success("预热任务已启动", pathList);
    }
    
    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(String extension) {
        if (extension == null) {
            return null;
        }
        String ext = extension.toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            default:
                return null;
        }
    }

    /**
     * 从文件路径中提取目录路径
     * 例如：docker/test/file.txt -> /docker/test
     */
    private String extractDirPath(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return "/";
        }
        
        // 去掉开头的 /
        String path = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        
        // 找到最后一个 / 的位置
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            // 提取目录部分
            String dirPath = "/" + path.substring(0, lastSlashIndex);
            return dirPath;
        } else {
            // 没有 /，说明在根目录
            return "/";
        }
    }

    /**
     * 将绝对路径转为相对路径（去掉 baseDir 部分）
     * 例如：D:\home\test\xxx.docx -> /test/xxx.docx
     *      /volume1/test/xxx.docx -> /test/xxx.docx
     *      /test/xxx.docx -> /test/xxx.docx（已经是相对路径，不变）
     */
    private String normalizeToRelativePath(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }
        String baseDir = fileStorageService.getBaseDir();
        if (baseDir == null) {
            return filePath;
        }
        // 统一分隔符
        String normalized = filePath.replace('\\', '/');
        String normalizedBase = baseDir.replace('\\', '/');
        // 去掉 baseDir 部分
        if (normalized.startsWith(normalizedBase)) {
            String relative = normalized.substring(normalizedBase.length());
            if (!relative.startsWith("/")) {
                relative = "/" + relative;
            }
            return relative;
        }
        // 已经是相对路径，直接返回
        return filePath;
    }

    private String buildTargetUploadPath(String path, String fileName) {
        String normalizedPath = StringUtils.isEmpty(path) ? "/" : path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (normalizedPath.endsWith("/")) {
            return normalizedPath + fileName;
        }
        return normalizedPath;
    }

    private String buildUploadId(Long userId, String targetPath, String fileHash, Long fileSize) {
        String hashBase = userId + "|" + targetPath + "|" + (fileHash == null ? "" : fileHash) + "|" + fileSize;
        return UUID.nameUUIDFromBytes(hashBase.getBytes()).toString().replace("-", "");
    }

    private File getChunkTempDir(String uploadId) {
        return new File(RuoYiConfig.getProfile(), ".chunk-temp" + File.separator + uploadId);
    }

    private Set<Integer> scanUploadedChunkIndexes(File tempDir) {
        if (tempDir == null || !tempDir.exists() || !tempDir.isDirectory()) {
            return Collections.emptySet();
        }
        Set<Integer> indexes = new HashSet<>();
        File[] files = tempDir.listFiles((dir, name) -> name.endsWith(".part"));
        if (files == null) {
            return indexes;
        }
        for (File file : files) {
            String name = file.getName();
            try {
                int index = Integer.parseInt(name.substring(0, name.indexOf('.')));
                if (file.length() > 0) {
                    indexes.add(index);
                }
            } catch (Exception ignored) {
            }
        }
        return indexes;
    }

    private List<Integer> getMissingChunkIndexes(ChunkUploadSession session) {
        List<Integer> missingIndexes = new ArrayList<>();
        for (int i = 0; i < session.getTotalChunks(); i++) {
            File partFile = new File(session.getTempDir(), i + ".part");
            if (!session.getUploadedChunks().contains(i) || !partFile.exists() || partFile.length() == 0) {
                missingIndexes.add(i);
            }
        }
        return missingIndexes;
    }

    private void mergeChunks(ChunkUploadSession session, File mergedFile) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(mergedFile, false)) {
            byte[] buffer = new byte[1024 * 1024];
            for (int i = 0; i < session.getTotalChunks(); i++) {
                File partFile = new File(session.getTempDir(), i + ".part");
                try (FileInputStream inputStream = new FileInputStream(partFile)) {
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                }
            }
            outputStream.flush();
        }
    }

    private void cleanupChunkSession(String uploadId) {
        ChunkUploadSession session = CHUNK_UPLOAD_SESSION_MAP.remove(uploadId);
        if (session == null) {
            return;
        }
        deleteRecursively(session.getTempDir());
    }

    private void cleanupExpiredChunkSessions() {
        long now = System.currentTimeMillis();
        List<String> expiredIds = new ArrayList<>();
        for (Map.Entry<String, ChunkUploadSession> entry : CHUNK_UPLOAD_SESSION_MAP.entrySet()) {
            if (now - entry.getValue().getLastUpdatedAt() > CHUNK_SESSION_EXPIRE_MILLIS) {
                expiredIds.add(entry.getKey());
            }
        }
        for (String expiredId : expiredIds) {
            cleanupChunkSession(expiredId);
        }
    }

    private boolean isAsyncTransferEnabled() {
        String configValue = configService.selectConfigByKey(UPLOAD_ASYNC_TRANSFER_KEY);
        if (StringUtils.isNotEmpty(configValue)) {
            return Convert.toBool(configValue);
        }
        return "webdav".equalsIgnoreCase(fileStorageService.getStorageType());
    }

    private File moveMergedFileToAsyncStaging(String taskId, File mergedFile) throws IOException {
        File stagingDir = new File(RuoYiConfig.getProfile(), ".async-transfer" + File.separator + taskId);
        if (!stagingDir.exists() && !stagingDir.mkdirs()) {
            throw new IOException("创建异步转存目录失败");
        }
        File stagedFile = new File(stagingDir, "payload.bin");
        Files.move(mergedFile.toPath(), stagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return stagedFile;
    }

    private void processAsyncTransfer(String taskId) {
        AsyncTransferTask task = ASYNC_TRANSFER_TASK_MAP.get(taskId);
        if (task == null) {
            return;
        }

        long storageStartAt = System.currentTimeMillis();
        task.setStatus(AsyncTransferStatus.PROCESSING.name());
        task.setLastUpdatedAt(System.currentTimeMillis());

        try {
            MultipartFile mergedMultipart = new LocalTempMultipartFile(task.getFileName(), task.getStagingFile());
            InputStream contentStream = mergedMultipart.getInputStream();
            String fullPath = fileStorageService.upload(mergedMultipart, task.getTargetPath());

            task.setFinalPath(fullPath);
            task.setStatus(AsyncTransferStatus.SUCCESS.name());
            task.setErrorMessage(null);
            task.setLastUpdatedAt(System.currentTimeMillis());

            fullTextSearchService.indexFileAsync(fullPath, task.getFileName(), contentStream);

            directoryCacheService.invalidateCache(task.getDirPath());
            directoryCacheService.invalidateAllUserCacheForDir(task.getDirPath());

            if (isUploadPerfLogEnabled()) {
                long storageCost = System.currentTimeMillis() - storageStartAt;
                log.info("async transfer timing taskId={}, file={}, storage={}ms", task.getTaskId(), task.getFileName(), storageCost);
            }
        } catch (Exception e) {
            task.setStatus(AsyncTransferStatus.FAILED.name());
            task.setErrorMessage(e.getMessage());
            task.setLastUpdatedAt(System.currentTimeMillis());
            log.error("async transfer failed taskId={}, file={}", task.getTaskId(), task.getFileName(), e);
        } finally {
            if (task.getStagingFile() != null) {
                deleteRecursively(task.getStagingFile().getParentFile());
            }
        }
    }

    private void cleanupExpiredAsyncTasks() {
        long now = System.currentTimeMillis();
        List<String> expiredIds = new ArrayList<>();
        for (Map.Entry<String, AsyncTransferTask> entry : ASYNC_TRANSFER_TASK_MAP.entrySet()) {
            AsyncTransferTask task = entry.getValue();
            if (AsyncTransferStatus.QUEUED.name().equals(task.getStatus()) || AsyncTransferStatus.PROCESSING.name().equals(task.getStatus())) {
                continue;
            }
            if (now - task.getLastUpdatedAt() > ASYNC_TASK_EXPIRE_MILLIS) {
                expiredIds.add(entry.getKey());
            }
        }
        for (String expiredId : expiredIds) {
            ASYNC_TRANSFER_TASK_MAP.remove(expiredId);
        }
    }

    private boolean isUploadPerfLogEnabled() {
        String configValue = configService.selectConfigByKey(UPLOAD_PERF_LOG_KEY);
        if (StringUtils.isEmpty(configValue)) {
            return false;
        }
        return Convert.toBool(configValue);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    public static class ChunkInitRequest {
        private String path;
        private String fileName;
        private String fileHash;
        private Long fileSize;
        private Integer chunkSize;
        private Integer totalChunks;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Integer getChunkSize() { return chunkSize; }
        public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
    }

    public static class ChunkCompleteRequest {
        private String uploadId;

        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    }

    private static class ChunkUploadSession {
        private String uploadId;
        private Long userId;
        private String dirPath;
        private String targetPath;
        private String fileName;
        private Integer totalChunks;
        private File tempDir;
        private Set<Integer> uploadedChunks = ConcurrentHashMap.newKeySet();
        private long lastUpdatedAt;

        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDirPath() { return dirPath; }
        public void setDirPath(String dirPath) { this.dirPath = dirPath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
        public File getTempDir() { return tempDir; }
        public void setTempDir(File tempDir) { this.tempDir = tempDir; }
        public Set<Integer> getUploadedChunks() { return uploadedChunks; }
        public long getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    }

    private enum AsyncTransferStatus {
        QUEUED,
        PROCESSING,
        SUCCESS,
        FAILED
    }

    private static class AsyncTransferTask {
        private String taskId;
        private String uploadId;
        private Long userId;
        private String dirPath;
        private String targetPath;
        private String fileName;
        private File stagingFile;
        private String finalPath;
        private String status;
        private String errorMessage;
        private long lastUpdatedAt;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDirPath() { return dirPath; }
        public void setDirPath(String dirPath) { this.dirPath = dirPath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public File getStagingFile() { return stagingFile; }
        public void setStagingFile(File stagingFile) { this.stagingFile = stagingFile; }
        public String getFinalPath() { return finalPath; }
        public void setFinalPath(String finalPath) { this.finalPath = finalPath; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    }

    private static class LocalTempMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final File file;

        LocalTempMultipartFile(String originalFilename, File file) {
            this.originalFilename = originalFilename;
            this.file = file;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return file == null || !file.exists() || file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            try (FileInputStream in = new FileInputStream(file)) {
                return in.readAllBytes();
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileInputStream in = new FileInputStream(file); FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }

    /**
     * 重命名文件或文件夹
     * @param oldPath 原路径
     * @param newName 新名称
     * @return 操作结果
     */
    @Log(title = "文件重命名", businessType = BusinessType.UPDATE)
    @PostMapping("/rename")
    public AjaxResult rename(@RequestParam("oldPath") String oldPath,
                             @RequestParam("newName") String newName) throws Exception {
        // 参数校验
        if (StringUtils.isEmpty(oldPath) || StringUtils.isEmpty(newName)) {
            return AjaxResult.error("参数不能为空");
        }
        
        // 检查新名称是否合法
        if (newName.contains("/") || newName.contains("\\")) {
            return AjaxResult.error("名称不能包含路径分隔符");
        }
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 检查是否为根目录下的直接子项重命名：只有管理员才能重命名根目录下的文件/文件夹
        String normalizedOldPath = oldPath.startsWith("/") ? oldPath : "/" + oldPath;
        if (!"/".equals(normalizedOldPath)) {
            String pathWithoutRoot = normalizedOldPath.substring(1); // 去掉开头的 /
            // 如果路径中没有 /，说明是根目录下的直接子项
            if (!pathWithoutRoot.contains("/") && !SecurityUtils.isAdmin()) {
                return AjaxResult.error("只有管理员才能重命名根目录下的文件或文件夹");
            }
        }
        
        // 检查是否有重命名权限（需要写权限）
        // 只检查第一层目录的写权限
        String permissionPath = extractDirPath(oldPath);
        if (!fileDirPermissionService.hasWritePermission(userId, permissionPath)) {
            return AjaxResult.error("无权重命名此文件/文件夹");
        }
        
        // 构建新路径
        String parentPath = extractDirPath(normalizedOldPath);
        String newPath = parentPath.endsWith("/") 
            ? parentPath + newName 
            : parentPath + "/" + newName;
        
        // 检查新路径是否已存在
        if (fileStorageService.exists(newPath)) {
            return AjaxResult.error("该名称已存在");
        }
        
        // 执行重命名
        boolean success = fileStorageService.rename(normalizedOldPath, newPath);
        if (!success) {
            return AjaxResult.error("重命名失败");
        }
        
        // 更新数据库中的权限记录
        fileDirPermissionService.renamePath(normalizedOldPath, newPath);
        
        // 更新全文索引
        if (fileStorageService.isDirectory(newPath)) {
            // 文件夹：删除旧前缀索引，然后异步重建新文件夹下的文件索引
            fullTextSearchService.removeByPrefix(normalizedOldPath);
            final String reindexPath = newPath;
            final Long reindexUserId = userId;
            new Thread(() -> {
                try {
                    rebuildIndexRecursive(reindexPath, reindexUserId, 0);
                    log.info("文件夹重命名后索引重建完成: {}", reindexPath);
                } catch (Exception e) {
                    log.warn("文件夹重命名后索引重建失败: {} - {}", reindexPath, e.getMessage());
                }
            }, "rename-reindex").start();
        } else if (DocumentContentExtractor.isSupportedDocument(newName)) {
            // 文件：删旧路径，加新路径
            fullTextSearchService.removeFile(normalizedOldPath);
            try (InputStream is = fileStorageService.download(newPath)) {
                if (is != null) fullTextSearchService.indexFile(newPath, newName, is);
            }
        }

        // 清除相关缓存
        directoryCacheService.invalidateCache(parentPath);
        directoryCacheService.invalidateAllUserCacheForDir(parentPath);

        return AjaxResult.success("重命名成功");
    }

    /**
     * 移动文件或文件夹到指定目录
     * @param sourcePath 源路径
     * @param targetDir 目标目录
     * @return 操作结果
     */
    @Log(title = "文件移动", businessType = BusinessType.UPDATE)
    @PostMapping("/move")
    public AjaxResult move(@RequestParam("sourcePath") String sourcePath,
                           @RequestParam("targetDir") String targetDir) throws Exception {
        // 参数校验
        if (StringUtils.isEmpty(sourcePath) || StringUtils.isEmpty(targetDir)) {
            return AjaxResult.error("参数不能为空");
        }
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 规范化路径
        String normalizedSourcePath = sourcePath.startsWith("/") ? sourcePath : "/" + sourcePath;
        String normalizedTargetDir = targetDir.startsWith("/") ? targetDir : "/" + targetDir;
        
        // 检查源路径是否存在
        if (!fileStorageService.exists(normalizedSourcePath)) {
            return AjaxResult.error("源文件/文件夹不存在");
        }
        
        // 检查目标目录是否存在
        if (!fileStorageService.exists(normalizedTargetDir)) {
            return AjaxResult.error("目标目录不存在");
        }
        
        // 检查是否有源路径的删除权限（只检查第一层目录）
        String sourcePermissionPath = extractDirPath(normalizedSourcePath);
        if (!fileDirPermissionService.hasDeletePermission(userId, sourcePermissionPath)) {
            return AjaxResult.error("无权移动此文件/文件夹");
        }
        
        // 检查是否有目标目录的上传权限（只检查第一层目录）
        if (!fileDirPermissionService.hasWritePermission(userId, normalizedTargetDir)) {
            return AjaxResult.error("无权移动到目标目录");
        }
        
        // 构建目标路径
        String fileName = normalizedSourcePath.substring(normalizedSourcePath.lastIndexOf('/') + 1);
        String targetPath = normalizedTargetDir.endsWith("/") 
            ? normalizedTargetDir + fileName 
            : normalizedTargetDir + "/" + fileName;
        
        // 检查目标路径是否已存在
        if (fileStorageService.exists(targetPath)) {
            return AjaxResult.error("目标目录已存在同名文件/文件夹");
        }
        
        // 在移动前判断是否是目录（移动后源路径就不存在了）
        boolean isSourceDirectory = fileStorageService.isDirectory(normalizedSourcePath);
        
        // 执行移动
        boolean success = fileStorageService.rename(normalizedSourcePath, targetPath);
        if (!success) {
            return AjaxResult.error("移动失败");
        }
        
        // 如果是文件夹，更新数据库中的路径
        if (isSourceDirectory) {
            fileDirPermissionService.renamePath(normalizedSourcePath, targetPath);
        }
        
        // 更新全文索引
        if (isSourceDirectory) {
            fullTextSearchService.removeByPrefix(normalizedSourcePath);
            final String reindexPath = targetPath;
            final Long moveUserId = userId;
            new Thread(() -> {
                try {
                    rebuildIndexRecursive(reindexPath, moveUserId, 0);
                    log.info("文件夹移动后索引重建完成: {}", reindexPath);
                } catch (Exception e) {
                    log.warn("文件夹移动后索引重建失败: {} - {}", reindexPath, e.getMessage());
                }
            }, "move-reindex").start();
        } else if (DocumentContentExtractor.isSupportedDocument(fileName)) {
            fullTextSearchService.removeFile(normalizedSourcePath);
            try (InputStream is = fileStorageService.download(targetPath)) {
                if (is != null) fullTextSearchService.indexFile(targetPath, fileName, is);
            }
        }

        // 清除相关缓存
        directoryCacheService.invalidateCache(extractDirPath(normalizedSourcePath));
        directoryCacheService.invalidateCache(normalizedTargetDir);
        directoryCacheService.invalidateAllUserCacheForDir(extractDirPath(normalizedSourcePath));
        directoryCacheService.invalidateAllUserCacheForDir(normalizedTargetDir);

        return AjaxResult.success("移动成功");
    }
    
}