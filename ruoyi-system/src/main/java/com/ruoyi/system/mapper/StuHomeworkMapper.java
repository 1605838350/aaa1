package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.StuHomework;

/**
 * 作业任务Mapper接口
 *
 * @author ruoyi
 */
public interface StuHomeworkMapper
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
     * @param homeworkIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteStuHomeworkByIds(Long[] homeworkIds);
}
