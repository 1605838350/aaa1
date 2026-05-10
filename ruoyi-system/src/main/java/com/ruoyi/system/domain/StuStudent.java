package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学生信息表 stu_student（独立于sys_user）
 *
 * @author ruoyi
 */
public class StuStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学生ID */
    @Excel(name = "学生ID", cellType = ColumnType.NUMERIC)
    private Long studentId;

    /** 入学年份 */
    @Excel(name = "入学年份")
    private Integer admissionYear;

    /** 班级（1-6） */
    @Excel(name = "班级")
    private Integer classNum;

    /** 学号（1-99） */
    @Excel(name = "学号")
    private Integer studentNum;

    /** 登录账号 */
    @Excel(name = "登录账号")
    private String account;

    /** 密码 */
    private String password;

    /** 学生姓名 */
    @Excel(name = "学生姓名")
    private String studentName;

    /** 性别 */
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginDate;

    /** 登录令牌 */
    private String loginToken;

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Integer getAdmissionYear()
    {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear)
    {
        this.admissionYear = admissionYear;
    }

    /**
     * 根据入学年份动态计算当前年级
     * 规则：9月入学，年级 = 当前年份 - 入学年份
     */
    public Integer getGrade()
    {
        if (admissionYear == null)
        {
            return null;
        }
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        return currentYear - admissionYear;
    }

    public Integer getClassNum()
    {
        return classNum;
    }

    public void setClassNum(Integer classNum)
    {
        this.classNum = classNum;
    }

    public Integer getStudentNum()
    {
        return studentNum;
    }

    public void setStudentNum(Integer studentNum)
    {
        this.studentNum = studentNum;
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
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

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public String getLoginToken()
    {
        return loginToken;
    }

    public void setLoginToken(String loginToken)
    {
        this.loginToken = loginToken;
    }

    /**
     * 根据入学年份、班级、学号生成登录账号
     * 格式：入学年份(4位) + 班级(2位) + 学号(2位)
     * 例如：2024年入学1班1号 → 20240101
     */
    public static String generateAccount(Integer admissionYear, Integer classNum, Integer studentNum)
    {
        String classStr = String.format("%02d", classNum);
        String studentStr = String.format("%02d", studentNum);
        return admissionYear + classStr + studentStr;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("studentId", getStudentId())
            .append("admissionYear", getAdmissionYear())
            .append("grade", getGrade())
            .append("classNum", getClassNum())
            .append("studentNum", getStudentNum())
            .append("account", getAccount())
            .append("studentName", getStudentName())
            .append("sex", getSex())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("loginIp", getLoginIp())
            .append("loginDate", getLoginDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
