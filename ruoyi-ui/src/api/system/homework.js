import request from '@/utils/request'

// 查询作业任务列表
export function listHomework(query) {
  return request({
    url: '/system/homework/list',
    method: 'get',
    params: query
  })
}

// 查询作业任务详细
export function getHomework(homeworkId) {
  return request({
    url: '/system/homework/' + homeworkId,
    method: 'get'
  })
}

// 新增作业任务
export function addHomework(data) {
  return request({
    url: '/system/homework',
    method: 'post',
    data: data
  })
}

// 修改作业任务
export function updateHomework(data) {
  return request({
    url: '/system/homework',
    method: 'put',
    data: data
  })
}

// 删除作业任务
export function delHomework(homeworkId) {
  return request({
    url: '/system/homework/' + homeworkId,
    method: 'delete'
  })
}

// 查询作业提交列表
export function listHomeworkSubmit(query) {
  return request({
    url: '/system/homework/submit/list',
    method: 'get',
    params: query
  })
}

// 批改作业
export function gradeHomework(data) {
  return request({
    url: '/system/homework/submit',
    method: 'put',
    data: data
  })
}

// 删除作业提交
export function deleteHomeworkSubmit(submitId) {
  return request({
    url: '/system/homework/submit/' + submitId,
    method: 'delete'
  })
}
