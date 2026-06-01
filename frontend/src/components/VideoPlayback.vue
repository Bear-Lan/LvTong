<template>
  <div class="video-playback">
    <div v-if="status" class="overlay">
      <p>{{ status }}</p>
    </div>
    <video ref="videoEl" class="playback-video" playsinline muted></video>
    <div v-if="!isConnected && !status" class="overlay">
      <p>{{ channelName }}</p>
      <button @click="startPlay"><span>播放录像</span></button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import flvjs from 'flv.js'

const props = defineProps({
  channelName: { type: String, required: true },
  channelId: { type: [Number, String], default: null },
  startTime: { type: String, default: '' },
  endTime: { type: String, default: '' }
})

const videoEl = ref(null)
let player = null
const isConnected = ref(false)
const status = ref('')

const startPlay = () => {
  if (!props.channelId || !props.startTime || !props.endTime) {
    status.value = '参数无效'
    return
  }

  if (player) {
    player.destroy()
    player = null
  }

  status.value = '正在连接...'
  isConnected.value = false

  const url = `/api/hikNet/playBackVideo?startTime=${encodeURIComponent(props.startTime)}&endTime=${encodeURIComponent(props.endTime)}&channel=${props.channelId}`
  console.log('[VideoPlayback] 请求URL:', url)

  if (!flvjs.isSupported()) {
    status.value = '浏览器不支持 FLV'
    return
  }

  player = flvjs.createPlayer({
  type: 'flv',
  url: url,
  autoCleanupSourceBuffer: true,
  autoCleanupMinDelay: 30,    // 缓冲区超过30秒就开始清理
  autoCleanupMaxDelay: 60,   // 最多保留60秒
  enableWorker: true,        // 启用worker分担主线程
  enableStashBuffer: false   // 关闭stash，避免堆积
})


  player.on('error', (e) => {
    console.error('[flv.js] error', e)
    status.value = '播放失败'
  })

  player.attachMediaElement(videoEl.value)
  player.load()
  player.play().catch(() => {})

  videoEl.value.addEventListener('playing', () => {
    console.log('[video] playing')
    status.value = ''
    isConnected.value = true
  })

  videoEl.value.addEventListener('canplay', () => {
    console.log('[video] canplay')
  })
}

const stopPlay = () => {
  if (player) {
    player.pause()
    player.unload()
    player.detachMediaElement()
    player.destroy()
    player = null
  }
  isConnected.value = false
  status.value = ''
}

defineExpose({ startPlay, stopPlay })
</script>

<style scoped>
.video-playback {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.playback-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
}

.overlay p {
  margin: 0 0 12px;
  font-size: 14px;
}

.overlay.error {
  color: #f56c6c;
}

.overlay button {
  padding: 8px 24px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.overlay.error button {
  background: #f56c6c;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(64, 158, 255, 0.2);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>