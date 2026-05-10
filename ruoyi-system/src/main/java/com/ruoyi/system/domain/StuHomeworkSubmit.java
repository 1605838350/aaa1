package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业提交对象 stu_homework_submit
 *
 * @author ruoyi
 */
public class StuHomeworkSubmit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 提交ID */
    private Long submitId;

    /** 作业ID */
    private Long homeworkId;

    /** 学生ID */
    private Long studentId;

    /** 文件名 */
    private String fileName;

    /** 文件路径 */
    private String filePath;

    /** 提交时间 */
    private Date submitTime;

    /** 分数 */
    private Integer score;

    /** 评语 */
    private String comment;

    /** 状态（0已提交 1已批改） */
    private String status;

    /** 学生姓名（非数据库字段） */
    private String studentName;

    /** 学生账号（非数据库字段） */
    private String account;

    /** 作业标题（非数据库字段） */
    private String homeworkTitle;

    /** 入学年份（非数据库字段，用于班级查询） */
    private Integer admissionYear;

    /** 班级（非数据库字段，用于班级查询） */
    private Integer classNum;

    public Long getSubmitId()
    {
        return submitId;
    }

    public void setSubmitId(Long submitId)
    {
        this.submitId = submitId;
    }

    public Long getHomeworkId()
    {
        return homeworkId;
    }

    public void setHomeworkId(Long homeworkId)
    {
        this.homeworkId = homeworkId;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public Integer getScore()
    {
        return score;
    }

    public void setScore(Integer score)
    {
        this.score = score;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public String getHomeworkTitle()
    {
        return homeworkTitle;
    }

    public void setHomeworkTitle(String homeworkTitle)
    {
        this.homeworkTitle = homeworkTitle;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("submitId", getSubmitId())
            .append("homeworkId", getHomeworkId())
            .append("studentId", getStudentId())
            .append("fileName", getFileName())
            .append("filePath", getFilePath())
            .append("submitTime", getSubmitTime())
            .append("score", getScore())
            .append("comment", getComment())
            .append("status", getStatus())
            .toString();
    }
}