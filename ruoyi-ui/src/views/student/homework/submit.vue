<template>
  <div class="student-homework-page">
    <!-- 顶部栏 -->
    <div class="page-header">
      <div class="back-btn" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        返回首页
      </div>
      <h2>作业提交</h2>
      <div class="student-info">
        {{ studentName }}（{{ admissionYear }}年{{ classNum }}班）
      </div>
    </div>

    <!-- 作业列表 -->
    <div v-loading="loading" class="homework-list">
      <div v-if="sortedHomeworkList.length === 0" class="empty-tip">
        暂无作业
      </div>
      <div
        v-for="item in sortedHomeworkList"
        :key="item.homeworkId"
        class="homework-card"
        :class="{ 'is-overdue': isOverdue(item) }"
      >
        <div class="card-header">
          <span class="title">{{ item.title }}</span>
          <el-tag v-if="item.submitStatus === '1'" type="success" size="medium">已批改</el-tag>
          <el-tag v-else-if="item.submitFileName" type="success" size="medium">已提交</el-tag>
          <el-tag v-else type="danger" size="medium">未提交</el-tag>
        </div>
        <div class="card-body">
          <p class="content">{{ item.content || '无内容' }}</p>
          <p class="deadline" :class="deadlineInfo(item).cls">
            <i class="el-icon-time"></i>
            {{ deadlineInfo(item).text }}
          </p>
          <div v-if="item.submitStatus === '1'" class="grade-info">
            <p><strong>分数：</strong>{{ item.submitScore }} 分</p>
            <p v-if="item.submitComment"><strong>评语：</strong>{{ item.submitComment }}</p>
          </div>
          <div v-else-if="item.submitFileName" class="file-info">
            <p><strong>已提交文件：</strong>{{ item.submitFileName }}</p>
            <p><strong>提交时间：</strong>{{ parseTime(item.submitTime) }}</p>
          </div>
        </div>
        <div class="card-footer">
          <el-upload
            v-if="!item.submitStatus || item.submitStatus !== '1'"
            :action="uploadAction"
            :data="{ path: encodeURIComponent(item.storagePath || ''), studentId: studentId }"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="(file) => beforeUpload(file, item)"
            :on-progress="(e, f) => handleUploadProgress(e, f, item)"
            :on-success="(res, file) => handleUploadSuccess(res, file, item)"
            :on-error="(res, file) => handleUploadError(res, file, item)"
          >
            <el-button
              :type="item.submitFileName ? 'warning' : 'primary'"
              size="small"
              :icon="item.submitFileName ? 'el-icon-refresh' : 'el-icon-upload2'"
              :loading="uploadingId === item.homeworkId"
              :disabled="uploadingId && uploadingId !== item.homeworkId"
            >
              {{ uploadingId === item.homeworkId ? `上传中 ${uploadPercent}%` : (item.submitFileName ? '重新提交' : '上传作业') }}
            </el-button>
          </el-upload>
          <el-button
            v-if="item.submitFilePath"
            type="success"
            size="small"
            icon="el-icon-download"
            @click="handleDownload(item)"
            :disabled="uploadingId === item.homeworkId"
          >
            下载
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listStudentHomework, submitHomework } from "@/api/student/homework";

