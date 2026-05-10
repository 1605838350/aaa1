/**
 * 图片/视频灯箱预览 mixin
 *
 * 使用组件需提供:
 *   - fetchPreviewBlob(path): Promise<Blob>  获取文件 blob
 *   - getMediaCandidates(): Array              返回当前可预览的媒体文件列表
 *     每个元素需包含: fileName, path
 */
export default {
  data() {
    return {
      // 图片灯箱
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

      // 视频灯箱
      videoViewerVisible: false,
      videoViewerFiles: [],
      videoViewerIndex: 0,
      videoViewerUrl: '',
      videoViewerLoading: false,
      videoViewerError: '',
      videoViewerScale: 1
    };
  },
  computed: {
    imageViewerCurrentFileName() {
      const f = this.imageViewerFiles[this.imageViewerIndex];
      return f ? f.fileName : '';
    },
    videoViewerCurrentFileName() {
      const f = this.videoViewerFiles[this.videoViewerIndex];
      return f ? f.fileName : '';
    },
    imageViewerTransformStyle() {
      return {
        transform: `translate(${this.imageViewerOffsetX}px, ${this.imageViewerOffsetY}px) scale(${this.imageViewerScale})`
      };
    },
    videoViewerTransformStyle() {
      return {
        transform: `scale(${this.videoViewerScale})`
      };
    }
  },
  mounted() {
    window.addEventListener('keydown', this._mediaViewerKeydown);
    window.addEventListener('mousemove', this._mediaViewerDragMove);
    window.addEventListener('mouseup', this._mediaViewerEndDrag);
    window.addEventListener('wheel', this._mediaViewerGlobalWheel, { passive: false });
  },
  beforeDestroy() {
    window.removeEventListener('keydown', this._mediaViewerKeydown);
    window.removeEventListener('mousemove', this._mediaViewerDragMove);
    window.removeEventListener('mouseup', this._mediaViewerEndDrag);
    window.removeEventListener('wheel', this._mediaViewerGlobalWheel);
    this.clearImageViewerBlobUrls();
    this.clearVideoViewerBlobUrls();
  },
  methods: {
    // ---- 工具方法 ----
    isImageFile(fileName) {
      return /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName || '');
    },
    isVideoFile(fileName) {
      return /\.(mp4|webm|ogg|m4v|mov|avi|mkv|flv|wmv|rmvb)$/i.test(fileName || '');
    },

    // ---- 图片灯箱 ----
    _mediaPath(fileItem) {
      return fileItem.path || fileItem.filePath || '';
    },
    getImageCandidates() {
      const list = this.getMediaCandidates ? this.getMediaCandidates() : [];
      return list.filter(f => !f.isDirectory && this.isImageFile(f.fileName));
    },
    getVideoCandidates() {
      const list = this.getMediaCandidates ? this.getMediaCandidates() : [];
      return list.filter(f => !f.isDirectory && this.isVideoFile(f.fileName));
    },

    async ensureImageBlobUrl(fileItem) {
      if (!fileItem) return '';
      if (fileItem._blobUrl) return fileItem._blobUrl;
      const blob = await this.fetchPreviewBlob(this._mediaPath(fileItem));
      const blobUrl = URL.createObjectURL(blob);
      this.$set(fileItem, '_blobUrl', blobUrl);
      return blobUrl;
    },
    async ensureVideoBlobUrl(fileItem) {
      if (!fileItem) return '';
      if (fileItem._videoBlobUrl) return fileItem._videoBlobUrl;
      const blob = await this.fetchPreviewBlob(this._mediaPath(fileItem));
      const blobUrl = URL.createObjectURL(blob);
      this.$set(fileItem, '_videoBlobUrl', blobUrl);
      return blobUrl;
    },

    async loadCurrentImage() {
      const file = this.imageViewerFiles[this.imageViewerIndex];
      if (!file) {
        this.imageViewerUrl = '';
        this.imageViewerError = '未找到图片';
        return;
      }
      this.imageViewerLoading = true;
      this.imageViewerError = '';
      try {
        this.imageViewerUrl = await this.ensureImageBlobUrl(file);
        this.preloadAdjacentImages();
      } catch (_e) {
        this.imageViewerUrl = '';
        this.imageViewerError = '图片加载失败';
      } finally {
        this.imageViewerLoading = false;
      }
    },
    preloadAdjacentImages() {
      if (this.imageViewerFiles.length < 2) return;
      const total = this.imageViewerFiles.length;
      const prev = (this.imageViewerIndex - 1 + total) % total;
      const next = (this.imageViewerIndex + 1) % total;
      [prev, next].forEach(i => {
        const f = this.imageViewerFiles[i];
        if (f && !f._blobUrl) this.ensureImageBlobUrl(f).catch(() => {});
      });
    },

    async openImageViewer(file) {
      const candidates = this.getImageCandidates();
      if (candidates.length === 0) {
        this.$message.warning('当前没有可预览的图片');
        return;
      }
      const filePath = this._mediaPath(file);
      const idx = candidates.findIndex(f => this._mediaPath(f) === filePath);
      this.imageViewerFiles = candidates;
      this.imageViewerIndex = idx >= 0 ? idx : 0;
      this.imageViewerVisible = true;
      this.imageViewerUrl = '';
      this.imageViewerError = '';
      this.resetImageTransform();
      await this.loadCurrentImage();
    },

    showPrevImage() {
      if (this.imageViewerFiles.length <= 1) return;
      this.imageViewerIndex = (this.imageViewerIndex - 1 + this.imageViewerFiles.length) % this.imageViewerFiles.length;
      this.resetImageTransform();
      this.loadCurrentImage();
    },
    showNextImage() {
      if (this.imageViewerFiles.length <= 1) return;
      this.imageViewerIndex = (this.imageViewerIndex + 1) % this.imageViewerFiles.length;
      this.resetImageTransform();
      this.loadCurrentImage();
    },
    resetImageTransform() {
      this.imageViewerScale = 1;
      this.imageViewerOffsetX = 0;
      this.imageViewerOffsetY = 0;
      this.imageViewerDragging = false;
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
    clearImageViewerBlobUrls() {
      (this.imageViewerFiles || []).forEach(f => {
        if (f && f._blobUrl) {
          URL.revokeObjectURL(f._blobUrl);
          this.$delete(f, '_blobUrl');
        }
      });
    },

    handleImageWheel(event) {
      if (!this.imageViewerUrl || this.imageViewerLoading) return;
      const dir = event.deltaY < 0 ? 1 : -1;
      const ns = this.imageViewerScale + dir * 0.12;
      const clamped = Math.max(1, Math.min(5, Number(ns.toFixed(2))));
      this.imageViewerScale = clamped;
      if (clamped === 1) {
        this.imageViewerOffsetX = 0;
        this.imageViewerOffsetY = 0;
      }
    },
    startImageDrag(event) {
      if (this.imageViewerScale <= 1 || !this.imageViewerUrl) return;
      this.imageViewerDragging = true;
      this.imageViewerDragStartX = event.clientX;
      this.imageViewerDragStartY = event.clientY;
      this.imageViewerStartOffsetX = this.imageViewerOffsetX;
      this.imageViewerStartOffsetY = this.imageViewerOffsetY;
    },
    _mediaViewerDragMove(event) {
      if (!this.imageViewerDragging) return;
      this.imageViewerOffsetX = this.imageViewerStartOffsetX + event.clientX - this.imageViewerDragStartX;
      this.imageViewerOffsetY = this.imageViewerStartOffsetY + event.clientY - this.imageViewerDragStartY;
    },
    _mediaViewerEndDrag() {
      this.imageViewerDragging = false;
    },

    // ---- 视频灯箱 ----
    async loadCurrentVideo() {
      const file = this.videoViewerFiles[this.videoViewerIndex];
      if (!file) {
        this.videoViewerUrl = '';
        this.videoViewerError = '未找到视频';
        return;
      }
      this.videoViewerLoading = true;
      this.videoViewerError = '';
      try {
        this.videoViewerUrl = await this.ensureVideoBlobUrl(file);
      } catch (_e) {
        this.videoViewerUrl = '';
        this.videoViewerError = '视频加载失败';
      } finally {
        this.videoViewerLoading = false;
      }
    },

    async openVideoViewer(file) {
      const candidates = this.getVideoCandidates();
      if (candidates.length === 0) {
        this.$message.warning('当前没有可预览的视频');
        return;
      }
      const filePath = this._mediaPath(file);
      const idx = candidates.findIndex(f => this._mediaPath(f) === filePath);
      this.videoViewerFiles = candidates;
      this.videoViewerIndex = idx >= 0 ? idx : 0;
      this.videoViewerVisible = true;
      this.videoViewerUrl = '';
      this.videoViewerError = '';
      this.resetVideoTransform();
      await this.loadCurrentVideo();
    },
    showPrevVideo() {
      if (this.videoViewerFiles.length <= 1) return;
      this.videoViewerIndex = (this.videoViewerIndex - 1 + this.videoViewerFiles.length) % this.videoViewerFiles.length;
      this.resetVideoTransform();
      this.loadCurrentVideo();
    },
    showNextVideo() {
      if (this.videoViewerFiles.length <= 1) return;
      this.videoViewerIndex = (this.videoViewerIndex + 1) % this.videoViewerFiles.length;
      this.resetVideoTransform();
      this.loadCurrentVideo();
    },
    resetVideoTransform() {
      this.videoViewerScale = 1;
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
    clearVideoViewerBlobUrls() {
      (this.videoViewerFiles || []).forEach(f => {
        if (f && f._videoBlobUrl) {
          URL.revokeObjectURL(f._videoBlobUrl);
          this.$delete(f, '_videoBlobUrl');
        }
      });
    },
    handleVideoWheel(event) {
      if (!this.videoViewerUrl || this.videoViewerLoading) return;
      const dir = event.deltaY < 0 ? 1 : -1;
      this.videoViewerScale = Math.max(1, Math.min(3, Number((this.videoViewerScale + dir * 0.12).toFixed(2))));
    },

    // ---- 键盘/全局事件 ----
    _mediaViewerKeydown(event) {
      if (this.imageViewerVisible) {
        if (event.key === 'Escape') { this.closeImageViewer(); return; }
        if (event.key === 'ArrowLeft') { this.showPrevImage(); return; }
        if (event.key === 'ArrowRight') { this.showNextImage(); return; }
      }
      if (this.videoViewerVisible) {
        if (event.key === 'Escape') { this.closeVideoViewer(); return; }
        if (event.key === 'ArrowLeft') { this.showPrevVideo(); return; }
        if (event.key === 'ArrowRight') { this.showNextVideo(); return; }
      }
    },
    _mediaViewerGlobalWheel(event) {
      if (!this.imageViewerVisible && !this.videoViewerVisible) return;
      if (!event.ctrlKey) return;
      event.preventDefault();
      if (this.imageViewerVisible) this.handleImageWheel(event);
      else if (this.videoViewerVisible) this.handleVideoWheel(event);
    }
  }
};
