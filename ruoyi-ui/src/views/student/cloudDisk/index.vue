<template>
  <div class="cloud-disk-page">
    <!-- 顶部栏 -->
    <div class="page-header">
      <div class="back-btn" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        返回首页
      </div>
      <h2>我的云盘</h2>
      <div class="file-count">共 {{ fileList.length }} 个文件</div>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <el-input
        v-model="searchKey"
        placeholder="搜作业名字或文件名"
        clearable
        prefix-icon="el-icon-search"
        @input="handleSearch"
      />
    </div>

    <!-- 文件列表（按作业分组） -->
    <div v-loading="loading" class="file-list">
      <div v-if="filteredList.length === 0 && !loading" class="empty-tip">
        <div class="empty-icon">
          <i class="el-icon-folder-opened"></i>
        </div>
        <p>还没有提交过文件哦~</p>
        <p class="sub-tip">去作业提交页面上传你的第一个文件吧</p>
      </div>

      <div v-for="group in groupedFiles" :key="group.title" class="homework-group">
        <div class="group-header" @click="toggleGroup(group.title)">
          <i :class="expandedGroups[group.title] ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" class="group-arrow"></i>
          <span class="group-title">{{ group.title }}</span>
          <span class="group-count">{{ group.count }} 个</span>
        </div>
        <div v-show="expandedGroups[group.title]" class="group-files">
          <div
            v-for="file in group.files"
            :key="file.submitId"
            class="file-card"
          >
            <div class="file-icon" :style="{ background: getIconBg(file) }">
              <i :class="getFileIcon(file)"></i>
            </div>
            <div class="file-info">
              <div class="file-name" @click="handlePreview(file)">{{ getDisplayName(file) }}</div>
              <div class="file-meta">
                <span class="meta-item" title="提交时间">
                  <i class="el-icon-time"></i>
                  {{ formatTime(file.submitTime) }}
                </span>
                <span v-if="file.score !== null && file.score !== undefined" class="meta-item" title="分数">
                  <i class="el-icon-trophy"></i>
                  {{ file.score }} 分
                </span>
                <el-tag v-if="file.status === '1'" type="success" size="mini">已批改</el-tag>
                <el-tag v-else type="warning" size="mini">已提交</el-tag>
              </div>
            </div>
            <div class="file-actions">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-view"
                circle
                title="预览"
                @click="handlePreview(file)"
              />
              <el-button
                type="success"
                size="small"
                icon="el-icon-download"
                circle
                title="下载"
                @click="handleDownload(file)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Office 文件预览对话框 -->
    <el-dialog
      :title="previewTitle"
      :visible.sync="previewVisible"
      width="80%"
      :close-on-click-modal="false"
      append-to-body
      top="3vh"
    >
      <FilePreview
        :src="previewData"
        :type="previewType"
        @rendered="() => {}"
        @error="() => $message.error('文档预览失败')"
      />
    </el-dialog>

    <!-- 图片灯箱预览 -->
    <transition name="image-viewer-fade">
      <div v-if="imageViewerVisible" class="image-viewer-overlay" @click.self="closeImageViewer">
        <button class="image-viewer-reset" type="button" @click="resetImageTransform">重置</button>
        <button class="image-viewer-close" type="button" @click="closeImageViewer">
          <i class="el-icon-close"></i>
        </button>
        <button v-if="imageViewerFiles.length > 1" class="image-viewer-nav image-viewer-nav-prev" type="button" @click.stop="showPrevImage">
          <i class="el-icon-arrow-left"></i>
        </button>
        <div class="image-viewer-stage" @wheel.prevent="handleImageWheel">
          <i v-if="imageViewerLoading" class="el-icon-loading image-viewer-loading"></i>
          <img
            v-else-if="imageViewerUrl"
            :src="imageViewerUrl"
            :alt="imageViewerCurrentFileName"
            class="image-viewer-img"
            :class="{ 'is-draggable': imageViewerScale > 1, 'is-dragging': imageViewerDragging }"
            :style="imageViewerTransformStyle"
            @mousedown.prevent="startImageDrag"
            @dblclick.stop="resetImageTransform"
            @click.stop
          />
          <div v-else class="image-viewer-error">{{ imageViewerError || '图片加载失败' }}</div>
        </div>
        <button v-if="imageViewerFiles.length > 1" class="image-viewer-nav image-viewer-nav-next" type="button" @click.stop="showNextImage">
          <i class="el-icon-arrow-right"></i>
        </button>
        <div v-if="imageViewerFiles.length > 0" class="image-viewer-index">
          {{ imageViewerIndex + 1 }} / {{ imageViewerFiles.length }}
          <span class="image-viewer-name">{{ imageViewerCurrentFileName }}</span>
        </div>
      </div>
    </transition>

    <!-- 视频灯箱预览 -->
    <transition name="image-viewer-fade">
      <div v-if="videoViewerVisible" class="video-viewer-overlay" @click.self="closeVideoViewer">
        <button class="video-viewer-close" type="button" @click="closeVideoViewer">
          <i class="el-icon-close"></i>
        </button>
        <button v-if="videoViewerFiles.length > 1" class="video-viewer-nav video-viewer-nav-prev" type="button" @click.stop="showPrevVideo">
          <i class="el-icon-arrow-left"></i>
        </button>
        <div class="video-viewer-stage" @click.stop @wheel.prevent="handleVideoWheel">
          <i v-if="videoViewerLoading" class="el-icon-loading image-viewer-loading"></i>
          <video
            v-else-if="videoViewerUrl"
            :src="videoViewerUrl"
            class="video-viewer-video"
            :style="videoViewerTransformStyle"
            controls autoplay preload="metadata"
            @dblclick.stop="resetVideoTransform"
          />
          <div v-else class="image-viewer-error">{{ videoViewerError || '视频加载失败' }}</div>
        </div>
        <button v-if="videoViewerFiles.length > 1" class="video-viewer-nav video-viewer-nav-next" type="button" @click.stop="showNextVideo">
          <i class="el-icon-arrow-right"></i>
        </button>
        <div v-if="videoViewerFiles.length > 0" class="video-viewer-index">
          {{ videoViewerIndex + 1 }} / {{ videoViewerFiles.length }}
          <span class="image-viewer-name">{{ videoViewerCurrentFileName }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { listStudentFiles, previewStudentFile } from "@/api/student/cloudDisk";
import FilePreview from "@/components/FilePreview";
import mediaViewer from "@/mixins/mediaViewer";

export default {
  name: "StudentCloudDisk",
  components: { FilePreview },
  mixins: [mediaViewer],
  data() {
    return {
      loading: false,
      fileList: [],
      searchKey: "",
      // 预览
      previewVisible: false,
      previewData: null,
      previewTitle: '',
      previewType: '',
      expandedGroups: {}
    };
  },
  computed: {
    filteredList() {
      if (!this.searchKey) return this.fileList;
      const key = this.searchKey.toLowerCase();
      return this.fileList.filter(f =>
        (f.fileName && f.fileName.toLowerCase().includes(key)) ||
        (f.homeworkTitle && f.homeworkTitle.toLowerCase().includes(key))
      );
    },
    groupedFiles() {
      const map = {};
      this.filteredList.forEach(f => {
        const key = f.homeworkTitle || '未分类';
        if (!map[key]) map[key] = [];
        map[key].push(f);
      });
      return Object.entries(map).map(([title, files]) => ({
        title,
        files,
        count: files.length
      }));
    }
  },
  created() {
    this.init();
    this.getFileList();
  },
  methods: {
    init() {
      const info = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      this.studentId = info.studentId;
      if (!this.studentId) {
        this.$message.error("请重新登录");
        this.$router.push("/login");
      }
    },
    getFileList() {
      this.loading = true;
      listStudentFiles(this.studentId)
        .then(res => {
          this.fileList = res.data || [];
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    handleSearch() {},
    toggleGroup(title) {
      this.$set(this.expandedGroups, title, !this.expandedGroups[title]);
    },
    getDisplayName(file) {
      if (!file.fileName) return '-';
      const name = file.fileName;
      // Try to strip the prefix pattern "202605071635_account_" to show cleaner name
      const match = name.match(/^\d{12}_.+?_(.+)$/);
      return match ? match[1] : name;
    },
    getFileIcon(file) {
      if (!file.fileName) return "el-icon-document";
      const ext = file.fileName.split('.').pop().toLowerCase();
      const map = {
        doc: "el-icon-document", docx: "el-icon-document",
        pdf: "el-icon-document-copy",
        xls: "el-icon-s-data", xlsx: "el-icon-s-data",
        ppt: "el-icon-present", pptx: "el-icon-present",
        jpg: "el-icon-picture-outline", jpeg: "el-icon-picture-outline",
        png: "el-icon-picture-outline", gif: "el-icon-picture-outline",
        bmp: "el-icon-picture-outline", webp: "el-icon-picture-outline",
        txt: "el-icon-tickets", log: "el-icon-tickets",
        md: "el-icon-tickets", json: "el-icon-tickets",
        zip: "el-icon-files", rar: "el-icon-files", "7z": "el-icon-files",
        mp4: "el-icon-video-camera", avi: "el-icon-video-camera",
        mp3: "el-icon-headset", wav: "el-icon-headset"
      };
      return map[ext] || "el-icon-document";
    },
    getIconBg(file) {
      if (!file.fileName) return "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
      const ext = file.fileName.split('.').pop().toLowerCase();
      if (/doc|docx/.test(ext)) return "linear-gradient(135deg, #2b7bd6 0%, #4a9ff5 100%)";
      if (/pdf/.test(ext)) return "linear-gradient(135deg, #e74c3c 0%, #f0625c 100%)";
      if (/xls|xlsx/.test(ext)) return "linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)";
      if (/ppt|pptx/.test(ext)) return "linear-gradient(135deg, #e67e22 0%, #f39c12 100%)";
      if (/jpg|jpeg|png|gif|bmp|webp/.test(ext)) return "linear-gradient(135deg, #9b59b6 0%, #c39bd3 100%)";
      if (/zip|rar|7z/.test(ext)) return "linear-gradient(135deg, #795548 0%, #a1887f 100%)";
      if (/mp4|avi|mov/.test(ext)) return "linear-gradient(135deg, #1abc9c 0%, #16a085 100%)";
      return "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
    },
    formatTime(time) {
      if (!time) return "-";
      const d = new Date(time);
      const pad = n => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    },
    handlePreview(file) {
      if (!file.fileName) return;
      const fileName = file.fileName.toLowerCase();
      const isPdf = /\.pdf$/i.test(fileName);
      const isWord = /\.(doc|docx)$/i.test(fileName);
      const isExcel = /\.(xls|xlsx)$/i.test(fileName);
      const isImage = this.isImageFile(fileName);
      const isVideo = this.isVideoFile(fileName);
      const isText = /\.(txt|log|md|json|xml|yaml|yml|js|css|html|java|py|c|cpp|h|hpp)$/i.test(fileName);

      if (isWord || isExcel || isPdf) {
        this.previewOfficeFile(file);
      } else if (isImage) {
        this.openImageViewer(file);
      } else if (isVideo) {
        this.openVideoViewer(file);
      } else if (isText) {
        previewStudentFile(file.filePath).then(blob => {
          const reader = new FileReader();
          reader.onload = (e) => {
            this.$alert(e.target.result || '(空文件)', this.getDisplayName(file), {
              confirmButtonText: '关闭',
              customClass: 'text-preview-dialog'
            });
          };
          reader.onerror = () => this.$message.error('读取文件失败');
          reader.readAsText(blob);
        }).catch(() => this.$message.error('预览失败'));
      } else {
        this.$confirm('该文件类型不支持在线预览，是否下载？', '提示', {
          confirmButtonText: '下载',
          cancelButtonText: '取消',
          type: 'info'
        }).then(() => this.handleDownload(file)).catch(() => {});
      }
    },
    fetchPreviewBlob(path) {
      return previewStudentFile(path);
    },
    getMediaCandidates() {
      return this.filteredList;
    },
    previewOfficeFile(file) {
      const ext = file.fileName.split('.').pop().toLowerCase();
      const info = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      const url = process.env.VUE_APP_BASE_API + '/student/preview?path='
        + encodeURIComponent(file.filePath)
        + '&studentId=' + this.studentId
        + '&token=' + encodeURIComponent(info.token || '');

      this.previewData = url;
      this.previewType = ext;
      this.previewTitle = this.getDisplayName(file);
      this.previewVisible = true;
    },
    handleDownload(file) {
      const info = JSON.parse(localStorage.getItem("studentInfo") || "{}");
      const url = process.env.VUE_APP_BASE_API + '/student/download?path='
        + encodeURIComponent(file.filePath)
        + '&studentId=' + this.studentId
        + '&token=' + encodeURIComponent(info.token || '');
      const link = document.createElement('a');
      link.href = url;
      link.download = this.getDisplayName(file);
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    goBack() {
      this.$router.push("/student");
    }
  }
};
</script>

<style lang="scss" scoped>
.cloud-disk-page {
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
  margin-bottom: 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

  .back-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #409eff;
    cursor: pointer;
    font-size: 15px;
    &:hover { color: #66b1ff; }
  }

  h2 {
    margin: 0;
    font-size: 22px;
    color: #333;
  }

  .file-count {
    font-size: 15px;
    color: #999;
  }
}

.search-bar {
  margin-bottom: 15px;

  ::v-deep .el-input__inner {
    border-radius: 14px;
    border: none;
    height: 48px;
    font-size: 15px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding-left: 44px;
  }
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.homework-group {
  .group-header {
    display: flex;
    align-items: center;
    gap: 10px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 14px;
    padding: 12px 18px;
    cursor: pointer;
    user-select: none;
    transition: background 0.2s;

    &:hover {
      background: rgba(255, 255, 255, 1);
    }

    .group-arrow {
      font-size: 16px;
      color: #909399;
      transition: transform 0.2s;
    }

    .group-title {
      flex: 1;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .group-count {
      font-size: 13px;
      color: #909399;
      background: #f4f4f5;
      padding: 3px 12px;
      border-radius: 10px;
    }
  }

  .group-files {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 8px;
    padding-left: 8px;
  }
}

.empty-tip {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;

  .empty-icon {
    font-size: 56px;
    color: #c0c4cc;
    margin-bottom: 15px;
  }

  p {
    margin: 0;
    font-size: 16px;
    color: #909399;
  }

  .sub-tip {
    margin-top: 8px;
    font-size: 14px;
    color: #b0b3bb;
  }
}

.file-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 14px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.25s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }
}

.file-icon {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  i {
    font-size: 26px;
    color: #fff;
  }
}

.file-info {
  flex: 1;
  min-width: 0;

  .file-name {
    font-size: 16px;
    font-weight: 500;
    color: #409eff;
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: pointer;

    &:hover {
      color: #66b1ff;
      text-decoration: underline;
    }
  }

  .file-meta {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px;

    .meta-item {
      font-size: 13px;
      color: #999;
      display: flex;
      align-items: center;
      gap: 4px;
      white-space: nowrap;

      i { font-size: 14px; }
    }
  }
}

.file-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;

  .el-button {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
}

// 图片/视频灯箱样式
.image-viewer-fade-enter-active,
.image-viewer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.image-viewer-fade-enter,
.image-viewer-fade-leave-to {
  opacity: 0;
}

.image-viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 20, 20, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.image-viewer-stage {
  max-width: 72vw;
  max-height: 82vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-viewer-img {
  max-width: 72vw;
  max-height: 82vh;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 12px 38px rgba(0, 0, 0, 0.35);
  &.is-draggable { cursor: grab; user-select: none; }
  &.is-dragging { cursor: grabbing; }
}
.image-viewer-loading {
  color: #fff;
  font-size: 34px;
}
.image-viewer-error {
  color: #fff;
  font-size: 14px;
  padding: 16px 24px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.36);
}
.image-viewer-close {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.36);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.image-viewer-reset {
  position: absolute;
  top: 18px;
  right: 62px;
  height: 34px;
  border: none;
  border-radius: 17px;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.36);
  padding: 0 12px;
  font-size: 13px;
}
.image-viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.34);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.image-viewer-nav-prev { left: 18px; }
.image-viewer-nav-next { right: 18px; }
.image-viewer-index {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.42);
  border-radius: 18px;
  padding: 6px 12px;
  max-width: calc(100vw - 48px);
  display: flex;
  gap: 8px;
  align-items: center;
}
.image-viewer-name {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.9;
}

.video-viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 20, 20, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.video-viewer-stage {
  max-width: 78vw;
  max-height: 84vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.video-viewer-video {
  max-width: 78vw;
  max-height: 84vh;
  border-radius: 6px;
  background: #000;
  box-shadow: 0 12px 38px rgba(0, 0, 0, 0.35);
  outline: none;
}
.video-viewer-close {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.36);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.video-viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.34);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.video-viewer-nav-prev { left: 18px; }
.video-viewer-nav-next { right: 18px; }
.video-viewer-index {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.42);
  border-radius: 18px;
  padding: 6px 12px;
  max-width: calc(100vw - 48px);
  display: flex;
  gap: 8px;
  align-items: center;
}

// 响应式
@media (max-width: 600px) {
  .cloud-disk-page { padding: 12px; }

  .page-header {
    padding: 14px 18px;
    h2 { font-size: 19px; }
    .file-count { font-size: 13px; }
  }

  .file-card {
    padding: 14px 16px;
    gap: 12px;
  }

  .file-icon {
    width: 42px;
    height: 42px;
    i { font-size: 22px; }
  }

  .file-actions {
    .el-button { width: 36px; height: 36px; font-size: 14px; }
  }

  .file-meta {
    gap: 10px !important;
    .meta-item { font-size: 12px; }
  }
}
</style>
