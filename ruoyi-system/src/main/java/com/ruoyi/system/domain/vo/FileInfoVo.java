package com.ruoyi.system.domain.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 文件信息 VO
 *
 * @author ruoyi
 */
public class FileInfoVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 文件名称 */
    private String name;

    /** 文件路径 */
    private String path;

    /** 是否为目录 */
    private boolean directory;

    /** 文件大小（字节） */
    private Long size;

    /** 最后修改时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastModified;

    /** 文件扩展名 */
    private String extension;

    /** 是否可读 */
    private boolean readable;

    /** 是否可写 */
    private boolean writable;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public boolean isDirectory()
    {
        return directory;
    }

    public void setDirectory(boolean directory)
    {
        this.directory = directory;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
        this.lastModified = lastModified;
    }

    public String getExtension()
    {
        return extension;
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    public boolean isReadable()
    {
        return readable;
    }

    public void setReadable(boolean readable)
    {
        this.readable = readable;
    }

    public boolean isWritable()
    {
        return writable;
    }

    public void setWritable(boolean writable)
    {
        this.writable = writable;
    }

    /**
     * 获取格式化后的文件大小
     */
    public String getFormattedSize()
    {
        if (size == null || size < 0)
        {
            return "-";
        }
        if (size < 1024)
        {
            return size + " B";
        }
        else if (size < 1024 * 1024)
        {
            return String.format("%.2f KB", size / 1024.0);
        }
        else if (size < 1024 * 1024 * 1024)
        {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
        else
        {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
