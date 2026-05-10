<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧目录树 -->
      <el-col :span="6">
        <div class="head-container">
          <el-input
            v-model="filterText"
            placeholder="请输入目录名称筛选"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container tree-container">
          <el-tree
            ref="tree"
            :data="dirList"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            :default-expand-all="false"
            :default-expanded-keys="rootDirIds"
            :highlight-current="true"
            node-key="dirId"
            @node-click="handleNodeClick"
          >
            <span class="custom-tree-node" slot-scope="{ node, data }">
              <span>
                <i :class="getIcon(data)"></i>
                {{ data.dirName }}
              </span>
            </span>
          </el-tree>
        </div>
      </el-col>

      <!-- 右侧详情和列表 -->
      <el-col :span="18">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
          <el-form-item label="目录名称" prop="dirName">
            <el-input
              v-model="queryParams.dirName"
              placeholder="请输入目录名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option
                v-for="dict in dict.type.sys_dir_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAdd"
              v-hasPermi="['system:dir:add']"
            >新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="info"
              plain
              icon="el-icon-sort"
              size="mini"
              @click="toggleExpandAll"
            >展开/折叠</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <!-- 当前选中目录信息 -->
        <el-card v-if="currentNode" class="box-card mb8">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-folder-opened"></i> 当前选中：{{ currentNode.dirName }}</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="currentNode = null">关闭</el-button>
          </div>
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="目录ID">{{ currentNode.dirId }}</el-descriptions-item>
            <el-descriptions-item label="目录名称">{{ currentNode.dirName }}</el-descriptions-item>
            <el-descriptions-item label="文件路径">{{ currentNode.path }}</el-descriptions-item>
            <el-descriptions-item label="显示顺序">{{ currentNode.orderNum }}</el-descriptions-item>
            <el-descriptions-item label="权限标识">{{ currentNode.perms }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <dict-tag :options="dict.type.sys_dir_status" :value="currentNode.status"/>
            </el-descriptions-item>
            <el-descriptions-item label="创建者">{{ currentNode.createBy }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(currentNode.createTime, '{y}-{m}-{d}') }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 10px;">
            <el-button size="mini" type="primary" icon="el-icon-edit" @click="handleUpdate(currentNode)" v-hasPermi="['system:dir:edit']">修改</el-button>
            <el-button size="mini" type="success" icon="el-icon-plus" @click="handleAdd(currentNode)" v-hasPermi="['system:dir:add']">新增子目录</el-button>
            <el-button size="mini" type="warning" icon="el-icon-key" @click="handlePermission(currentNode)" v-hasPermi="['system:dir:permission']">权限</el-button>
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="handleDelete(currentNode)" v-hasPermi="['system:dir:remove']">删除</el-button>
          </div>
        </el-card>

        <!-- 子目录列表 -->
        <el-table
          v-loading="loading"
          :data="childrenList"
          row-key="dirId"
          :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
          border
        >
          <el-table-column label="目录名称" prop="dirName" min-width="200">
            <template slot-scope="scope">
              <span>
                <i :class="getIcon(scope.row)" style="margin-right: 5px;"></i>
                <span :style="{paddingLeft: (getLevel(scope.row) - 1) * 20 + 'px'}">{{ scope.row.dirName }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="文件路径" prop="path" show-overflow-tooltip />
          <el-table-column label="显示顺序" align="center" prop="orderNum" width="100" />
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.sys_dir_status" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="120">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['system:dir:edit']"
              >修改</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-plus"
                @click="handleAdd(scope.row)"
                v-hasPermi="['system:dir:add']"
              >新增</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-key"
                @click="handlePermission(scope.row)"
                v-hasPermi="['system:dir:permission']"
              >权限</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['system:dir:remove']"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 添加或修改文件目录权限对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="目录名称" prop="dirName">
          <el-input v-model="form.dirName" placeholder="请输入目录名称" />
        </el-form-item>
        <el-form-item label="父目录ID" prop="parentId">
          <treeselect v-model="form.parentId" :options="dirOptions" :normalizer="normalizer" placeholder="请选择父目录ID" />
        </el-form-item>
        <el-form-item label="文件系统路径" prop="path">
          <el-input v-model="form.path" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="orderNum">
          <el-input v-model="form.orderNum" placeholder="请输入显示顺序" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_dir_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog title="权限分配" :visible.sync="permissionOpen" width="700px" append-to-body>
      <div v-if="currentDir">
        <el-alert
          title="提示：保存权限时会自动向上级父目录授予相应权限，确保父目录有足够的访问权限。"
          type="info"
          :closable="false"
          style="margin-bottom: 15px;"
        />
        <p>当前目录：<strong>{{ currentDir.dirName }}</strong>（{{ currentDir.path }}）</p>
        <el-divider></el-divider>
        
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAddPermission"
            >添加权限</el-button>
          </el-col>
        </el-row>

        <el-table :data="permissionList" v-loading="permissionLoading">
          <el-table-column label="角色" align="center" prop="roleName" />
          <el-table-column label="查看" align="center" width="80">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.canView" @change="updatePermission(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="下载" align="center" width="80">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.canDownload" @change="updatePermission(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="上传" align="center" width="80">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.canUpload" @change="updatePermission(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="删除" align="center" width="80">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.canDelete" @change="updatePermission(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDeletePermission(scope.row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="permissionOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 添加权限对话框 -->
    <el-dialog title="添加权限" :visible.sync="addPermissionOpen" width="400px" append-to-body>
      <el-form ref="permissionForm" :model="permissionForm" label-width="80px">
        <el-form-item label="选择角色" prop="roleId">
          <el-select v-model="permissionForm.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权限">
          <el-checkbox v-model="permissionForm.canView">查看</el-checkbox>
          <el-checkbox v-model="permissionForm.canDownload">下载</el-checkbox>
          <el-checkbox v-model="permissionForm.canUpload">上传</el-checkbox>
          <el-checkbox v-model="permissionForm.canDelete">删除</el-checkbox>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitPermission">确 定</el-button>
        <el-button @click="addPermissionOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDir, getDir, delDir, addDir, updateDir } from "@/api/system/dir"
import { listPermission, savePermission, deletePermission } from "@/api/system/dirPermission"
import { listRole } from "@/api/system/role"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "Dir",
  dicts: ['sys_dir_status'],
  components: {
    Treeselect
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 文件目录权限表格数据
      dirList: [],
      // 子目录列表
      childrenList: [],
      // 文件目录权限树选项
      dirOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认收起
      isExpandAll: false,
      // 根目录ID列表（用于默认展开）
      rootDirIds: [],
      // 重新渲染表格状态
      refreshTable: true,
      // 筛选文本
      filterText: '',
      // 当前选中节点
      currentNode: null,
      // 树形组件属性配置
      defaultProps: {
        children: 'children',
        label: 'dirName'
      },
      // 查询参数
      queryParams: {
        dirName: null,
        parentId: null,
        path: null,
        orderNum: null,
        perms: null,
        status: null,
        createBy: null,
        updateBy: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        dirName: [
          { required: true, message: "目录名称不能为空", trigger: "blur" }
        ],
        path: [
          { required: true, message: "文件系统路径不能为空", trigger: "blur" }
        ],
      },
      // 权限分配相关
      permissionOpen: false,
      permissionLoading: false,
      currentDir: null,
      permissionList: [],
      addPermissionOpen: false,
      permissionForm: {
        roleId: null,
        canView: true,
        canDownload: false,
        canUpload: false,
        canDelete: false
      },
      roleOptions: []
    }
  },
  watch: {
    // 筛选文本变化时过滤树
    filterText(val) {
      this.$refs.tree.filter(val)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询文件目录权限列表 */
    getList() {
      this.loading = true
      listDir(this.queryParams).then(response => {
        this.dirList = this.handleTree(response.data, "dirId", "parentId")
        this.childrenList = this.dirList
        // 设置根目录ID用于默认展开
        this.rootDirIds = this.dirList.map(dir => dir.dirId)
        this.loading = false
      })
    },
    /** 筛选节点 */
    filterNode(value, data) {
      if (!value) return true
      return data.dirName.indexOf(value) !== -1
    },
    /** 点击树节点 */
    handleNodeClick(data) {
      this.currentNode = data
      // 显示该节点的子目录
      if (data.children && data.children.length > 0) {
        this.childrenList = data.children
      } else {
        this.childrenList = [data]
      }
    },
    /** 获取目录图标 */
    getIcon(data) {
      // 所有目录都显示文件夹图标
      return 'el-icon-folder'
    },
    /** 获取目录层级 */
    getLevel(row) {
      let level = 1
      let parentId = row.parentId
      const findLevel = (list, pid, lev) => {
        for (const item of list) {
          if (item.dirId === pid) {
            return lev + 1
          }
          if (item.children) {
            const childLev = findLevel(item.children, pid, lev + 1)
            if (childLev) return childLev
          }
        }
        return null
      }
      if (parentId === 0) return 1
      const lev = findLevel(this.dirList, parentId, 1)
      return lev || 1
    },
    /** 转换文件目录权限数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.dirId,
        label: node.dirName,
        children: node.children
      }
    },
	/** 查询文件目录权限下拉树结构 */
    getTreeselect() {
      listDir().then(response => {
        this.dirOptions = []
        const data = { dirId: 0, dirName: '顶级节点', children: [] }
        data.children = this.handleTree(response.data, "dirId", "parentId")
        this.dirOptions.push(data)
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        dirId: null,
        dirName: null,
        parentId: null,
        path: null,
        orderNum: null,
        perms: null,
        status: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset()
      this.getTreeselect()
      if (row != null && row.dirId) {
        this.form.parentId = row.dirId
      } else {
        this.form.parentId = 0
      }
      this.open = true
      this.title = "添加文件目录权限"
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      // 获取所有节点ID
      const getAllNodeIds = (nodes) => {
        let ids = []
        nodes.forEach(node => {
          ids.push(node.dirId)
          if (node.children && node.children.length > 0) {
            ids = ids.concat(getAllNodeIds(node.children))
          }
        })
        return ids
      }
      
      this.$nextTick(() => {
        if (!this.isExpandAll) {
          // 当前折叠状态，展开全部
          const allIds = getAllNodeIds(this.dirList)
          allIds.forEach(id => {
            const node = this.$refs.tree.getNode(id)
            if (node) {
              node.expanded = true
            }
          })
        } else {
          // 当前展开状态，折叠（只保留根目录展开）
          this.dirList.forEach(dir => {
            const node = this.$refs.tree.getNode(dir.dirId)
            if (node) {
              node.expanded = true // 根目录展开
              // 折叠所有子节点
              this.collapseChildren(dir)
            }
          })
        }
        this.isExpandAll = !this.isExpandAll
      })
    },
    /** 折叠子节点 */
    collapseChildren(node) {
      if (node.children && node.children.length > 0) {
        node.children.forEach(child => {
          const childNode = this.$refs.tree.getNode(child.dirId)
          if (childNode) {
            childNode.expanded = false
          }
          this.collapseChildren(child)
        })
      }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      this.getTreeselect()
      if (row != null) {
        this.form.parentId = row.parentId
      }
      getDir(row.dirId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改文件目录权限"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.dirId != null) {
            updateDir(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDir(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除文件目录权限编号为"' + row.dirId + '"的数据项？').then(function() {
        return delDir(row.dirId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 权限分配按钮操作 */
    handlePermission(row) {
      this.currentDir = row
      this.permissionOpen = true
      this.getPermissionList()
      this.getRoleOptions()
    },
    /** 获取权限列表 */
    getPermissionList() {
      this.permissionLoading = true
      listPermission({ dirId: this.currentDir.dirId }).then(response => {
        this.permissionList = response.data || []
        this.permissionLoading = false
      })
    },
    /** 获取角色选项 */
    getRoleOptions() {
      listRole({ pageSize: 1000 }).then(response => {
        this.roleOptions = response.rows || []
      })
    },
    /** 添加权限按钮 */
    handleAddPermission() {
      this.permissionForm = {
        roleId: null,
        canView: true,
        canDownload: false,
        canUpload: false,
        canDelete: false
      }
      this.addPermissionOpen = true
    },
    /** 提交权限 */
    submitPermission() {
      if (!this.permissionForm.roleId) {
        this.$modal.msgError("请选择角色")
        return
      }
      const data = {
        roleId: this.permissionForm.roleId,
        dirId: this.currentDir.dirId,
        canView: this.permissionForm.canView ? 1 : 0,
        canDownload: this.permissionForm.canDownload ? 1 : 0,
        canUpload: this.permissionForm.canUpload ? 1 : 0,
        canDelete: this.permissionForm.canDelete ? 1 : 0
      }
      savePermission(data).then(() => {
        this.$modal.msgSuccess("添加成功，父目录已自动授权")
        this.addPermissionOpen = false
        this.getPermissionList()
      })
    },
    /** 更新权限 */
    updatePermission(row) {
      // 检测是否有关权限被取消（从true变为false）
      const revokedPerms = []
      if (row._oldCanView && !row.canView) revokedPerms.push('查看')
      if (row._oldCanDownload && !row.canDownload) revokedPerms.push('下载')
      if (row._oldCanUpload && !row.canUpload) revokedPerms.push('上传')
      if (row._oldCanDelete && !row.canDelete) revokedPerms.push('删除')
      
      // 如果有权限被取消，需要确认
      if (revokedPerms.length > 0) {
        const confirmMsg = '此操作将取消【' + revokedPerms.join('、') + '】权限，\n该目录的所有子目录的对应权限也将被同步取消。\n\n确认继续？'
        this.$confirm(confirmMsg, '权限取消确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.doUpdatePermission(row)
        }).catch(() => {
          // 用户取消，恢复原状态
          row.canView = row._oldCanView
          row.canDownload = row._oldCanDownload
          row.canUpload = row._oldCanUpload
          row.canDelete = row._oldCanDelete
        })
      } else {
        this.doUpdatePermission(row)
      }
    },
    /** 执行更新权限 */
    doUpdatePermission(row) {
      const data = {
        roleId: row.roleId,
        dirId: this.currentDir.dirId,
        canView: row.canView ? 1 : 0,
        canDownload: row.canDownload ? 1 : 0,
        canUpload: row.canUpload ? 1 : 0,
        canDelete: row.canDelete ? 1 : 0
      }
      savePermission(data).then(() => {
        this.$modal.msgSuccess("更新成功，父目录已自动授权")
      })
    },
    /** 删除权限 */
    handleDeletePermission(row) {
      this.$modal.confirm('是否确认删除该角色的权限？').then(() => {
        return deletePermission(row.roleId, this.currentDir.dirId)
      }).then(() => {
        this.getPermissionList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.tree-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  max-height: calc(100vh - 250px);
  overflow-y: auto;
  background-color: #fafafa;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.el-tree {
  background-color: transparent;
}

.el-tree-node__content {
  height: 32px;
}

.box-card {
  margin-bottom: 15px;
}

.box-card .el-card__header {
  padding: 10px 15px;
  background-color: #f5f7fa;
}

.mb8 {
  margin-bottom: 8px;
}

.head-container {
  margin-bottom: 10px;
}
</style>