export default {
  name: "StudentHomework",
  data() {
    return {
      loading: false,
      studentId: null,
      studentName: "",
      admissionYear: null,
      classNum: null,
      homeworkList: [],
      uploadingId: null,
      uploadPercent: 0
    };
  },
  computed: {
    uploadAction() {
      return process.env.VUE_APP_BASE_API + "/student/upload";
    },
    uploadHeaders() {
      const studentInfo = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      return {
        "X-Student-Token": studentInfo.token || ""
      };
    },
    uploadData() {
      return {
        studentId: this.studentId
      };
    },
    sortedHomeworkList() {
      const list = [...this.homeworkList];
      const now = new Date();
      list.sort((a, b) => {
        const getStatus = (item) => {
          if (item.submitStatus === '1') return 2;
          if (item.submitFileName) return 1;
          return 0;
        };
        const sa = getStatus(a);
        const sb = getStatus(b);
        if (sa !== sb) return sa - sb;
        if (sa === 0) {
          const da = a.deadline ? new Date(a.deadline) : null;
          const db = b.deadline ? new Date(b.deadline) : null;
          if (!da && !db) return 0;
          if (!da) return 1;
          if (!db) return -1;
          return da - db;
        }
        return 0;
      });
      return list;
    }
  },
  created() {
    this.initStudentInfo();
    this.getHomeworkList();
  },
  methods: {
    initStudentInfo() {
      const studentInfo = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      this.studentId = studentInfo.studentId;
      this.studentName = studentInfo.studentName || "同学";
      this.admissionYear = studentInfo.admissionYear;
      this.classNum = studentInfo.classNum;
      if (!this.studentId || !this.admissionYear || !this.classNum) {
        this.$message.error("学生信息不完整，请重新登录");
        this.$router.push("/login");
      }
    },
    getHomeworkList() {
      this.loading = true;
      listStudentHomework(this.admissionYear, this.classNum, this.studentId)
        .then(response => {
          this.homeworkList = response.data || [];
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    deadlineInfo(item) {
      if (!item.deadline) {
        return { text: '不限时', cls: '' };
      }
      const now = new Date();
      const deadline = new Date(item.deadline);
      const diffMs = deadline - now;
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
      const pad = n => String(n).padStart(2, '0');
      const timeStr = `${pad(deadline.getHours())}:${pad(deadline.getMinutes())}`;
      if (diffMs < 0) {
        const absDays = Math.abs(diffDays);
        return { text: absDays === 0 ? `今天 ${timeStr} 已过期` : `已过期 ${absDays} 天`, cls: 'deadline-overdue' };
      }
      if (diffDays === 0) {
        const hoursLeft = diffMs / (1000 * 60 * 60);
        return { text: `今天 ${timeStr} 截止`, cls: hoursLeft < 2 ? 'deadline-urgent' : '' };
      }
      if (diffDays === 1) return { text: `明天 ${timeStr} 截止`, cls: '' };
      if (diffDays <= 7) return { text: `还有 ${diffDays} 天`, cls: '' };
      return { text: `${deadline.getMonth() + 1}月${deadline.getDate()}日 ${timeStr} 截止`, cls: '' };
    },
    isOverdue(item) {
      if (!item.deadline || item.submitFileName) return false;
      return new Date(item.deadline) < new Date();
    },
    beforeUpload(file, item) {
      const maxSize = (item.maxFileSize && item.maxFileSize > 0) ? item.maxFileSize : 500;
      const isLtMax = file.size / 1024 / 1024 < maxSize;
      if (!isLtMax) {
        this.$message.error(`文件大小不能超过 ${maxSize}MB!`);
        return false;
      }
      this.uploadingId = item.homeworkId;
      return true;
    },
    handleUploadProgress(event, file, item) {
      this.uploadPercent = Math.floor(event.percent);
    },
    handleUploadSuccess(response, file, item) {
      this.uploadingId = null;
      this.uploadPercent = 0;
      if (response.code === 200) {
        // 后端返回加了学号前缀的实际文件名
        const fileName = response.data || file.name;
        submitHomework({
          homeworkId: item.homeworkId,
          studentId: this.studentId,
          fileName: fileName
        }).then(() => {
          this.$message.success("提交成功");
          this.getHomeworkList();
        }).catch(() => {
          this.$message.error("提交记录失败");
        });
      } else {
        this.$message.error(response.msg || "上传失败");
      }
    },
    handleUploadError(res, file, item) {
      this.uploadingId = null;
      this.uploadPercent = 0;
      this.$message.error("上传失败");
    },
    handleDownload(item) {
      if (!item.submitFilePath) return;
      const studentInfo = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      const url = process.env.VUE_APP_BASE_API + '/student/download?path='
        + encodeURIComponent(item.submitFilePath)
        + '&studentId=' + this.studentId
        + '&token=' + encodeURIComponent(studentInfo.token || '');
      const link = document.createElement('a');
      link.href = url;
      link.download = item.submitFileName || '';
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    goBack() {
      this.$router.push("/student");
    },
    parseTime(time, pattern) {
      if (!time) return "";
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hour = String(date.getHours()).padStart(2, "0");
      const minute = String(date.getMinutes()).padStart(2, "0");
      if (pattern === "{y}-{m}-{d} {h}:{i}") {
        return `${year}-${month}-${day} ${hour}:${minute}`;
      }
      return `${year}-${month}-${day} ${hour}:${minute}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.student-homework-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 18px 28px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

  .back-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #409eff;
    cursor: pointer;
    font-size: 15px;

    &:hover {
      color: #66b1ff;
    }
  }

  h2 {
    margin: 0;
    font-size: 22px;
    color: #333;
  }

  .student-info {
    font-size: 15px;
    color: #666;
  }
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.empty-tip {
  text-align: center;
  color: #fff;
  padding: 60px;
  font-size: 17px;
}

.homework-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 22px 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border-left: 5px solid transparent;
  transition: border-color 0.3s;

  &.is-overdue {
    border-left-color: #f56c6c;
    background: rgba(255, 245, 245, 0.97);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }

    ::v-deep .el-tag {
      font-size: 14px;
      padding: 0 12px;
      height: 28px;
      line-height: 28px;
    }
  }

  .card-body {
    margin-bottom: 16px;

    .content {
      color: #666;
      font-size: 15px;
      margin: 0 0 12px;
      line-height: 1.7;
    }

    .deadline {
      color: #999;
      font-size: 14px;
      margin: 0 0 12px;

      i {
        margin-right: 6px;
      }

      &.deadline-urgent {
        color: #e6a23c;
        font-weight: 600;
      }

      &.deadline-overdue {
        color: #f56c6c;
        font-weight: 600;
      }
    }

    .grade-info {
      background: #f0f9ff;
      border-radius: 10px;
      padding: 14px 16px;
      margin-top: 12px;

      p {
        margin: 6px 0;
        font-size: 15px;
        color: #333;
      }
    }

    .file-info {
      background: #f5f7fa;
      border-radius: 10px;
      padding: 14px 16px;
      margin-top: 12px;

      p {
        margin: 6px 0;
        font-size: 15px;
        color: #333;
      }
    }
  }

  .card-footer {
    display: flex;
    gap: 12px;

    .el-button {
      font-size: 15px;
      padding: 10px 18px;
    }
  }
}
</style>
