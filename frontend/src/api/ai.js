import request from '@/utils/request'

/**
 * 车辆照片识别
 */
export function detectVehicle(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/vehicle',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 货物类型识别
 */
export function detectGoods(file) {
  const formData = new FormData()
  formData.append('real_image', file)
  return request({
    url: '/ai/goods',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 行驶证识别
 */
export function detectOCR(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/ai/driver-license',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 车轴识别
 */
export function detectAxle(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/axle',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 车厢识别
 */
export function detectCarriage(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/carriage',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 货物透视图识别 - product_xray
 */
export function detectProductXray(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/product-xray',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 雷达车头识别 - truck_lidar
 */
export function detectTruckLidarHead(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/truck-lidar-head',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 雷达车高识别 - truck_lidar
 */
export function detectTruckLidarHeight(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/truck-lidar-height',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 车厢混装识别 - product_xray
 */
export function detectMixedLoad(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/mixed-load',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 车厢货物装载率识别 - truck_xray_box
 */
export function detectTruckXrayBox(file) {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/ai/truck-xray-box',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}