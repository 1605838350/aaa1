package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业任务对象 stu_homework
 *
 * @author ruoyi
 */
public class StuHomework extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 作业ID */
    private Long homeworkId;

    /** 作业标题 */
    @Excel(name = "作业标题")
    private String title;

    /** 作业内容 */
    private String content;

    /** 截止时间 */
    @Excel(name = "截止时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 目标入学年份 */
    private Integer admissionYear;

    /** 目标班级 */
    private Integer classNum;

    /** 存储路径 */
    private String storagePath;

    /** 上传文件大小限制（MB） */
    private Integer maxFileSize;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志 */
    private String delFlag;

    /** 年级（动态计算） */
    private Integer grade;

    /** 提交ID（非数据库字段） */
    private Long submitId;

    /** 提交文件名（非数据库字段） */
    private String submitFileName;

    /** 提交文件路径（非数据库字段） */
    private String submitFilePath;

    /** 提交时间（非数据库字段） */
    private Date submitTime;

    /** 提交分数（非数据库字段） */
    private Integer submitScore;

    /** 提交评语（非数据库字段） */
    private String submitComment;

    /** 提交状态（非数据库字段） */
    private String submitStatus;

    /** 已提交人数（非数据库字段） */
    private Integer submitCount;

    /** 已批改人数（非数据库字段） */
    private Integer gradedCount;

    public Long getHomeworkId()
    {
        return homeworkId;
    }

    public void setHomeworkId(Long homeworkId)
    {
        this.homeworkId = homeworkId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getDeadline()
    {
        return deadline;
    }

    public void setDeadline(Date deadline)
    {
        this.deadline = deadline;
    }

    public Integer getAdmissionYear()
    {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear)
    {
        this.admissionYear = admissionYear;
    }

    public Integer getClassNum()
    {
        return classNum;
    }

    public void setClassNum(Integer classNum)
    {
        this.classNum = classNum;
    }

    public String getStoragePath()
    {
        return storagePath;
    }

    public void setStoragePath(String storagePath)
    {
        this.storagePath = storagePath;
    }

    public Integer getMaxFileSize()
    {
        return maxFileSize;
    }

    public void setMaxFileSize(Integer maxFileSize)
    {
        this.maxFileSize = maxFileSize;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    /**
     * 动态计算年级：当前年份 - 入学年份
     */
    public Integer getGrade()
    {
        if (admissionYear == null) return null;
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        return currentYear - admissionYear;
    }

    public void setSubmitId(Long submitId)
    {
        this.submitId = submitId;
    }

    public String getSubmitFileName()
    {
        return submitFileName;
    }

    public void setSubmitFileName(String submitFileName)
    {
        this.submitFileName = submitFileName;
    }

    public String getSubmitFilePath()
    {
        return submitFilePath;
    }

    public void setSubmitFilePath(String submitFilePath)
    {
        this.submitFilePath = submitFilePath;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public Integer getSubmitScore()
    {
        return submitScore;
    }

    public void setSubmitScore(Integer submitScore)
    {
        this.submitScore = submitScore;
    }

    public String getSubmitComment()
    {
        return submitComment;
    }

    public void setSubmitComment(String submitComment)
    {
        this.submitComment = submitComment;
    }

    public String getSubmitStatus()
    {
        return submitStatus;
    }

    public void setSubmitStatus(String submitStatus)
    {
        this.submitStatus = submitStatus;
    }

    public Integer getSubmitCount()
    {
        return submitCount;
    }

    public void setSubmitCount(Integer submitCount)
    {
        this.submitCount = submitCount;
    }

    public Integer getGradedCount()
    {
        return gradedCount;
    }

    public void setGradedCount(Integer gradedCount)
    {
        this.gradedCount = gradedCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("homeworkId", getHomeworkId())
            .append("title", getTitle())
            .append("content", getContent())
            .append("deadline", getDeadline())
            .append("admissionYear", getAdmissionYear())
            .append("classNum", getClassNum())
            .append("storagePath", getStoragePath())
            .append("maxFileSize", getMaxFileSize())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
