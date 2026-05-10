package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.StuStudent;
import com.ruoyi.system.service.IStuStudentService;

/**
 * 学生信息Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/student")
public class StuStudentController extends BaseController
{
    @Autowired
    private IStuStudentService stuStudentService;

    /**
     * 查询学生列表
     */
    @PreAuthorize("@ss.hasPermi('student:list')")
    @GetMapping("/list")
    public TableDataInfo list(StuStudent stuStudent)
    {
        startPage();
        List<StuStudent> list = stuStudentService.selectStuStudentList(stuStudent);
        return getDataTable(list);
    }

    /**
     * 导出学生列表
     */
    @PreAuthorize("@ss.hasPermi('student:list:export')")
    @Log(title = "学生管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StuStudent stuStudent)
    {
        List<StuStudent> list = stuStudentService.selectStuStudentList(stuStudent);
        ExcelUtil<StuStudent> util = new ExcelUtil<StuStudent>(StuStudent.class);
        util.exportExcel(response, list, "学生数据");
    }

    /**
     * 获取学生详细信息
     */
    @PreAuthorize("@ss.hasPermi('student:list:query')")
    @GetMapping("/{studentId}")
    public AjaxResult getInfo(@PathVariable("studentId") Long studentId)
    {
        return success(stuStudentService.selectStuStudentById(studentId));
    }

    /**
     * 根据登录账号查询学生
     */
    @PreAuthorize("@ss.hasPermi('student:list:query')")
    @GetMapping("/account/{account}")
    public AjaxResult getInfoByAccount(@PathVariable("account") String account)
    {
        return success(stuStudentService.selectStuStudentByAccount(account));
    }

    /**
     * 新增学生
     */
    @PreAuthorize("@ss.hasPermi('student:list:add')")
    @Log(title = "学生管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody StuStudent stuStudent)
    {
        return toAjax(stuStudentService.insertStuStudent(stuStudent));
    }

    /**
     * 修改学生
     */
    @PreAuthorize("@ss.hasPermi('student:list:edit')")
    @Log(title = "学生管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody StuStudent stuStudent)
    {
        return toAjax(stuStudentService.updateStuStudent(stuStudent));
    }

    /**
     * 批量新增学生（按年级、班级、人数自动生成账号）
     */
    @PreAuthorize("@ss.hasPermi('student:list:add')")
    @Log(title = "学生管理", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody Map<String, Object> params)
    {
        Integer grade = (Integer) params.get("grade");
        Integer classNum = (Integer) params.get("classNum");
        Integer count = (Integer) params.get("count");
        if (count == null || count <= 0)
        {
            return error("人数必须大于0");
        }
        int successCount = stuStudentService.batchInsertStudents(grade, classNum, count);
        return success("成功创建 " + successCount + " 个学生账号");
    }

    /**
     * 删除学生
     */
    @PreAuthorize("@ss.hasPermi('student:list:remove')")
    @Log(title = "学生管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{studentIds}")
    public AjaxResult remove(@PathVariable Long[] studentIds)
    {
        return toAjax(stuStudentService.deleteStuStudentByIds(studentIds));
    }
}
