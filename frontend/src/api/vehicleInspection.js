/**
 * 车辆查验模块 API 接口封装
 *
 * 【模块说明】
 * 统一封装所有车辆查验相关的 HTTP 请求，
 * 避免在 Vue 组件中直接调用 axios，使业务逻辑与网络请求解耦。
 *
 * 【请求封装说明】
 * - 所有请求均通过 request 实例（基于 axios）发送
 * - baseURL 已配置为 /api（由 vite 代理转发至后端）
 * - Token 认证由 request 拦截器自动注入（无需手动处理）
 *
 * 【RESTful 接口对照】
 * - GET    /inspection/list   → 查询列表（支持分页+多条件筛选）
 * - GET    /inspection/{id}  → 查询单条详情
 * - POST   /inspection        → 新增记录
 * - PUT    /inspection/{id}  → 更新记录
 * - GET    /inspection/products → 获取农产品品种列表
 *
 * 【响应数据约定】
 * 所有接口统一返回 ApiResponse<T> 结构：
 *   {
 *     code: number,      // 业务状态码，200=成功，其他=失败
 *     message?: string,  // 失败时的错误信息
 *     data?: T           // 成功时的数据
 *   }
 *
 * 拦截器已处理 code !== 200 的错误（ElMessage 提示 + Promise.reject），
 * 因此 then 分支中 data 即为业务数据，无需再判断 code。
 *
 * 【错误处理约定】
 * - 401 未授权 → 拦截器自动跳转登录页
 * - 404 资源不存在 → 拦截器提示 "请求资源不存在"
 * - 500 服务器错误 → 拦截器提示 "服务器错误"
 * - 网络断开 → 拦截器提示 "网络连接失败"
 *
 * @see request.js  axios 封装（拦截器、baseURL 配置）
 */

import request from '@/utils/request'

// ================================================================
// 查询接口
// ================================================================

/**
 * 获取查验记录列表（支持分页 + 多条件筛选）
 *
 * 【适用场景】
 * HistoricalRecords.vue 页面加载时、点击"查询"按钮时、分页切换时调用。
 *
 * 【请求参数】所有参数均为可选，不传则不作为筛选条件
 * @param {Object} params
 * @param {string}   [params.plateNumber]   车牌号（模糊查询，支持部分匹配）
 * @param {string}   [params.driverPhone]    司机电话（精确查询）
 * @param {string}   [params.operatorName]   查验员姓名（精确查询）
 * @param {number}   [params.resultStatus]  查验结果（1=合格, 2=不合格）
 * @param {string[]} [params.dateRange]      时间范围 [开始时间, 结束时间]（格式: yyyy-MM-dd HH:mm:ss）
 * @param {number}   [params.page]          页码（从 1 开始）
 * @param {number}   [params.pageSize]       每页条数
 *
 * 【返回数据示例】
 * {
 *   records: [          // 当前页记录列表
 *     { id, plateNumber, plateNumberGc, resultStatus, resultStatusText, ... }
 *   ],
 *   total: 100,          // 满足条件的总记录数
 *   page: 1,            // 当前页码
 *   pageSize: 10         // 每页条数
 * }
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 分页数据
 */
export function getInspectionList(params) {
  return request({
    url: '/inspection/list',
    method: 'get',
    params
  })
}

/**
 * 导出查询（全量数据）
 *
 * 【适用场景】
 * HistoricalRecords.vue 导出Excel时调用。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 全量数据列表
 */
export function getInspectionExport(params) {
  return request({
    url: '/inspection/export',
    method: 'get',
    params
  })
}

/**
 * 根据 ID 获取单条查验记录详情
 *
 * 【适用场景】
 * InspectionDetail.vue 详情弹窗打开时调用。
 *
 * @param {number} id 查验记录主键
 * @returns {Promise} 记录详情对象
 */
export function getInspectionById(id) {
  return request({
    url: `/inspection/${id}`,
    method: 'get'
  })
}

// ================================================================
// 写接口
// ================================================================

/**
 * 新增查验记录
 *
 * 【适用场景】
 * InspectionEdit.vue 新增表单提交时调用。
 *
 * 【请求体（data）】
 * 表单中所有填写的字段组成的对象。
 * 注意：车牌号码（plateNumber）为必填项。
 *
 * 【返回数据】
 * 创建成功后返回完整的记录对象（含自动生成的主键 id）。
 *
 * @param {Object} data 新增记录数据
 * @returns {Promise} 创建后的记录（含 id）
 */
export function createInspection(data) {
  return request({
    url: '/inspection',
    method: 'post',
    data
  })
}

/**
 * 更新查验记录（支持部分更新）
 *
 * 【适用场景】
 * InspectionEdit.vue 编辑表单提交时调用。
 *
 * 【PATCH 语义说明】
 * 该接口支持部分更新——只需提交需要修改的字段，未提交的字段保持原值不变。
 * 底层 Service 层只更新非 null 的字段，null 字段会被跳过。
 *
 * 【请求体（data）】
 * { id: 1, plateNumber: '京A12345', resultStatus: 2, nopassType: 11 }
 * ↑ 只需提交需要修改的字段，不必提交整条记录。
 *
 * @param {number} id   待更新记录的主键
 * @param {Object} data 需要修改的字段及新值
 * @returns {Promise} 更新后的记录
 */
export function updateInspection(id, data) {
  return request({
    url: `/inspection/${id}`,
    method: 'put',
    data
  })
}

/**
 * 获取农产品品种列表（用于货物类型下拉选择）
 *
 * 【适用场景】
 * InspectionEdit.vue 货物类型下拉框加载选项时调用。
 *
 * @returns {Promise} 品种列表 [{ productCode, varietyName, category, productType }, ...]
 */
export function getProductList() {
  return request({
    url: '/inspection/products',
    method: 'get'
  })
}

/**
 * 获取不合格类型选项列表（用于下拉选择）
 *
 * 【适用场景】
 * InspectionEdit.vue 不合格类型下拉框加载选项时调用。
 *
 * @returns {Promise} 选项列表 [{ value: 11, label: '车货总质量超限' }, ...]
 */
export function getNopassTypeOptions() {
  return request({
    url: '/inspection/nopass-types',
    method: 'get'
  })
}

// ================================================================
// Dashboard 统计接口
// ================================================================

/**
 * 获取首页 Dashboard 统计数据
 *
 * @param {string} timeType 时间类型：day=今日, month=本月, year=本年
 * @returns {Promise} Dashboard 统计数据
 */
export function getDashboardStats(timeType = 'day') {
  return request({
    url: '/inspection/dashboard',
    method: 'get',
    params: { timeType }
  })
}
