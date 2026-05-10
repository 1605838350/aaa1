package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysFileDir;

/**
 * 文件目录权限Mapper接口
 * 
 * @author xyj
 * @date 2026-03-26
 */
public interface SysFileDirMapper 
{
    /**
     * 查询文件目录权限
     * 
     * @param dirId 文件目录权限主键
     * @return 文件目录权限
     */
    public SysFileDir selectSysFileDirByDirId(Long dirId);

    /**
     * 根据ID查询目录（别名方法）
     * 
     * @param dirId 目录ID
     * @return 目录信息
     */
    public SysFileDir selectDirById(Long dirId);

    /**
     * 查询文件目录权限列表
     * 
     * @param sysFileDir 文件目录权限
     * @return 文件目录权限集合
     */
    public List<SysFileDir> selectSysFileDirList(SysFileDir sysFileDir);

    /**
     * 新增文件目录权限
     * 
     * @param sysFileDir 文件目录权限
     * @return 结果
     */
    public int insertSysFileDir(SysFileDir sysFileDir);

    /**
     * 修改文件目录权限
     * 
     * @param sysFileDir 文件目录权限
     * @return 结果
     */
    public int updateSysFileDir(SysFileDir sysFileDir);

    /**
     * 删除文件目录权限
     * 
     * @param dirId 文件目录权限主键
     * @return 结果
     */
    public int deleteSysFileDirByDirId(Long dirId);

    /**
     * 批量删除文件目录权限
     * 
     * @param dirIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysFileDirByDirIds(Long[] dirIds);

    /**
     * 根据父ID查询子目录列表
     * 
     * @param parentId 父目录ID
     * @return 子目录列表
     */
    public List<SysFileDir> selectChildrenByParentId(Long parentId);
    
    /**
     * 根据路径查询目录
     * 
     * @param path 目录路径
     * @return 目录信息
     */
    public SysFileDir selectDirByPath(String path);
}
