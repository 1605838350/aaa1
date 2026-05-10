<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="年级" prop="grade">
        <el-select v-model="queryGrade" placeholder="请选择年级" clearable @change="handleGradeChange">
          <el-option
            v-for="item in gradeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="班级" prop="classNum">
        <el-select v-model="queryParams.classNum" placeholder="请选择班级" clearable>
          <el-option
            v-for="item in classOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="账号" prop="account">
        <el-input
          v-model="queryParams.account"
          placeholder="请输入账号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="studentName">
        <el-input
          v-model="queryParams.studentName"
          placeholder="请输入姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['student:list:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleBatchAdd"
          v-hasPermi="['student:list:add']"
        >批量新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['student:list:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['student:list:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="studentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" width="150" />
      <el-table-column label="登录账号" align="center" prop="account" width="150" />
      <el-table-column label="姓名" align="center" prop="studentName" width="150" />
      <el-table-column label="性别" align="center" prop="sex" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_user_sex" :value="scope.row.sex"/>
        </template>
      </el-table-column>
      <el-table-column label="入学年份" align="center" prop="admissionYear" width="120" />
      <el-table-column label="年级" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.grade }}年级</span>
        </template>
      </el-table-column>
      <el-table-column label="班级" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.classNum }}班</span>
        </template>
      </el-table-column>
      <el-table-column label="学号" align="center" prop="studentNum" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['student:list:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['student:list:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改学生对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="入学年份" prop="admissionYear">
          <el-input-number v-model="form.admissionYear" :min="2000" :max="2100" controls-position="right" />
        </el-form-item>
        <el-form-item label="班级" prop="classNum">
          <el-select v-model="form.classNum" placeholder="请选择班级">
            <el-option
              v-for="item in classOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学号" prop="studentNum">
          <el-input-number v-model="form.studentNum" :min="1" :max="99" controls-position="right" />
        </el-form-item>
        <el-form-item label="姓名" prop="studentName">
          <el-input v-model="form.studentName" placeholder="请输入姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio label="0">男</el-radio>
            <el-radio label="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="form.studentId == undefined">
          <el-input v-model="form.password" placeholder="请输入密码" type="password" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
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

    <!-- 批量新增学生对话框 -->
    <el-dialog title="批量新增学生" :visible.sync="batchOpen" width="400px" append-to-body>
      <el-form ref="batchForm" :model="batchForm" :rules="batchRules" label-width="80px">
        <el-form-item label="年级" prop="grade">
          <el-select v-model="batchForm.grade" placeholder="请选择年级" style="width: 100%">
            <el-option
              v-for="item in gradeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classNum">
          <el-select v-model="batchForm.classNum" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="item in classOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="人数" prop="count">
          <el-input-number v-model="batchForm.count" :min="1" :max="99" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitBatchForm">确 定</el-button>
        <el-button @click="batchOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listStudent, getStudent, addStudent, updateStudent, delStudent, exportStudent, batchAddStudent } from "@/api/system/student";

export default {
  name: "StudentList",
  dicts: ['sys_user_sex'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 学生表格数据
      studentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 班级选项
      classOptions: [
        { value: 1, label: '1班' },
        { value: 2, label: '2班' },
        { value: 3, label: '3班' },
        { value: 4, label: '4班' },
        { value: 5, label: '5班' },
        { value: 6, label: '6班' }
      ],
      // 年级选项
      gradeOptions: [
        { value: 1, label: '一年级' },
        { value: 2, label: '二年级' },
        { value: 3, label: '三年级' },
        { value: 4, label: '四年级' },
        { value: 5, label: '五年级' },
        { value: 6, label: '六年级' }
      ],
      // 查询年级
      queryGrade: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        admissionYear: null,
        classNum: null,
        account: null,
        studentName: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        admissionYear: [
          { required: true, message: "入学年份不能为空", trigger: "blur" }
        ],
        classNum: [
          { required: true, message: "班级不能为空", trigger: "change" }
        ],
        studentNum: [
          { required: true, message: "学号不能为空", trigger: "blur" }
        ],
        studentName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        sex: [
          { required: true, message: "性别不能为空", trigger: "change" }
        ],
        password: [
          { required: true, message: "密码不能为空", trigger: "blur" },
          { min: 5, max: 20, message: "密码长度必须介于 5 和 20 之间", trigger: "blur" }
        ]
      },
      // 批量新增对话框显示
      batchOpen: false,
      // 批量新增表单
      batchForm: {
        grade: null,
        classNum: null,
        count: 1
      },
      // 批量新增校验
      batchRules: {
        grade: [
          { required: true, message: "年级不能为空", trigger: "change" }
        ],
        classNum: [
          { required: true, message: "班级不能为空", trigger: "change" }
        ],
        count: [
          { required: true, message: "人数不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询学生列表 */
    getList() {
      this.loading = true;
      listStudent(this.queryParams).then(response => {
        this.studentList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        studentId: undefined,
        admissionYear: new Date().getFullYear(),
        classNum: 1,
        studentNum: 1,
        studentName: undefined,
        sex: "0",
        password: undefined,
        status: "0",
        remark: undefined
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    // 年级变化时计算入学年份
    handleGradeChange(grade) {
      if (grade) {
        const currentYear = new Date().getFullYear()
        this.queryParams.admissionYear = currentYear - grade
      } else {
        this.queryParams.admissionYear = null
      }
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryGrade = null
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.studentId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加学生";
    },
    /** 批量新增按钮操作 */
    handleBatchAdd() {
      this.batchForm = {
        grade: null,
        classNum: null,
        count: 1
      };
      this.batchOpen = true;
    },
    /** 批量新增提交 */
    submitBatchForm() {
      this.$refs["batchForm"].validate(valid => {
        if (valid) {
          batchAddStudent(this.batchForm).then(response => {
            this.$modal.msgSuccess(response.msg || "批量新增成功");
            this.batchOpen = false;
            this.getList();
          });
        }
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const studentId = row.studentId || this.ids;
      getStudent(studentId).then(response => {
        this.form = response.data;
        this.form.password = undefined; // 修改时不修改密码
        this.open = true;
        this.title = "修改学生";
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.studentId != undefined) {
            updateStudent(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addStudent(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const studentIds = row.studentId || this.ids;
      this.$modal.confirm('是否确认删除学生编号为"' + studentIds + '"的数据项？').then(function() {
        return delStudent(studentIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 状态修改 */
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.studentName + '"学生吗？').then(function() {
        return updateStudent(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('/system/student/export', {
        ...this.queryParams
      }, `student_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
