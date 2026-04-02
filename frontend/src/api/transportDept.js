import request from '@/utils/request'

/**
 * 上报单条查验记录到交通部平台
 * @param {number} id - 查验记录 ID
 */
export function uploadSingle(id) {
  return request({
    url: `/transport-dept/upload/${id}`,
    method: 'post'
  })
}

/**
 * 批量上报查验记录到交通部平台
 * @param {number[]} ids - 查验记录 ID 数组
 */
export function uploadBatch(ids) {
  return request({
    url: '/transport-dept/upload/batch',
    method: 'post',
    data: ids
  })
}
