import request from '@/utils/request'

// NVR通道相关接口
export function listChannels() {
  return request.get('/stream/channels')
}

export function getStreamUrl(channel) {
  return request.get('/stream/url', { params: { channel } })
}