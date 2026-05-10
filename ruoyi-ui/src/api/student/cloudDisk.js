import request from '@/utils/request'

function getStudentToken() {
  const studentInfo = JSON.parse(localStorage.getItem('studentInfo') || '{}')
  return studentInfo.token || ''
}

function getStudentId() {
  const studentInfo = JSON.parse(localStorage.getItem('studentInfo') || '{}')
  return studentInfo.studentId
}

// 查询学生所有已提交文件
export function listStudentFiles(studentId) {
  return request({
    url: '/student/files',
    method: 'get',
    params: { studentId },
    headers: {
      'X-Student-Token': getStudentToken(),
      'isToken': false
    }
  })
}

// 查询班级共享文件（同班同学提交的作业）
export function listClassShare(studentId) {
  return request({
    url: '/student/class-share',
    method: 'get',
    params: { studentId },
    headers: {
      'X-Student-Token': getStudentToken(),
      'isToken': false
    }
  })
}

// 学生预览文件（返回 blob，用于在线预览）
export function previewStudentFile(path) {
  return request({
    url: '/student/preview',
    method: 'get',
    params: {
      path: path,
      studentId: getStudentId(),
      token: getStudentToken()
    },
    responseType: 'blob',
    headers: {
      'isToken': false
    }
  })
}
