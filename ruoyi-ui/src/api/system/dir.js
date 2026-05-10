import request from '@/utils/request'

// 查询文件目录权限列表
export function listDir(query) {
  return request({
    url: '/system/dir/list',
    method: 'get',
    params: query
  })
}

// 查询文件目录权限详细
export function getDir(dirId) {
  return request({
    url: '/system/dir/' + dirId,
    method: 'get'
  })
}

// 新增文件目录权限
export function addDir(data) {
  return request({
    url: '/system/dir',
    method: 'post',
    data: data
  })
}

// 修改文件目录权限
export function updateDir(data) {
  return request({
    url: '/system/dir',
    method: 'put',
    data: data
  })
}

// 删除文件目录权限
export function delDir(dirId) {
  return request({
    url: '/system/dir/' + dirId,
    method: 'delete'
  })
}

// 同步目录权限到子目录
export function syncPermissionToChildren(roleId, dirId) {
  return request({
    url: '/system/dir/permission/sync/' + roleId + '/' + dirId,
    method: 'post'
  })
}
