import request from '@/utils/request'

/**
 * 车辆照片识别
 * 识别车厢类型、车轮数量
 * @param {File} file - 车辆图片文件
 * @returns {Promise}
 */
export function detectVehicle(file) {
  const formData = new FormData()
  formData.append('image', file)

  return request({
    url: '/ai/vehicle',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 货物类型识别
 * 识别货物类型
 * @param {File} file - 货物图片文件
 * @returns {Promise}
 */
export function detectGoods(file) {
  const formData = new FormData()
  formData.append('real_image', file)

  return request({
    url: '/ai/goods',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 行驶证识别
 * 识别行驶证正反面信息
 * @param {File} file - 行驶证图片文件
 * @returns {Promise}
 */
export function detectDriverLicense(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/ai/driver-license',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 车轴识别
 * 识别车轴数量和位置
 * @param {File} file - 车轴图片文件
 * @returns {Promise}
 */
export function detectAxle(file) {
  const formData = new FormData()
  formData.append('image', file)

  return request({
    url: '/ai/axle',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 车厢识别
 * 识别车厢类型
 * @param {File} file - 车厢图片文件
 * @returns {Promise}
 */
export function detectCarriage(file) {
  const formData = new FormData()
  formData.append('image', file)

  return request({
    url: '/ai/carriage',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}