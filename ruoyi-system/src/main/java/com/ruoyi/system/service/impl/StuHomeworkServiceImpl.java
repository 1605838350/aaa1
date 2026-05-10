package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.StuHomework;
import com.ruoyi.system.domain.StuHomeworkSubmit;
import com.ruoyi.system.mapper.StuHomeworkMapper;
import com.ruoyi.system.mapper.StuHomeworkSubmitMapper;
import com.ruoyi.system.service.IStuHomeworkService;

/**
 * 作业管理 服务层实现
 *
 * @author ruoyi
 */
@Service
public class StuHomeworkServiceImpl implements IStuHomeworkService
{
    @Autowired
    private StuHomeworkMapper stuHomeworkMapper;

    @Autowired
    private StuHomeworkSubmitMapper stuHomeworkSubmitMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public StuHomework selectStuHomeworkById(Long homeworkId)
    {
        return stuHomeworkMapper.selectStuHomeworkById(homeworkId);
    }

    @Override
    public List<StuHomework> selectStuHomeworkList(StuHomework stuHomework)
    {
        return stuHomeworkMapper.selectStuHomeworkList(stuHomework);
    }

    @Override
    public int insertStuHomework(StuHomework stuHomework)
    {
        stuHomework.setCreateBy(SecurityUtils.getUsername());
        return stuHomeworkMapper.insertStuHomework(stuHomework);
    }

    @Override
    public int updateStuHomework(StuHomework stuHomework)
    {
        stuHomework.setUpdateBy(SecurityUtils.getUsername());

        // 检测存储路径是否变更，如果变了需要移动已提交的文件
        if (stuHomework.getHomeworkId() != null && stuHomework.getStoragePath() != null)
        {
            StuHomework oldHomework = stuHomeworkMapper.selectStuHomeworkById(stuHomework.getHomeworkId());
            if (oldHomework != null && oldHomework.getStoragePath() != null
                && !oldHomework.getStoragePath().equals(stuHomework.getStoragePath()))
            {
                moveSubmitFiles(oldHomework.getStoragePath(), stuHomework.getStoragePath(), stuHomework.getHomeworkId());
            }
        }

        return stuHomeworkMapper.updateStuHomework(stuHomework);
    }

    /**
     * 移动作业已提交的文件到新目录
     */
    private void moveSubmitFiles(String oldPath, String newPath, Long homeworkId)
    {
        // 查询该作业所有已提交的记录
        StuHomeworkSubmit query = new StuHomeworkSubmit();
        query.setHomeworkId(homeworkId);
        List<StuHomeworkSubmit> submits = stuHomeworkSubmitMapper.selectStuHomeworkSubmitList(query);
        if (submits == null || submits.isEmpty())
        {
            return;
        }

        // 确保新目录存在
        try
        {
            fileStorageService.mkdir(newPath);
        }
        catch (Exception e)
        {
            // 忽略，目录可能已存在
        }

        for (StuHomeworkSubmit submit : submits)
        {
            if (submit.getFileName() == null)
            {
                continue;
            }
            try
            {
                String oldFilePath = oldPath;
                if (!oldFilePath.endsWith("/"))
                {
                    oldFilePath = oldFilePath + "/";
                }
                oldFilePath = oldFilePath + submit.getFileName();

                String newFilePath = newPath;
                if (!newFilePath.endsWith("/"))
                {
                    newFilePath = newFilePath + "/";
                }
                newFilePath = newFilePath + submit.getFileName();

                // 检查旧文件是否存在，存在则移动
                if (fileStorageService.exists(oldFilePath))
                {
                    if (fileStorageService.rename(oldFilePath, newFilePath))
                    {
                        // 更新数据库中的文件路径
                        submit.setFilePath(newFilePath);
                        stuHomeworkSubmitMapper.updateStuHomeworkSubmit(submit);
                    }
                }
            }
            catch (Exception e)
            {
                // 单个文件移动失败不影响其他文件
            }
        }
    }

    @Override
    public int deleteStuHomeworkById(Long homeworkId)
    {
        return stuHomeworkMapper.deleteStuHomeworkById(homeworkId);
    }

    @Override
    public int deleteStuHomeworkByIds(Long[] homeworkIds)
    {
        return stuHomeworkMapper.deleteStuHomeworkByIds(homeworkIds);
    }

