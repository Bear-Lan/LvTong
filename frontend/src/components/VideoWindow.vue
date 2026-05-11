<template>
  <div class="video-window" :class="{ 'is-playing': isConnected }">
    <div ref="videoWrapper" class="video-wrapper">
      <video
        ref="videoElement"
        class="webrtc-video"
        :style="{ objectFit: fit }"
        autoplay
        playsinline
      ></video>
    </div>

    <div class="video-actions">
      <button
        class="action-btn"
        :class="{ 'is-disabled': isLoading }"
        @click.stop="retryConnect"
        title="重连"
        :disabled="isLoading"
      >
        <el-icon><RefreshRight /></el-icon>
      </button>
      <button class="action-btn" @click.stop="toggleFullscreen" title="全屏">
        <el-icon><FullScreen /></el-icon>
      </button>
      <button class="action-btn" @click.stop="handleScreenshot" title="截图">
        <el-icon><Camera /></el-icon>
      </button>
      <button class="action-btn" @click.stop="toggleMute" :title="isMuted ? '取消静音' : '静音'">
        <el-icon><Mute v-if="isMuted" /><Microphone v-else /></el-icon>
      </button>
    </div>

    <div v-if="isLoading" class="video-loading">
      <div class="loading-spinner"></div>
      <p>正在连接 {{ channelName }}...</p>
    </div>


    <div v-else-if="!channelId" class="video-placeholder">
      <el-icon><VideoCamera /></el-icon>
      <p>{{ channelName }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { FullScreen, Camera, VideoCamera, RefreshRight, Microphone, Mute } from '@element-plus/icons-vue'

const props = defineProps({
  channelKey: { type: String, required: true },
  channelName: { type: String, required: true },
  channelId: { type: [Number, String], default: null },
  mediaServerUrl: { type: String, default: 'http://127.0.0.1:8889' },
  fit: { type: String, default: 'fill' },
  isRotated: { type: Boolean, default: false }
})

const emit = defineEmits(['fullscreen', 'screenshot'])

const videoElement = ref(null)
const videoWrapper = ref(null)
const isConnected = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const isMuted = ref(false)
let peerConnection = null

// 旋转90度后拉伸铺满（不裁切、不溢出）
const updateRotatedStyle = () => {
  if (!videoElement.value || !videoWrapper.value) return
  if (!props.isRotated) {
    videoElement.value.style.width = '100%'
    videoElement.value.style.height = '100%'
    videoElement.value.style.transform = ''
    return
  }
  const cw = videoWrapper.value.clientWidth
  const ch = videoWrapper.value.clientHeight
  // 旋转后，宽高互换 → 强制拉伸到容器宽高，旋转后刚好填满
  videoElement.value.style.width = `${ch}px`
  videoElement.value.style.height = `${cw}px`
  videoElement.value.style.transform = 'rotate(90deg)'
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
      videoElement.value.onloadedmetadata = () => {
        updateRotatedStyle()
      }
    }
  }

  peerConnection.oniceconnectionstatechange = () => {
    if (peerConnection.iceConnectionState === 'connected') {
      isConnected.value = true
      isLoading.value = false
    } else if (peerConnection.iceConnectionState === 'disconnected' || peerConnection.iceConnectionState === 'failed') {
      isConnected.value = false
      isLoading.value = false
      // 自动重试
      setTimeout(() => {
        if (props.channelId) {
          retryConnect()
        }
      }, 3000)
    }
  }

  try {
    const offer = await peerConnection.createOffer({
      offerToReceiveAudio: 1,
      offerToReceiveVideo: 1
    })

    await peerConnection.setLocalDescription(offer)
    await new Promise(r => setTimeout(r, 1000))

    const whepUrl = `${props.mediaServerUrl}/channel_${props.channelId}/whep`
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

const toggleMute = () => {
  if (!videoElement.value) return
  isMuted.value = !isMuted.value
  videoElement.value.muted = isMuted.value
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
  setTimeout(() => {
    if (props.channelId && videoElement.value) {
      startStream()
    }
  }, 100)
})

onUnmounted(() => {
  stopStream()
})

defineExpose({ toggleFullscreen, handleScreenshot })
</script>

<style scoped>
.video-window {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #1a1a1a;
  border: 1px solid rgba(80, 80, 80, 0.6);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image:
    linear-gradient(rgba(60, 60, 60, 0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(60, 60, 60, 0.3) 1px, transparent 1px);
  background-size: 20px 20px;
}

.video-window.is-playing {
  background-image: none;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}


.webrtc-video {
  width: 100%;
  height: 100%;
}

.video-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
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

.action-btn.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
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
  color: #409eff;
  background: rgba(0, 0, 0, 0.6);
  z-index: 20;
  animation: pulse-bg 2s ease-in-out infinite;
}

@keyframes pulse-bg {
  0%, 100% { background: rgba(0, 0, 0, 0.6); }
  50% { background: rgba(0, 0, 0, 0.4); }
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(64, 158, 255, 0.2);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.3);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.video-loading p {
  font-size: 14px;
  margin: 0;
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
