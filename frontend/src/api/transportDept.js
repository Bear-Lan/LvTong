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
 * 上报单条查验记录到交通部平台（排除指定图片）
 * @param {number} id - 查验记录 ID
 * @param {string[]} excludePhotoTypes - 要排除的图片类型ID列表，如 [11,12,13]
 */
export function uploadSingleWithExclude(id, excludePhotoTypes) {
  return request({
    url: `/transport-dept/upload/${id}/exclude`,
    method: 'post',
    data: excludePhotoTypes
  })
}

/**
 * 批量上报查验记录到交通部平台（暂未使用）
 * @param {number[]} ids - 查验记录 ID 数组
 */
// export function uploadBatch(ids) {
//   return request({
//     url: '/transport-dept/upload/batch',
//     method: 'post',
//     data: ids
//   })
// }
