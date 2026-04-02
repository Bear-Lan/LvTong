import request from '@/utils/request'

// 管理员新增用户
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

// 用户登录
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 获取用户列表
export function getUserList() {
  return request({
    url: '/user/list',
    method: 'get'
  })
}

// 获取用户详情
export function getUserById(id) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

// 更新用户（管理员操作）
export function updateUser(id, data) {
  return request({
    url: `/user/${id}`,
    method: 'put',
    data
  })
}

// 更新个人资料（普通用户）
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  })
}

// 重置用户密码（管理员操作）
export function resetPassword(id) {
  return request({
    url: `/user/${id}/reset-password`,
    method: 'post'
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 根据用户名查询用户
export function getUserByUsername(username) {
  return request({
    url: `/user/findByUsername?username=${username}`,
    method: 'get'
  })
}

// 获取所有用户（用于核验员下拉选择，返回 { phone, realName }）
export function getUserPhoneList() {
  return request({
    url: '/user/phones',
    method: 'get'
  })
}
