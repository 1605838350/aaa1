package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.StuStudent;

/**
 * 学生信息 服务层（独立于sys_user）
 *
 * @author ruoyi
 */
public interface IStuStudentService
{
    /**
     * 查询学生信息
     *
     * @param studentId 学生ID
     * @return 学生信息
     */
    public StuStudent selectStuStudentById(Long studentId);

    /**
     * 通过登录账号查询学生
     *
     * @param account 登录账号
     * @return 学生信息
     */
    public StuStudent selectStuStudentByAccount(String account);

    /**
     * 查询学生列表
     *
     * @param stuStudent 学生信息
     * @return 学生信息集合
     */
    public List<StuStudent> selectStuStudentList(StuStudent stuStudent);

    /**
     * 新增学生
     *
     * @param stuStudent 学生信息
     * @return 结果
     */
    public int insertStuStudent(StuStudent stuStudent);

    /**
     * 修改学生
     *
     * @param stuStudent 学生信息
     * @return 结果
     */
    public int updateStuStudent(StuStudent stuStudent);

    /**
     * 删除学生
     *
     * @param studentId 学生ID
     * @return 结果
     */
    public int deleteStuStudentById(Long studentId);

    /**
     * 批量删除学生
     *
     * @param studentIds 需要删除的学生ID
     * @return 结果
     */
    public int deleteStuStudentByIds(Long[] studentIds);

    /**
     * 批量新增学生（按年级、班级、人数自动生成账号）
     *
     * @param grade 年级（1-6）
     * @param classNum 班级（1-6）
     * @param count 人数
     * @return 成功创建的数量
     */
    public int batchInsertStudents(Integer grade, Integer classNum, int count);

    /**
     * 校验账号是否唯一
     *
     * @param stuStudent 学生信息
     * @return 结果
     */
    public boolean checkAccountUnique(StuStudent stuStudent);
}
