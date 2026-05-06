<template>
  <div class="video-window" :class="{ 'is-playing': isConnected }">
    <video
      ref="videoElement"
      class="webrtc-video"
      autoplay
      playsinline
    ></video>

    <div class="video-label">
      <span class="channel-name">{{ channelName }}</span>
      <span class="timestamp">{{ formattedTime }}</span>
    </div>

    <div class="video-actions">
      <button class="action-btn" @click.stop="toggleFullscreen" title="全屏">
        <el-icon><FullScreen /></el-icon>
      </button>
      <button class="action-btn" @click.stop="handleScreenshot" title="截图">
        <el-icon><Camera /></el-icon>
      </button>
    </div>

    <div v-if="isLoading" class="video-loading">
      <div class="loading-spinner"></div>
      <p>正在连接视频...</p>
    </div>

    <div v-else-if="hasError" class="video-error">
      <el-icon><VideoCamera /></el-icon>
      <p>视频连接失败</p>
      <button class="retry-btn" @click="retryConnect">重试</button>
    </div>

    <div v-else-if="!channelId" class="video-placeholder">
      <el-icon><VideoCamera /></el-icon>
      <p>{{ channelName }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { FullScreen, Camera, VideoCamera } from '@element-plus/icons-vue'

const props = defineProps({
  channelKey: { type: String, required: true },
  channelName: { type: String, required: true },
  channelId: { type: [Number, String], default: null }
})

const emit = defineEmits(['fullscreen', 'screenshot'])

const MEDIA_SERVER = 'http://127.0.0.1:8889'

const videoElement = ref(null)
const isConnected = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const formattedTime = ref('')
let peerConnection = null
let timeInterval = null

const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')
  const weekDay = weekDays[now.getDay()]
  formattedTime.value = `${year}-${month}-${day} ${hour}:${minute}:${second} ${weekDay}`
}

const startStream = async () => {
  if (!props.channelId || !videoElement.value) return

  isLoading.value = true
  hasError.value = false
  stopStream()

  peerConnection = new RTCPeerConnection()

  peerConnection.ontrack = (event) => {
    if (videoElement.value && event.streams[0]) {
      videoElement.value.srcObject = event.streams[0]
      isConnected.value = true
      isLoading.value = false
    }
  }

  peerConnection.oniceconnectionstatechange = () => {
    if (peerConnection.iceConnectionState === 'connected') {
      isConnected.value = true
      isLoading.value = false
    } else if (peerConnection.iceConnectionState === 'disconnected' || peerConnection.iceConnectionState === 'failed') {
      isConnected.value = false
      hasError.value = true
      isLoading.value = false
    }
  }

  try {
    const offer = await peerConnection.createOffer({
      offerToReceiveAudio: 1,
      offerToReceiveVideo: 1
    })

    await peerConnection.setLocalDescription(offer)
    await new Promise(r => setTimeout(r, 1000))

    const whepUrl = `${MEDIA_SERVER}/channel_${props.channelId}/whep`
    const response = await fetch(whepUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/sdp' },
      body: peerConnection.localDescription.sdp
    })

    if (!response.ok) {
      console.error(`WHEP请求失败: ${response.status}`)
      hasError.value = true
      isLoading.value = false
      return
    }

    const answerSdp = await response.text()
    await peerConnection.setRemoteDescription(new RTCSessionDescription({
      type: 'answer',
      sdp: answerSdp
    }))
  } catch (e) {
    console.error('连接视频流失败:', e)
    hasError.value = true
    isLoading.value = false
  }
}

const stopStream = () => {
  if (peerConnection) {
    peerConnection.close()
    peerConnection = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
  isConnected.value = false
  isLoading.value = false
}

const retryConnect = () => {
  hasError.value = false
  startStream()
}

const toggleFullscreen = () => {
  if (!videoElement.value) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    videoElement.value.parentElement.requestFullscreen()
  }
}

const handleScreenshot = () => {
  if (!videoElement.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoElement.value.videoWidth
  canvas.height = videoElement.value.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(videoElement.value, 0, 0)
  const link = document.createElement('a')
  link.download = `${props.channelName}_${Date.now()}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
  emit('screenshot')
}

watch(() => props.channelId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await nextTick()
    setTimeout(() => {
      if (newId) {
        startStream()
      }
    }, 100)
  }
})

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  setTimeout(() => {
    if (props.channelId && videoElement.value) {
      startStream()
    }
  }, 100)
})

onUnmounted(() => {
  stopStream()
  if (timeInterval) clearInterval(timeInterval)
})

defineExpose({ toggleFullscreen, handleScreenshot })
</script>

<style scoped>
.video-window {
  position: relative;
  width: 100%;
  height: 100%;
  background: #1a1a1a;
  border: 1px solid rgba(80, 80, 80, 0.6);
  border-radius: 4px;
  overflow: hidden;
  background-image:
    linear-gradient(rgba(60, 60, 60, 0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(60, 60, 60, 0.3) 1px, transparent 1px);
  background-size: 20px 20px;
}

.video-window.is-playing {
  background-image: none;
}

.webrtc-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-label {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 2px;
  font-size: 12px;
  color: #ccc;
  z-index: 10;
}

.video-label .channel-name {
  font-weight: 500;
  color: #fff;
}

.video-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  z-index: 10;
}

.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 4px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}

.action-btn .el-icon {
  font-size: 16px;
}

.video-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #888;
  background: rgba(0, 0, 0, 0.5);
  z-index: 20;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.video-loading p {
  font-size: 14px;
  margin: 0;
}

.video-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #f56c6c;
  background: rgba(0, 0, 0, 0.6);
  z-index: 20;
}

.video-error .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.video-error p {
  font-size: 14px;
  margin: 0 0 12px 0;
}

.retry-btn {
  padding: 6px 16px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.retry-btn:hover {
  background: #66b1ff;
}

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
  background: rgba(0, 0, 0, 0.3);
}

.video-placeholder .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.video-placeholder p {
  font-size: 14px;
  margin: 0;
}
</style>
