package com.ruoyi.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.SysFileDir;
import com.ruoyi.system.domain.SysRoleFileDir;
import com.ruoyi.system.service.IFileDirPermissionService;
import com.ruoyi.system.service.ISysFileDirService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 文件目录权限Controller
 * 
 * @author xyj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/system/dir")
public class SysFileDirController extends BaseController
{
    @Autowired
    private ISysFileDirService sysFileDirService;
    
    @Autowired
    private IFileDirPermissionService permissionService;
    
    @Autowired
    private ISysRoleService roleService;

    /**
     * 查询文件目录权限列表
     */
    @PreAuthorize("@ss.hasPermi('system:dir:list')")
    @GetMapping("/list")
    public AjaxResult list(SysFileDir sysFileDir)
    {
        List<SysFileDir> list = sysFileDirService.selectSysFileDirList(sysFileDir);
        return success(list);
    }

    /**
     * 导出文件目录权限列表
     */
    @PreAuthorize("@ss.hasPermi('system:dir:export')")
    @Log(title = "文件目录权限", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysFileDir sysFileDir)
    {
        List<SysFileDir> list = sysFileDirService.selectSysFileDirList(sysFileDir);
        ExcelUtil<SysFileDir> util = new ExcelUtil<SysFileDir>(SysFileDir.class);
        util.exportExcel(response, list, "文件目录权限数据");
    }

    /**
     * 获取文件目录权限详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dir:query')")
    @GetMapping(value = "/{dirId}")
    public AjaxResult getInfo(@PathVariable("dirId") Long dirId)
    {
        return success(sysFileDirService.selectSysFileDirByDirId(dirId));
    }

    /**
     * 新增文件目录权限
     */
    @PreAuthorize("@ss.hasPermi('system:dir:add')")
    @Log(title = "文件目录权限", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysFileDir sysFileDir)
    {
        return toAjax(sysFileDirService.insertSysFileDir(sysFileDir));
    }

    /**
     * 修改文件目录权限
     */
    @PreAuthorize("@ss.hasPermi('system:dir:edit')")
    @Log(title = "文件目录权限", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysFileDir sysFileDir)
    {
        return toAjax(sysFileDirService.updateSysFileDir(sysFileDir));
    }

    /**
     * 删除文件目录权限
     */
    @PreAuthorize("@ss.hasPermi('system:dir:remove')")
    @Log(title = "文件目录权限", businessType = BusinessType.DELETE)
	@DeleteMapping("/{dirIds}")
    public AjaxResult remove(@PathVariable Long[] dirIds)
    {
        return toAjax(sysFileDirService.deleteSysFileDirByDirIds(dirIds));
    }

    /**
     * 查询目录权限列表
     */
    @PreAuthorize("@ss.hasPermi('system:dir:permission')")
    @GetMapping("/permission/list")
    public AjaxResult permissionList(@RequestParam(required = false) Long dirId, @RequestParam(required = false) Long roleId)
    {
        List<SysRoleFileDir> permissions;
        
        if (dirId != null) {
            // 按目录查询
            permissions = permissionService.selectPermissionsByDirId(dirId);
            
            // 获取角色名称
            List<SysRole> roles = roleService.selectRoleAll();
            for (SysRoleFileDir permission : permissions) {
                SysRole role = roles.stream()
                    .filter(r -> r.getRoleId().equals(permission.getRoleId()))
                    .findFirst()
                    .orElse(null);
                if (role != null) {
                    permission.setRoleName(role.getRoleName());
                }
            }
        } else if (roleId != null) {
            // 按角色查询
            permissions = permissionService.selectPermissionsByRoleId(roleId);
        } else {
            return AjaxResult.error("请指定 dirId 或 roleId");
        }
        
        return success(permissions);
    }

    /**
     * 保存目录权限（会自动向上级父目录授权）
     */
    @PreAuthorize("@ss.hasPermi('system:dir:permission')")
    @Log(title = "文件目录权限分配", businessType = BusinessType.INSERT)
    @PostMapping("/permission")
    public AjaxResult savePermission(@RequestBody SysRoleFileDir permission)
    {
        return toAjax(permissionService.saveRoleDirPermission(permission));
    }

    /**
     * 删除目录权限
     */
    @PreAuthorize("@ss.hasPermi('system:dir:permission')")
    @Log(title = "文件目录权限分配", businessType = BusinessType.DELETE)
    @DeleteMapping("/permission/{roleId}/{dirId}")
    public AjaxResult deletePermission(@PathVariable Long roleId, @PathVariable Long dirId)
    {
        return toAjax(permissionService.deleteRoleDirPermission(roleId, dirId));
    }
}
