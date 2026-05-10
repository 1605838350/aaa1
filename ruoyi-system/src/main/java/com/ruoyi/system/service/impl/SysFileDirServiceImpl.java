package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysFileDirMapper;
import com.ruoyi.system.domain.SysFileDir;
import com.ruoyi.system.domain.SysRoleFileDir;
import com.ruoyi.system.service.ISysFileDirService;
import com.ruoyi.system.service.IFileDirPermissionService;

/**
 * 文件目录权限Service业务层处理
 * 
 * @author xyj
 * @date 2026-03-26
 */
@Service
public class SysFileDirServiceImpl implements ISysFileDirService 
{
    @Autowired
    private SysFileDirMapper sysFileDirMapper;
    
    @Autowired
    private IFileDirPermissionService fileDirPermissionService;

    /**
     * 查询文件目录权限
     * 
     * @param dirId 文件目录权限主键
     * @return 文件目录权限
     */
    @Override
    public SysFileDir selectSysFileDirByDirId(Long dirId)
    {
        return sysFileDirMapper.selectSysFileDirByDirId(dirId);
    }

    /**
     * 查询文件目录权限列表
     * 
     * @param sysFileDir 文件目录权限
     * @return 文件目录权限
     */
    @Override
    public List<SysFileDir> selectSysFileDirList(SysFileDir sysFileDir)
    {
        return sysFileDirMapper.selectSysFileDirList(sysFileDir);
    }

    /**
     * 新增文件目录权限
     * 
     * @param sysFileDir 文件目录权限
     * @return 结果
     */
    @Override
    public int insertSysFileDir(SysFileDir sysFileDir)
    {
        sysFileDir.setCreateTime(DateUtils.getNowDate());
        int result = sysFileDirMapper.insertSysFileDir(sysFileDir);
        
        // 新增目录成功后，自动给超级管理员(roleId=1)分配全部权限
        if (result > 0 && sysFileDir.getDirId() != null)
        {
            SysRoleFileDir adminPermission = new SysRoleFileDir();
            adminPermission.setRoleId(1L); // 超级管理员角色ID
            adminPermission.setDirId(sysFileDir.getDirId());
            adminPermission.setCanView(true);
            adminPermission.setCanDownload(true);
            adminPermission.setCanUpload(true);
            adminPermission.setCanDelete(true);
            fileDirPermissionService.saveRoleDirPermission(adminPermission);
        }
        
        return result;
    }

    /**
     * 修改文件目录权限
     * 
     * @param sysFileDir 文件目录权限
     * @return 结果
     */
    @Override
    public int updateSysFileDir(SysFileDir sysFileDir)
    {
        sysFileDir.setUpdateTime(DateUtils.getNowDate());
        return sysFileDirMapper.updateSysFileDir(sysFileDir);
    }

    /**
     * 批量删除文件目录权限
     * 
     * @param dirIds 需要删除的文件目录权限主键
     * @return 结果
     */
    @Override
    public int deleteSysFileDirByDirIds(Long[] dirIds)
    {
        return sysFileDirMapper.deleteSysFileDirByDirIds(dirIds);
    }

    /**
     * 删除文件目录权限信息
     * 
     * @param dirId 文件目录权限主键
     * @return 结果
     */
    @Override
    public int deleteSysFileDirByDirId(Long dirId)
    {
        return sysFileDirMapper.deleteSysFileDirByDirId(dirId);
    }
}
