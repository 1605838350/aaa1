package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.StuHomework;
import com.ruoyi.system.domain.StuHomeworkSubmit;
import com.ruoyi.system.service.IStuHomeworkService;

/**
 * 作业管理Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/homework")
public class StuHomeworkController extends BaseController
{
    @Autowired
    private IStuHomeworkService stuHomeworkService;

    /**
     * 查询作业任务列表
     */
    @PreAuthorize("@ss.hasPermi('homework:list:list')")
    @GetMapping("/list")
    public TableDataInfo list(StuHomework stuHomework)
    {
        startPage();
        List<StuHomework> list = stuHomeworkService.selectStuHomeworkList(stuHomework);
        return getDataTable(list);
    }

    /**
     * 获取作业任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('homework:list:query')")
    @GetMapping("/{homeworkId}")
    public AjaxResult getInfo(@PathVariable("homeworkId") Long homeworkId)
    {
        return success(stuHomeworkService.selectStuHomeworkById(homeworkId));
    }

    /**
     * 新增作业任务
     */
    @PreAuthorize("@ss.hasPermi('homework:list:add')")
    @Log(title = "作业管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody StuHomework stuHomework)
    {
        return toAjax(stuHomeworkService.insertStuHomework(stuHomework));
    }

    /**
     * 修改作业任务
     */
    @PreAuthorize("@ss.hasPermi('homework:list:edit')")
    @Log(title = "作业管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody StuHomework stuHomework)
    {
        return toAjax(stuHomeworkService.updateStuHomework(stuHomework));
    }

    /**
     * 删除作业任务
     */
    @PreAuthorize("@ss.hasPermi('homework:list:remove')")
    @Log(title = "作业管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{homeworkIds}")
    public AjaxResult remove(@PathVariable Long[] homeworkIds)
    {
        return toAjax(stuHomeworkService.deleteStuHomeworkByIds(homeworkIds));
    }

    /**
     * 查询作业提交列表（详情页用）
     */
    @PreAuthorize("@ss.hasPermi('homework:list:detail')")
    @GetMapping("/submit/list")
    public TableDataInfo submitList(StuHomeworkSubmit stuHomeworkSubmit)
    {
        startPage();
        List<StuHomeworkSubmit> list = stuHomeworkService.selectStuHomeworkSubmitList(stuHomeworkSubmit);
        return getDataTable(list);
    }

    /**
     * 批改作业
     */
    @PreAuthorize("@ss.hasPermi('homework:list:grade')")
    @Log(title = "作业管理", businessType = BusinessType.UPDATE)
    @PutMapping("/submit")
    public AjaxResult grade(@RequestBody StuHomeworkSubmit stuHomeworkSubmit)
    {
        return toAjax(stuHomeworkService.gradeStuHomeworkSubmit(stuHomeworkSubmit));
    }

    /**
     * 删除作业提交
     */
    @PreAuthorize("@ss.hasPermi('homework:list:remove')")
    @Log(title = "作业管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/submit/{submitId}")
    public AjaxResult deleteSubmit(@PathVariable Long submitId)
    {
        return toAjax(stuHomeworkService.deleteHomeworkSubmit(submitId));
    }
}
