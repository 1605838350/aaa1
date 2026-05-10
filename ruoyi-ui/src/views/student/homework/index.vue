<template>
  <div class="app-container">
    <!-- 班级切换 -->
    <el-row class="mb8">
      <el-col :span="24">
        <span style="margin-right: 10px; font-weight: bold;">年级：</span>
        <el-select v-model="queryGrade" placeholder="请选择年级" style="width: 120px" @change="handleGradeChange">
          <el-option
            v-for="item in gradeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <span style="margin-left: 15px; margin-right: 10px; font-weight: bold;">班级：</span>
        <el-select v-model="queryClassNum" placeholder="请选择班级" style="width: 120px" @change="handleClassChange">
          <el-option
            v-for="item in classOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="queryParams.title"
          placeholder="搜索作业标题"
          clearable
          style="width: 200px; margin-left: 15px;"
          @keyup.enter.native="handleQuery"
        />
        <el-button type="primary" icon="el-icon-search" size="mini" style="margin-left: 10px;" @click="handleQuery">搜索</el-button>
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          style="margin-left: 15px;"
          @click="handleAdd"
          v-hasPermi="['homework:list:add']"
        >发布作业</el-button>
      </el-col>
    </el-row>

    <!-- 作业列表 -->
    <el-table v-loading="loading" :data="homeworkList">
      <el-table-column label="作业标题" align="center" prop="title" />
      <el-table-column label="内容" align="center" prop="content" show-overflow-tooltip />
      <el-table-column label="截止时间" align="center" prop="deadline" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.deadline, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="存储位置" align="center" prop="storagePath" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="提交情况" align="center" width="120">
        <template slot-scope="scope">
          <span v-if="scope.row.submitCount !== null">
            已交 {{ scope.row.submitCount }}
            <span v-if="scope.row.gradedCount > 0"> / 批 {{ scope.row.gradedCount }}</span>
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="250">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['homework:list:detail']"
          >查看提交</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['homework:list:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['homework:list:remove']"
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

    <!-- 发布/修改作业对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入作业标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" placeholder="请输入作业内容/要求" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            placeholder="选择截止时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%" @change="onGradeChange">
            <el-option
              v-for="item in gradeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classNum">
          <el-select v-model="form.classNum" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="item in classOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="存储位置" prop="storagePath">
          <el-input v-model="form.storagePath" placeholder="请选择文件存储路径" readonly>
            <el-button slot="append" icon="el-icon-folder-opened" @click="openDirSelect">选择</el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="上传限制(MB)" prop="maxFileSize">
          <el-input-number v-model="form.maxFileSize" :min="1" :max="500" placeholder="MB" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 查看提交详情抽屉 -->
    <el-drawer :title="detailTitle" :visible.sync="detailOpen" size="65%" append-to-body>
      <div style="padding: 0 20px 20px;">
        <el-form :model="submitQuery" ref="submitQueryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="学生姓名" prop="studentName">
            <el-input
              v-model="submitQuery.studentName"
              placeholder="请输入学生姓名"
              clearable
              style="width: 200px"
              @keyup.enter.native="getSubmitList"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="getSubmitList">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetSubmitQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-radio-group v-model="submitStatusFilter" size="small" style="margin-bottom: 12px;">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="submitted">已提交</el-radio-button>
          <el-radio-button label="pending">待批改</el-radio-button>
          <el-radio-button label="graded">已批改</el-radio-button>
          <el-radio-button label="unsubmitted">未提交</el-radio-button>
        </el-radio-group>

        <el-table v-loading="submitLoading" :data="filteredSubmitList" border>
          <el-table-column label="学生姓名" align="center" prop="studentName" width="100" />
          <el-table-column label="账号" align="center" prop="account" width="120" />
          <el-table-column label="提交时间" align="center" prop="submitTime" width="160">
            <template slot-scope="scope">
              <span>{{ scope.row.submitTime ? parseTime(scope.row.submitTime) : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="文件名" align="center" prop="fileName" show-overflow-tooltip>
            <template slot-scope="scope">
              <span v-if="scope.row.fileName" class="preview-link" @click="handlePreview(scope.row)">{{ scope.row.fileName }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="分数" align="center" prop="score" width="80">
            <template slot-scope="scope">
              <span>{{ scope.row.score !== null ? scope.row.score : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.fileName" :type="scope.row.status === '1' ? 'success' : 'warning'">
                {{ scope.row.status === '1' ? '已批改' : '已提交' }}
              </el-tag>
              <el-tag v-else type="info">未提交</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
            <template slot-scope="scope">
              <el-button v-if="scope.row.fileName" size="mini" type="text" icon="el-icon-download" @click="handleDownload(scope.row)">下载</el-button>
              <el-button v-if="scope.row.fileName" size="mini" type="text" icon="el-icon-edit" @click="handleGrade(scope.row)" v-hasPermi="['homework:list:grade']">批改</el-button>
              <el-button v-if="scope.row.fileName" size="mini" type="text" icon="el-icon-delete" @click="handleDeleteSubmit(scope.row)" v-hasPermi="['homework:list:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="submitTotal>0"
          :total="submitTotal"
          :page.sync="submitQuery.pageNum"
          :limit.sync="submitQuery.pageSize"
          @pagination="getSubmitList"
        />
      </div>
    </el-drawer>

    <!-- 批改对话框 -->
    <el-dialog title="批改作业" :visible.sync="gradeOpen" width="400px" append-to-body>
      <el-form ref="gradeForm" :model="gradeForm" label-width="60px">
        <el-form-item label="学生">
          <span>{{ gradeForm.studentName }}</span>
        </el-form-item>
        <el-form-item label="分数">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitGrade">确 定</el-button>
        <el-button @click="gradeOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      :title="previewTitle"
      :visible.sync="previewVisible"
      width="80%"
      :close-on-click-modal="false"
      append-to-body
    >
      <FilePreview
        :src="previewData"
        :type="previewType"
        @rendered="() => {}"
        @error="() => $message.error('文档预览失败')"
      />
    </el-dialog>

    <!-- 目录选择对话框 -->
    <el-dialog title="选择存储目录" :visible.sync="dirSelectOpen" width="400px" append-to-body>
      <div style="max-height: 400px; overflow-y: auto;">
        <el-tree
          ref="dirTree"
          :load="loadDirNode"
          lazy
          :props="dirTreeProps"
          node-key="path"
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleDirNodeClick"
        >
          <span class="custom-tree-node" slot-scope="{ node }">
            <i :class="node.expanded ? 'el-icon-folder-opened' : 'el-icon-folder'" style="color: #E6A23C; margin-right: 5px;"></i>
            <span>{{ node.label }}</span>
          </span>
        </el-tree>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="success" plain icon="el-icon-folder-add" @click="createNewFolder" :disabled="!selectedDirPath">新建文件夹</el-button>
        <el-button type="primary" @click="confirmDirSelect" :disabled="!selectedDirPath">确 定</el-button>
        <el-button @click="dirSelectOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 图片灯箱预览 -->
    <transition name="homework-viewer-fade">
      <div v-if="imageViewerVisible" class="image-viewer-overlay" @click.self="closeImageViewer">
        <button class="image-viewer-reset" type="button" @click="resetImageTransform">重置</button>
        <button class="image-viewer-close" type="button" @click="closeImageViewer"><i class="el-icon-close"></i></button>
        <button v-if="imageViewerFiles.length > 1" class="image-viewer-nav image-viewer-nav-prev" type="button" @click.stop="showPrevImage"><i class="el-icon-arrow-left"></i></button>
        <div class="image-viewer-stage" @wheel.prevent="handleImageWheel">
          <i v-if="imageViewerLoading" class="el-icon-loading image-viewer-loading"></i>
          <img v-else-if="imageViewerUrl" :src="imageViewerUrl" :alt="imageViewerCurrentFileName" class="image-viewer-img" :class="{ 'is-draggable': imageViewerScale > 1, 'is-dragging': imageViewerDragging }" :style="imageViewerTransformStyle" @mousedown.prevent="startImageDrag" @dblclick.stop="resetImageTransform" @click.stop />
          <div v-else class="image-viewer-error">{{ imageViewerError || '图片加载失败' }}</div>
        </div>
        <button v-if="imageViewerFiles.length > 1" class="image-viewer-nav image-viewer-nav-next" type="button" @click.stop="showNextImage"><i class="el-icon-arrow-right"></i></button>
        <div v-if="imageViewerFiles.length > 0" class="image-viewer-index">{{ imageViewerIndex + 1 }} / {{ imageViewerFiles.length }} <span class="image-viewer-name">{{ imageViewerCurrentFileName }}</span></div>
      </div>
    </transition>

    <!-- 视频灯箱预览 -->
    <transition name="homework-viewer-fade">
      <div v-if="videoViewerVisible" class="video-viewer-overlay" @click.self="closeVideoViewer">
        <button class="video-viewer-close" type="button" @click="closeVideoViewer"><i class="el-icon-close"></i></button>
        <button v-if="videoViewerFiles.length > 1" class="video-viewer-nav video-viewer-nav-prev" type="button" @click.stop="showPrevVideo"><i class="el-icon-arrow-left"></i></button>
        <div class="video-viewer-stage" @click.stop @wheel.prevent="handleVideoWheel">
          <i v-if="videoViewerLoading" class="el-icon-loading image-viewer-loading"></i>
          <video v-else-if="videoViewerUrl" :src="videoViewerUrl" class="video-viewer-video" :style="videoViewerTransformStyle" controls autoplay preload="metadata" @dblclick.stop="resetVideoTransform" />
          <div v-else class="image-viewer-error">{{ videoViewerError || '视频加载失败' }}</div>
        </div>
        <button v-if="videoViewerFiles.length > 1" class="video-viewer-nav video-viewer-nav-next" type="button" @click.stop="showNextVideo"><i class="el-icon-arrow-right"></i></button>
        <div v-if="videoViewerFiles.length > 0" class="video-viewer-index">{{ videoViewerIndex + 1 }} / {{ videoViewerFiles.length }} <span class="image-viewer-name">{{ videoViewerCurrentFileName }}</span></div>
      </div>
    </transition>
  </div>
</template>

<script>
import { listHomework, getHomework, addHomework, updateHomework, delHomework, listHomeworkSubmit, gradeHomework, deleteHomeworkSubmit } from "@/api/system/homework";
import { listFiles, createFolder, previewFile, getPreviewUrl } from "@/api/tool/file";
import FilePreview from "@/components/FilePreview";
import mediaViewer from "@/mixins/mediaViewer";

export default {
  name: "HomeworkList",
  components: { FilePreview },
  mixins: [mediaViewer],
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      total: 0,
      homeworkList: [],
      open: false,
      title: "",
      detailOpen: false,
      detailTitle: "",
      submitLoading: false,
      submitTotal: 0,
      submitList: [],
      gradeOpen: false,
      dirSelectOpen: false,
      selectedDirPath: "",
      // 文件预览
      previewVisible: false,
      previewData: null,
      previewTitle: '',
      previewType: '',
      dirTreeProps: {
        label: 'label',
        children: 'children',
        isLeaf: 'leaf'
      },
      queryGrade: null,
      queryClassNum: null,
      gradeOptions: [
        { value: 1, label: '一年级' },
        { value: 2, label: '二年级' },
        { value: 3, label: '三年级' },
        { value: 4, label: '四年级' },
        { value: 5, label: '五年级' },
        { value: 6, label: '六年级' }
      ],
      classOptions: [
        { value: 1, label: '1班' },
        { value: 2, label: '2班' },
        { value: 3, label: '3班' },
        { value: 4, label: '4班' },
        { value: 5, label: '5班' },
        { value: 6, label: '6班' }
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        admissionYear: null,
        classNum: null
      },
      submitQuery: {
        pageNum: 1,
        pageSize: 10,
        homeworkId: null,
        studentName: null
      },
      submitStatusFilter: '',
      form: {},
      gradeForm: {},
      rules: {
        title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
        grade: [{ required: true, message: "年级不能为空", trigger: "change" }],
        classNum: [{ required: true, message: "班级不能为空", trigger: "change" }],
        storagePath: [{ required: true, message: "存储位置不能为空", trigger: "blur" }]
      }
    };
  },
  computed: {
    filteredSubmitList() {
      if (!this.submitStatusFilter) return this.submitList;
      return this.submitList.filter(row => {
        if (this.submitStatusFilter === 'submitted') return row.fileName;
        if (this.submitStatusFilter === 'pending') return row.fileName && row.status !== '1';
        if (this.submitStatusFilter === 'graded') return row.status === '1';
        if (this.submitStatusFilter === 'unsubmitted') return !row.fileName;
        return true;
      });
    }
  },
  created() {
    this.queryGrade = 1;
    this.queryClassNum = 1;
    this.updateQueryAdmissionYear();
    this.getList();
  },
  methods: {
    updateQueryAdmissionYear() {
      if (this.queryGrade) {
        const currentYear = new Date().getFullYear();
        this.queryParams.admissionYear = currentYear - this.queryGrade;
      }
      if (this.queryClassNum) {
        this.queryParams.classNum = this.queryClassNum;
      }
    },
    handleGradeChange() {
      this.updateQueryAdmissionYear();
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleClassChange() {
      this.updateQueryAdmissionYear();
      this.queryParams.pageNum = 1;
      this.getList();
    },
    getList() {
      this.loading = true;
      listHomework(this.queryParams).then(response => {
        this.homeworkList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      const currentYear = new Date().getFullYear();
      this.form = {
        homeworkId: undefined,
        title: undefined,
        content: undefined,
        deadline: undefined,
        grade: this.queryGrade,
        classNum: this.queryClassNum,
        admissionYear: currentYear - this.queryGrade,
        storagePath: undefined,
        maxFileSize: 500,
        status: "0"
      };
      this.resetForm("form");
    },
    onGradeChange() {
      if (this.form.grade) {
        const currentYear = new Date().getFullYear();
        this.form.admissionYear = currentYear - this.form.grade;
      }
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "发布作业";
    },
    handleUpdate(row) {
      this.reset();
      const homeworkId = row.homeworkId;
      getHomework(homeworkId).then(response => {
        this.form = response.data;
        if (this.form.maxFileSize == null) {
          this.form.maxFileSize = 500;
        }
        const currentYear = new Date().getFullYear();
        this.form.grade = currentYear - this.form.admissionYear;
        this.open = true;
        this.title = "修改作业";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.homeworkId != undefined) {
            updateHomework(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addHomework(this.form).then(() => {
              this.$modal.msgSuccess("发布成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除作业"' + row.title + '"？').then(() => {
        return delHomework(row.homeworkId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleDetail(row) {
      this.detailTitle = `"${row.title}" 提交情况`;
      this.submitQuery.homeworkId = row.homeworkId;
      this.submitQuery.pageNum = 1;
      this.submitQuery.studentName = null;
      this.submitStatusFilter = '';
      this.detailOpen = true;
      this.getSubmitList();
    },
    resetSubmitQuery() {
      this.submitQuery.studentName = null;
      this.submitQuery.pageNum = 1;
      this.getSubmitList();
    },
    getSubmitList() {
      this.submitLoading = true;
      listHomeworkSubmit(this.submitQuery).then(response => {
        this.submitList = response.rows;
        this.submitTotal = response.total;
        this.submitLoading = false;
      });
    },
    handleDownload(row) {
      this.download(row.filePath, {}, row.fileName);
    },
    handlePreview(row) {
      if (!row.fileName) return;
      const previewPath = row.filePath || '';
      const fileName = row.fileName.toLowerCase();
      const isPdf = /\.pdf$/i.test(fileName);
      const isWord = /\.(doc|docx)$/i.test(fileName);
      const isExcel = /\.(xls|xlsx)$/i.test(fileName);
      const isImage = this.isImageFile(fileName);
      const isVideo = this.isVideoFile(fileName);
      const isText = /\.(txt|log|md|json|xml|yaml|yml|js|css|html|java|py|c|cpp|h|hpp)$/i.test(fileName);

      if (isWord || isExcel || isPdf) {
        this.previewOfficeFile(row, previewPath);
      } else if (isImage) {
        this.openImageViewer({ fileName: row.fileName, path: previewPath, isDirectory: false });
      } else if (isVideo) {
        this.openVideoViewer({ fileName: row.fileName, path: previewPath, isDirectory: false });
      } else if (isText) {
        // 文本文件：读取内容显示
        previewFile(previewPath).then(blob => {
          const reader = new FileReader();
          reader.onload = (e) => {
            this.$alert(e.target.result || '(空文件)', row.fileName, {
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
        }).then(() => this.handleDownload(row)).catch(() => {});
      }
    },
    fetchPreviewBlob(path) {
      return previewFile(path);
    },
    getMediaCandidates() {
      return this.submitList.filter(row => row.fileName).map(row => ({
        fileName: row.fileName,
        path: row.filePath,
        isDirectory: false
      }));
    },
    previewOfficeFile(row, previewPath) {
      const ext = row.fileName.split('.').pop().toLowerCase();
      const url = getPreviewUrl(previewPath);

      this.previewData = url;
      this.previewType = ext;
      this.previewTitle = row.fileName;
      this.previewVisible = true;
    },
    handleGrade(row) {
      this.gradeForm = {
        submitId: row.submitId,
        studentName: row.studentName,
        score: row.score || 0,
        comment: row.comment || ""
      };
      this.gradeOpen = true;
    },
    submitGrade() {
      gradeHomework(this.gradeForm).then(() => {
        this.$modal.msgSuccess("批改成功");
        this.gradeOpen = false;
        this.getSubmitList();
      });
    },
    handleDeleteSubmit(row) {
      this.$modal.confirm('是否确认删除学生 "' + row.studentName + '" 的作业提交？文件将一并删除。').then(() => {
        return deleteHomeworkSubmit(row.submitId);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.getSubmitList();
      }).catch(() => {});
    },
    openDirSelect() {
      this.selectedDirPath = "";
      this.dirSelectOpen = true;
      // 弹窗打开后自动展开根目录
      this.$nextTick(() => {
        setTimeout(() => {
          const tree = this.$refs.dirTree;
          if (tree) {
            const rootNode = tree.getNode('/');
            if (rootNode && !rootNode.expanded) {
              rootNode.expand();
            }
          }
        }, 200);
      });
    },
    loadDirNode(node, resolve) {
      if (node.level === 0) {
        return resolve([{
          label: '/',
          path: '/',
          leaf: false
        }]);
      }
      const path = node.data.path;
      listFiles(path, 1, 50, false).then(response => {
        const files = response.data || [];
        const dirs = files.filter(f => f.directory === true || f.isDirectory === true);
        const nodes = dirs.map(dir => ({
          label: dir.name || dir.fileName,
          path: dir.path,
          leaf: false
        }));
        resolve(nodes);
      }).catch(() => {
        resolve([]);
      });
    },
    handleDirNodeClick(data) {
      this.selectedDirPath = data.path;
    },
    confirmDirSelect() {
      this.form.storagePath = this.selectedDirPath;
      this.dirSelectOpen = false;
    },
    createNewFolder() {
      if (!this.selectedDirPath) {
        this.$message.warning("请先选择一个目录");
        return;
      }
      this.$prompt('请输入文件夹名称', '新建文件夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return '文件夹名称不能为空';
          }
          return true;
        }
      }).then(({ value }) => {
        const folderName = value.trim();
        createFolder(this.selectedDirPath, folderName).then(() => {
          this.$modal.msgSuccess("创建成功");
          // 刷新当前选中的目录节点，重新加载子目录
          const tree = this.$refs.dirTree;
          if (tree) {
            const node = tree.getNode(this.selectedDirPath);
            if (node) {
              node.loaded = false;
              node.isLeaf = false;
              node.loadData(() => {
                node.expanded = true;
              });
            }
          }
        }).catch((err) => {
          this.$message.error(err.message || "创建失败");
        });
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
.preview-link {
  color: #409EFF;
  cursor: pointer;
}
.preview-link:hover {
  text-decoration: underline;
}

/* 图片/视频灯箱 */
.homework-viewer-fade-enter-active,
.homework-viewer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.homework-viewer-fade-enter,
.homework-viewer-fade-leave-to {
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
</style>
