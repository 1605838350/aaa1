package com.ruoyi.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.StuStudent;
import com.ruoyi.system.mapper.StuStudentMapper;
import com.ruoyi.system.service.IStuStudentService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 学生信息 服务层实现（独立于sys_user）
 *
 * @author ruoyi
 */
@Service
public class StuStudentServiceImpl implements IStuStudentService
{
    private static final Logger log = LoggerFactory.getLogger(StuStudentServiceImpl.class);

    @Autowired
    private StuStudentMapper stuStudentMapper;

    @Autowired
    private ISysConfigService configService;

    /**
     * 查询学生信息
     */
    @Override
    public StuStudent selectStuStudentById(Long studentId)
    {
        return stuStudentMapper.selectStuStudentById(studentId);
    }

    /**
     * 通过登录账号查询学生
     */
    @Override
    public StuStudent selectStuStudentByAccount(String account)
    {
        return stuStudentMapper.selectStuStudentByAccount(account);
    }

    /**
     * 查询学生列表
     */
    @Override
    public List<StuStudent> selectStuStudentList(StuStudent stuStudent)
    {
        return stuStudentMapper.selectStuStudentList(stuStudent);
    }

    /**
     * 新增学生
     */
    @Override
    public int insertStuStudent(StuStudent stuStudent)
    {
        // 1. 生成登录账号
        String account = StuStudent.generateAccount(stuStudent.getAdmissionYear(), stuStudent.getClassNum(), stuStudent.getStudentNum());
        stuStudent.setAccount(account);

        // 2. 校验账号唯一性
        if (!checkAccountUnique(stuStudent))
        {
            throw new ServiceException("新增学生'" + account + "'失败，账号已存在");
        }

        // 3. 设置默认密码（如果未提供）
        if (StringUtils.isEmpty(stuStudent.getPassword()))
        {
            String defaultPassword = configService.selectConfigByKey("sys.user.initPassword");
            stuStudent.setPassword(SecurityUtils.encryptPassword(StringUtils.isNotEmpty(defaultPassword) ? defaultPassword : "123456"));
        }
        else
        {
            stuStudent.setPassword(SecurityUtils.encryptPassword(stuStudent.getPassword()));
        }

        // 4. 初始化状态
        stuStudent.setDelFlag("0");
        stuStudent.setStatus("0");
        stuStudent.setCreateBy(SecurityUtils.getUsername());

        return stuStudentMapper.insertStuStudent(stuStudent);
    }

    /**
     * 修改学生
     */
    @Override
    public int updateStuStudent(StuStudent stuStudent)
    {
        // 如果入学年份、班级或学号有变，重新生成账号
        if (stuStudent.getAdmissionYear() != null && stuStudent.getClassNum() != null && stuStudent.getStudentNum() != null)
        {
            String newAccount = StuStudent.generateAccount(stuStudent.getAdmissionYear(), stuStudent.getClassNum(), stuStudent.getStudentNum());
            stuStudent.setAccount(newAccount);
        }

        // 如果修改了密码，重新加密
        if (StringUtils.isNotEmpty(stuStudent.getPassword()) && stuStudent.getPassword().length() < 60)
        {
            stuStudent.setPassword(SecurityUtils.encryptPassword(stuStudent.getPassword()));
        }
        else
        {
            stuStudent.setPassword(null); // 不更新密码字段
        }

        // 设置更新人：优先使用传入的updateBy，否则尝试SecurityUtils，兜底为system
        String updateBy = stuStudent.getUpdateBy();
        if (StringUtils.isEmpty(updateBy))
        {
            try
            {
                updateBy = SecurityUtils.getUsername();
            }
            catch (Exception e)
            {
                updateBy = "system";
            }
        }
        stuStudent.setUpdateBy(updateBy);
        return stuStudentMapper.updateStuStudent(stuStudent);
    }

    /**
     * 删除学生
     */
    @Override
    public int deleteStuStudentById(Long studentId)
    {
        return stuStudentMapper.deleteStuStudentById(studentId);
    }

    /**
     * 批量删除学生
     */
    @Override
    public int deleteStuStudentByIds(Long[] studentIds)
    {
        return stuStudentMapper.deleteStuStudentByIds(studentIds);
    }

    /**
     * 批量新增学生（按年级、班级、人数自动生成账号）
     */
    @Override
    public int batchInsertStudents(Integer grade, Integer classNum, int count)
    {
        if (grade == null || classNum == null || count <= 0 || count > 99)
        {
            throw new ServiceException("参数错误：年级、班级不能为空，人数需在1-99之间");
        }

        // 计算入学年份：当前年份 - 年级
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int admissionYear = currentYear - grade;

        // 统一密码
        String defaultPassword = SecurityUtils.encryptPassword("ck123456");

        // 获取当前操作用户
        String createBy;
        try
        {
            createBy = SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            createBy = "system";
        }

        int successCount = 0;
        for (int i = 1; i <= count; i++)
        {
            String account = StuStudent.generateAccount(admissionYear, classNum, i);

            // 检查账号是否已存在，已存在则跳过
            StuStudent existing = stuStudentMapper.selectStuStudentByAccount(account);
            if (existing != null)
            {
                continue;
            }

            StuStudent stu = new StuStudent();
            stu.setAdmissionYear(admissionYear);
            stu.setClassNum(classNum);
            stu.setStudentNum(i);
            stu.setAccount(account);
            stu.setPassword(defaultPassword);
            stu.setStudentName("");
            stu.setDelFlag("0");
            stu.setStatus("0");
            stu.setCreateBy(createBy);

            stuStudentMapper.insertStuStudent(stu);
            successCount++;
        }

        return successCount;
    }

    /**
     * 校验账号是否唯一
     */
    @Override
    public boolean checkAccountUnique(StuStudent stuStudent)
    {
        Long studentId = StringUtils.isNull(stuStudent.getStudentId()) ? -1L : stuStudent.getStudentId();
        StuStudent info = stuStudentMapper.checkAccountUnique(stuStudent.getAccount());
        if (StringUtils.isNotNull(info) && info.getStudentId().longValue() != studentId.longValue())
        {
            return false;
        }
        return true;
    }
}
