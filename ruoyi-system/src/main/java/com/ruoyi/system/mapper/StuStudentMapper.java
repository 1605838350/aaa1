package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.StuStudent;

/**
 * 学生信息 数据层
 *
 * @author ruoyi
 */
public interface StuStudentMapper
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
     * @param studentIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteStuStudentByIds(Long[] studentIds);

    /**
     * 校验账号是否唯一
     *
     * @param account 登录账号
     * @return 结果
     */
    public StuStudent checkAccountUnique(String account);
}
