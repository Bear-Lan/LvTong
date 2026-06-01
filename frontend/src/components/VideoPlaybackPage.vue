<template>
  <div class="video-playback-page">
    <div class="page-header">
      <h1 class="page-title">录像回放</h1>
      <span class="page-time-range">{{ startTime }} ~ {{ endTime }}</span>
      <button class="download-btn" :disabled="downloading" @click="downloadVideo">
        {{ downloading ? '下载中...' : '下载录像' }}
      </button>
    </div>
    <div class="video-tab-container">
      <div class="video-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.name }}
        </button>
      </div>
      <div class="video-content">
        <VideoPlayback
          ref="videoRef"
          :channel-name="currentChannel.name"
          :channel-id="currentChannel.id"
          :start-time="startTime"
          :end-time="endTime"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import VideoPlayback from '@/components/VideoPlayback.vue'
import { listChannels } from '@/api/stream'

const route = useRoute()
const videoRef = ref(null)

const tabs = [
  { key: 'lane', name: '车道' },
  { key: 'ptz360', name: '360球机' },
  { key: 'appointment', name: '预约机' },
  { key: 'rear', name: '车尾' },
  { key: 'front', name: '车头' }
]

const channelMap = ref({})
const startTime = ref('')
const endTime = ref('')
const activeTab = ref('lane')
const downloading = ref(false)

const downloadVideo = () => {
  if (downloading.value) return
  downloading.value = true
  const ch = currentChannel.value
  const fileName = `${ch.name}_${startTime.value.replace(/[/:]/g, '-')}.mp4`
  window.location.href = `/api/hikNet/downloadVideo?startTime=${encodeURIComponent(startTime.value)}&endTime=${encodeURIComponent(endTime.value)}&channel=${ch.id}&fileName=${encodeURIComponent(fileName)}`
  setTimeout(() => { downloading.value = false }, 3000)
}

const currentChannel = computed(() => {
  const ch = channelMap.value[activeTab.value]
  return { id: ch?.channel ?? null, name: tabs.find(t => t.key === activeTab.value)?.name ?? '' }
})

const switchTab = (key) => {
  console.log('[VideoPlaybackPage] switchTab', key, 'current channelId:', currentChannel.value.id)
  if (activeTab.value === key) return
  if (videoRef.value) {
    videoRef.value.stopPlay()
  }
  activeTab.value = key
}

onMounted(async () => {
  startTime.value = route.query.startTime || ''
  endTime.value = route.query.endTime || ''
  if (!startTime.value || !endTime.value) return

  try {
    const res = await listChannels()
    if (res.code === 200 && res.data) {
      console.log('[VideoPlaybackPage] channels:', res.data)
      const map = {}
      for (const c of res.data) {
        if (c.cameraType === 'lane') map.lane = c
        else if (c.cameraType === 'appointment') map.appointment = c
        else if (c.cameraType === 'front') map.front = c
        else if (c.cameraType === 'rear') map.rear = c
        else if (c.cameraType === 'ptz360') map.ptz360 = c
      }
      channelMap.value = map
    }
  } catch (e) {
    console.error('[VideoPlaybackPage]', e)
  }

  document.title = '录像回放'
})

onUnmounted(() => {
  if (videoRef.value) {
    videoRef.value.stopPlay()
  }
  fetch('/api/hikNet/stopPlayback').catch(() => {})
})
</script>

<style scoped>
.video-playback-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0a0a0a;
}

.page-header {
  padding: 12px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-bottom: 2px solid #0f4c75;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  letter-spacing: 2px;
}

.page-time-range {
  font-size: 13px;
  color: #a0a8b6;
}

.download-btn {
  margin-left: auto;
  padding: 8px 20px;
  background: #409eff;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.download-btn:disabled {
  background: #6a6a6a;
  cursor: not-allowed;
}

.video-tab-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.video-tabs {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: #1a1a2e;
  border-bottom: 1px solid #0f4c75;
  flex-shrink: 0;
}

.tab-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: #a0a8b6;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e0e0e0;
}

.tab-btn.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  font-weight: 500;
}

.video-content {
  flex: 1;
  padding: 8px;
  min-height: 0;
}

.video-content > * {
  width: 100%;
  height: 100%;
}
</style>