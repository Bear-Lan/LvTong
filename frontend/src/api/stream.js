import request from '@/utils/request'

// NVR通道相关接口
export function listChannels() {
  return request.get('/stream/channels', { cache: false })
}

export function getStreamUrl(channel) {
  return request.get('/stream/url', { params: { channel }, cache: false })
}