package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.TreeEntity;

/**
 * 文件目录权限对象 sys_file_dir
 * 
 * @author xyj
 * @date 2026-03-26
 */
public class SysFileDir extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 目录ID */
    private Long dirId;

    /** 目录名称 */
    @Excel(name = "目录名称")
    private String dirName;

    /** 文件系统路径 */
    @Excel(name = "文件系统路径")
    private String path;

    /** 权限标识 */
    @Excel(name = "权限标识")
    private String perms;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public void setDirId(Long dirId) 
    {
        this.dirId = dirId;
    }

    public Long getDirId() 
    {
        return dirId;
    }

    public void setDirName(String dirName) 
    {
        this.dirName = dirName;
    }

    public String getDirName() 
    {
        return dirName;
    }

    public void setPath(String path) 
    {
        this.path = path;
    }

    public String getPath() 
    {
        return path;
    }

    public void setPerms(String perms) 
    {
        this.perms = perms;
    }

    public String getPerms() 
    {
        return perms;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("dirId", getDirId())
            .append("dirName", getDirName())
            .append("parentId", getParentId())
            .append("path", getPath())
            .append("orderNum", getOrderNum())
            .append("perms", getPerms())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
