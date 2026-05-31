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

    <!-- 底部控制栏 -->
    <div v-if="isConnected" class="video-controls">
      <button class="ctrl-btn" @click.stop="togglePause" :title="isPaused ? '继续' : '暂停'">
        <el-icon><VideoPause v-if="!isPaused" /><VideoPlay v-else /></el-icon>
      </button>
      <div class="progress-bar" @click.stop="seekTo"
        @mousedown.prevent="onProgressMouseDown"
      >
        <div class="progress-track">
          <div class="progress-buffered" :style="{ width: (isDragging ? dragProgress : bufferedPercent) + '%' }"></div>
          <div class="progress-fill" :style="{ width: (isDragging ? dragProgress : progressPercent) + '%' }"></div>
        </div>
      </div>
      <span class="time-label">{{ formatTime(currentTime) }} / {{ formatTime(totalSeconds) }}</span>
      <button class="ctrl-btn" @click.stop="toggleMute" :title="isMuted ? '取消静音' : '静音'">
        <el-icon><Mute v-if="isMuted" /><Microphone v-else /></el-icon>
      </button>
      <button class="ctrl-btn" @click.stop="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        <el-icon><FullScreen /></el-icon>
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
import { VideoCamera, Microphone, Mute, Warning, VideoPlay, VideoPause, FullScreen } from '@element-plus/icons-vue'
import flvjs from 'flv.js'

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

// 计算总时长（秒），基于传入的时间范围而非视频流 duration
const calcTotalSeconds = () => {
  if (!props.startTime || !props.endTime) return 0
  const start = new Date(props.startTime).getTime()
  const end = new Date(props.endTime).getTime()
  if (isNaN(start) || isNaN(end) || end <= start) return 0
  return Math.round((end - start) / 1000)
}

const videoElement = ref(null)
const videoWrapper = ref(null)
const flvPlayer = ref(null)
const isPlaying = ref(false)
const isConnected = ref(false)
const isLoading = ref(false)
const hasError = ref(false)
const isEnded = ref(false)
const isPaused = ref(false)
const errorMessage = ref('播放失败')
const isMuted = ref(true)
const currentTime = ref(0)
const totalSeconds = ref(calcTotalSeconds())
const progressPercent = ref(0)
const isFullscreen = ref(false)
const bufferedPercent = ref(0)

// 拖动进度条相关状态
const isDragging = ref(false)
const dragProgress = ref(0)

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const onTimeUpdate = () => {
  if (!flvPlayer.value) return
  currentTime.value = flvPlayer.value.currentTime
  if (totalSeconds.value > 0) {
    progressPercent.value = Math.min(100, (currentTime.value / totalSeconds.value) * 100)
  }
  // 更新已缓冲进度
  const buffered = flvPlayer.value.buffered
  if (buffered && buffered.length > 0 && totalSeconds.value > 0) {
    const bufferedEnd = buffered.end(buffered.length - 1)
    bufferedPercent.value = Math.min(100, (bufferedEnd / totalSeconds.value) * 100)
  }
}

const onProgressMouseDown = () => {
  isDragging.value = true
  dragProgress.value = progressPercent.value
  document.addEventListener('mousemove', onProgressMouseMove)
  document.addEventListener('mouseup', onProgressMouseUp)
}

const onProgressMouseMove = (e) => {
  const bar = document.querySelector('.progress-bar')
  if (!bar) return
  const rect = bar.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  dragProgress.value = Math.round(ratio * 100)
}

