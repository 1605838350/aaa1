import request from '@/utils/request'

function getStudentToken() {
  const studentInfo = JSON.parse(localStorage.getItem('studentInfo') || '{}')
  return studentInfo.token || ''
}

// 查询学生作业列表
export function listStudentHomework(admissionYear, classNum, studentId) {
  return request({
    url: '/student/homework/list',
    method: 'get',
    params: { admissionYear, classNum, studentId },
    headers: {
      'X-Student-Token': getStudentToken(),
      'isToken': false
    }
  })
}

// 学生提交作业
export function submitHomework(data) {
  return request({
    url: '/student/homework/submit',
    method: 'post',
    data: data,
    headers: {
      'X-Student-Token': getStudentToken(),
      'isToken': false
    }
  })
}
