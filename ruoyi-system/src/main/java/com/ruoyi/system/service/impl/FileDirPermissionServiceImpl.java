package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysFileDir;
import com.ruoyi.system.domain.SysRoleFileDir;
import com.ruoyi.system.mapper.FileDirPermissionMapper;
import com.ruoyi.system.mapper.SysFileDirMapper;
import com.ruoyi.system.service.IFileDirPermissionService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件目录权限检查服务实现
 * 
 * 【方案三】权限结果缓存：权限检查结果单独缓存，减少数据库查询
 * 
 * @author ruoyi
 */
@Service
public class FileDirPermissionServiceImpl implements IFileDirPermissionService
{
    /** 【方案三】权限缓存 key 前缀 */
    private static final String PERM_CACHE_PREFIX = "user_perm:";
    
    /** 【方案三】权限缓存过期时间（分钟） */
    private static final int PERM_CACHE_MINUTES = 5;
    
    @Autowired
    private FileDirPermissionMapper permissionMapper;
    
    @Autowired
    private SysFileDirMapper dirMapper;
    
    /** 【方案三】Redis 缓存 */
    @Autowired(required = false)
    private RedisCache redisCache;

    /**
     * 检查用户是否有指定目录的查看权限
     * 只检查第一层目录（根目录下的直接子目录）的权限
     */
    @Override
    public boolean hasViewPermission(Long userId, String path)
    {
        String firstLevelPath = getFirstLevelPath(path);
        
        // 超级管理员：只检查目录是否已在 sys_file_dir 表中注册
        if (SecurityUtils.isAdmin(userId)) {
            return dirMapper.selectDirByPath(firstLevelPath) != null;
        }
        
        // 尝试从缓存获取
        Boolean cached = getPermissionFromCache(userId, firstLevelPath, "view");
        if (cached != null) {
            return cached;
        }
        
        // 只查询第一层目录的权限
        SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
        boolean result = permission != null && Boolean.TRUE.equals(permission.getCanView());
        
        // 缓存结果
        setPermissionToCache(userId, firstLevelPath, "view", result);
        return result;
    }

