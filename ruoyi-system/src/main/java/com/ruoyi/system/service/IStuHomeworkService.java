package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.StuHomework;
import com.ruoyi.system.domain.StuHomeworkSubmit;

/**
 * 作业管理 服务层
 *
 * @author ruoyi
 */
public interface IStuHomeworkService
{
    /**
     * 查询作业任务
     *
     * @param homeworkId 作业任务ID
     * @return 作业任务
     */
    public StuHomework selectStuHomeworkById(Long homeworkId);

    /**
     * 查询作业任务列表
     *
     * @param stuHomework 作业任务
     * @return 作业任务集合
     */
    public List<StuHomework> selectStuHomeworkList(StuHomework stuHomework);

    /**
     * 新增作业任务
     *
     * @param stuHomework 作业任务
     * @return 结果
     */
    public int insertStuHomework(StuHomework stuHomework);

    /**
     * 修改作业任务
     *
     * @param stuHomework 作业任务
     * @return 结果
     */
    public int updateStuHomework(StuHomework stuHomework);

    /**
     * 删除作业任务
     *
     * @param homeworkId 作业任务ID
     * @return 结果
     */
    public int deleteStuHomeworkById(Long homeworkId);

    /**
     * 批量删除作业任务
     *
     * @param homeworkIds 需要删除的作业任务ID
     * @return 结果
     */
    public int deleteStuHomeworkByIds(Long[] homeworkIds);

    /**
     * 查询作业提交列表（关联学生信息）
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 作业提交集合
     */
    public List<StuHomeworkSubmit> selectStuHomeworkSubmitList(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 批改作业
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 结果
     */
    public int gradeStuHomeworkSubmit(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 学生查询作业列表（含提交状态）
     *
     * @param admissionYear 入学年份
     * @param classNum 班级
     * @param studentId 学生ID
     * @return 作业任务集合
     */
    public List<StuHomework> selectStudentHomeworkList(Integer admissionYear, Integer classNum, Long studentId);

    /**
     * 学生提交作业
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 结果
     */
    public int submitHomework(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 删除作业提交（同时删除文件）
     *
     * @param submitId 提交ID
     * @return 结果
     */
    public int deleteHomeworkSubmit(Long submitId);
}
