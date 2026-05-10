package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysRoleFileDir;

/**
 * 文件目录权限检查服务
 * 
 * @author ruoyi
 */
public interface IFileDirPermissionService
{
    /**
     * 检查用户是否有指定目录的查看权限
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return true-有权限 false-无权限
     */
    public boolean hasViewPermission(Long userId, String path);

    /**
     * 批量检查用户对多个路径的查看权限
     * 优化：减少数据库查询次数
     * 
     * @param userId 用户ID
     * @param paths 目录路径列表
     * @return 有权限的路径列表
     */
    public List<String> batchCheckViewPermission(Long userId, List<String> paths);

    /**
     * 检查用户是否有指定目录的下载权限
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return true-有权限 false-无权限
     */
    public boolean hasDownloadPermission(Long userId, String path);

    /**
     * 检查用户是否有指定目录的上传权限
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return true-有权限 false-无权限
     */
    public boolean hasUploadPermission(Long userId, String path);

    /**
     * 检查用户是否有指定目录的删除权限
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return true-有权限 false-无权限
     */
    public boolean hasDeletePermission(Long userId, String path);

    /**
     * 检查用户是否有指定目录的写权限（上传、重命名等）
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return true-有权限 false-无权限
     */
    public boolean hasWritePermission(Long userId, String path);

    /**
     * 获取用户对指定目录的权限信息
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return 权限信息（null表示无权限）
     */
    public SysRoleFileDir getUserDirPermission(Long userId, String path);

    /**
     * 根据角色ID和目录ID查询权限
     * 
     * @param roleId 角色ID
     * @param dirId 目录ID
     * @return 权限信息
     */
    public SysRoleFileDir selectRoleDirPermission(Long roleId, Long dirId);

    /**
     * 根据目录ID查询所有角色权限
     * 
     * @param dirId 目录ID
     * @return 权限列表
     */
    public List<SysRoleFileDir> selectPermissionsByDirId(Long dirId);

    /**
     * 根据角色ID查询所有目录权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    public List<SysRoleFileDir> selectPermissionsByRoleId(Long roleId);

    /**
     * 保存角色目录权限
     * 
     * @param roleFileDir 权限信息
     * @return 结果
     */
    public int saveRoleDirPermission(SysRoleFileDir roleFileDir);

    /**
     * 删除角色目录权限
     * 
     * @param roleId 角色ID
     * @param dirId 目录ID
     * @return 结果
     */
    public int deleteRoleDirPermission(Long roleId, Long dirId);

    /**
     * 批量保存角色目录权限
     * 
     * @param roleId 角色ID
     * @param permissions 权限列表
     * @return 结果
     */
    public int batchSaveRoleDirPermissions(Long roleId, List<SysRoleFileDir> permissions);

    /**
     * 新建文件夹并自动分配权限
     * 超级管理员：自动拥有所有权限
     * 其他角色：继承父目录的权限
     * 
     * @param parentPath 父目录路径
     * @param folderName 新文件夹名称
     * @param userId 创建用户ID
     * @return 新目录的完整路径
     */
    public String createFolderWithPermission(String parentPath, String folderName, Long userId) throws Exception;
    
    /**
     * 删除目录及其权限
     * 删除目录时同时删除该目录的所有权限记录
     * 
     * @param dirPath 目录路径
     * @return 是否删除成功
     */
    public boolean deleteDirWithPermission(String dirPath) throws Exception;
    
    /**
     * 重命名路径
     * 更新数据库中所有相关的权限记录路径
     * 
     * @param oldPath 原路径
     * @param newPath 新路径
     * @return 是否更新成功
     */
    public boolean renamePath(String oldPath, String newPath) throws Exception;
}
