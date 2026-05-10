package com.ruoyi.web.controller.student;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.system.domain.StuHomework;
import com.ruoyi.system.domain.StuHomeworkSubmit;
import com.ruoyi.system.domain.StuStudent;
import com.ruoyi.system.service.IStuHomeworkService;
import com.ruoyi.system.service.IStuStudentService;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.web.service.FullTextSearchService;
/**
 * 学生登录接口（独立于教师/管理员的若依认证体系）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/student")
public class StudentLoginController extends BaseController
{
    @Autowired
    private IStuStudentService stuStudentService;

    @Autowired
    private IStuHomeworkService stuHomeworkService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FullTextSearchService fullTextSearchService;

    /**
     * 学生登录
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody StuStudent student)
    {
        if (student.getAccount() == null || student.getPassword() == null)
        {
            return error("账号或密码不能为空");
        }

        // 查询学生
        StuStudent stu = stuStudentService.selectStuStudentByAccount(student.getAccount());
        if (stu == null)
        {
            return error("账号不存在");
        }

        // 校验状态
        if ("1".equals(stu.getStatus()))
        {
            return error("账号已停用");
        }

        // 校验密码
        if (!SecurityUtils.matchesPassword(student.getPassword(), stu.getPassword()))
        {
            return error("密码错误");
        }

        // 生成登录令牌
        String token = UUID.randomUUID().toString().replace("-", "");

        // 更新登录信息
        StuStudent update = new StuStudent();
        update.setStudentId(stu.getStudentId());
        update.setLoginIp(ServletUtils.getRequest().getRemoteAddr());
        update.setLoginToken(token);
        stuStudentService.updateStuStudent(update);

        // 返回学生信息（脱敏，不返回密码）
        stu.setPassword(null);
        stu.setLoginToken(token);
        AjaxResult ajax = success("登录成功");
        ajax.put("student", stu);
        ajax.put("token", token);
        return ajax;
    }

    /**
     * 学生查询已提交的所有文件（我的云盘）
     */
    @GetMapping("/files")
    public AjaxResult listFiles(@RequestParam("studentId") Long studentId,
                                @RequestHeader(value = "X-Student-Token", required = false) String token)
    {
        if (!verifyStudentToken(studentId, token))
        {
            return error("身份验证失败，请重新登录");
        }
        StuHomeworkSubmit query = new StuHomeworkSubmit();
        query.setStudentId(studentId);
        List<StuHomeworkSubmit> list = stuHomeworkService.selectStuHomeworkSubmitList(query);
        return success(list);
    }

    /**
     * 学生查看班级共享文件（同班同学提交的所有作业）
     */
    @GetMapping("/class-share")
    public AjaxResult classShare(@RequestParam("studentId") Long studentId,
                                  @RequestHeader(value = "X-Student-Token", required = false) String token)
    {
        if (!verifyStudentToken(studentId, token))
        {
            return error("身份验证失败，请重新登录");
        }
        StuStudent stu = stuStudentService.selectStuStudentById(studentId);
        if (stu == null || stu.getAdmissionYear() == null || stu.getClassNum() == null)
        {
            return error("班级信息不完整");
        }
        StuHomeworkSubmit query = new StuHomeworkSubmit();
        query.setAdmissionYear(stu.getAdmissionYear());
        query.setClassNum(stu.getClassNum());
        List<StuHomeworkSubmit> list = stuHomeworkService.selectStuHomeworkSubmitList(query);
        return success(list);
    }

    /**
     * 学生修改个人信息（姓名、性别）
     */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody StuStudent student,
                                     @RequestHeader(value = "X-Student-Token", required = false) String token)
    {
        if (!verifyStudentToken(student.getStudentId(), token))
        {
            return error("身份验证失败，请重新登录");
        }
        if (student.getStudentId() == null)
        {
            return error("学生ID不能为空");
        }
        if (StringUtils.isEmpty(student.getStudentName()))
        {
            return error("姓名不能为空");
        }
        if (StringUtils.isEmpty(student.getSex()))
        {
            return error("性别不能为空");
        }

        StuStudent update = new StuStudent();
        update.setStudentId(student.getStudentId());
        update.setStudentName(student.getStudentName());
        update.setSex(student.getSex());
        update.setUpdateBy("student");

        stuStudentService.updateStuStudent(update);
        return success("修改成功");
    }

    /**
     * 学生查询作业列表
     */
    @GetMapping("/homework/list")
    public AjaxResult homeworkList(@RequestParam("admissionYear") Integer admissionYear,
                                    @RequestParam("classNum") Integer classNum,
                                    @RequestParam("studentId") Long studentId,
                                    @RequestHeader(value = "X-Student-Token", required = false) String token)
    {
        if (!verifyStudentToken(studentId, token))
        {
            return error("身份验证失败，请重新登录");
        }
        if (admissionYear == null || classNum == null || studentId == null)
        {
            return error("参数不能为空");
        }
        List<StuHomework> list = stuHomeworkService.selectStudentHomeworkList(admissionYear, classNum, studentId);
        return success(list);
    }

    /**
     * 学生提交作业
     */
    @PostMapping("/homework/submit")
    public AjaxResult submitHomework(@RequestBody StuHomeworkSubmit submit,
                                      @RequestHeader(value = "X-Student-Token", required = false) String token)
    {
        if (!verifyStudentToken(submit.getStudentId(), token))
        {
            return error("身份验证失败，请重新登录");
        }
        if (submit.getHomeworkId() == null || submit.getStudentId() == null)
        {
            return error("作业ID和学生ID不能为空");
        }
        if (StringUtils.isEmpty(submit.getFileName()))
        {
            return error("文件名不能为空");
        }

        // 重新提交时，从全文索引中清理旧文件
        try {
            StuHomeworkSubmit query = new StuHomeworkSubmit();
            query.setStudentId(submit.getStudentId());
            query.setHomeworkId(submit.getHomeworkId());
            List<StuHomeworkSubmit> existingList = stuHomeworkService.selectStuHomeworkSubmitList(query);
            if (existingList != null && !existingList.isEmpty()) {
                StuHomeworkSubmit existing = existingList.get(0);
                if (existing.getFileName() != null) {
                    StuHomework homework = stuHomeworkService.selectStuHomeworkById(submit.getHomeworkId());
                    if (homework != null && homework.getStoragePath() != null) {
                        String oldPath = homework.getStoragePath();
                        if (!oldPath.endsWith("/")) oldPath += "/";
                        oldPath += existing.getFileName();
                        fullTextSearchService.removeFile(oldPath);
                    }
                }
            }
        } catch (Exception e) {
            // 索引清理失败不影响提交
        }

        int result = stuHomeworkService.submitHomework(submit);
        return toAjax(result);
    }

    /**
     * 学生上传文件（独立于教师权限体系）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file,
                                  @RequestParam("path") String path,
                                  @RequestParam("studentId") Long studentId,
                                  @RequestHeader(value = "X-Student-Token", required = false) String token) throws Exception
    {
        if (!verifyStudentToken(studentId, token))
        {
            return error("身份验证失败，请重新登录");
        }
        if (file.isEmpty())
        {
            return error("上传文件不能为空");
        }
        if (StringUtils.isEmpty(path))
        {
            return error("上传路径不能为空");
        }

        // 解码前端 encodeURIComponent 编码的中文路径
        path = java.net.URLDecoder.decode(path, "UTF-8");

        String subPath;
        // 给文件名加时间+学号前缀，防止重名
        String originalName = file.getOriginalFilename();
        String timePrefix = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date());
        String prefixedName = originalName;
        // 查询学号
        String account = getStudentAccount(studentId);
        if (StringUtils.isNotEmpty(account))
        {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > 0)
            {
                prefixedName = timePrefix + "_" + account + "_" + originalName.substring(0, dotIndex) + originalName.substring(dotIndex);
            }
            else
            {
                prefixedName = timePrefix + "_" + account + "_" + originalName;
            }
        }
        if (path.endsWith("/"))
        {
            subPath = path + prefixedName;
        }
        else
        {
            subPath = path + "/" + prefixedName;
        }

        InputStream contentStream = file.getInputStream();
        String fullPath = fileStorageService.upload(file, subPath);

        fullTextSearchService.indexFileAsync(fullPath, prefixedName, contentStream);

        // 提交作业记录时需要知道实际文件名
        return AjaxResult.success("上传成功", prefixedName);
    }

    /**
     * 学生预览文件（返回文件流，用于在线预览）
     */
    @GetMapping("/preview")
    public void previewFile(@RequestParam("path") String path,
                            @RequestParam("studentId") Long studentId,
                            @RequestParam(value = "token", required = false) String token,
                            HttpServletResponse response) throws Exception
    {
        if (!verifyStudentToken(studentId, token))
        {
            response.setStatus(401);
            response.getWriter().write("身份验证失败");
            return;
        }
        if (StringUtils.isEmpty(path))
        {
            response.setStatus(400);
            response.getWriter().write("文件路径不能为空");
            return;
        }

        String fileName = FileUtils.getName(path);
        String storageType = fileStorageService.getStorageType();

        if ("local".equals(storageType))
        {
            File file = new File(fileStorageService.getFullPath(path));
            if (!file.exists())
            {
                response.setStatus(404);
                response.getWriter().write("文件不存在");
                return;
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
            response.setHeader("Content-Length", "" + file.length());
            try (FileInputStream fis = new FileInputStream(file))
            {
                IOUtils.copy(fis, response.getOutputStream());
            }
        }
        else
        {
            try (InputStream is = fileStorageService.download(path))
            {
                response.reset();
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.setContentType("application/octet-stream; charset=UTF-8");
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                IOUtils.copy(is, response.getOutputStream());
            }
        }
    }

    /**
     * 学生下载文件（独立于教师权限体系）
     */
    @GetMapping("/download")
    public void downloadFile(@RequestParam("path") String path,
                             @RequestParam("studentId") Long studentId,
                             @RequestParam(value = "token", required = false) String token,
                             HttpServletResponse response) throws Exception
    {
        if (!verifyStudentToken(studentId, token))
        {
            response.setStatus(401);
            response.getWriter().write("身份验证失败");
            return;
        }
        if (StringUtils.isEmpty(path))
        {
            response.setStatus(400);
            response.getWriter().write("文件路径不能为空");
            return;
        }

        // 兼容绝对路径
        String baseDir = fileStorageService.getBaseDir();
        if (baseDir != null && path.contains(":"))
        {
            String normalized = path.replace('\\', '/');
            String normalizedBase = baseDir.replace('\\', '/');
            if (normalized.startsWith(normalizedBase))
            {
                path = normalized.substring(normalizedBase.length());
                while (path.startsWith("/"))
                {
                    path = path.substring(1);
                }
                path = "/" + path;
            }
        }

        String fileName = FileUtils.getName(path);
        String storageType = fileStorageService.getStorageType();

        if ("local".equals(storageType))
        {
            File file = new File(fileStorageService.getFullPath(path));
            if (!file.exists())
            {
                response.setStatus(404);
                response.getWriter().write("文件不存在");
                return;
            }
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");
            try (FileInputStream fis = new FileInputStream(file))
            {
                IOUtils.copy(fis, response.getOutputStream());
            }
        }
        else
        {
            try (InputStream is = fileStorageService.download(path))
            {
                response.reset();
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                response.setContentType("application/octet-stream; charset=UTF-8");
                IOUtils.copy(is, response.getOutputStream());
            }
        }
    }

    /**
     * 获取学号
     */
    private String getStudentAccount(Long studentId)
    {
        StuStudent student = stuStudentService.selectStuStudentById(studentId);
        return student != null ? student.getAccount() : null;
    }

    /**
     * 验证学生Token
     */
    private boolean verifyStudentToken(Long studentId, String token)
    {
        if (studentId == null || StringUtils.isEmpty(token))
        {
            return false;
        }
        StuStudent stu = stuStudentService.selectStuStudentById(studentId);
        return stu != null && token.equals(stu.getLoginToken());
    }

}
