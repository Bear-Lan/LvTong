<template>
  <div class="video-playback" :class="{ 'is-playing': isConnected }">
    <div ref="videoWrapper" class="video-wrapper">
      <video
        ref="videoElement"
        class="playback-video"
        :style="{ objectFit: fit }"
        playsinline
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMetadata"
      ></video>
    </div>

    <!-- 底部控制栏 -->
    <div v-if="isConnected" class="video-controls">
      <button class="ctrl-btn" @click.stop="togglePause" :title="isPaused ? '继续' : '暂停'">
        <el-icon><VideoPause v-if="!isPaused" /><VideoPlay v-else /></el-icon>
      </button>
      <div class="progress-bar" @click.stop="seekTo">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
      </div>
      <span class="time-label">{{ formatTime(currentTime) }} / {{ formatTime(totalSeconds) }}</span>
      <button class="ctrl-btn" @click.stop="toggleMute" :title="isMuted ? '取消静音' : '静音'">
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
      <p class="placeholder-tip">无通道信息</p>
    </div>

    <div v-else-if="isEnded" class="video-ended">
      <el-icon><VideoPlay /></el-icon>
      <p>录像已播完</p>
      <button class="play-btn" @click.stop="handleReplay">
        <el-icon><VideoPlay /></el-icon>
        <span>重新播放</span>
      </button>
    </div>

    <div v-else-if="hasError" class="video-error">
      <el-icon><Warning /></el-icon>
      <p>{{ errorMessage }}</p>
      <button class="retry-btn" @click.stop="retryConnect">重试</button>
    </div>

    <div v-else-if="!isPlaying" class="video-placeholder">
      <el-icon><VideoCamera /></el-icon>
      <p>{{ channelName }}</p>
      <button class="play-btn" @click.stop="handlePlay">
        <el-icon><VideoPlay /></el-icon>
        <span>播放录像</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted, watch, nextTick } from 'vue'
import { VideoCamera, Microphone, Mute, Warning, VideoPlay, VideoPause } from '@element-plus/icons-vue'

const props = defineProps({
  channelKey: { type: String, required: true },
  channelName: { type: String, required: true },
  channelId: { type: [Number, String], default: null },
  mediaServerUrl: { type: String, default: '' },
  startTime: { type: String, default: '' },
  endTime: { type: String, default: '' },
  fit: { type: String, default: 'contain' },
  isRotated: { type: Boolean, default: false }
})

const videoElement = ref(null)
const videoWrapper = ref(null)
const isPlaying = ref(false)
const isConnected = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const isEnded = ref(false)
const isPaused = ref(false)
const errorMessage = ref('播放失败')
const isMuted = ref(true)
const currentTime = ref(0)
const totalSeconds = ref(0)
const progressPercent = ref(0)

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const onTimeUpdate = () => {
  if (!videoElement.value) return
  currentTime.value = videoElement.value.currentTime
  if (totalSeconds.value > 0) {
    progressPercent.value = Math.min(100, (currentTime.value / totalSeconds.value) * 100)
  }
}

const onLoadedMetadata = () => {
  if (!videoElement.value) return
  totalSeconds.value = videoElement.value.duration || 0
  isConnected.value = true
  isPaused.value = false
  isLoading.value = false
  updateRotatedStyle()
  videoElement.value.play().catch(() => {})
}

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

const startStream = () => {
  if (!props.channelId || !props.startTime || !props.endTime || !videoElement.value) return

  isLoading.value = true
  hasError.value = false
  errorMessage.value = '播放失败'
  isEnded.value = false
  isPaused.value = false
  currentTime.value = 0
  totalSeconds.value = 0
  progressPercent.value = 0

  videoElement.value.pause()
  videoElement.value.src = ''
  videoElement.value.load()

  const apiUrl = `/api/hikNet/playBackVideo?startTime=${encodeURIComponent(props.startTime)}&endTime=${encodeURIComponent(props.endTime)}&channel=${props.channelId}`

  nextTick(() => {
    if (!videoElement.value) return
    videoElement.value.src = apiUrl
    videoElement.value.muted = isMuted.value

    videoElement.value.onerror = (e) => {
      console.error('视频加载失败:', e)
      errorMessage.value = '播放失败，请重试'
      hasError.value = true
      isLoading.value = false
      isConnected.value = false
      isPlaying.value = false
    }

    videoElement.value.onended = () => {
      isEnded.value = true
      isConnected.value = false
      isPaused.value = false
      isLoading.value = false
    }

    videoElement.value.play().catch((e) => {
      console.error('play()失败:', e)
      isLoading.value = false
    })
  })
}

const stopStream = () => {
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
  }
  isPlaying.value = false
  isConnected.value = false
  isLoading.value = false
  isEnded.value = false
  isPaused.value = false
  currentTime.value = 0
  totalSeconds.value = 0
  progressPercent.value = 0
}

const retryConnect = () => {
  hasError.value = false
  isPlaying.value = true
}

const handlePlay = () => {
  isPlaying.value = true
}

const handleReplay = () => {
  isEnded.value = false
  stopStream()
  nextTick(() => {
    isPlaying.value = true
  })
}

const togglePause = () => {
  if (!videoElement.value) return
  if (isPaused.value) {
    videoElement.value.play()
    isPaused.value = false
  } else {
    videoElement.value.pause()
    isPaused.value = true
  }
}

const seekTo = (e) => {
  if (!videoElement.value || !totalSeconds.value) return
  const rect = e.currentTarget.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  videoElement.value.currentTime = ratio * totalSeconds.value
}

const toggleMute = () => {
  if (!videoElement.value) return
  isMuted.value = !isMuted.value
  videoElement.value.muted = isMuted.value
}

watch(isPlaying, (playing) => {
  if (playing && props.channelId && props.startTime && props.endTime) {
    nextTick(() => startStream())
  } else if (!playing) {
    stopStream()
  }
})

onUnmounted(() => {
  stopStream()
  // 通知后端停止回放，释放 NVR 和 FFmpeg 资源
  fetch('/api/hikNet/stopPlayback').catch(() => {})
})
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

/* 底部控制栏 */
.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 36px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  z-index: 10;
}

.ctrl-btn {
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
  flex-shrink: 0;
}

.ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}

.ctrl-btn .el-icon {
  font-size: 14px;
}

.progress-bar {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.progress-track {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #409eff;
  border-radius: 2px;
  transition: width 0.1s linear;
}

.time-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  flex-shrink: 0;
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
}

@keyframes pulse-bg {
  0%, 100% { background: rgba(0, 0, 0, 0.6); }
  50% { background: rgba(0, 0, 0, 0.4); }
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(64, 158, 255, 0.2);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.video-loading p {
  font-size: 13px;
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
  font-size: 28px;
  margin-bottom: 6px;
}

.video-error p {
  font-size: 13px;
  margin: 0 0 10px;
}

.retry-btn {
  padding: 5px 14px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
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
  font-size: 28px;
  margin-bottom: 6px;
}

.video-placeholder p {
  font-size: 13px;
  margin: 0 0 10px;
}

.placeholder-tip {
  font-size: 11px !important;
  color: #888 !important;
  margin: 0 !important;
}

.video-ended {
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
  background: rgba(0, 0, 0, 0.5);
  z-index: 20;
}

.video-ended .el-icon {
  font-size: 28px;
  margin-bottom: 6px;
}

.video-ended p {
  font-size: 13px;
  margin: 0 0 10px;
}

.play-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.play-btn:hover {
  background: #66b1ff;
}

.play-btn .el-icon {
  font-size: 16px;
}
</style>