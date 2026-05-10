<template>
  <div class="file-preview-container">
    <!-- 预览区域 - 根据文件类型动态渲染 -->
    <div class="preview-area" v-if="fileData">
      <!-- Word 预览 -->
      <vue-office-docx 
        v-if="fileType === 'docx'"
        :src="fileData"
        style="height: 70vh; width: 100%;"
        @rendered="handleRenderSuccess"
        @error="handleRenderError"
      />
      
      <!-- PDF 预览 -->
      <vue-office-pdf 
        v-else-if="fileType === 'pdf'"
        :src="fileData"
        style="height: 70vh; width: 100%;"
        @rendered="handleRenderSuccess"
        @error="handleRenderError"
      />
      
      <!-- Excel 预览 -->
      <vue-office-excel 
        v-else-if="fileType === 'xlsx' || fileType === 'xls'"
        :src="fileData"
        style="height: 70vh; width: 100%;"
        @rendered="handleRenderSuccess"
        @error="handleRenderError"
      />
      
      <!-- 不支持的文件类型提示 -->
      <div v-else class="unsupported">
        暂不支持 .{{ fileType }} 格式的预览
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="loading">
      <i class="el-icon-loading"></i>
      <p>正在加载文档...</p>
    </div>

    <!-- 未选择文件时的占位提示 -->
    <div v-else class="placeholder">
      <div class="placeholder-icon">📄</div>
      <p>请选择 Word、PDF 或 Excel 文件进行预览</p>
    </div>
  </div>
</template>

<script>
import VueOfficeDocx from '@vue-office/docx'
import VueOfficePdf from '@vue-office/pdf'
import VueOfficeExcel from '@vue-office/excel'

import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'

export default {
  name: 'FilePreview',
  components: {
    VueOfficeDocx,
    VueOfficePdf,
    VueOfficeExcel
  },
  props: {
    // 文件数据 (ArrayBuffer)
    src: {
      type: [ArrayBuffer, String],
      default: null
    },
    // 文件类型
    type: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false
    }
  },
  computed: {
    fileData() {
      return this.src
    },
    fileType() {
      return this.type.toLowerCase()
    }
  },
  methods: {
    handleRenderSuccess() {
      this.loading = false
      this.$emit('rendered')
    },
    handleRenderError(error) {
      this.loading = false
      this.$emit('error', error)
    }
  },
  watch: {
    src: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.loading = true
        }
      }
    }
  }
}
</script>

<style scoped>
.file-preview-container {
  width: 100%;
  height: 100%;
}

.preview-area {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
  height: 70vh;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 70vh;
  background-color: #f5f7fa;
  border-radius: 8px;
  color: #909399;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.placeholder p {
  font-size: 14px;
  margin: 0;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 70vh;
  background-color: #f5f7fa;
  border-radius: 8px;
  color: #409EFF;
}

.loading i {
  font-size: 48px;
  margin-bottom: 16px;
}

.unsupported {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 70vh;
  color: #f56c6c;
  font-size: 14px;
  background-color: #fef0f0;
}
</style>
