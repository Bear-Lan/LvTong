<template>
  <div class="video-playback" :class="{ 'is-playing': isConnected }">
    <div ref="videoWrapper" class="video-wrapper">
      <video
        ref="videoElement"
        class="playback-video"
        :style="{ objectFit: fit }"
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

    <div v-else-if="!isPlaying" class="video-placeholder">
      <el-icon><VideoCamera /></el-icon>
      <p>{{ channelName }}</p>
      <button class="play-btn" @click.stop="handlePlay">
        <el-icon><VideoPlay /></el-icon>
        <span>播放录像</span>
      </button>
    </div>

    <div v-else-if="hasError" class="video-error">
      <el-icon><Warning /></el-icon>
      <p>{{ errorMessage }}</p>
      <button class="retry-btn" @click.stop="retryConnect">重试</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { FullScreen, Camera, VideoCamera, RefreshRight, Microphone, Mute, Warning, VideoPlay } from '@element-plus/icons-vue'

const props = defineProps({
  channelKey: { type: String, required: true },
  channelName: { type: String, required: true },
  channelId: { type: [Number, String], default: null },
  mediaServerUrl: { type: String, default: '' },
  startTime: { type: String, default: '' },
  endTime: { type: String, default: '' },
  fit: { type: String, default: 'fill' },
  isRotated: { type: Boolean, default: false }
})

const emit = defineEmits(['fullscreen', 'screenshot'])

const videoElement = ref(null)
const videoWrapper = ref(null)
const isPlaying = ref(false)
const isConnected = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const errorMessage = ref('播放失败')
const isMuted = ref(true)

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
  videoElement.value.style.width = `${ch}px`
  videoElement.value.style.height = `${cw}px`
  videoElement.value.style.transform = 'rotate(90deg)'
}

const startStream = async () => {
  if (!props.channelId || !videoElement.value) return
  if (!props.startTime || !props.endTime) return

  isLoading.value = true
  hasError.value = false
  errorMessage.value = '播放失败'
  stopStream()

  const apiUrl = `/api/hikNet/playBackVideo?startTime=${encodeURIComponent(props.startTime)}&endTime=${encodeURIComponent(props.endTime)}&channel=${props.channelId}`

  try {
    videoElement.value.src = apiUrl
    videoElement.value.muted = isMuted.value

    videoElement.value.onloadedmetadata = () => {
      isLoading.value = false
      isConnected.value = true
      updateRotatedStyle()
      videoElement.value.play().catch(() => {})
    }

    videoElement.value.onerror = () => {
      errorMessage.value = '播放失败，请重试'
      hasError.value = true
      isLoading.value = false
      isConnected.value = false
    }

    videoElement.value.onended = () => {
      isConnected.value = false
      isLoading.value = false
      errorMessage.value = '播放结束'
    }
  } catch (e) {
    console.error('连接视频流失败:', e)
    hasError.value = true
    isLoading.value = false
  }
}

const stopStream = () => {
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
    videoElement.value.onloadedmetadata = null
    videoElement.value.onerror = null
    videoElement.value.onended = null
  }
  isConnected.value = false
  isLoading.value = false
}

const retryConnect = () => {
  hasError.value = false
  isPlaying.value = true
  startStream()
}

const handlePlay = () => {
  isPlaying.value = true
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

watch(() => [props.channelId, props.startTime, props.endTime, isPlaying], async ([newId, newStart, newEnd, playing]) => {
  if (newId && newStart && newEnd && playing) {
    await nextTick()
    startStream()
  }
}, { immediate: false })

onMounted(() => {
  // 默认不播放，点击播放按钮后才开始
})

onUnmounted(() => {
  stopStream()
})

defineExpose({ toggleFullscreen, handleScreenshot })
</script>

<style scoped>
.video-playback {
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

.video-playback.is-playing {
  background-image: none;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.playback-video {
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
  margin: 0 0 12px;
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
  margin: 0 0 12px;
}

.play-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.play-btn:hover {
  background: #66b1ff;
}

.play-btn .el-icon {
  font-size: 18px;
}
</style>