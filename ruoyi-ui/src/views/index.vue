<template>
  <div class="app-container home">
    <!-- 顶部：智能搜索模块 -->
    <el-card shadow="hover" class="search-card">
      <el-row :gutter="20" type="flex" align="middle">
        <el-col :span="16">
          <div class="search-wrapper">
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入文件名进行搜索..."
              clearable
              @keyup.enter.native="handleSearch"
              prefix-icon="el-icon-search"
              size="medium"
              class="search-input"
            />
            <div class="search-actions">
              <el-button type="primary" size="medium" icon="el-icon-search" @click="handleSearch">搜全部</el-button>
              <el-button type="success" size="medium" icon="el-icon-document" @click="handleFastSearch">搜文件</el-button>
              <el-button type="info" size="medium" icon="el-icon-tickets" @click="handleContentSearch">穿透搜索</el-button>
              <el-button v-hasRole="['admin']" type="warning" size="medium" icon="el-icon-refresh" @click="handleRebuildIndex" title="重建索引后穿透搜索才能正常工作">重建索引</el-button>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <span class="tips"><i class="el-icon-info"></i> 支持 Word、PDF、Excel、TXT 深度内容搜索</span>
        </el-col>
      </el-row>
    </el-card>

    <!-- 主体：文件管理模块 -->
    <!-- 注意：此处加上了 type="flex" 使左右两侧自适应等高 -->
    <el-row :gutter="20" class="main-content" type="flex">

      <!-- 左侧：动态目录树 -->
      <el-col :span="5" class="tree-col">
        <el-card shadow="never" class="tree-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-folder-opened"></i> 我的云盘目录</span>
          </div>
          <!-- 树容器，超出高度会自动出现内滚动条 -->
          <el-tree
            ref="folderTree"
            :data="folderTree"
            :props="defaultProps"
            node-key="id"
            @node-click="handleNodeClick"
            @node-expand="handleNodeExpand"
            highlight-current
            :load="loadNode"
            lazy
            :expand-on-click-node="false"
          >
            <span class="custom-tree-node" slot-scope="{ node }">
              <i :class="node.expanded ? 'el-icon-folder-opened' : 'el-icon-folder'" style="color: #E6A23C; margin-right: 5px;"></i>
              <span>{{ node.label }}</span>
            </span>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：文件展示区 / 搜索结果展示区 -->
      <el-col :span="19" class="file-col">
        <el-card shadow="never" class="file-card">
          <!-- 顶部面包屑或搜索提示 (不参与滚动) -->
          <div slot="header" class="file-header">
            <el-breadcrumb separator-class="el-icon-arrow-right" v-if="!isSearching">
              <el-breadcrumb-item @click.native="handleBreadcrumbClick('/')">/</el-breadcrumb-item>
              <el-breadcrumb-item 
                v-for="(item, index) in breadcrumbItems" 
                :key="index"
                :class="{ 'is-link': item.path }"
                @click.native="item.path && handleBreadcrumbClick(item.path)"
              >{{ item.label }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div v-else class="search-title">
              包含关键词 <span class="highlight-text">"{{ queryParams.keyword }}"</span> 的搜索结果：
              <el-button type="text" @click="exitSearch" icon="el-icon-back">返回目录</el-button>
            </div>

            <div class="actions" v-if="!isSearching">
              <el-button type="warning" size="mini" icon="el-icon-refresh" @click="handleRefresh" :loading="isRefreshing">刷新</el-button>
              <el-button type="primary" size="mini" icon="el-icon-upload" @click="openUploadDialog">上传文件</el-button>
              <el-button type="success" size="mini" icon="el-icon-folder-add" @click="handleCreateFolder">新建文件夹</el-button>
              <el-button 
                v-if="!isMoveMode && selectedFiles.length > 0"
                type="info" 
                size="mini" 
                icon="el-icon-rank" 
                @click="startMoveMode"
              >移动选中 ({{ selectedFiles.length }})</el-button>
              <el-button 
                v-if="!isMoveMode && selectedFiles.length > 0"
                type="danger" 
                size="mini" 
                icon="el-icon-delete" 
                @click="handleBatchDelete"
              >删除选中 ({{ selectedFiles.length }})</el-button>
              <template v-if="isMoveMode">
                <el-button type="primary" size="mini" icon="el-icon-check" @click="confirmMove">确定移动至 "{{ currentDirPath }}"</el-button>
                <el-button type="default" size="mini" icon="el-icon-close" @click="cancelMoveMode">取消</el-button>
                <span class="move-hint">已选择 {{ selectedFiles.length }} 个文件/文件夹</span>
              </template>
            </div>
          </div>

          <!-- 移动模式提示 -->
          <div v-if="isMoveMode" class="move-mode-panel">
            <div class="move-mode-title">
              <i class="el-icon-info"></i>
              移动模式：已选择 {{ selectedFiles.length }} 个文件/文件夹
            </div>
            <div class="move-mode-files">
              <el-tag v-for="file in selectedFiles" :key="file.path" size="small" class="move-file-tag">
                {{ file.fileName }}
              </el-tag>
            </div>
            <div class="move-mode-target">
              目标目录：<strong>{{ currentDirPath }}</strong>
            </div>
          </div>

          <!-- 列表视图：height="100%" 会让表头固定，表体内部自动滚动 -->
          <div class="table-container file-list-container" ref="tableContainer">
            <el-table
              ref="fileTable"
              v-loading="loading"
              :data="isSearching ? searchResults : tableDataWithLoader"
              style="width: 100%"
              height="100%"
              row-key="path"
              @selection-change="handleSelectionChange"
              :row-class-name="tableRowClassName"
            >
              <el-table-column
                v-if="!isMoveMode"
                type="selection"
                width="55"
                :selectable="(row) => !row._isLoaderRow && !row._isLoadingItem"
              ></el-table-column>
              <el-table-column prop="fileName" label="文件名/搜索摘要" min-width="300">
                <template slot-scope="scope">
                  <!-- 加载状态行 -->
                  <div v-if="scope.row._isLoaderRow" class="loader-row">
                    <div v-if="isLoadingMore" class="loader-loading">
                      <i class="el-icon-loading"></i>
                      <span>正在加载更多...</span>
                    </div>
                    <div v-else-if="hasMore && fileList.length < totalCount" class="loader-more">
                      <i class="el-icon-arrow-down"></i>
                      <span>向下滚动加载更多</span>
                      <span class="loader-count">(已加载 {{ fileList.length }} 项)</span>
                    </div>
                    <div v-else class="loader-done">
                      <i class="el-icon-check"></i>
                      <span>已全部加载</span>
                      <span class="loader-count">(共 {{ fileList.length }} 项)</span>
                    </div>
                  </div>
                  <!-- 搜索加载中提示 -->
                  <div v-else-if="scope.row._isLoadingItem" class="loading-item-row">
                    <i class="el-icon-loading"></i>
                    <span>{{ scope.row._loadingText }}</span>
                  </div>
                  <!-- 普通文件行 -->
                  <div
                    v-else
                    class="file-name-cell"
                    :class="{ 'is-folder': scope.row.isDirectory, 'is-dragging': dragSource && dragSource.path === scope.row.path, 'is-drag-over': dragOverRow && dragOverRow.path === scope.row.path && scope.row.isDirectory }"
                    @click="handleFileNameClick(scope.row)"
                    draggable="true"
                    @dragstart="handleDragStart($event, scope.row)"
                    @dragover="handleDragOver($event, scope.row)"
                    @dragleave="handleDragLeave($event, scope.row)"
                    @drop="handleDrop($event, scope.row)"
                    @dragend="handleDragEnd($event)"
                  >
                    <i v-if="scope.row.isDirectory" class="el-icon-folder file-icon file-folder"></i>
                    <div v-else class="file-icon file-type-icon-wrap">
                      <svg-icon :icon-class="getFileTypeMeta(scope.row.fileName).iconClass" class-name="file-type-svg" />
                      <span v-if="getFileTypeMeta(scope.row.fileName).showExtTag" class="file-ext-mini">{{ getFileTypeMeta(scope.row.fileName).extTag }}</span>
                    </div>
                    <div class="file-info">
                      <span class="file-name">{{ scope.row.fileName }}</span>
                      <div v-if="isSearching && scope.row.summary" class="search-summary" v-html="scope.row.summary"></div>
                      <div v-if="isSearching" class="file-path-hint">路径: {{ scope.row.path }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="fileSize" label="大小" width="120" v-if="!isSearching">
                <template slot-scope="scope">
                  <span v-if="!scope.row._isLoaderRow && !scope.row.isDirectory">{{ formatSize(scope.row.fileSize) }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="updateTime" label="修改时间" width="160">
                <template slot-scope="scope">
                  <span v-if="!scope.row._isLoaderRow">{{ scope.row.updateTime }}</span>
                </template>
              </el-table-column>

              <el-table-column label="操作" min-width="280" align="center">
                <template slot-scope="scope">
                  <template v-if="!scope.row._isLoaderRow">
                    <div class="op-actions" :class="{ 'is-overflow': isActionOverflow(scope.row) }" :data-row-key="scope.row.path">
                      <div class="op-primary">
                        <el-button
                          type="text"
                          size="mini"
                          icon="el-icon-view"
                          class="op-btn"
                          @click="handlePreview(scope.row)"
                        >{{ scope.row.isDirectory ? '打开' : '预览' }}</el-button>
                        <el-button
                          type="text"
                          size="mini"
                          icon="el-icon-download"
                          class="op-btn op-collapse"
                          @click="handleDownload(scope.row)"
                          :disabled="scope.row.isDirectory"
                        >下载</el-button>
                        <el-button
                          type="text"
                          size="mini"
                          icon="el-icon-edit"
                          class="op-btn op-collapse"
                          @click="handleRename(scope.row)"
                        >重命名</el-button>
                        <el-button
                          type="text"
                          size="mini"
                          icon="el-icon-delete"
                          class="op-btn op-danger op-collapse"
                          :disabled="!scope.row.canDelete"
                          @click="handleDelete(scope.row)"
                        >删除</el-button>
                      </div>
                      <el-dropdown class="op-more" trigger="click" @command="handleActionCommand">
                        <span class="el-dropdown-link">
                          更多<i class="el-icon-arrow-down el-icon--right"></i>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                          <el-dropdown-item :command="{ action: 'download', row: scope.row }" :disabled="scope.row.isDirectory">下载</el-dropdown-item>
                          <el-dropdown-item :command="{ action: 'rename', row: scope.row }">重命名</el-dropdown-item>
                          <el-dropdown-item :command="{ action: 'delete', row: scope.row }" :disabled="!scope.row.canDelete">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </el-dropdown>
                    </div>
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 文件预览对话框 -->
    <el-dialog
      :title="previewTitle"
      :visible.sync="previewVisible"
      width="80%"
      :close-on-click-modal="false"
      custom-class="preview-dialog"
    >
      <FilePreview
        :src="previewData"
        :type="previewType"
        @rendered="handlePreviewRendered"
        @error="handlePreviewError"
      />
    </el-dialog>

    <transition name="image-viewer-fade">
      <div v-if="imageViewerVisible" class="image-viewer-overlay" @click.self="closeImageViewer">
        <button class="image-viewer-reset" type="button" @click="resetImageTransform">
          重置
        </button>

        <button class="image-viewer-close" type="button" @click="closeImageViewer">
          <i class="el-icon-close"></i>
        </button>

        <button
          v-if="imageViewerFiles.length > 1"
          class="image-viewer-nav image-viewer-nav-prev"
          type="button"
          @click.stop="showPrevImage"
        >
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

        <button
          v-if="imageViewerFiles.length > 1"
          class="image-viewer-nav image-viewer-nav-next"
          type="button"
          @click.stop="showNextImage"
        >
          <i class="el-icon-arrow-right"></i>
        </button>

        <div v-if="imageViewerFiles.length > 0" class="image-viewer-index">
          {{ imageViewerIndex + 1 }} / {{ imageViewerFiles.length }}
          <span class="image-viewer-name">{{ imageViewerCurrentFileName }}</span>
        </div>
      </div>
    </transition>

    <transition name="image-viewer-fade">
      <div v-if="videoViewerVisible" class="video-viewer-overlay" @click.self="closeVideoViewer">
        <button class="video-viewer-close" type="button" @click="closeVideoViewer">
          <i class="el-icon-close"></i>
        </button>

        <button
          v-if="videoViewerFiles.length > 1"
          class="video-viewer-nav video-viewer-nav-prev"
          type="button"
          @click.stop="showPrevVideo"
        >
          <i class="el-icon-arrow-left"></i>
        </button>

        <div class="video-viewer-stage" @click.stop @wheel.prevent="handleVideoWheel">
          <i v-if="videoViewerLoading" class="el-icon-loading image-viewer-loading"></i>
          <video
            v-else-if="videoViewerUrl"
            :src="videoViewerUrl"
            class="video-viewer-video"
            :style="videoViewerTransformStyle"
            controls
            autoplay
            preload="metadata"
            @dblclick.stop="resetVideoTransform"
          ></video>
          <div v-else class="image-viewer-error">{{ videoViewerError || '视频加载失败' }}</div>
        </div>

        <button
          v-if="videoViewerFiles.length > 1"
          class="video-viewer-nav video-viewer-nav-next"
          type="button"
          @click.stop="showNextVideo"
        >
          <i class="el-icon-arrow-right"></i>
        </button>

        <div v-if="videoViewerFiles.length > 0" class="video-viewer-index">
          {{ videoViewerIndex + 1 }} / {{ videoViewerFiles.length }}
          <span class="image-viewer-name">{{ videoViewerCurrentFileName }}</span>
        </div>
      </div>
    </transition>

    <el-dialog
      title="上传文件"
      :visible.sync="uploadDialogVisible"
      width="620px"
      top="8vh"
      custom-class="upload-file-dialog"
      append-to-body
      :before-close="beforeUploadDialogClose"
      @close="handleUploadDialogClose"
    >
      <div class="upload-dialog-content">
        <div class="upload-dialog-path">当前目录：{{ currentDirPath || '/' }}</div>
        <div v-if="uploadPendingCount > 0" class="uploading-status">
          <i class="el-icon-loading"></i>
          正在上传 {{ uploadPendingCount }} / {{ uploadTotalCount }} 个文件...
        </div>
        <el-upload
          ref="dialogUploader"
          class="upload-dialog-uploader"
          drag
          action=""
          :auto-upload="false"
          :before-upload="beforeUpload"
          :show-file-list="true"
          :file-list="uploadFileList"
          :on-change="handleUploadFileChange"
          :on-remove="handleUploadFileRemove"
          :multiple="true"
        >
          <div class="upload-drop-content" @click.stop.prevent>
            <div class="el-upload__text">将文件拖拽至此区域</div>
            <div class="el-upload__tip upload-or-tip">-或-</div>
            <el-button type="primary" size="medium" class="upload-trigger-btn" @click.stop.prevent="triggerUploadSelect">上传文件</el-button>
            <div class="el-upload__tip upload-limit-tip">文件大小限制{{ maxUploadSizeMB }}MB</div>
          </div>
        </el-upload>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button :disabled="uploadPendingCount > 0" @click="uploadDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="uploadSubmitting" :disabled="uploadFileList.length === 0 || uploadPendingCount > 0" @click="submitChunkUpload">确 定 上 传</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listDir } from "@/api/system/dir"
import { listFiles, downloadFile, previewFile, previewTextFile, deleteFile, getPreviewUrl, refreshCache, warmupCache, createFolder, renameFile, moveFile, initChunkUpload, uploadChunk, completeChunkUpload, getChunkTransferStatus, cancelChunkUpload, searchFiles, searchFilesByName, searchContent, rebuildSearchIndex } from "@/api/tool/file";
import FilePreview from "@/components/FilePreview";

export default {
  name: "Index",
  components: {
    FilePreview
  },
  data() {
    return {
      loading: false,
      isSearching: false,
      isLoadingMore: false,
      isRefreshing: false,
      hasMore: true,
      totalCount: 0,

      queryParams: {
        keyword: "",
        folderId: null,
        pageNum: 1,
        pageSize: 20   // 每页10条，滚动加载
      },

      currentPath: [],
      currentDirPath: "/",

      defaultProps: {
        children: 'children',
        label: 'label',
        isLeaf: 'leaf'
      },

      // 目录树数据
      folderTree: [],

      // 文件列表数据
      fileList: [],
      searchResults: [],
      
      // 批量选择
      selectedFiles: [],
      
      // 移动模式
      isMoveMode: false,
      moveTargetDir: null,

      // 文件预览
      previewVisible: false,
      previewData: null,
      previewTitle: '',
      previewType: '',

      // 图片灯箱预览
      imageViewerVisible: false,
      imageViewerFiles: [],
      imageViewerIndex: 0,
      imageViewerUrl: '',
      imageViewerLoading: false,
      imageViewerError: '',
      imageViewerScale: 1,
      imageViewerOffsetX: 0,
      imageViewerOffsetY: 0,
      imageViewerDragging: false,
      imageViewerDragStartX: 0,
      imageViewerDragStartY: 0,
      imageViewerStartOffsetX: 0,
      imageViewerStartOffsetY: 0,

      // 视频灯箱预览
      videoViewerVisible: false,
      videoViewerFiles: [],
      videoViewerIndex: 0,
      videoViewerUrl: '',
      videoViewerLoading: false,
      videoViewerError: '',
      videoViewerScale: 1,

      // 拖动状态
      dragSource: null,
      dragOverRow: null,
      isBatchDrag: false,

      // 滚动容器引用
      scrollWrapper: null,

      // 上传弹窗状态
      uploadDialogVisible: false,
      maxUploadSizeMB: 400,
      uploadPendingCount: 0,
      uploadTotalCount: 0,
      uploadSubmitting: false,
      uploadFileList: [],
      uploadRefreshTimer: null,
      transferTaskList: [],
      transferStatusTimer: null,

      // 操作列溢出状态
      actionOverflowMap: {},
      actionOverflowTimer: null
    };
  },
  computed: {
    // 带有加载状态行的表格数据
    tableDataWithLoader() {
      if (this.isSearching || this.fileList.length === 0) {
        return this.fileList;
      }
      // 在数据末尾添加一个加载状态行
      const loaderRow = {
        fileId: '__loader_row__',
        _isLoaderRow: true
      };
      return [...this.fileList, loaderRow];
    },
    
    // 面包屑导航项
    breadcrumbItems() {
      const items = [];
      const pathParts = this.currentDirPath.split('/').filter(p => p);
      let currentPath = '';
      
      pathParts.forEach((part, index) => {
        currentPath += '/' + part;
        // 最后一项不可点击
        const isLast = index === pathParts.length - 1;
        items.push({
          label: part,
          path: isLast ? null : currentPath
        });
      });
      
      return items;
    },

    imageViewerCurrentFileName() {
      const currentFile = this.imageViewerFiles[this.imageViewerIndex];
      return currentFile ? currentFile.fileName : '';
    },

    videoViewerCurrentFileName() {
      const currentFile = this.videoViewerFiles[this.videoViewerIndex];
      return currentFile ? currentFile.fileName : '';
    },

    videoViewerTransformStyle() {
      return {
        transform: `scale(${this.videoViewerScale})`
      };
    },

    imageViewerTransformStyle() {
      return {
        transform: `translate(${this.imageViewerOffsetX}px, ${this.imageViewerOffsetY}px) scale(${this.imageViewerScale})`
      };
    }
  },
  created() {
    this.getDirTree();
    // 首次加载根目录
    this.getList();
  },
  mounted() {
    this.$nextTick(() => {
      this.bindScrollEvent();
      this.updateActionOverflowState();
      // 等文件列表加载完再展开目录树，避免同时请求后端
      this.expandFirstLevel();
    });
    window.addEventListener('resize', this.handleWindowResize);
    
    // 监听预览窗口的下载消息
    window.addEventListener('message', this.handlePreviewMessage);
    window.addEventListener('keydown', this.handleImageViewerKeydown);
    window.addEventListener('mousemove', this.handleImageDragMove);
    window.addEventListener('mouseup', this.endImageDrag);
    window.addEventListener('wheel', this.handleImageViewerGlobalWheel, { passive: false });
  },

  updated() {
    this.scheduleActionOverflowCalc();
  },
  beforeDestroy() {
    // 组件销毁前解绑滚动事件
    this.unbindScrollEvent();
    window.removeEventListener('resize', this.handleWindowResize);
    window.removeEventListener('message', this.handlePreviewMessage);
    window.removeEventListener('keydown', this.handleImageViewerKeydown);
    window.removeEventListener('mousemove', this.handleImageDragMove);
    window.removeEventListener('mouseup', this.endImageDrag);
    window.removeEventListener('wheel', this.handleImageViewerGlobalWheel);
    this.clearImageViewerBlobUrls();
    this.clearVideoViewerBlobUrls();
    if (this.actionOverflowTimer) {
      clearTimeout(this.actionOverflowTimer);
      this.actionOverflowTimer = null;
    }
    if (this.uploadRefreshTimer) {
      clearTimeout(this.uploadRefreshTimer);
      this.uploadRefreshTimer = null;
    }
    if (this.transferStatusTimer) {
      clearTimeout(this.transferStatusTimer);
      this.transferStatusTimer = null;
    }
  },
  methods: {
    // 绑定表格滚动事件
    bindScrollEvent() {
      const tableRef = this.$refs.fileTable;
      if (!tableRef) return;

      // el-table 内部的滚动容器
      const scrollWrapper = tableRef.$el.querySelector('.el-table__body-wrapper');
      if (scrollWrapper) {
        this.scrollWrapper = scrollWrapper;
        scrollWrapper.addEventListener('scroll', this.handleTableScroll);
      }
    },
    // 解绑表格滚动事件
    unbindScrollEvent() {
      if (this.scrollWrapper) {
        this.scrollWrapper.removeEventListener('scroll', this.handleTableScroll);
        this.scrollWrapper = null;
      }
    },
    handleWindowResize() {
      this.scheduleActionOverflowCalc();
    },
    scheduleActionOverflowCalc() {
      if (this.actionOverflowTimer) {
        clearTimeout(this.actionOverflowTimer);
      }
      this.actionOverflowTimer = setTimeout(() => {
        this.updateActionOverflowState();
      }, 16);
    },
    updateActionOverflowState() {
      this.$nextTick(() => {
        const rows = this.$el.querySelectorAll('.op-actions[data-row-key]');
        const nextMap = {};
        rows.forEach((rowEl) => {
          const rowKey = rowEl.getAttribute('data-row-key');
          const primaryEl = rowEl.querySelector('.op-primary');
          if (!rowKey || !primaryEl) {
            return;
          }
          nextMap[rowKey] = primaryEl.scrollWidth > rowEl.clientWidth;
        });
        this.actionOverflowMap = nextMap;
      });
    },
    isActionOverflow(row) {
      if (!row || !row.path) {
        return false;
      }
      return this.actionOverflowMap[row.path] === true;
    },
    handleSearch() {
      const keyword = this.queryParams.keyword.trim();
      if (!keyword) {
        this.$message.warning("请输入检索关键词");
        return;
      }
      
      this.isSearching = true;
      // 搜索时不显示 loading，用骨架屏代替
      this.loading = false;
      this.searchResults = [];
      
      // 先搜索当前目录（第一层），快速显示结果
      this.searchCurrentDir(keyword);
    },

    // 快速搜文件（基于 Lucene 索引，仅文件，毫秒级）
    handleFastSearch() {
      const keyword = this.queryParams.keyword.trim();
      if (!keyword) {
        this.$message.warning('请输入检索关键词');
        return;
      }
      this.isSearching = true;
      this.loading = false;
      this.searchResults = [{ _isLoadingItem: true, _loadingText: '正在搜索...' }];

      searchFilesByName(keyword, this.currentDirPath, 200).then(response => {
        const files = response.data || [];
        this.searchResults = files.map(file => ({
          fileId: file.path,
          fileName: file.name || file.fileName,
          updateTime: file.lastModified || file.updateTime,
          path: file.path,
          canDelete: true,
          isDirectory: false,
          summary: this.highlightKeyword(file.name || file.fileName, keyword)
        }));
        if (files.length === 0) {
          this.$message.info('未找到匹配的文件（索引中无此文件，试试搜全部）');
        }
      }).catch(error => {
        this.$message.error('搜索失败：' + (error.message || '未知错误'));
        this.searchResults = [];
      });
    },

    // 穿透搜索（内容搜索）
    handleContentSearch() {
      const keyword = this.queryParams.keyword.trim();
      if (!keyword) {
        this.$message.warning("请输入搜索关键词");
        return;
      }
      
      this.isSearching = true;
      this.loading = false;
      this.searchResults = [];
      
      // 显示加载提示
      this.searchResults = [{ _isLoadingItem: true, _loadingText: '正在搜索文档内容...' }];
      
      // 调用穿透搜索接口
      searchContent(keyword, this.currentDirPath, 50).then(response => {
        const results = response.data || [];
        
        if (results.length > 0) {
          // 显示搜索结果
          this.searchResults = results.map(item => ({
            fileId: item.path,
            fileName: item.fileName,
            updateTime: new Date(item.updateTime).toISOString(),
            path: item.path,
            canDelete: true,
            isDirectory: false,
            summary: this.highlightKeyword(item.contentSnippet, keyword),
            isContentSearch: true
          }));
          this.$message.success(`找到 ${results.length} 个匹配文档`);
        } else {
          this.searchResults = [];
          this.$message.info("未找到包含该关键词的文档");
        }
      }).catch(error => {
        this.$message.error("搜索失败：" + (error.message || "未知错误"));
        this.searchResults = [];
      });
    },
    
    // 重建索引
    handleRebuildIndex() {
      this.$confirm('重建索引会扫描当前目录及子目录下的所有文档，这可能需要一些时间。是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          text: '正在重建索引，请稍候...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
        
        rebuildSearchIndex(this.currentDirPath).then(response => {
          loading.close();
          this.$message.success(response.msg || '索引重建任务已启动');
        }).catch(error => {
          loading.close();
          this.$message.error('重建索引失败：' + (error.message || '未知错误'));
        });
      }).catch(() => {});
    },
    
    // 搜索当前目录
    searchCurrentDir(keyword) {
      // 先显示一个加载提示
      this.searchResults = [{ _isLoadingItem: true, _loadingText: '正在搜索...' }];
      
      searchFiles(keyword, this.currentDirPath, 1, 50).then(response => {
        const files = response.data || [];
        
        if (files.length > 0) {
          // 立即显示第一层结果
          const newResults = files.map(file => ({
            fileId: file.fileId || file.id,
            fileName: file.name || file.fileName,
            updateTime: file.updateTime,
            path: file.path || this.currentDirPath,
            canDelete: file.canDelete !== false,
            isDirectory: file.directory || file.isDirectory,
            summary: this.highlightKeyword(file.name || file.fileName, keyword)
          }));
          this.searchResults = newResults;
        } else {
          this.searchResults = [];
        }
        
        // 然后异步搜索子目录
        this.searchSubDirs(keyword, 2);
      }).catch(error => {
        this.$message.error("搜索失败：" + (error.message || "未知错误"));
        this.searchResults = [];
      });
    },
    
    // 搜索子目录
    async searchSubDirs(keyword, depth) {
      // 最多搜索5层
      if (depth > 5) {
        if (this.searchResults.length === 0) {
          this.$message.info("未找到匹配的文件");
        }
        return;
      }
      
      // 添加一个加载中的占位项，显示正在搜索
      const loadingItem = { 
        _isLoadingItem: true, 
        _loadingText: '正在搜索...' 
      };
      this.searchResults.push(loadingItem);
      
      try {
        const response = await searchFiles(keyword, this.currentDirPath, depth, 50);
        const files = response.data || [];
        
        // 移除加载占位项
        this.searchResults = this.searchResults.filter(r => !r._isLoadingItem);
        
        if (files.length > 0) {
          const newResults = files.map(file => ({
            fileId: file.fileId || file.id,
            fileName: file.name || file.fileName,
            updateTime: file.updateTime,
            path: file.path || this.currentDirPath,
            canDelete: file.canDelete !== false,
            isDirectory: file.directory || file.isDirectory,
            summary: this.highlightKeyword(file.name || file.fileName, keyword)
          }));
          
          // 追加结果
          this.searchResults = [...this.searchResults, ...newResults];
        }
        
        // 继续搜索下一层
        this.searchSubDirs(keyword, depth + 1);
      } catch (error) {
        // 移除加载占位项
        this.searchResults = this.searchResults.filter(r => !r._isLoadingItem);
        // 子目录搜索失败不影响整体
      }
    },
    
    // 高亮显示关键词
    highlightKeyword(text, keyword) {
      if (!text || !keyword) return text;
      const regex = new RegExp(`(${keyword})`, 'gi');
      return text.replace(regex, '<span style="color: #F56C6C; font-weight: bold; background: #fdf2f2; padding: 0 4px;">$1</span>');
    },

    exitSearch() {
      this.isSearching = false;
      this.queryParams.keyword = "";
      this.searchResults = [];
      this.getList();
    },

    // 手动刷新当前目录
    handleRefresh() {
      this.isRefreshing = true;
      refreshCache(this.currentDirPath).then(() => {
        this.$modal.msgSuccess("刷新成功");
        this.getList();
      }).catch(() => {
        this.$message.error("刷新失败");
      }).finally(() => {
        this.isRefreshing = false;
      });
    },

    // 懒加载目录树节点
    loadNode(node, resolve) {
      // 根节点，先显示根目录节点
      if (node.level === 0) {
        return resolve([{
          id: '/',
          label: '/',
          path: '/',
          leaf: false
        }]);
      }

      // 加载子目录
      const path = node.data.path;
      listFiles(path).then(response => {
        const files = response.data || [];
        // 只取目录（兼容 directory 和 isDirectory 两种字段名）
        const dirs = files.filter(f => f.directory === true || f.isDirectory === true);
        const nodes = dirs.map(dir => ({
          id: dir.path,
          label: dir.name,
          path: dir.path,
          leaf: false
        }));
        resolve(nodes);
      }).catch(err => {
        resolve([]);
      });
    },

    // 默认展开第一层目录
    expandFirstLevel() {
      // 等待树节点渲染完成
      setTimeout(() => {
        const tree = this.$refs.folderTree;
        if (tree) {
          // 获取根节点（level 0 的节点）
          const rootNode = tree.getNode('/');
          if (rootNode) {
            // 懒加载模式下需要先加载子节点再展开
            if (!rootNode.loaded) {
              // 手动触发加载
              rootNode.loadData(() => {
                rootNode.expanded = true;
              });
            } else if (!rootNode.expanded) {
              rootNode.expanded = true;
            }
          }
        }
      }, 200);
    },

    // 节点展开时 - 异步预热子目录缓存
    handleNodeExpand(data) {
      // 用户展开目录时，异步预热该目录的文件列表
      // 这样用户点击进入时，数据已在缓存中，加载更快
      if (data.path) {
        warmupCache(data.path).catch(() => {
          // 忽略错误，预热失败不影响正常使用
        });
      }
    },

    // 预热根目录缓存
    warmupRootCache() {
      // 静默调用预热接口，不阻塞页面加载
      warmupCache('/').catch(() => {
        // 忽略错误，预热失败不影响正常使用
      });
    },

    // 获取目录树（保留用于兼容，但实际使用懒加载）
    getDirTree() {
      // 懒加载模式下不需要预加载
      this.folderTree = [];
    },

    handleNodeClick(data) {
      this.isSearching = false;
      this.queryParams.folderId = data.id;
      this.currentDirPath = data.path || "/";
      this.currentPath = this.getPathArray(data);
      // 重置分页状态
      this.queryParams.pageNum = 1;
      this.fileList = [];
      this.totalCount = 0;
      this.hasMore = true;
      this.getList();
    },

    // 获取路径数组
    getPathArray(node) {
      const paths = [];
      let current = node;
      while (current) {
        paths.unshift(current.label);
        // 在树中查找父节点
        current = this.findParent(this.folderTree, current.id);
      }
      return paths;
    },

    // 处理面包屑点击
    handleBreadcrumbClick(path) {
      this.isSearching = false;
      this.currentDirPath = path;
      // 重置分页状态
      this.queryParams.pageNum = 1;
      this.fileList = [];
      this.totalCount = 0;
      this.hasMore = true;
      this.getList();
      // 同步更新目录树选中状态
      this.syncTreeSelection(path);
    },

    // 同步目录树选中状态
    syncTreeSelection(path) {
      // 在树中查找对应路径的节点
      const findNodeByPath = (nodes, targetPath) => {
        for (const node of nodes) {
          if (node.path === targetPath) {
            return node;
          }
          if (node.children) {
            const found = findNodeByPath(node.children, targetPath);
            if (found) return found;
          }
        }
        return null;
      };
      
      const node = findNodeByPath(this.folderTree, path);
      if (node && this.$refs.folderTree) {
        this.$refs.folderTree.setCurrentKey(node.id);
      }
    },

    // 查找父节点
    findParent(tree, nodeId) {
      for (const node of tree) {
        if (node.children) {
          for (const child of node.children) {
            if (child.id === nodeId) {
              return node;
            }
          }
          const found = this.findParent(node.children, nodeId);
          if (found) return found;
        }
      }
      return null;
    },

    getList(isLoadMore = false) {
      if (isLoadMore) {
        this.isLoadingMore = true;
      } else {
        this.loading = true;
        this.queryParams.pageNum = 1;
        this.fileList = [];
        this.totalCount = 0;
        this.hasMore = true;
      }

      // 正常加载，使用缓存提高性能
      listFiles(this.currentDirPath, this.queryParams.pageNum, this.queryParams.pageSize, false).then(response => {
        const files = response.data || [];

        // 保存分页信息
        this.totalCount = response.total || 0;
        this.hasMore = response.hasMore !== false;

        // 映射数据并去重（根据 path）
        const seenPaths = new Set();
        const mappedFiles = files.map(file => {
          const isDir = file.directory === true;
          return {
            fileId: file.path,
            fileName: file.name,
            fileSize: file.size,
            updateTime: this.parseTime(file.lastModified),
            path: file.path,
            isDirectory: isDir,
            canDelete: true,
            sortWeight: isDir ? 0 : 1
          };
        }).filter(file => {
          if (seenPaths.has(file.path)) {
            return false;
          }
          seenPaths.add(file.path);
          return true;
        });

        if (isLoadMore) {
          // 追加数据
          this.fileList = [...this.fileList, ...mappedFiles];
          this.isLoadingMore = false;
        } else {
          // 首次加载
          this.fileList = mappedFiles;
          this.loading = false;
        }
      }).catch(() => {
        this.loading = false;
        this.isLoadingMore = false;
      });
    },

    // 加载更多
    loadMore() {
      if (this.isLoadingMore || !this.hasMore || this.isSearching) {
        return;
      }
      this.queryParams.pageNum++;
      this.getList(true);
    },

    // 表格滚动处理（带节流）
    handleTableScroll(event) {
      // 节流：150ms内只处理一次，平衡响应速度和性能
      if (this.scrollTimer) {
        return;
      }

      const target = event.target;
      const scrollHeight = target.scrollHeight || 0;
      const scrollTop = target.scrollTop || 0;
      const clientHeight = target.clientHeight || 0;
      const distanceToBottom = scrollHeight - scrollTop - clientHeight;

      // 提前触发加载：距离底部400px时就开始加载，用户无感知等待
      if (distanceToBottom < 400 && distanceToBottom >= 0) {
        this.loadMore();
      }

      // 设置节流定时器
      this.scrollTimer = setTimeout(() => {
        this.scrollTimer = null;
      }, 150);
    },

    formatSize(size) {
      if (!size) return "0 B";
      const unitArr = ["B", "KB", "MB", "GB"];
      let index = 0;
      let srcsize = parseFloat(size);
      index = Math.floor(Math.log(srcsize) / Math.log(1024));
      let sizeText = srcsize / Math.pow(1024, index);
      return sizeText.toFixed(2) + " " + unitArr[index];
    },

    getFileExtension(fileName) {
      if (!fileName || typeof fileName !== 'string') {
        return '';
      }
      const fileNameLower = fileName.toLowerCase();
      const index = fileNameLower.lastIndexOf('.');
      if (index < 0 || index === fileNameLower.length - 1) {
        return '';
      }
      return fileNameLower.substring(index + 1);
    },

    getFileTypeMeta(fileName) {
      const extension = this.getFileExtension(fileName);
      const typeMap = {
        word: ['doc', 'docx', 'wps', 'odt', 'rtf'],
        excel: ['xls', 'xlsx', 'csv', 'ods', 'numbers'],
        ppt: ['ppt', 'pptx', 'pps', 'ppsx', 'odp', 'key'],
        pdf: ['pdf'],
        zip: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz', 'tgz', 'iso'],
        image: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'tif', 'tiff', 'ico', 'heic'],
        video: ['mp4', 'avi', 'mov', 'mkv', 'rmvb', 'flv', 'wmv', 'm4v', 'webm'],
        audio: ['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a', 'wma', 'amr'],
        code: ['java', 'js', 'ts', 'vue', 'jsx', 'tsx', 'py', 'c', 'cpp', 'h', 'hpp', 'cs', 'go', 'rs', 'php', 'rb', 'swift', 'kt', 'html', 'htm', 'css', 'scss', 'less', 'xml', 'json', 'yml', 'yaml', 'sql', 'sh', 'bat', 'ps1', 'cmd'],
        text: ['txt', 'md', 'log', 'ini', 'conf', 'properties'],
        cad: ['dwg', 'dxf'],
        font: ['ttf', 'otf', 'woff', 'woff2'],
        db: ['db', 'sqlite', 'sqlitedb', 'mdb', 'accdb'],
        exe: ['exe', 'msi', 'apk', 'ipa', 'dmg']
      };

      const typeIconMap = {
        word: 'file-office-word',
        excel: 'file-office-excel',
        ppt: 'file-office-ppt',
        pdf: 'file-office-pdf',
        zip: 'file-win-zip',
        image: 'file-image',
        video: 'file-video',
        audio: 'file-audio',
        code: 'file-code',
        text: 'file-text',
        cad: 'file-code',
        font: 'file-text',
        db: 'file-code',
        exe: 'file-generic',
        unknown: 'file-generic'
      };

      const matchedType = Object.keys(typeMap).find(type => typeMap[type].includes(extension));
      if (!matchedType) {
        return {
          iconClass: typeIconMap.unknown,
          showExtTag: !!extension,
          extTag: extension ? extension.toUpperCase().slice(0, 4) : ''
        };
      }

      return {
        iconClass: typeIconMap[matchedType] || typeIconMap.unknown,
        showExtTag: false,
        extTag: ''
      };
    },

    handleActionCommand(command) {
      if (!command || !command.row) {
        return;
      }
      const { action, row } = command;
      if (action === 'download') {
        this.handleDownload(row);
      } else if (action === 'rename') {
        this.handleRename(row);
      } else if (action === 'delete') {
        this.handleDelete(row);
      }
    },
    async handlePreviewBlobError(blob) {
      if (!(blob instanceof Blob) || blob.size === 0) {
        this.$message.error("预览内容为空");
        return true;
      }
      const contentType = (blob.type || '').toLowerCase();
      if (!contentType.includes('application/json')) {
        return false;
      }
      try {
        const text = await blob.text();
        const result = JSON.parse(text);
        this.$message.error(result.msg || "预览失败");
      } catch (error) {
        this.$message.error("预览失败");
      }
      return true;
    },

    isImageFile(fileName) {
      return /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName || '');
    },

    isVideoFile(fileName) {
      return /\.(mp4|webm|ogg|m4v|mov|avi|mkv|flv|wmv|rmvb)$/i.test(fileName || '');
    },

    getImagePreviewCandidates() {
      const sourceList = this.isSearching ? this.searchResults : this.fileList;
      return (sourceList || []).filter(item => {
        if (!item || item._isLoaderRow || item._isLoadingItem || item.isDirectory) {
          return false;
        }
        return this.isImageFile(item.fileName);
      });
    },

    getVideoPreviewCandidates() {
      const sourceList = this.isSearching ? this.searchResults : this.fileList;
      return (sourceList || []).filter(item => {
        if (!item || item._isLoaderRow || item._isLoadingItem || item.isDirectory) {
          return false;
        }
        return this.isVideoFile(item.fileName);
      });
    },

    async ensureImageBlobUrl(fileItem) {
      if (!fileItem) {
        return '';
      }
      if (fileItem._blobUrl) {
        return fileItem._blobUrl;
      }
      const blob = await previewFile(fileItem.path);
      const hasError = await this.handlePreviewBlobError(blob);
      if (hasError) {
        throw new Error('blob-preview-error');
      }
      const blobUrl = URL.createObjectURL(blob);
      this.$set(fileItem, '_blobUrl', blobUrl);
      return blobUrl;
    },

    async ensureVideoBlobUrl(fileItem) {
      if (!fileItem) {
        return '';
      }
      if (fileItem._videoBlobUrl) {
        return fileItem._videoBlobUrl;
      }
      const blob = await previewFile(fileItem.path);
      const hasError = await this.handlePreviewBlobError(blob);
      if (hasError) {
        throw new Error('blob-preview-error');
      }
      const blobUrl = URL.createObjectURL(blob);
      this.$set(fileItem, '_videoBlobUrl', blobUrl);
      return blobUrl;
    },

    async loadCurrentImage() {
      const currentFile = this.imageViewerFiles[this.imageViewerIndex];
      if (!currentFile) {
        this.imageViewerUrl = '';
        this.imageViewerError = '未找到图片';
        return;
      }

      this.imageViewerLoading = true;
      this.imageViewerError = '';

      try {
        this.imageViewerUrl = await this.ensureImageBlobUrl(currentFile);
        this.preloadAdjacentImages();
      } catch (error) {
        this.imageViewerUrl = '';
        this.imageViewerError = '图片加载失败';
      } finally {
        this.imageViewerLoading = false;
      }
    },

    preloadAdjacentImages() {
      if (this.imageViewerFiles.length < 2) {
        return;
      }
      const total = this.imageViewerFiles.length;
      const prevIndex = (this.imageViewerIndex - 1 + total) % total;
      const nextIndex = (this.imageViewerIndex + 1) % total;
      [prevIndex, nextIndex].forEach(index => {
        const candidate = this.imageViewerFiles[index];
        if (candidate && !candidate._blobUrl) {
          this.ensureImageBlobUrl(candidate).catch(() => {});
        }
      });
    },

    async loadCurrentVideo() {
      const currentFile = this.videoViewerFiles[this.videoViewerIndex];
      if (!currentFile) {
        this.videoViewerUrl = '';
        this.videoViewerError = '未找到视频';
        return;
      }

      this.videoViewerLoading = true;
      this.videoViewerError = '';

      try {
        this.videoViewerUrl = await this.ensureVideoBlobUrl(currentFile);
      } catch (error) {
        this.videoViewerUrl = '';
        this.videoViewerError = '视频加载失败';
      } finally {
        this.videoViewerLoading = false;
      }
    },

    async openImageViewer(row) {
      const imageCandidates = this.getImagePreviewCandidates();
      if (imageCandidates.length === 0) {
        this.$message.warning('当前没有可预览的图片');
        return;
      }

      const foundIndex = imageCandidates.findIndex(item => item.path === row.path);
      this.imageViewerFiles = imageCandidates;
      this.imageViewerIndex = foundIndex >= 0 ? foundIndex : 0;
      this.imageViewerVisible = true;
      this.imageViewerUrl = '';
      this.imageViewerError = '';
      this.resetImageTransform();
      await this.loadCurrentImage();
    },

    async openVideoViewer(row) {
      const videoCandidates = this.getVideoPreviewCandidates();
      if (videoCandidates.length === 0) {
        this.$message.warning('当前没有可预览的视频');
        return;
      }

      const foundIndex = videoCandidates.findIndex(item => item.path === row.path);
      this.videoViewerFiles = videoCandidates;
      this.videoViewerIndex = foundIndex >= 0 ? foundIndex : 0;
      this.videoViewerVisible = true;
      this.videoViewerUrl = '';
      this.videoViewerError = '';
      this.resetVideoTransform();
      await this.loadCurrentVideo();
    },

    showPrevImage() {
      if (this.imageViewerFiles.length <= 1) {
        return;
      }
      const total = this.imageViewerFiles.length;
      this.imageViewerIndex = (this.imageViewerIndex - 1 + total) % total;
      this.resetImageTransform();
      this.loadCurrentImage();
    },

    showNextImage() {
      if (this.imageViewerFiles.length <= 1) {
        return;
      }
      const total = this.imageViewerFiles.length;
      this.imageViewerIndex = (this.imageViewerIndex + 1) % total;
      this.resetImageTransform();
      this.loadCurrentImage();
    },

    showPrevVideo() {
      if (this.videoViewerFiles.length <= 1) {
        return;
      }
      const total = this.videoViewerFiles.length;
      this.videoViewerIndex = (this.videoViewerIndex - 1 + total) % total;
      this.resetVideoTransform();
      this.loadCurrentVideo();
    },

    showNextVideo() {
      if (this.videoViewerFiles.length <= 1) {
        return;
      }
      const total = this.videoViewerFiles.length;
      this.videoViewerIndex = (this.videoViewerIndex + 1) % total;
      this.resetVideoTransform();
      this.loadCurrentVideo();
    },

    resetVideoTransform() {
      this.videoViewerScale = 1;
    },

    resetImageTransform() {
      this.imageViewerScale = 1;
      this.imageViewerOffsetX = 0;
      this.imageViewerOffsetY = 0;
      this.imageViewerDragging = false;
    },

    handleImageWheel(event) {
      if (!this.imageViewerUrl || this.imageViewerLoading) {
        return;
      }
      const direction = event.deltaY < 0 ? 1 : -1;
      const nextScale = this.imageViewerScale + direction * 0.12;
      const clamped = Math.max(1, Math.min(5, Number(nextScale.toFixed(2))));
      this.imageViewerScale = clamped;
      if (clamped === 1) {
        this.imageViewerOffsetX = 0;
        this.imageViewerOffsetY = 0;
      }
    },

    handleVideoWheel(event) {
      if (!this.videoViewerUrl || this.videoViewerLoading) {
        return;
      }
      const direction = event.deltaY < 0 ? 1 : -1;
      const nextScale = this.videoViewerScale + direction * 0.12;
      this.videoViewerScale = Math.max(1, Math.min(3, Number(nextScale.toFixed(2))));
    },

    handleImageViewerGlobalWheel(event) {
      if (!this.imageViewerVisible && !this.videoViewerVisible) {
        return;
      }
      if (event.ctrlKey) {
        event.preventDefault();
        if (this.imageViewerVisible) {
          this.handleImageWheel(event);
        } else if (this.videoViewerVisible) {
          this.handleVideoWheel(event);
        }
      }
    },

    startImageDrag(event) {
      if (this.imageViewerScale <= 1 || !this.imageViewerUrl) {
        return;
      }
      this.imageViewerDragging = true;
      this.imageViewerDragStartX = event.clientX;
      this.imageViewerDragStartY = event.clientY;
      this.imageViewerStartOffsetX = this.imageViewerOffsetX;
      this.imageViewerStartOffsetY = this.imageViewerOffsetY;
    },

    handleImageDragMove(event) {
      if (!this.imageViewerDragging) {
        return;
      }
      const deltaX = event.clientX - this.imageViewerDragStartX;
      const deltaY = event.clientY - this.imageViewerDragStartY;
      this.imageViewerOffsetX = this.imageViewerStartOffsetX + deltaX;
      this.imageViewerOffsetY = this.imageViewerStartOffsetY + deltaY;
    },

    endImageDrag() {
      if (!this.imageViewerDragging) {
        return;
      }
      this.imageViewerDragging = false;
    },

    clearImageViewerBlobUrls() {
      (this.imageViewerFiles || []).forEach(file => {
        if (file && file._blobUrl) {
          URL.revokeObjectURL(file._blobUrl);
          this.$delete(file, '_blobUrl');
        }
      });
    },

    clearVideoViewerBlobUrls() {
      (this.videoViewerFiles || []).forEach(file => {
        if (file && file._videoBlobUrl) {
          URL.revokeObjectURL(file._videoBlobUrl);
          this.$delete(file, '_videoBlobUrl');
        }
      });
    },

    closeImageViewer() {
      this.imageViewerVisible = false;
      this.imageViewerLoading = false;
      this.imageViewerUrl = '';
      this.imageViewerError = '';
      this.resetImageTransform();
      this.clearImageViewerBlobUrls();
      this.imageViewerFiles = [];
      this.imageViewerIndex = 0;
    },

    closeVideoViewer() {
      this.videoViewerVisible = false;
      this.videoViewerLoading = false;
      this.videoViewerUrl = '';
      this.videoViewerError = '';
      this.resetVideoTransform();
      this.clearVideoViewerBlobUrls();
      this.videoViewerFiles = [];
      this.videoViewerIndex = 0;
    },

    handleImageViewerKeydown(event) {
      if (this.imageViewerVisible) {
        if (event.key === 'Escape') {
          this.closeImageViewer();
        } else if (event.key === 'ArrowLeft') {
          this.showPrevImage();
        } else if (event.key === 'ArrowRight') {
          this.showNextImage();
        }
        return;
      }

      if (!this.videoViewerVisible) {
        return;
      }
      if (event.key === 'Escape') {
        this.closeVideoViewer();
      } else if (event.key === 'ArrowLeft') {
        this.showPrevVideo();
      } else if (event.key === 'ArrowRight') {
        this.showNextVideo();
      }
    },

    handleFileNameClick(row) {
      // 在搜索模式下，点击文件夹跳转到对应目录
      if (this.isSearching && row.isDirectory) {
        this.exitSearch();
        this.currentDirPath = row.path;
        this.getList();
        return;
      }
      // 其他情况使用默认的预览/打开逻辑
      this.handlePreview(row);
    },

    handlePreview(row) {
      if (row.isDirectory) {
        // 点击目录，进入该目录
        this.currentDirPath = row.path;
        this.getList();
      } else {
        // 预览文件
        const fileName = row.fileName.toLowerCase();

        // 判断文件类型
        const isImage = /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName);
        const isPdf = /\.pdf$/i.test(fileName);
        const isText = /\.(txt|log|md|json|xml|yaml|yml|js|css|html|java|py|c|cpp|h|hpp)$/i.test(fileName);
        const isDocx = /\.docx$/i.test(fileName);
        const isDoc = /\.doc$/i.test(fileName);
        const isExcel = /\.(xls|xlsx)$/i.test(fileName);

        if (isDocx || isExcel || isPdf) {
          // docx/Excel/PDF：使用 @vue-office 预览
          this.previewOfficeFile(row);
        } else if (isDoc) {
          // .doc 旧格式：后端 POI 提取纯文本预览
          this.previewDocText(row);
        } else if (isImage) {
          // 图片：当前页面灯箱预览
          this.openImageViewer(row);
        } else if (this.isVideoFile(fileName)) {
          // 视频：当前页面灯箱预览
          this.openVideoViewer(row);
        } else if (isText) {
          // 文本文件：读取并显示在对话框中
          previewFile(row.path).then(blob => {
            // 拦截器直接返回了 blob 数据
            const reader = new FileReader();
            reader.onload = (e) => {
              const content = e.target.result;
              this.$alert(content || '(空文件)', row.fileName, {
                confirmButtonText: '关闭',
                customClass: 'text-preview-dialog',
                dangerouslyUseHTMLString: false
              });
            };
            reader.onerror = () => {
              this.$message.error("读取文件内容失败");
            };
            reader.readAsText(blob);
          }).catch((err) => {
            this.$message.error("预览失败");
          });
        } else {
          // 其他文件：提示下载
          this.$confirm('该文件类型不支持在线预览，是否下载？', '提示', {
            confirmButtonText: '下载',
            cancelButtonText: '取消',
            type: 'info'
          }).then(() => {
            this.handleDownload(row);
          }).catch(() => {});
        }
      }
    },
    // 处理预览窗口的消息
    handlePreviewMessage(event) {
      if (event.data && event.data.type === 'download') {
        this.handleDownload({
          path: event.data.path,
          fileName: event.data.fileName,
          isDirectory: false
        });
      }
    },
    
    // 预览渲染成功
    handlePreviewRendered() {
      // 文档预览渲染完成
    },
    
    // 预览渲染失败
    handlePreviewError(error) {
      this.$message.error('文档预览失败：' + error);
    },
    
    // 预览 Office 文件（docx/Excel/PDF）— URL 模式，由 @vue-office 内部 fetch 渐进加载
    previewOfficeFile(row) {
      const ext = row.fileName.split('.').pop().toLowerCase();
      const url = getPreviewUrl(row.path);

      this.previewData = url;
      this.previewType = ext;
      this.previewTitle = row.fileName;
      this.previewVisible = true;
    },

    // 预览 .doc 文件（后端 POI 纯文本提取）
    async previewDocText(row) {
      try {
        const res = await previewTextFile(row.path);
        const text = res.data || '';
        this.$alert(text || '(无法提取文本内容)', row.fileName, {
          confirmButtonText: '关闭',
          customClass: 'text-preview-dialog'
        });
      } catch (error) {
        this.$message.error('预览失败：' + (error.message || '未知错误'));
      }
    },
    
    handleDownload(row) {
      if (row.isDirectory) {
        this.$message.warning("不能下载目录");
        return;
      }
      downloadFile(row.path, row.fileName);
    },
    handleDelete(row) {
      const isDir = row.isDirectory;
      const typeName = isDir ? '目录' : '文件';
      
      this.$modal.confirm(`是否确认删除${typeName} "${row.fileName}"？${isDir ? '注意：目录及其所有内容将被删除！' : ''}`).then(() => {
        // 使用局部loading
        const loadingInstance = this.$loading({
          target: '.file-list-container',
          text: '删除中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        return deleteFile(row.path, isDir).then(() => {
          this.$modal.msgSuccess("删除成功");
          // 立即从列表中移除（乐观更新）
          this.fileList = this.fileList.filter(f => f.path !== row.path);
          // 强制 Vue 更新
          this.$forceUpdate();
        }).finally(() => {
          loadingInstance.close();
        });
      }).catch(() => {});
    },

    // 重命名文件/文件夹
    handleRename(row) {
      const isDir = row.isDirectory;
      const typeName = isDir ? '文件夹' : '文件';
      
      this.$prompt(`请输入新的${typeName}名称`, '重命名', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: row.fileName,
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return '名称不能为空';
          }
          if (value.includes('/') || value.includes('\\')) {
            return '名称不能包含路径分隔符';
          }
          if (value === row.fileName) {
            return '新名称与原名称相同';
          }
          return true;
        }
      }).then(({ value }) => {
        const newName = value.trim();
        
        // 使用局部loading
        const loadingInstance = this.$loading({
          target: '.file-list-container',
          text: '重命名中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        renameFile(row.path, newName).then(() => {
          this.$modal.msgSuccess("重命名成功");
          // 乐观更新：更新当前行的名称和路径
          const oldPath = row.path;
          const parentPath = oldPath.substring(0, oldPath.lastIndexOf('/')) || '/';
          const newPath = parentPath.endsWith('/') ? parentPath + newName : parentPath + '/' + newName;
          
          // 更新列表中的数据
          const index = this.fileList.findIndex(f => f.path === oldPath);
          if (index !== -1) {
            this.fileList[index].fileName = newName;
            this.fileList[index].path = newPath;
            this.fileList[index].fileId = newPath;
            // 触发响应式更新
            this.fileList = [...this.fileList];
          }
          
          // 刷新目录树
          this.refreshCurrentTreeNode();
        }).catch((err) => {
          this.$message.error(err.message || "重命名失败");
        }).finally(() => {
          loadingInstance.close();
        });
      }).catch(() => {});
    },

    // 移动文件/文件夹
    handleMove(row) {
      const isDir = row.isDirectory;
      const typeName = isDir ? '文件夹' : '文件';
      
      // 构建目标目录选择列表（排除当前目录和自身）
      const buildDirOptions = (nodes, level = 0) => {
        let options = [];
        for (const node of nodes) {
          // 排除当前文件/文件夹所在的目录和自身（如果是文件夹）
          const isCurrentDir = node.path === this.currentDirPath;
          const isSelf = isDir && node.path === row.path;
          
          if (!isSelf) {
            options.push({
              label: '  '.repeat(level) + node.label,
              value: node.path,
              disabled: isCurrentDir
            });
          }
          
          if (node.children && node.children.length > 0) {
            options = options.concat(buildDirOptions(node.children, level + 1));
          }
        }
        return options;
      };
      
      const dirOptions = buildDirOptions(this.folderTree);
      
      this.$msgbox({
        title: `移动${typeName}`,
        message: `
          <div style="margin-bottom: 10px;">将 "${row.fileName}" 移动到：</div>
          <select id="move-target-dir" style="width: 100%; padding: 8px; border: 1px solid #dcdfe6; border-radius: 4px;">
            <option value="">请选择目标目录</option>
            <option value="/">根目录</option>
            ${dirOptions.map(opt => `<option value="${opt.value}" ${opt.disabled ? 'disabled' : ''}>${opt.label}</option>`).join('')}
          </select>
        `,
        dangerouslyUseHTMLString: true,
        showCancelButton: true,
        confirmButtonText: '移动',
        cancelButtonText: '取消',
        beforeClose: (action, instance, done) => {
          if (action === 'confirm') {
            const targetDir = document.getElementById('move-target-dir').value;
            if (!targetDir) {
              this.$message.warning('请选择目标目录');
              return;
            }
            
            instance.confirmButtonLoading = true;
            instance.confirmButtonText = '移动中...';
            
            moveFile(row.path, targetDir).then(() => {
              this.$modal.msgSuccess("移动成功");
              // 从列表中移除
              this.fileList = this.fileList.filter(f => f.path !== row.path);
              // 刷新目录树
              this.refreshCurrentTreeNode();
              done();
            }).catch((err) => {
              this.$message.error(err.message || "移动失败");
              instance.confirmButtonLoading = false;
              instance.confirmButtonText = '移动';
            });
          } else {
            done();
          }
        }
      }).catch(() => {});
    },

    // 选择变化
    handleSelectionChange(selection) {
      // 只有在非移动模式下才更新选择
      // 移动模式下，选择已经保存，不需要再更新
      if (!this.isMoveMode) {
        this.selectedFiles = selection;
      }
    },

    // 开始移动模式
    startMoveMode() {
      if (this.selectedFiles.length === 0) {
        this.$message.warning('请先选择要移动的文件/文件夹');
        return;
      }
      this.isMoveMode = true;
      this.moveTargetDir = null;
      this.$message.info('请切换到目标目录，然后点击"确定移动至此处"');
    },

    // 取消移动模式
    cancelMoveMode() {
      this.isMoveMode = false;
      this.moveTargetDir = null;
      // 清空选择
      this.$refs.fileTable.clearSelection();
      this.selectedFiles = [];
    },

    // 确认移动
    confirmMove() {
      if (this.selectedFiles.length === 0) {
        this.$message.warning('没有要移动的文件');
        return;
      }
      
      const targetDir = this.currentDirPath;
      
      // 检查是否移动到自己所在的目录
      const filesInCurrentDir = this.selectedFiles.filter(f => {
        const parentPath = f.path.substring(0, f.path.lastIndexOf('/')) || '/';
        return parentPath === targetDir;
      });
      
      if (filesInCurrentDir.length === this.selectedFiles.length) {
        this.$message.warning('文件已经在当前目录，无需移动');
        return;
      }
      
      // 过滤掉已经在目标目录的文件
      const filesToMove = this.selectedFiles.filter(f => {
        const parentPath = f.path.substring(0, f.path.lastIndexOf('/')) || '/';
        return parentPath !== targetDir;
      });
      
      if (filesToMove.length === 0) {
        this.$message.warning('选中的文件已经在当前目录');
        return;
      }
      
      // 确认对话框
      this.$modal.confirm(`确定将 ${filesToMove.length} 个文件/文件夹移动到 "${targetDir}" 吗？`).then(async () => {
        // 使用局部loading
        const loadingInstance = this.$loading({
          target: '.file-list-container',
          text: '移动中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        // 串行移动，避免并发问题
        let successCount = 0;
        let failCount = 0;
        
        for (const file of filesToMove) {
          try {
            await moveFile(file.path, targetDir);
            successCount++;
          } catch (err) {
            failCount++;
          }
        }
        
        loadingInstance.close();
        
        if (successCount > 0) {
          this.$modal.msgSuccess(`成功移动 ${successCount} 个文件/文件夹${failCount > 0 ? '，失败 ' + failCount + ' 个' : ''}`);
          // 刷新文件列表
          this.getList();
          // 刷新目录树
          this.refreshCurrentTreeNode();
        } else {
          this.$message.error('移动失败');
        }
        
        // 退出移动模式
        this.isMoveMode = false;
        this.selectedFiles = [];
      }).catch(() => {});
    },

    // 表格行样式类
    tableRowClassName({row}) {
      if (row._isLoaderRow) return 'loader-row';
      if (this.dragOverRow && this.dragOverRow.path === row.path && row.isDirectory) {
        return 'drag-over-row';
      }
      return '';
    },

    // 拖动开始
    handleDragStart(event, row) {
      // 不能拖动加载行
      if (row._isLoaderRow) {
        event.preventDefault();
        return;
      }
      
      // 如果有选中的文件，且当前拖动的文件在选中列表中，则批量拖动
      if (this.selectedFiles.length > 0 && this.selectedFiles.some(f => f.path === row.path)) {
        // 批量拖动选中的文件
        this.dragSource = row;
        this.isBatchDrag = true;
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', 'batch');
      } else {
        // 单文件拖动
        this.dragSource = row;
        this.isBatchDrag = false;
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', row.path);
      }
    },

    // 拖动经过
    handleDragOver(event, row) {
      event.preventDefault();
      // 只能拖放到文件夹上
      if (row.isDirectory && row.path !== this.dragSource?.path) {
        this.dragOverRow = row;
        event.dataTransfer.dropEffect = 'move';
      }
    },

    // 拖动离开
    handleDragLeave(event, row) {
      if (this.dragOverRow && this.dragOverRow.path === row.path) {
        this.dragOverRow = null;
      }
    },

    // 放下
    handleDrop(event, row) {
      event.preventDefault();
      this.dragOverRow = null;
      
      // 只能拖放到文件夹上
      if (!row.isDirectory) {
        this.$message.warning('只能拖放到文件夹中');
        return;
      }
      
      const targetPath = row.path;
      
      // 批量拖动
      if (this.isBatchDrag && this.selectedFiles.length > 0) {
        // 过滤掉目标目录中的文件和子目录
        const filesToMove = this.selectedFiles.filter(f => {
          // 不能拖放到自己
          if (f.path === targetPath) return false;
          // 不能拖放到子目录
          if (targetPath.startsWith(f.path + '/')) return false;
          // 不能拖放到已经是目标目录的文件
          const parentPath = f.path.substring(0, f.path.lastIndexOf('/')) || '/';
          return parentPath !== targetPath;
        });
        
        if (filesToMove.length === 0) {
          this.$message.warning('选中的文件已经在目标目录中');
          return;
        }
        
        // 执行批量移动
        this.executeBatchDragMove(filesToMove, row);
        return;
      }
      
      // 单文件拖动
      if (this.dragSource) {
        const sourcePath = this.dragSource.path;
        
        // 检查是否是拖放到自己
        if (sourcePath === targetPath) {
          this.$message.warning('不能拖放到自身');
          return;
        }
        
        // 检查是否是拖放到子目录
        if (targetPath.startsWith(sourcePath + '/')) {
          this.$message.warning('不能拖放到子目录中');
          return;
        }
        
        // 执行移动
        this.executeDragMove(this.dragSource, row);
      }
    },

    // 拖动结束
    handleDragEnd(event) {
      this.dragSource = null;
      this.dragOverRow = null;
      this.isBatchDrag = false;
    },

    // 执行拖动移动
    executeDragMove(sourceRow, targetRow) {
      const sourcePath = sourceRow.path;
      const targetDir = targetRow.path;

      // 显示加载
      const loadingInstance = this.$loading({
        target: '.file-list-container',
        text: '移动中...',
        spinner: 'el-icon-loading',
        background: 'rgba(255, 255, 255, 0.7)'
      });
      
      moveFile(sourcePath, targetDir).then(() => {
        this.$modal.msgSuccess(`移动成功`);
        // 从列表中移除
        this.fileList = this.fileList.filter(f => f.path !== sourcePath);
        // 刷新目录树
        this.refreshCurrentTreeNode();
      }).catch((err) => {
        this.$message.error(err.message || "移动失败");
      }).finally(() => {
        loadingInstance.close();
      });
    },

    // 执行批量拖动移动
    executeBatchDragMove(filesToMove, targetRow) {
      const targetDir = targetRow.path;

      // 显示加载
      const loadingInstance = this.$loading({
        target: '.file-list-container',
        text: `正在移动 ${filesToMove.length} 个文件...`,
        spinner: 'el-icon-loading',
        background: 'rgba(255, 255, 255, 0.7)'
      });
      
      // 串行移动
      let successCount = 0;
      let failCount = 0;
      
      const moveNext = async (index) => {
        if (index >= filesToMove.length) {
          // 全部完成
          loadingInstance.close();
          
          if (successCount > 0) {
            this.$modal.msgSuccess(`成功移动 ${successCount} 个文件/文件夹${failCount > 0 ? '，失败 ' + failCount + ' 个' : ''}`);
            // 刷新文件列表
            this.getList();
            // 刷新目录树
            this.refreshCurrentTreeNode();
          } else {
            this.$message.error('移动失败');
          }
          
          // 清空选择
          this.selectedFiles = [];
          this.$refs.fileTable && this.$refs.fileTable.clearSelection();
          return;
        }
        
        const file = filesToMove[index];
        try {
          await moveFile(file.path, targetDir);
          successCount++;
        } catch (err) {
          failCount++;
        }

        // 继续下一个
        moveNext(index + 1);
      };
      
      // 开始移动
      moveNext(0);
    },

    // 批量删除
    handleBatchDelete() {
      if (this.selectedFiles.length === 0) {
        this.$message.warning('请先选择要删除的文件/文件夹');
        return;
      }
      
      const fileNames = this.selectedFiles.map(f => f.fileName).join(', ');
      const h = this.$createElement;
      this.$msgbox({
        title: '系统提示',
        message: h('div', null, [
          h('p', null, `确定删除选中的 ${this.selectedFiles.length} 个文件/文件夹吗？`),
          h('p', { style: 'color: #f56c6c; font-size: 12px; margin-top: 10px;' }, fileNames)
        ]),
        showCancelButton: true,
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        // 使用局部loading
        const loadingInstance = this.$loading({
          target: '.file-list-container',
          text: '删除中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        // 串行删除，避免并发问题
        let successCount = 0;
        let failCount = 0;
        
        for (const file of this.selectedFiles) {
          try {
            // 传递 isDirectory 参数，让后端正确删除文件目录权限
            await deleteFile(file.path, file.isDirectory);
            successCount++;
          } catch (err) {
            failCount++;
          }
        }
        
        loadingInstance.close();
        
        if (successCount > 0) {
          this.$modal.msgSuccess(`成功删除 ${successCount} 个文件/文件夹${failCount > 0 ? '，失败 ' + failCount + ' 个' : ''}`);
          // 刷新文件列表
          this.getList();
          // 刷新目录树
          this.refreshCurrentTreeNode();
        } else {
          this.$message.error('删除失败');
        }
        
        // 清空选择
        this.selectedFiles = [];
        this.$refs.fileTable && this.$refs.fileTable.clearSelection();
      }).catch(() => {});
    },

    // 上传文件
    openUploadDialog() {
      this.uploadDialogVisible = true;
    },
    handleUploadDialogClose() {
      if (this.$refs.dialogUploader) {
        this.$refs.dialogUploader.clearFiles();
      }
      this.uploadFileList = [];
      this.uploadSubmitting = false;
      this.uploadPendingCount = 0;
      this.uploadTotalCount = 0;
    },
    trackTransferTask(fileName, taskId) {
      if (!taskId) {
        return;
      }
      const exists = this.transferTaskList.some(item => item.taskId === taskId)
      if (!exists) {
        this.transferTaskList.push({ taskId, fileName })
      }
      this.scheduleTransferStatusPolling()
    },
    scheduleTransferStatusPolling() {
      if (this.transferStatusTimer) {
        return;
      }
      this.transferStatusTimer = setTimeout(() => {
        this.transferStatusTimer = null
        this.pollTransferStatus()
      }, 2500)
    },
    async pollTransferStatus() {
      if (!this.transferTaskList.length) {
        return;
      }

      const pendingTasks = [...this.transferTaskList]
      const finishedTaskIds = []

      await Promise.all(pendingTasks.map(async (task) => {
        try {
          const resp = await getChunkTransferStatus(task.taskId)
          const data = resp.data || {}
          if (data.status === 'SUCCESS') {
            finishedTaskIds.push(task.taskId)
            this.$message.success(`${task.fileName} 已完成后台转存`)
            this.scheduleUploadListRefresh()
          } else if (data.status === 'FAILED') {
            finishedTaskIds.push(task.taskId)
            this.$message.error(`${task.fileName} 转存失败：${data.error || '未知错误'}`)
          }
        } catch (error) {
          if (String(error).includes('不存在或已过期')) {
            finishedTaskIds.push(task.taskId)
          }
        }
      }))

      if (finishedTaskIds.length > 0) {
        this.transferTaskList = this.transferTaskList.filter(item => !finishedTaskIds.includes(item.taskId))
      }
      if (this.transferTaskList.length > 0) {
        this.scheduleTransferStatusPolling()
      }
    },
    beforeUploadDialogClose(done) {
      if (this.uploadPendingCount > 0 || this.uploadSubmitting) {
        this.$message.warning('文件上传中，请稍后关闭窗口');
        return;
      }
      done();
    },
    triggerUploadSelect() {
      const uploader = this.$refs.dialogUploader;
      if (!uploader) {
        return;
      }
      const input = (uploader.$refs && uploader.$refs.input)
        || (uploader.$el && uploader.$el.querySelector('input.el-upload__input'));
      if (input) {
        input.value = null;
        input.click();
      }
    },
    handleUploadAreaClick() {
    },
    scheduleUploadListRefresh() {
      if (this.uploadRefreshTimer) {
        clearTimeout(this.uploadRefreshTimer);
      }
      this.uploadRefreshTimer = setTimeout(() => {
        this.getList();
        this.uploadRefreshTimer = null;
      }, 300);
    },
    handleUploadFileChange(file, fileList) {
      this.uploadFileList = fileList;
    },
    handleUploadFileRemove(file, fileList) {
      this.uploadFileList = fileList;
    },
    async submitChunkUpload() {
      if (!this.uploadFileList.length) {
        this.$message.warning('请先选择文件');
        return;
      }

      this.uploadSubmitting = true;
      this.uploadTotalCount = this.uploadFileList.length;
      this.uploadPendingCount = 0;

      let successCount = 0;
      let failCount = 0;
      let queuedCount = 0;

      const fileQueue = [...this.uploadFileList];
      const fileConcurrency = 2;
      let fileCursor = 0;
      const runFileWorker = async () => {
        while (fileCursor < fileQueue.length) {
          const current = fileCursor;
          fileCursor += 1;
          const uploadItem = fileQueue[current];
          this.uploadPendingCount += 1;
          try {
            const uploadResult = await this.uploadFileByChunk(uploadItem);
            if (uploadResult && uploadResult.async) {
              queuedCount += 1;
            }
            uploadItem.status = 'success';
            uploadItem.percentage = 100;
            successCount += 1;
          } catch (error) {
            uploadItem.status = 'exception';
            failCount += 1;
            this.$message.error(`${uploadItem.name} 上传失败: ${error.message || error}`);
          } finally {
            this.uploadPendingCount = Math.max(0, this.uploadPendingCount - 1);
          }
        }
      };

      const workers = [];
      const workerCount = Math.min(fileConcurrency, fileQueue.length || 1);
      for (let i = 0; i < workerCount; i++) {
        workers.push(runFileWorker());
      }
      await Promise.all(workers);

      this.uploadSubmitting = false;
      if (successCount > 0) {
        this.scheduleUploadListRefresh();
      }
      if (queuedCount > 0) {
        this.$message.success(`上传完成：成功入队 ${queuedCount} 个，立即完成 ${successCount - queuedCount} 个，失败 ${failCount} 个`);
      } else {
        this.$message.success(`上传完成：成功 ${successCount} 个，失败 ${failCount} 个`);
      }
      if (failCount === 0) {
        this.uploadDialogVisible = false;
      }
    },
    async uploadFileByChunk(uploadItem) {
      const fileStartAt = Date.now();
      const file = uploadItem.raw || uploadItem;
      const chunkSize = 8 * 1024 * 1024;
      const totalChunks = Math.ceil(file.size / chunkSize);
      const uploadPath = this.currentDirPath && this.currentDirPath.endsWith('/')
        ? this.currentDirPath
        : `${this.currentDirPath || '/'}${this.currentDirPath && this.currentDirPath.endsWith('/') ? '' : '/'}`;
      const fileHash = `${file.name}_${file.size}_${file.lastModified}`;

      const initStartAt = Date.now();
      const initResp = await initChunkUpload({
        path: uploadPath,
        fileName: file.name,
        fileHash,
        fileSize: file.size,
        chunkSize,
        totalChunks
      });
      const initCost = Date.now() - initStartAt;
      const initData = initResp.data || {};
      const uploadId = initData.uploadId;
      if (!uploadId) {
        throw new Error('初始化分片上传失败');
      }

      const uploadedSet = new Set(initData.uploadedChunks || []);
      let completedChunks = uploadedSet.size;
      uploadItem.percentage = Math.floor((completedChunks / totalChunks) * 100);

      const pendingIndexes = [];
      for (let i = 0; i < totalChunks; i++) {
        if (!uploadedSet.has(i)) {
          pendingIndexes.push(i);
        }
      }

      const concurrency = 3;
      const chunkStartAt = Date.now();
      let cursor = 0;
      const uploadWorker = async () => {
        while (cursor < pendingIndexes.length) {
          const current = cursor;
          cursor += 1;
          const chunkIndex = pendingIndexes[current];
          const start = chunkIndex * chunkSize;
          const end = Math.min(file.size, start + chunkSize);
          const chunkBlob = file.slice(start, end);

          let uploaded = false;
          let retry = 0;
          while (!uploaded && retry < 3) {
            try {
              await uploadChunk(uploadId, chunkIndex, chunkBlob);
              uploaded = true;
            } catch (error) {
              retry += 1;
              if (retry >= 3) {
                throw error;
              }
            }
          }

          completedChunks += 1;
          uploadItem.percentage = Math.floor((completedChunks / totalChunks) * 100);
        }
      };

      try {
        const workerCount = Math.min(concurrency, pendingIndexes.length || 1);
        const workers = [];
        for (let i = 0; i < workerCount; i++) {
          workers.push(uploadWorker());
        }
        await Promise.all(workers);

        const chunkCost = Date.now() - chunkStartAt;
        const completeStartAt = Date.now();
        const completeResp = await completeChunkUpload(uploadId);
        const completeData = completeResp.data || {};
        if (completeData.async && completeData.taskId) {
          this.trackTransferTask(file.name, completeData.taskId);
        }
        const completeCost = Date.now() - completeStartAt;
        const totalCost = Date.now() - fileStartAt;
        console.info(`[chunk-upload] ${file.name} timing: init=${initCost}ms, chunk=${chunkCost}ms, complete=${completeCost}ms, total=${totalCost}ms`);
        return {
          async: completeData.async === true,
          taskId: completeData.taskId || null
        };
      } catch (error) {
        try {
          await cancelChunkUpload(uploadId);
        } catch (cancelError) {
          // 取消上传失败，忽略
        }
        throw error;
      }
    },

    // 上传前的钩子
    beforeUpload(file) {
      const maxSize = this.maxUploadSizeMB * 1024 * 1024;
      if (file.size > maxSize) {
        this.$message.warning(`文件大小不能超过 ${this.maxUploadSizeMB}MB`);
        return false;
      }
      return true;
    },

    // 新建文件夹
    handleCreateFolder() {
      this.$prompt('请输入文件夹名称', '新建文件夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return '文件夹名称不能为空';
          }
          // 检查是否包含非法字符
          if (/[\\/:*?"<>|]/.test(value)) {
            return '文件夹名称包含非法字符';
          }
          return true;
        }
      }).then(({ value }) => {
        // 使用局部loading而不是全屏loading
        const loadingInstance = this.$loading({
          target: '.file-list-container',
          text: '创建中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        // 调用新建文件夹 API（自动分配权限）
        const folderName = value.trim();
        createFolder(this.currentDirPath, folderName).then(() => {
          this.$modal.msgSuccess("创建成功");
          // 立即将新文件夹添加到列表（乐观更新，无需等待加载）
          const newFolderPath = this.currentDirPath.endsWith('/') 
            ? this.currentDirPath + folderName 
            : this.currentDirPath + '/' + folderName;
          const newFolder = {
            fileId: newFolderPath,
            fileName: folderName,
            fileSize: 0,
            updateTime: this.parseTime(new Date()),
            path: newFolderPath,
            isDirectory: true,
            canDelete: true,
            sortWeight: 0
          };
          // 插入到列表开头（文件夹排序在前）
          this.fileList = [newFolder, ...this.fileList];
          // 强制表格重新计算布局
          this.$nextTick(() => {
            if (this.$refs.fileTable) {
              this.$refs.fileTable.doLayout();
            }
          });
          // 异步刷新目录树（不阻塞）
          this.refreshCurrentTreeNode();
        }).catch((err) => {
          this.$message.error(err.message || "创建失败");
        }).finally(() => {
          loadingInstance.close();
        });
      }).catch(() => {});
    },
    
    // 刷新当前树节点（只刷新树，不刷新文件列表）
    refreshCurrentTreeNode() {
      // 保存当前选中的节点路径
      const currentPath = this.currentDirPath;
      const currentNode = this.$refs.folderTree?.getCurrentNode?.();
      
      if (currentNode) {
        // 如果当前有选中的节点，刷新该节点
        const node = this.$refs.folderTree.getNode(currentNode);
        if (node) {
          // 清空子节点，标记为未加载，然后重新展开
          node.childNodes = [];
          node.loaded = false;
          node.isLeaf = false;
          // 使用 doCreateChildren 重新加载（不刷新文件列表）
          node.loadData();
        }
      }
      // 不调用 getList()，避免触发第二次 loading
    },
    // 时间格式化
    parseTime(timestamp) {
      if (!timestamp) return "";
      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hour}:${minute}`;
    }
  }
};
</script>

<style scoped lang="scss">
/* 搜索区域样式 */
.search-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .search-input {
    flex: 1;
  }
  
  .search-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
}

/* 加载中提示样式 */
.loading-item-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  color: #909399;
  font-size: 14px;
  
  i {
    margin-right: 8px;
    font-size: 16px;
  }
}

/* 骨架屏样式 */
.skeleton-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  
  .skeleton-icon {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    margin-right: 12px;
  }
  
  .skeleton-content {
    flex: 1;
    
    .skeleton-line {
      height: 16px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      margin-bottom: 8px;
      
      &.short {
        width: 60%;
        height: 12px;
      }
    }
  }
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 面包屑链接样式 */
::v-deep .el-breadcrumb__item.is-link {
  cursor: pointer;
  color: #409EFF;
  
  &:hover {
    color: #66b1ff;
  }
}

/* 移动模式提示 */
.move-hint {
  margin-left: 10px;
  color: #E6A23C;
  font-size: 14px;
}

/* 移动模式面板 */
.move-mode-panel {
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 12px 15px;
  margin-bottom: 10px;
}

/* 拖动样式 */
.file-name-cell[draggable="true"] {
  cursor: move;
}

.file-name-cell.is-dragging {
  opacity: 0.5;
}

.file-name-cell.is-drag-over {
  background-color: #ecf5ff;
  border: 2px dashed #409eff;
  border-radius: 4px;
}

.drag-over-row {
  background-color: #ecf5ff !important;
}

.move-mode-title {
  color: #E6A23C;
  font-weight: bold;
  margin-bottom: 8px;
}

.move-mode-files {
  margin-bottom: 8px;
}

.move-file-tag {
  margin: 2px;
}

.move-mode-target {
  color: #606266;
  font-size: 14px;
}

/* 核心布局逻辑：锁定外层，内部自适应伸缩 */
.home {
  background-color: #f0f2f5;
  padding: 20px;
  /* 锁定整体高度：视口高度减去若依顶部Navbar和TagsView高度(通常约84px)，加上内边距处理 */
  height: calc(100vh - 84px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* ★ 彻底干掉页面最外层的滚动条 ★ */

  .search-card {
    margin-bottom: 20px;
    flex-shrink: 0; /* 保证搜索框不被压缩 */
    .tips {
      color: #909399;
      font-size: 13px;
      margin-left: 10px;
    }
  }

  .main-content {
    flex: 1; /* 撑满底部剩余空间 */
    min-height: 0; /* 解决 flex 嵌套导致的高度溢出问题 */

    .tree-col, .file-col {
      height: 100%;
    }

    /* 左侧树状卡片设置 */
    .tree-card {
      height: 100%;
      display: flex;
      flex-direction: column;

      /* 修改 el-card 的内部包裹层，让它变成可滚动的容器 */
      ::v-deep .el-card__body {
        flex: 1;
        overflow-y: auto; /* ★ 左侧目录树独立滚动 ★ */
        padding: 10px;
        /* 美化一下滚动条 */
        &::-webkit-scrollbar {
          width: 6px;
        }
        &::-webkit-scrollbar-thumb {
          background-color: #dcdfe6;
          border-radius: 4px;
        }
      }
    }

    /* 右侧文件卡片设置 */
    .file-card {
      height: 100%;
      display: flex;
      flex-direction: column;

      ::v-deep .el-card__body {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden; /* el-card本体不滚动，把滚动权交接给内部的 el-table */
        padding: 15px;
      }

      .file-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px; // 与下方表格拉开点距离
        flex-shrink: 0;

        .actions {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .search-title {
          font-size: 14px;
          color: #606266;
          .highlight-text {
            color: #F56C6C;
            font-weight: bold;
            margin: 0 5px;
          }
        }
      }

      /* 表格专属容器，强制占满剩余空间 */
      .table-container {
        flex: 1;
        min-height: 0;
      }
    }
  }

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
  }

  .image-viewer-loading {
    color: #ffffff;
    font-size: 34px;
  }

  .image-viewer-error {
    color: #ffffff;
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

  .image-viewer-nav-prev {
    left: 18px;
  }

  .image-viewer-nav-next {
    right: 18px;
  }

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

  .image-viewer-img.is-draggable {
    cursor: grab;
    user-select: none;
  }

  .image-viewer-img.is-dragging {
    cursor: grabbing;
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

  .video-viewer-nav-prev {
    left: 18px;
  }

  .video-viewer-nav-next {
    right: 18px;
  }

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

  .file-name-cell {
    display: flex;
    align-items: flex-start;
    cursor: pointer;

    &.is-folder {
      .file-name {
        color: #409EFF;
        font-weight: 600;
      }
    }

    .file-icon {
      margin-right: 10px;
      margin-top: 2px;
      &.file-folder {
        color: #E6A23C;
        font-size: 26px;
      }
    }

    .file-type-icon-wrap {
      position: relative;
      width: 28px;
      height: 32px;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      ::v-deep .file-type-svg {
        width: 26px;
        height: 30px;
        filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.08));
      }

      .file-ext-mini {
        position: absolute;
        right: -7px;
        bottom: -4px;
        min-width: 18px;
        height: 12px;
        padding: 0 3px;
        border-radius: 8px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        background: #606266;
        color: #fff;
        font-size: 8px;
        font-weight: 600;
        letter-spacing: 0.2px;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.16);
      }
    }

    .file-info {
      flex: 1;
      .file-name {
        font-weight: 500;
        color: #303133;
      }
      .search-summary {
        margin-top: 6px;
        font-size: 12px;
        color: #606266;
        line-height: 1.5;
        background: #fafafa;
        padding: 6px;
        border-radius: 4px;
        white-space: normal; /* 确保搜索长文本可以换行 */
      }
      .file-path-hint {
        margin-top: 4px;
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .op-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: nowrap;
    white-space: nowrap;
    width: 100%;
    min-width: 0;

    .op-primary {
      display: inline-flex;
      align-items: center;
      min-width: 0;
      white-space: nowrap;
      overflow: hidden;
    }

    .op-btn {
      padding: 0 2px;
    }

    .op-btn + .op-btn {
      margin-left: 8px;
    }

    .op-danger {
      color: #F56C6C;
    }

    .op-more {
      display: none;
      margin-left: 8px;
      flex: 0 0 auto;
    }

    .el-dropdown-link {
      color: #409EFF;
      font-size: 12px;
      cursor: pointer;
      user-select: none;
    }

    &.is-overflow {
      .op-collapse {
        display: none;
      }

      .op-more {
        display: inline-flex;
        align-items: center;
      }
    }
  }

  .upload-dialog-content {
    width: 100%;
    max-width: 460px;
    margin: 0 auto;
  }

  .upload-dialog-path {
    margin-bottom: 10px;
    color: #606266;
    font-size: 14px;
    font-weight: 400;
    line-height: 1.5;
    text-align: center;
    word-break: break-all;
  }

  .uploading-status {
    margin-bottom: 10px;
    color: #409EFF;
    font-size: 14px;
    font-weight: 400;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    text-align: center;
  }

  .upload-dialog-uploader {
    width: 100%;
    max-width: 460px;
    margin: 0 auto;

    ::v-deep .el-upload {
      width: 100%;
      display: block;
    }

    ::v-deep .el-upload-dragger {
      width: 100%;
      height: 190px;
      min-height: 190px;
      max-height: 190px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: 1px dashed #b7cdf0;
      border-radius: 8px;
      background: #f8fbff;
      transition: border-color 0.2s ease, background-color 0.2s ease;
      box-sizing: border-box;
      padding: 22px 20px;
      margin: 0 auto;
    }

    ::v-deep .el-upload-dragger:hover {
      border-color: #409EFF;
      background: #eef5ff;
    }

    .upload-drop-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      height: 100%;
      text-align: center;
    }

    .el-upload__text {
      margin-top: 0;
      color: #5d6470;
      font-size: 18px;
      font-weight: 500;
      letter-spacing: 0;
      line-height: 1.4;
    }

    .el-upload__tip {
      margin-top: 0;
      color: #8f97a5;
      font-size: 14px;
      line-height: 1;
    }

    .upload-or-tip {
      font-size: 14px;
      margin-top: 0;
    }

    .upload-trigger-btn {
      margin-top: 2px;
      min-width: 132px;
      height: 36px;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      letter-spacing: 0;
      border: none;
      background: linear-gradient(90deg, #67b4f2 0%, #5067f2 100%);
      box-shadow: 0 6px 14px rgba(80, 103, 242, 0.26);
    }

    .upload-limit-tip {
      margin-top: 0;
      font-size: 13px;
      color: #7f8794;
    }

    ::v-deep .el-upload-list {
      margin-top: 12px;
      max-height: 150px;
      overflow-y: auto;
    }
  }

  ::v-deep .upload-file-dialog {
    width: calc(100vw - 32px);
    max-width: 620px;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 8px 22px rgba(0, 0, 0, 0.12);
    border: none;

    .el-dialog__header {
      border-bottom: none;
      padding: 16px 20px 8px;
      background: #fff;
    }

    .el-dialog__title {
      font-size: 20px;
      font-weight: 500;
      color: #303133;
    }

    .el-dialog__body {
      padding: 8px 20px 10px;
      background: #fff;
      display: flex;
      justify-content: center;
    }

    .el-dialog__footer {
      border-top: none;
      padding: 14px 20px 16px;
      background: #fff;
    }

    .dialog-footer .el-button {
      min-width: 120px;
      height: 42px;
      border-radius: 6px;
      font-size: 15px;
      font-weight: 500;
    }
  }

  /* 表格内加载状态行样式 */
  .loader-row {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 16px 0;

    > div {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 20px;
      border-radius: 20px;
      font-size: 13px;
    }

    .loader-loading {
      background: #ecf5ff;
      color: #409EFF;

      i {
        font-size: 16px;
        animation: rotating 1s linear infinite;
      }
    }

    .loader-more {
      background: #f4f4f5;
      color: #909399;
      animation: pulse 2s ease-in-out infinite;

      i {
        font-size: 14px;
        animation: bounce 1.5s ease-in-out infinite;
      }

      .loader-count {
        color: #c0c4cc;
        font-size: 12px;
      }
    }

    .loader-done {
      background: #f0f9eb;
      color: #67C23A;

      i {
        font-size: 14px;
      }

      .loader-count {
        color: #a0c98f;
        font-size: 12px;
      }
    }
  }

  /* 加载动画 */
  @keyframes rotating {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(3px); }
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.6; }
  }
}
</style>

<style lang="scss">
.upload-file-dialog {
  width: calc(100vw - 32px);
  max-width: 620px;
  border-radius: 10px;
  overflow: hidden;
}

.upload-file-dialog .el-dialog__body {
  padding: 8px 20px 10px;
  display: flex;
  justify-content: center;
}

.upload-file-dialog .upload-dialog-content,
.upload-file-dialog .upload-dialog-uploader {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
}

.upload-file-dialog .upload-dialog-uploader {
  display: flex;
  justify-content: center;
}

.upload-file-dialog .upload-dialog-uploader .el-upload,
.upload-file-dialog .upload-dialog-uploader .el-upload-dragger {
  width: 100%;
}

.upload-file-dialog .upload-dialog-uploader .el-upload-dragger {
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-file-dialog .upload-drop-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-file-dialog .upload-drop-content > *:not(.upload-trigger-btn) {
  width: 100%;
  text-align: center;
  margin-left: auto;
  margin-right: auto;
}

.upload-file-dialog .upload-trigger-btn {
  width: auto;
  min-width: 132px;
  padding: 0 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.upload-file-dialog .upload-dialog-path,
.upload-file-dialog .uploading-status,
.upload-file-dialog .upload-drop-content,
.upload-file-dialog .el-upload__text,
.upload-file-dialog .el-upload__tip {
  text-align: center;
}

.upload-file-dialog .uploading-status {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>

<style lang="scss">
/* 文本预览对话框样式（非 scoped，因为 $alert 挂载到 body） */
.text-preview-dialog {
  .el-message-box__content {
    overflow: visible !important;
  }
  .el-message-box__message {
    max-height: 55vh !important;
    overflow-y: auto !important;
    white-space: pre-wrap;
    word-wrap: break-word;
    font-family: 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.6;
    background: #f5f7fa;
    padding: 15px;
    border-radius: 4px;
  }
}
</style>
