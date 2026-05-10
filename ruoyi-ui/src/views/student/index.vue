<template>
  <div class="student-home">
    <!-- 顶部欢迎栏 -->
    <div class="header">
      <div class="welcome">
        <h1>你好，{{ studentName }}！</h1>
        <p>{{ currentDate }}</p>
      </div>
      <div class="header-actions">
        <div class="edit-btn" @click="openEditDialog">
          <i class="el-icon-edit"></i>
          修改资料
        </div>
        <div class="logout-btn" @click="logout">
          <i class="el-icon-switch-button"></i>
          退出
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 我的云盘 -->
      <div class="card" @click="goToCloudDisk">
        <div class="card-icon blue">
          <i class="el-icon-folder-opened"></i>
        </div>
        <div class="card-info">
          <div class="card-title">我的云盘</div>
          <div class="card-desc">存放我的文件</div>
        </div>
        <i class="el-icon-arrow-right card-arrow"></i>
      </div>

      <!-- 班级共享 -->
      <div class="card" @click="goToClassShare">
        <div class="card-icon green">
          <i class="el-icon-share"></i>
        </div>
        <div class="card-info">
          <div class="card-title">班级共享</div>
          <div class="card-desc">查看班级文件</div>
        </div>
        <i class="el-icon-arrow-right card-arrow"></i>
      </div>

      <!-- 作业提交 -->
      <div class="card" @click="goToHomework">
        <div class="card-icon orange">
          <i class="el-icon-edit-outline"></i>
        </div>
        <div class="card-info">
          <div class="card-title">作业提交</div>
          <div class="card-desc">
            上传作业
            <span v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }}项待交</span>
          </div>
        </div>
        <i class="el-icon-arrow-right card-arrow"></i>
      </div>
    </div>

    <!-- 修改资料对话框 -->
    <el-dialog title="修改个人资料" :visible.sync="editDialogVisible" width="350px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="60px">
        <el-form-item label="姓名" prop="studentName">
          <el-input v-model="editForm.studentName" placeholder="请输入姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="editForm.sex">
            <el-radio label="0">男</el-radio>
            <el-radio label="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitEdit">确 定</el-button>
        <el-button @click="editDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { listStudentHomework } from "@/api/student/homework";

export default {
  name: "StudentHome",
  data() {
    return {
      studentName: "同学",
      currentDate: "",
      pendingCount: 0,
      // 编辑对话框
      editDialogVisible: false,
      // 编辑表单
      editForm: {
        studentId: null,
        studentName: "",
        sex: "0"
      },
      // 编辑校验
      editRules: {
        studentName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        sex: [
          { required: true, message: "性别不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.checkLogin()
    this.setCurrentDate()
    this.getPendingCount()
  },
  methods: {
    // 检查学生登录状态
    checkLogin() {
      const studentInfo = localStorage.getItem('studentInfo')
      if (!studentInfo) {
        this.$router.push('/login')
        return
      }
      const student = JSON.parse(studentInfo)
      this.studentName = student.studentName || '同学'
    },
    // 打开编辑资料对话框
    openEditDialog() {
      const studentInfo = JSON.parse(localStorage.getItem('studentInfo') || '{}')
      this.editForm = {
        studentId: studentInfo.studentId,
        studentName: studentInfo.studentName || '',
        sex: studentInfo.sex || '0'
      }
      this.editDialogVisible = true
    },
    // 提交编辑
    submitEdit() {
      this.$refs['editForm'].validate(valid => {
        if (!valid) return
        const studentInfo = JSON.parse(localStorage.getItem('studentInfo') || '{}')
        request({
          url: '/student/profile',
          method: 'put',
          data: this.editForm,
          headers: {
            'X-Student-Token': studentInfo.token || '',
            'isToken': false
          }
        }).then(res => {
          if (res.code === 200) {
            this.$message.success('修改成功')
            // 更新本地存储
            studentInfo.studentName = this.editForm.studentName
            studentInfo.sex = this.editForm.sex
            localStorage.setItem('studentInfo', JSON.stringify(studentInfo))
            // 更新页面显示
            this.studentName = this.editForm.studentName
            this.editDialogVisible = false
          } else {
            this.$message.error(res.msg || '修改失败')
          }
        })
      })
    },
    setCurrentDate() {
      const weekDays = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"]
      const now = new Date()
      const month = now.getMonth() + 1
      const day = now.getDate()
      const weekDay = weekDays[now.getDay()]
      this.currentDate = `${month}月${day}日 ${weekDay}`
    },
    getPendingCount() {
      const info = JSON.parse(localStorage.getItem('studentInfo') || '{}');
      if (!info.studentId || !info.admissionYear || !info.classNum) return;
      listStudentHomework(info.admissionYear, info.classNum, info.studentId)
        .then(res => {
          const list = res.data || [];
          this.pendingCount = list.filter(item => !item.submitFileName).length;
        })
        .catch(() => {});
    },
    goToCloudDisk() {
      this.$router.push("/student/cloud-disk")
    },
    goToClassShare() {
      this.$router.push("/student/class-share")
    },
    goToHomework() {
      this.$router.push("/student/homework")
    },
    logout() {
      this.$confirm("确定要退出登录吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        // 清除学生登录信息
        localStorage.removeItem('studentInfo')
        this.$router.push("/login")
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.student-home {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

// 顶部欢迎栏
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 22px 28px;
  margin-bottom: 22px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

  .welcome {
    h1 {
      margin: 0;
      font-size: 28px;
      color: #333;
      font-weight: 600;
    }
    p {
      margin: 6px 0 0;
      font-size: 16px;
      color: #666;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .edit-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 12px 24px;
      background: #409eff;
      color: white;
      border-radius: 22px;
      cursor: pointer;
      font-size: 15px;
      transition: all 0.3s;

      &:hover {
        background: #66b1ff;
        transform: translateY(-2px);
      }

      i {
        font-size: 18px;
      }
    }
  }

  .logout-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 12px 24px;
    background: #ff6b6b;
    color: white;
    border-radius: 22px;
    cursor: pointer;
    font-size: 15px;
    transition: all 0.3s;

    &:hover {
      background: #ff5252;
      transform: translateY(-2px);
    }

    i {
      font-size: 18px;
    }
  }
}

// 主要内容区
.main-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px 26px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);

  &:hover {
    transform: translateX(6px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }

  .card-icon {
    width: 60px;
    height: 60px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 28px;
    color: white;

    &.blue  { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    &.green { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
    &.orange { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
  }

  .card-info {
    flex: 1;
  }

  .card-title {
    font-size: 19px;
    font-weight: 600;
    color: #303133;
  }

  .card-desc {
    font-size: 14px;
    color: #999;
    margin-top: 3px;

    .pending-badge {
      display: inline-block;
      margin-left: 10px;
      padding: 2px 10px;
      background: #f56c6c;
      color: #fff;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 500;
    }
  }

  .card-arrow {
    font-size: 20px;
    color: #c0c4cc;
    flex-shrink: 0;
  }
}

// 响应式适配
@media (max-width: 480px) {
  .student-home {
    padding: 15px;
  }

  .header {
    padding: 16px 20px;
    .welcome h1 { font-size: 22px; }
    .welcome p { font-size: 14px; }
  }

  .card {
    padding: 18px 20px;
    gap: 14px;

    .card-icon {
      width: 50px;
      height: 50px;
      font-size: 24px;
    }
    .card-title { font-size: 17px; }
  }
}
</style>
