package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * 角色和文件目录权限关联表 sys_role_file_dir
 * 
 * @author ruoyi
 */
public class SysRoleFileDir implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 目录ID */
    private Long dirId;

    /** 查看权限（0-否 1-是） */
    private Boolean canView;

    /** 下载权限（0-否 1-是） */
    private Boolean canDownload;

    /** 上传权限（0-否 1-是） */
    private Boolean canUpload;

    /** 删除权限（0-否 1-是） */
    private Boolean canDelete;

    /** 角色名称（非数据库字段） */
    private String roleName;

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public Long getDirId()
    {
        return dirId;
    }

    public void setDirId(Long dirId)
    {
        this.dirId = dirId;
    }

    public Boolean getCanView()
    {
        return canView;
    }

    public void setCanView(Boolean canView)
    {
        this.canView = canView;
    }

    public Boolean getCanDownload()
    {
        return canDownload;
    }

    public void setCanDownload(Boolean canDownload)
    {
        this.canDownload = canDownload;
    }

    public Boolean getCanUpload()
    {
        return canUpload;
    }

    public void setCanUpload(Boolean canUpload)
    {
        this.canUpload = canUpload;
    }

    public Boolean getCanDelete()
    {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete)
    {
        this.canDelete = canDelete;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }
}
