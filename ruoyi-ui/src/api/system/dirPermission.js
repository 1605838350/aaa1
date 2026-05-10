import request from '@/utils/request'

// 查询目录权限列表
export function listPermission(query) {
  return request({
    url: '/system/dir/permission/list',
    method: 'get',
    params: query
  })
}

// 保存目录权限
export function savePermission(data) {
  return request({
    url: '/system/dir/permission',
    method: 'post',
    data: data
  })
}

// 删除目录权限
export function deletePermission(roleId, dirId) {
  return request({
    url: '/system/dir/permission/' + roleId + '/' + dirId,
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