    /**
     * 批量检查用户对多个路径的查看权限
     * 只检查第一层目录权限
     */
    @Override
    public List<String> batchCheckViewPermission(Long userId, List<String> paths) {
        if (userId == null || paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 超级管理员：检查目录是否已在 sys_file_dir 表中注册
        if (SecurityUtils.isAdmin(userId)) {
            List<String> allowedPaths = new ArrayList<>();
            for (String path : paths) {
                String normalizedPath = path.startsWith("/") ? path : "/" + path;
                String firstLevelPath = getFirstLevelPath(normalizedPath);
                if (dirMapper.selectDirByPath(firstLevelPath) != null) {
                    allowedPaths.add(path);
                }
            }
            return allowedPaths;
        }
        
        List<String> allowedPaths = new ArrayList<>();
        
        // 收集所有需要检查的第一层目录路径
        Set<String> allFirstLevelPaths = new HashSet<>();
        Map<String, String> pathToFirstLevelMap = new HashMap<>();
        
        for (String path : paths) {
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            String firstLevelPath = getFirstLevelPath(normalizedPath);
            pathToFirstLevelMap.put(path, firstLevelPath);
            allFirstLevelPaths.add(firstLevelPath);
        }
        
        // 批量查询所有第一层目录的权限
        Map<String, SysRoleFileDir> permissionMap = new HashMap<>();
        for (String firstLevelPath : allFirstLevelPaths) {
            // 先检查缓存
            Boolean cached = getPermissionFromCache(userId, firstLevelPath, "view");
            if (cached != null) {
                // 缓存中有结果，创建一个临时对象
                SysRoleFileDir temp = new SysRoleFileDir();
                temp.setCanView(cached);
                permissionMap.put(firstLevelPath, temp);
            } else {
                // 查询数据库
                SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
                permissionMap.put(firstLevelPath, permission);
                // 缓存结果
                if (permission != null) {
                    setPermissionToCache(userId, firstLevelPath, "view", Boolean.TRUE.equals(permission.getCanView()));
                } else {
                    setPermissionToCache(userId, firstLevelPath, "view", false);
                }
            }
        }
        
        // 批量检查权限（只检查第一层目录）
        for (String path : paths) {
            String firstLevelPath = pathToFirstLevelMap.get(path);
            SysRoleFileDir permission = permissionMap.get(firstLevelPath);
            
            if (permission != null && Boolean.TRUE.equals(permission.getCanView())) {
                allowedPaths.add(path);
            }
        }
        
        return allowedPaths;
    }

    /**
     * 检查用户是否有指定目录的下载权限
     * 只检查第一层目录权限
     */
    @Override
    public boolean hasDownloadPermission(Long userId, String path)
    {
        String firstLevelPath = getFirstLevelPath(path);
        
        // 超级管理员：只检查目录是否已注册
        if (SecurityUtils.isAdmin(userId)) {
            return dirMapper.selectDirByPath(firstLevelPath) != null;
        }
        
        Boolean cached = getPermissionFromCache(userId, firstLevelPath, "download");
        if (cached != null) {
            return cached;
        }
        
        SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
        boolean result = permission != null && Boolean.TRUE.equals(permission.getCanDownload());
        
        setPermissionToCache(userId, firstLevelPath, "download", result);
        return result;
    }

    /**
     * 检查用户是否有指定目录的上传权限
     * 只检查第一层目录权限
     */
    @Override
    public boolean hasUploadPermission(Long userId, String path)
    {
        String firstLevelPath = getFirstLevelPath(path);
        
        // 超级管理员：只检查目录是否已注册
        if (SecurityUtils.isAdmin(userId)) {
            return dirMapper.selectDirByPath(firstLevelPath) != null;
        }
        
        Boolean cached = getPermissionFromCache(userId, firstLevelPath, "upload");
        if (cached != null) {
            return cached;
        }
        
        SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
        boolean result = permission != null && Boolean.TRUE.equals(permission.getCanUpload());
        
        setPermissionToCache(userId, firstLevelPath, "upload", result);
        return result;
    }

    /**
     * 检查用户是否有指定目录的删除权限
     * 只检查第一层目录权限
     */
    @Override
    public boolean hasDeletePermission(Long userId, String path)
    {
        String firstLevelPath = getFirstLevelPath(path);
        
        // 超级管理员：只检查目录是否已注册
        if (SecurityUtils.isAdmin(userId)) {
            return dirMapper.selectDirByPath(firstLevelPath) != null;
        }
        
        Boolean cached = getPermissionFromCache(userId, firstLevelPath, "delete");
        if (cached != null) {
            return cached;
        }
        
        SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
        boolean result = permission != null && Boolean.TRUE.equals(permission.getCanDelete());
        
        setPermissionToCache(userId, firstLevelPath, "delete", result);
        return result;
    }
    
    /**
     * 检查用户是否有指定目录的写权限
     * 写权限用于上传、重命名等操作
     * 只检查第一层目录权限
     */
    @Override
    public boolean hasWritePermission(Long userId, String path)
    {
        String firstLevelPath = getFirstLevelPath(path);
        
        // 超级管理员：只检查目录是否已注册
        if (SecurityUtils.isAdmin(userId)) {
            return dirMapper.selectDirByPath(firstLevelPath) != null;
        }
        
        Boolean cached = getPermissionFromCache(userId, firstLevelPath, "write");
        if (cached != null) {
            return cached;
        }
        
        // 写权限检查：需要上传权限
        SysRoleFileDir permission = permissionMapper.selectPermissionByUserAndPath(userId, firstLevelPath);
        boolean result = permission != null && Boolean.TRUE.equals(permission.getCanUpload());
        
        setPermissionToCache(userId, firstLevelPath, "write", result);
        return result;
    }

    /**
     * 【方案三】从缓存获取权限结果
     */
    private Boolean getPermissionFromCache(Long userId, String path, String permType) {
        if (redisCache == null || userId == null || path == null) {
            return null;
        }
        String cacheKey = buildPermCacheKey(userId, path, permType);
        try {
            return redisCache.getCacheObject(cacheKey);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 【方案三】缓存权限结果
     */
    private void setPermissionToCache(Long userId, String path, String permType, boolean result) {
        if (redisCache == null || userId == null || path == null) {
            return;
        }
        String cacheKey = buildPermCacheKey(userId, path, permType);
        try {
            redisCache.setCacheObject(cacheKey, result, PERM_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存失败不影响业务
        }
    }
    
    /**
     * 【方案三】构建权限缓存 key
     */
    private String buildPermCacheKey(Long userId, String path, String permType) {
        // 规范化路径
        String normalizedPath = path;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        } else if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return PERM_CACHE_PREFIX + userId + ":" + normalizedPath + ":" + permType;
    }
    
    /**
     * 【方案三】清除用户指定路径的权限缓存
     */
    public void clearUserPermissionCache(Long userId, String path) {
        if (redisCache == null || userId == null) {
            return;
        }
        
        // 清除该路径所有类型的权限缓存
        String[] permTypes = {"view", "download", "upload", "delete"};
        for (String permType : permTypes) {
            String cacheKey = buildPermCacheKey(userId, path, permType);
            try {
                redisCache.deleteObject(cacheKey);
            } catch (Exception e) {
                // 忽略删除失败
            }
        }
    }
    
    /**
     * 【方案三】清除用户所有权限缓存
     */
    public void clearAllUserPermissionCache(Long userId) {
        if (redisCache == null || userId == null) {
            return;
        }
        
        String pattern = PERM_CACHE_PREFIX + userId + ":*";
        try {
            Collection<String> keys = redisCache.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisCache.deleteObject(keys);
            }
        } catch (Exception e) {
            // 忽略删除失败
        }
    }

    /**
     * 获取路径的第一层目录（根目录下的直接子目录）
     * 例如：/test/流畅性/test07 -> /test
     *       /test -> /test
     *       / -> /
     */
    private String getFirstLevelPath(String path)
    {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // 确保以 / 开头
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        
        // 分割路径
        String[] parts = normalizedPath.split("/");
        
        // 找到第一个非空部分（第一层目录）
        for (String part : parts) {
            if (!part.isEmpty()) {
                return "/" + part;
            }
        }
        
        // 如果没有子目录，返回根目录
        return "/";
    }

    /**
     * 获取用户对指定目录的权限信息
     */
    @Override
    public SysRoleFileDir getUserDirPermission(Long userId, String path)
    {
        if (userId == null || path == null || path.isEmpty())
        {
            return null;
        }
        // 规范化路径
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return permissionMapper.selectPermissionByUserAndPath(userId, normalizedPath);
    }

    /**
     * 根据角色ID和目录ID查询权限
     */
    @Override
    public SysRoleFileDir selectRoleDirPermission(Long roleId, Long dirId)
    {
        return permissionMapper.selectPermissionByRoleAndDir(roleId, dirId);
    }

    /**
     * 根据目录ID查询所有角色权限
     */
    @Override
    public List<SysRoleFileDir> selectPermissionsByDirId(Long dirId)
    {
        return permissionMapper.selectPermissionsByDirId(dirId);
    }

    /**
     * 根据角色ID查询所有目录权限
     */
    @Override
    public List<SysRoleFileDir> selectPermissionsByRoleId(Long roleId)
    {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    /**
     * 保存角色目录权限
     * 【简化版】只需保存权限，无需递归处理父子目录
     */
    @Override
    public int saveRoleDirPermission(SysRoleFileDir roleFileDir)
    {
        if (roleFileDir == null || roleFileDir.getRoleId() == null || roleFileDir.getDirId() == null)
        {
            return 0;
        }

        int result;
        // 检查是否已存在
        int count = permissionMapper.checkPermissionExists(roleFileDir.getRoleId(), roleFileDir.getDirId());
        if (count > 0)
        {
            // 更新
            result = permissionMapper.updatePermission(roleFileDir);
        }
        else
        {
            // 插入
            result = permissionMapper.insertPermission(roleFileDir);
        }

        // 权限变更后清除缓存
        clearPermissionCacheAfterChange(roleFileDir.getRoleId(), roleFileDir.getDirId());

        return result;
    }
    
    /**
     * 【方案三】权限变更后清除相关缓存
     */
    private void clearPermissionCacheAfterChange(Long roleId, Long dirId) {
        if (redisCache == null) {
            return;
        }
        
        try {
            // 清除所有用户的权限缓存（简化处理：清除所有）
            // 生产环境可以更精确地清除特定用户的缓存
            String pattern = PERM_CACHE_PREFIX + "*";
            Collection<String> keys = redisCache.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisCache.deleteObject(keys);
                // 日志记录可以在这里添加
            }
        } catch (Exception e) {
            // 忽略清除失败
        }
    }

    /**
     * 删除角色目录权限
     * 【方案三】权限变更时清除相关缓存
     */
    @Override
    public int deleteRoleDirPermission(Long roleId, Long dirId)
    {
        int result = permissionMapper.deletePermission(roleId, dirId);
        
        // 【方案三】权限变更后清除缓存
        clearPermissionCacheAfterChange(roleId, dirId);
        
        return result;
    }

    /**
     * 批量保存角色目录权限
     */
    @Override
    public int batchSaveRoleDirPermissions(Long roleId, List<SysRoleFileDir> permissions)
    {
        if (roleId == null || permissions == null || permissions.isEmpty())
        {
            return 0;
        }

        int count = 0;
        for (SysRoleFileDir permission : permissions)
        {
            permission.setRoleId(roleId);
            count += saveRoleDirPermission(permission);
        }
        return count;
    }

    /**
     * 新建文件夹并自动分配权限
     * 【简化版】只有创建第一层目录时才需要添加权限记录
     * 子目录自动继承第一层目录权限，无需单独添加权限
     */
    @Override
    public String createFolderWithPermission(String parentPath, String folderName, Long userId) throws Exception {
        // 1. 规范化路径
        String normalizedParentPath = parentPath;
        if (normalizedParentPath == null || normalizedParentPath.isEmpty()) {
            normalizedParentPath = "/";
        }
        if (!normalizedParentPath.startsWith("/")) {
            normalizedParentPath = "/" + normalizedParentPath;
        }
        
        // 2. 构建新目录路径
        String newDirPath = normalizedParentPath.endsWith("/") 
            ? normalizedParentPath + folderName 
            : normalizedParentPath + "/" + folderName;
        
        // 3. 判断是否是第一层目录（父目录是根目录）
        boolean isFirstLevelDir = "/".equals(normalizedParentPath);
        
        // 4. 查找父目录信息
        SysFileDir parentDir = dirMapper.selectDirByPath(normalizedParentPath);
        if (parentDir == null && !"/".equals(normalizedParentPath)) {
            throw new RuntimeException("父目录不存在: " + normalizedParentPath);
        }
        
        // 5. 创建目录记录
        SysFileDir newDir = new SysFileDir();
        newDir.setDirName(folderName);
        newDir.setPath(newDirPath);
        newDir.setParentId(parentDir != null ? parentDir.getDirId() : 0L);
        newDir.setStatus("0"); // 正常状态
        dirMapper.insertSysFileDir(newDir);
        
        // 6. 只有创建第一层目录时才需要添加权限记录
        if (isFirstLevelDir) {
            // 第一层目录：需要为用户的所有角色创建权限记录
            // 获取用户的所有角色
            List<Long> roleIds = permissionMapper.selectRoleIdsByUserId(userId);
            
            // 确保管理员角色（roleId=1）也在列表中
            if (!roleIds.contains(1L)) {
                roleIds.add(1L);
            }
            
            Long newDirId = newDir.getDirId();
            for (Long roleId : roleIds) {
                // 创建新的权限记录
                SysRoleFileDir newPerm = new SysRoleFileDir();
                newPerm.setRoleId(roleId);
                newPerm.setDirId(newDirId);
                // 默认给予所有权限（创建者自动拥有权限）
                newPerm.setCanView(true);
                newPerm.setCanDownload(true);
                newPerm.setCanUpload(true);
                newPerm.setCanDelete(true);
                
                permissionMapper.insertPermission(newPerm);
            }
        }
        // 子目录不需要添加权限记录，继承第一层目录权限
        
        // 7. 清除相关缓存
        clearPermissionCacheAfterChange(null, newDir.getDirId());
        
        return newDirPath;
    }
    
    /**
     * 删除目录及其权限
     * 【简化版】只有第一层目录有权限记录，子目录无需处理权限
     */
    @Override
    public boolean deleteDirWithPermission(String dirPath) throws Exception {
        // 1. 规范化路径
        String normalizedPath = dirPath;
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            normalizedPath = "/";
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        
        // 2. 查找目录信息
        SysFileDir dir = dirMapper.selectDirByPath(normalizedPath);
        if (dir == null) {
            // 目录不存在，视为删除成功
            return true;
        }
        
        Long dirId = dir.getDirId();
        
        // 3. 判断是否是第一层目录
        boolean isFirstLevelDir = dir.getParentId() == null || dir.getParentId() == 0L;
        
        // 4. 只有第一层目录才需要删除权限记录
        if (isFirstLevelDir) {
            List<SysRoleFileDir> permissions = permissionMapper.selectPermissionsByDirId(dirId);
            for (SysRoleFileDir perm : permissions) {
                permissionMapper.deletePermission(perm.getRoleId(), dirId);
            }
        }
        // 子目录没有权限记录，不需要处理
        
        // 5. 递归删除子目录记录（不涉及权限）
        deleteChildrenDirs(dirId);
        
        // 6. 删除目录记录
        dirMapper.deleteSysFileDirByDirId(dirId);
        
        // 7. 清除缓存
        clearPermissionCacheAfterChange(null, dirId);
        
        return true;
    }
    
    /**
     * 递归删除子目录记录
     */
    private void deleteChildrenDirs(Long parentDirId) {
        // 查询子目录
        List<SysFileDir> children = dirMapper.selectChildrenByParentId(parentDirId);
        if (children == null || children.isEmpty()) {
            return;
        }
        
        for (SysFileDir child : children) {
            Long childDirId = child.getDirId();
            
            // 递归删除孙目录
            deleteChildrenDirs(childDirId);
            
            // 删除子目录记录
            dirMapper.deleteSysFileDirByDirId(childDirId);
        }
    }
    
    @Override
    @Transactional
    public boolean renamePath(String oldPath, String newPath) throws Exception {
        // 规范化路径
        String normalizedOldPath = oldPath.startsWith("/") ? oldPath : "/" + oldPath;
        String normalizedNewPath = newPath.startsWith("/") ? newPath : "/" + newPath;
        
        // 1. 更新当前目录的路径和名称（如果是文件夹）
        SysFileDir dir = dirMapper.selectDirByPath(normalizedOldPath);
        if (dir != null) {
            // 是文件夹，更新数据库
            dir.setPath(normalizedNewPath);
            // 提取新名称（路径最后一部分）
            String newDirName = normalizedNewPath.substring(normalizedNewPath.lastIndexOf('/') + 1);
            dir.setDirName(newDirName);
            
            // 更新 parent_id（如果是移动到不同父目录）
            String newParentPath = normalizedNewPath.substring(0, normalizedNewPath.lastIndexOf('/'));
            if (newParentPath.isEmpty()) {
                newParentPath = "/";
            }
            SysFileDir newParentDir = dirMapper.selectDirByPath(newParentPath);
            if (newParentDir != null) {
                dir.setParentId(newParentDir.getDirId());
            }
            
            dirMapper.updateSysFileDir(dir);
            
            // 2. 递归更新所有子目录的路径
            renameChildrenPaths(dir.getDirId(), normalizedOldPath, normalizedNewPath);
        }
        // 如果是文件，不需要更新数据库（文件不在数据库中）
        
        return true;
    }
    
    /**
     * 递归更新子目录的路径
     */
    private void renameChildrenPaths(Long parentDirId, String oldParentPath, String newParentPath) {
        // 获取实际的子目录列表
        List<SysFileDir> childDirs = dirMapper.selectChildrenByParentId(parentDirId);
        if (childDirs == null || childDirs.isEmpty()) {
            return;
        }
        
        for (SysFileDir child : childDirs) {
            String childOldPath = child.getPath();
            // 确保子路径以父路径开头
            if (!childOldPath.startsWith(oldParentPath)) {
                continue;
            }
            // 替换路径前缀（只替换开头的父路径部分）
            String childNewPath = newParentPath + childOldPath.substring(oldParentPath.length());
            
            // 更新子目录路径
            child.setPath(childNewPath);
            // 子目录的 parent_id 保持不变（因为父目录的 ID 没变）
            String childNewName = childNewPath.substring(childNewPath.lastIndexOf('/') + 1);
            if (!childNewName.equals(child.getDirName())) {
                child.setDirName(childNewName);
            }
            dirMapper.updateSysFileDir(child);
            
            // 递归更新孙目录
            renameChildrenPaths(child.getDirId(), oldParentPath, newParentPath);
        }
    }
}
