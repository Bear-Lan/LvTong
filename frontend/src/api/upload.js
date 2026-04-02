import request from '@/utils/request'

/**
 * 上传图片到服务器
 * @param {File} file - 文件对象
 * @param {'head'|'tail'|'license'|'goods'|'body'|'transparent'|'passcode'} type - 图片类型
 * @returns {Promise<{path: string, filename: string, type: string}>}
 */
export function uploadImage(file, type = 'head') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)

  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
