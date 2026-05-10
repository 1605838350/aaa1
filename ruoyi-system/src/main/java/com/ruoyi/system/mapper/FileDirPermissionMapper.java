package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SysRoleFileDir;

/**
 * 文件目录权限 Mapper
 * 
 * @author ruoyi
 */
public interface FileDirPermissionMapper
{
    /**
     * 根据用户ID和目录路径查询权限
     * 
     * @param userId 用户ID
     * @param path 目录路径
     * @return 权限信息
     */
    public SysRoleFileDir selectPermissionByUserAndPath(@Param("userId") Long userId, @Param("path") String path);

    /**
     * 根据角色ID和目录ID查询权限
     * 
     * @param roleId 角色ID
     * @param dirId 目录ID
     * @return 权限信息
     */
    public SysRoleFileDir selectPermissionByRoleAndDir(@Param("roleId") Long roleId, @Param("dirId") Long dirId);

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
     * 插入权限记录
     * 
     * @param roleFileDir 权限信息
     * @return 结果
     */
    public int insertPermission(SysRoleFileDir roleFileDir);

    /**
     * 更新权限记录
     * 
     * @param roleFileDir 权限信息
     * @return 结果
     */
    public int updatePermission(SysRoleFileDir roleFileDir);

    /**
     * 删除权限记录
     * 
     * @param roleId 角色ID
     * @param dirId 目录ID
     * @return 结果
     */
    public int deletePermission(@Param("roleId") Long roleId, @Param("dirId") Long dirId);

    /**
     * 批量删除权限记录
     * 
     * @param roleId 角色ID
     * @param dirIds 目录ID列表
     * @return 结果
     */
    public int batchDeletePermissions(@Param("roleId") Long roleId, @Param("dirIds") List<Long> dirIds);

    /**
     * 检查权限记录是否存在
     * 
     * @param roleId 角色ID
     * @param dirId 目录ID
     * @return 数量
     */
    public int checkPermissionExists(@Param("roleId") Long roleId, @Param("dirId") Long dirId);

    /**
     * 根据用户ID查询角色ID列表
     * 
     * @param userId 用户ID
     * @return 角色ID列表
     */
    public List<Long> selectRoleIdsByUserId(Long userId);
}