const onProgressMouseUp = (e) => {
  document.removeEventListener('mousemove', onProgressMouseMove)
  document.removeEventListener('mouseup', onProgressMouseUp)
  const bar = document.querySelector('.progress-bar')
  if (!bar) return
  const rect = bar.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  if (flvPlayer.value && totalSeconds.value > 0) {
    flvPlayer.value.currentTime = ratio * totalSeconds.value
  }
  isDragging.value = false
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
  totalSeconds.value = calcTotalSeconds()
  progressPercent.value = 0
  bufferedPercent.value = 0
  isDragging.value = false

  if (flvPlayer.value) {
    flvPlayer.value.pause()
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
  }
  isConnected.value = false

  const apiUrl = `/api/hikNet/playBackVideo?startTime=${encodeURIComponent(props.startTime)}&endTime=${encodeURIComponent(props.endTime)}&channel=${props.channelId}`

  nextTick(() => {
    if (!videoElement.value) return

    if (flvjs.isSupported()) {
      const player = flvjs.createPlayer({
        type: 'flv',
        url: apiUrl,
        hasAudio: false,
        hasVideo: true,
        isLive: false,
        enableStashBuffer: false,
        stashInitialSize: 128,
      }, {
        enableWorker: false,
        enableStashBuffer: false,
        stashInitialSize: 128,
        autoCleanupSourceBuffer: true,
        autoCleanupMinBackwardDuration: 2,
        autoCleanupMaxBackwardDuration: 5,
      })

      flvPlayer.value = player
      player.attachMediaElement(videoElement.value)
      player.load()

      player.on(flvjs.Events.ERROR, (errType, errDetail) => {
        console.error('flv.js error:', errType, errDetail)
        if (!isConnected.value) {
          errorMessage.value = '播放失败，请重试'
          hasError.value = true
          isLoading.value = false
          isConnected.value = false
          isPlaying.value = false
        }
      })

      player.on(flvjs.Events.LOADED_METADATA, () => {
        isConnected.value = true
        isPaused.value = false
        isLoading.value = false
        updateRotatedStyle()
        document.addEventListener('fullscreenchange', onFullscreenChange)
      })

      player.on(flvjs.Events.FIRST_VIDEO_FRAME_DECODED, () => {
        console.log('[flv.js] first video frame decoded, start playback')
        player.play().catch(() => {})
      })

      player.on(flvjs.Events.TIME_UPDATE, () => {
        onTimeUpdate()
      })

      player.on(flvjs.Events.ENDED, () => {
        isEnded.value = true
        isConnected.value = false
        isPaused.value = false
        isLoading.value = false
      })

      player.play().catch(() => {
        // play() 失败可能是暂时的，忽略
      })
    } else {
      errorMessage.value = '当前浏览器不支持 FLV 播放'
      hasError.value = true
      isLoading.value = false
    }
  })
}

const stopStream = () => {
  if (flvPlayer.value) {
    flvPlayer.value.pause()
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
  }
  isConnected.value = false
  isLoading.value = false
  isEnded.value = false
  isPaused.value = false
  currentTime.value = 0
  progressPercent.value = 0
  bufferedPercent.value = 0
}

const retryConnect = () => {
  hasError.value = false
  isLoading.value = true
  isPlaying.value = true
}

const handlePlay = () => {
  console.log('[VideoPlayback] handlePlay clicked, channelId:', props.channelId, 'startTime:', props.startTime, 'endTime:', props.endTime)
  isLoading.value = true
  isPlaying.value = true
}

const handleReplay = () => {
  isEnded.value = false
  if (flvPlayer.value) {
    flvPlayer.value.pause()
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
  }
  isConnected.value = false
  nextTick(() => {
    isPlaying.value = true
  })
}

const togglePause = () => {
  if (!flvPlayer.value) return
  if (isPaused.value) {
    flvPlayer.value.play()
    isPaused.value = false
  } else {
    flvPlayer.value.pause()
    isPaused.value = true
  }
}

const seekTo = (e) => {
  if (!flvPlayer.value || !totalSeconds.value) return
  const rect = e.currentTarget.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  flvPlayer.value.currentTime = ratio * totalSeconds.value
}

const toggleMute = () => {
  if (!videoElement.value) return
  isMuted.value = !isMuted.value
  videoElement.value.muted = isMuted.value
}

const toggleFullscreen = async () => {
  if (!videoElement.value) return
  if (!document.fullscreenElement) {
    await videoElement.value.requestFullscreen()
    isFullscreen.value = true
  } else {
    await document.exitFullscreen()
    isFullscreen.value = false
  }
}

const onFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
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
  document.removeEventListener('fullscreenchange', onFullscreenChange)
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

/* 全屏模式 */
.video-playback:fullscreen {
  background: #000;
}

.video-playback:fullscreen .video-wrapper {
  width: 100%;
  height: 100%;
}

.video-playback:fullscreen .playback-video {
  width: 100%;
  height: 100%;
}

.video-playback:fullscreen .video-controls {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
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
  position: relative;
  cursor: pointer;
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
  position: relative;
  z-index: 2;
}

.progress-buffered {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: rgba(255, 255, 255, 0.35);
  border-radius: 2px;
  transition: width 0.3s linear;
  z-index: 1;
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