    @Override
    public List<StuHomeworkSubmit> selectStuHomeworkSubmitList(StuHomeworkSubmit stuHomeworkSubmit)
    {
        // 按作业ID查询时，返回全班所有学生（含未提交），前端可筛选
        if (stuHomeworkSubmit.getHomeworkId() != null)
        {
            if (stuHomeworkSubmit.getAdmissionYear() == null || stuHomeworkSubmit.getClassNum() == null)
            {
                StuHomework hw = stuHomeworkMapper.selectStuHomeworkById(stuHomeworkSubmit.getHomeworkId());
                if (hw != null)
                {
                    stuHomeworkSubmit.setAdmissionYear(hw.getAdmissionYear());
                    stuHomeworkSubmit.setClassNum(hw.getClassNum());
                }
            }
            return stuHomeworkSubmitMapper.selectHomeworkSubmitWithAllStudents(stuHomeworkSubmit);
        }
        return stuHomeworkSubmitMapper.selectStuHomeworkSubmitList(stuHomeworkSubmit);
    }

    @Override
    public int gradeStuHomeworkSubmit(StuHomeworkSubmit stuHomeworkSubmit)
    {
        stuHomeworkSubmit.setStatus("1");
        return stuHomeworkSubmitMapper.updateStuHomeworkSubmit(stuHomeworkSubmit);
    }

    @Override
    public List<StuHomework> selectStudentHomeworkList(Integer admissionYear, Integer classNum, Long studentId)
    {
        // 查询该班级的作业列表
        StuHomework query = new StuHomework();
        query.setAdmissionYear(admissionYear);
        query.setClassNum(classNum);
        query.setStatus("0");
        List<StuHomework> homeworkList = stuHomeworkMapper.selectStuHomeworkList(query);
        // 查询每个作业的提交状态
        for (StuHomework homework : homeworkList)
        {
            StuHomeworkSubmit submit = stuHomeworkSubmitMapper.selectByStudentAndHomework(studentId, homework.getHomeworkId());
            if (submit != null)
            {
                homework.setSubmitId(submit.getSubmitId());
                homework.setSubmitFileName(submit.getFileName());
                homework.setSubmitFilePath(submit.getFilePath());
                homework.setSubmitTime(submit.getSubmitTime());
                homework.setSubmitScore(submit.getScore());
                homework.setSubmitComment(submit.getComment());
                homework.setSubmitStatus(submit.getStatus());
            }
        }
        return homeworkList;
    }

    @Override
    public int submitHomework(StuHomeworkSubmit stuHomeworkSubmit)
    {
        // 查询是否已提交
        StuHomeworkSubmit existing = stuHomeworkSubmitMapper.selectByStudentAndHomework(
                stuHomeworkSubmit.getStudentId(), stuHomeworkSubmit.getHomeworkId());
        if (existing != null)
        {
            // 删除旧文件
            try
            {
                StuHomework homework = stuHomeworkMapper.selectStuHomeworkById(stuHomeworkSubmit.getHomeworkId());
                if (homework != null && existing.getFileName() != null)
                {
                    String oldFilePath = homework.getStoragePath();
                    if (!oldFilePath.endsWith("/"))
                    {
                        oldFilePath = oldFilePath + "/";
                    }
                    oldFilePath = oldFilePath + existing.getFileName();
                    fileStorageService.delete(oldFilePath);
                }
            }
            catch (Exception e)
            {
                // 旧文件删除失败不影响重新提交
            }
            // 更新提交：只更新 fileName，重置批改状态
            existing.setFileName(stuHomeworkSubmit.getFileName());
            existing.setSubmitTime(new java.util.Date());
            existing.setStatus("0");
            existing.setScore(null);
            existing.setComment(null);
            return stuHomeworkSubmitMapper.updateStuHomeworkSubmit(existing);
        }
        else
        {
            // 新增提交
            stuHomeworkSubmit.setSubmitTime(new java.util.Date());
            stuHomeworkSubmit.setStatus("0");
            return stuHomeworkSubmitMapper.insertStuHomeworkSubmit(stuHomeworkSubmit);
        }
    }

    @Override
    public int deleteHomeworkSubmit(Long submitId)
    {
        StuHomeworkSubmit query = new StuHomeworkSubmit();
        query.setSubmitId(submitId);
        List<StuHomeworkSubmit> list = stuHomeworkSubmitMapper.selectStuHomeworkSubmitList(query);
        if (list != null && !list.isEmpty())
        {
            StuHomeworkSubmit submit = list.get(0);
            if (submit.getFileName() != null)
            {
                // 删除文件
                try
                {
                    StuHomework homework = stuHomeworkMapper.selectStuHomeworkById(submit.getHomeworkId());
                    if (homework != null)
                    {
                        String filePath = homework.getStoragePath();
                        if (!filePath.endsWith("/"))
                        {
                            filePath = filePath + "/";
                        }
                        filePath = filePath + submit.getFileName();
                        fileStorageService.delete(filePath);
                    }
                }
                catch (Exception e)
                {
                    // 文件删除失败不影响记录删除
                }
            }
        }
        return stuHomeworkSubmitMapper.deleteStuHomeworkSubmitById(submitId);
    }
}
