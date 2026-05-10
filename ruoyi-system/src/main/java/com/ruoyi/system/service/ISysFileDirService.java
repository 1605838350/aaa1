package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysFileDir;

/**
 * 文件目录权限Service接口
 * 
 * @author xyj
 * @date 2026-03-26
 */
public interface ISysFileDirService 
{
    /**
     * 查询文件目录权限
     * 
     * @param dirId 文件目录权限主键
     * @return 文件目录权限
     */
    public SysFileDir selectSysFileDirByDirId(Long dirId);

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
     * 批量删除文件目录权限
     * 
     * @param dirIds 需要删除的文件目录权限主键集合
     * @return 结果
     */
    public int deleteSysFileDirByDirIds(Long[] dirIds);

    /**
     * 删除文件目录权限信息
     * 
     * @param dirId 文件目录权限主键
     * @return 结果
     */
    public int deleteSysFileDirByDirId(Long dirId);
}
