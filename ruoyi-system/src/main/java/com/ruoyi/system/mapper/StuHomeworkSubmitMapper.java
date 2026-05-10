package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.StuHomeworkSubmit;

/**
 * 作业提交Mapper接口
 *
 * @author ruoyi
 */
public interface StuHomeworkSubmitMapper
{
    /**
     * 查询作业提交列表（关联学生信息）
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 作业提交集合
     */
    public List<StuHomeworkSubmit> selectStuHomeworkSubmitList(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 新增作业提交
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 结果
     */
    public int insertStuHomeworkSubmit(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 修改作业提交（批改）
     *
     * @param stuHomeworkSubmit 作业提交
     * @return 结果
     */
    public int updateStuHomeworkSubmit(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 根据学生ID和作业ID查询提交记录
     */
    public StuHomeworkSubmit selectByStudentAndHomework(@Param("studentId") Long studentId, @Param("homeworkId") Long homeworkId);
    
    /**
     * 查询某作业下全班学生的提交情况（含未提交学生）
     */
    public List<StuHomeworkSubmit> selectHomeworkSubmitWithAllStudents(StuHomeworkSubmit stuHomeworkSubmit);

    /**
     * 删除作业提交
     *
     * @param submitId 提交ID
     * @return 结果
     */
    public int deleteStuHomeworkSubmitById(Long submitId);

    /**
     * 批量删除作业提交
     *
     * @param submitIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteStuHomeworkSubmitByIds(Long[] submitIds);
}
