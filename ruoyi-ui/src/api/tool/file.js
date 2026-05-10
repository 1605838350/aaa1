import request from '@/utils/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'
import { saveAs } from 'file-saver'
import { blobValidate } from "@/utils/ruoyi"

const baseURL = process.env.VUE_APP_BASE_API

// 浏览目录（支持分页和缓存）
export function listFiles(path, pageNum = 1, pageSize = 50, refresh = false) {
  return request({
    url: '/file/list',
    method: 'get',
    params: { path, pageNum, pageSize, refresh },
    timeout: 60000 // 文件列表可能文件很多，设置60秒超时
  })
}

// 刷新目录缓存
export function refreshCache(path) {
  return request({
    url: '/file/refresh',
    method: 'get',
    params: { path },
    timeout: 60000
  })
}

// 获取目录文件数量
export function countFiles(path) {
  return request({
    url: '/file/count',
    method: 'get',
    params: { path },
    timeout: 60000
  })
}

// 搜索文件（按文件名，遍历目录）
export function searchFiles(keyword, path, pageNum = 1, pageSize = 50) {
  return request({
    url: '/file/search',
    method: 'get',
    params: { keyword, path, pageNum, pageSize },
    timeout: 60000
  })
}

// 快速搜文件（基于 Lucene 索引）
export function searchFilesByName(keyword, path, maxResults = 100) {
  return request({
    url: '/file/search/name',
    method: 'get',
    params: { keyword, path, maxResults },
    timeout: 15000
  })
}

// 穿透搜索（按文档内容）
export function searchContent(keyword, path, maxResults = 50) {
  return request({
    url: '/file/search/content',
    method: 'get',
    params: { keyword, path, maxResults },
    timeout: 60000
  })
}

// 流式搜索文件（SSE）
export function searchFilesStream(keyword, path, onResult, onComplete, onError) {
  const url = baseURL + '/file/search/stream?keyword=' + encodeURIComponent(keyword) + '&path=' + encodeURIComponent(path || '') + '&token=' + encodeURIComponent(getToken() || '')
  
  console.log('[DEBUG] SSE connect:', url)
  
  const eventSource = new EventSource(url)
  
  eventSource.onopen = () => {
    console.log('[DEBUG] SSE connection opened')
  }
  
  eventSource.addEventListener('result', (event) => {
    console.log('[DEBUG] SSE result:', event.data)
    try {
      const data = JSON.parse(event.data)
      onResult && onResult(data)
    } catch (e) {
      console.error('Parse result error:', e)
    }
  })
  
  eventSource.addEventListener('complete', () => {
    console.log('[DEBUG] SSE complete')
    onComplete && onComplete()
    eventSource.close()
  })
  
  eventSource.addEventListener('error', (event) => {
    console.log('[DEBUG] SSE error:', event)
    onError && onError(event.data || '搜索出错')
    eventSource.close()
  })
  
  eventSource.onerror = (error) => {
    console.log('[DEBUG] SSE onerror:', error)
    onError && onError('连接错误')
    eventSource.close()
  }
  
  // 返回关闭函数
  return () => {
    eventSource.close()
  }
}

// 下载文件
export function downloadFile(path, fileName) {
  // 使用原生方式下载，让浏览器显示进度条
  const url = baseURL + '/file/download?path=' + encodeURIComponent(path)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  
  // 添加token到URL（因为直接点击链接无法设置header）
  const token = getToken()
  if (token) {
    link.href = url + '&token=' + encodeURIComponent(token)
  }
  
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 预览文件（返回 blob URL）
export function previewFile(path) {
  return request({
    url: '/file/preview',
    method: 'get',
    params: { path },
    responseType: 'blob'
  })
}

// 提取文档纯文本（用于 .doc 等旧格式预览）
export function previewTextFile(path) {
  return request({
    url: '/file/preview-text',
    method: 'get',
    params: { path }
  })
}

// 获取预览 URL
export function getPreviewUrl(path) {
  const token = getToken()
  let url = process.env.VUE_APP_BASE_API + '/file/preview?path=' + encodeURIComponent(path)
  if (token) {
    url += '&token=' + encodeURIComponent(token)
  }
  return url
}

// 删除文件/目录
export function deleteFile(path, isDirectory = false) {
  return request({
    url: '/file/delete',
    method: 'delete',
    params: { path, isDirectory }
  })
}

// 重建全文搜索索引
export function rebuildSearchIndex(path) {
  return request({
    url: '/file/search/rebuild-index',
    method: 'post',
    params: { path }
  })
}

// 获取索引状态
export function getIndexStatus() {
  return request({
    url: '/file/search/index-status',
    method: 'get'
  })
}

// 上传文件
export function uploadFile(file, subPath) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('path', subPath)
  return request({
    url: '/file/upload-to',
    method: 'post',
    data: formData,
    timeout: 20 * 60 * 1000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 初始化分片上传会话
export function initChunkUpload(payload) {
  return request({
    url: '/file/chunk/init',
    method: 'post',
    data: payload,
    timeout: 120000
  })
}

// 上传单个分片
export function uploadChunk(uploadId, chunkIndex, chunk) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex)
  formData.append('chunk', chunk)
  return request({
    url: '/file/chunk/upload',
    method: 'post',
    data: formData,
    timeout: 20 * 60 * 1000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 查询分片上传状态
export function getChunkUploadStatus(uploadId) {
  return request({
    url: '/file/chunk/status',
    method: 'get',
    params: { uploadId },
    timeout: 120000
  })
}

// 完成分片上传（合并）
export function completeChunkUpload(uploadId) {
  return request({
    url: '/file/chunk/complete',
    method: 'post',
    data: { uploadId },
    timeout: 20 * 60 * 1000
  })
}

// 查询异步转存状态
export function getChunkTransferStatus(taskId) {
  return request({
    url: '/file/chunk/transfer-status',
    method: 'get',
    params: { taskId },
    timeout: 120000
  })
}

// 取消分片上传
export function cancelChunkUpload(uploadId) {
  return request({
    url: '/file/chunk/cancel',
    method: 'delete',
    params: { uploadId },
    timeout: 120000
  })
}

// 预热目录缓存（后台异步加载）
export function warmupCache(paths) {
  return request({
    url: '/file/warmup',
    method: 'get',
    params: { paths }
  })
}

// 新建文件夹
export function createFolder(parentPath, folderName) {
  return request({
    url: '/file/mkdir',
    method: 'post',
    params: { parentPath, folderName }
  })
}

// 重命名文件/文件夹
export function renameFile(oldPath, newName) {
  return request({
    url: '/file/rename',
    method: 'post',
    params: { oldPath, newName }
  })
}

// 移动文件/文件夹
export function moveFile(sourcePath, targetDir) {
  return request({
    url: '/file/move',
    method: 'post',
    params: { sourcePath, targetDir }
  })
}
