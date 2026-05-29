<template>
  <div class="video-playback-page">
    <div class="page-header">
      <h1 class="page-title">录像回放</h1>
      <span class="page-time-range">{{ startTime }} ~ {{ endTime }}</span>
    </div>
    <div class="video-split-container">
      <div class="split-left">
        <VideoPlayback
          class="lane-video"
          channel-key="lane"
          channel-name="车道"
          :channel-id="laneChannelId"
          :start-time="startTime"
          :end-time="endTime"
          :is-rotated="true"
        />
      </div>
      <div class="split-right">
        <div class="split-right-top">
          <VideoPlayback
            channel-key="ptz360"
            channel-name="360球机"
            :channel-id="ptz360ChannelId"
            :start-time="startTime"
            :end-time="endTime"
          />
          <VideoPlayback
            channel-key="appointment"
            channel-name="预约机"
            :channel-id="appointmentChannelId"
            :start-time="startTime"
            :end-time="endTime"
          />
        </div>
        <div class="split-right-bottom">
          <VideoPlayback
            channel-key="rear"
            channel-name="车尾"
            :channel-id="rearChannelId"
            :start-time="startTime"
            :end-time="endTime"
          />
          <VideoPlayback
            channel-key="front"
            channel-name="车头"
            :channel-id="frontChannelId"
            :start-time="startTime"
            :end-time="endTime"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import VideoPlayback from '@/components/VideoPlayback.vue'
import { listChannels } from '@/api/stream'

const router = useRouter()
const route = useRoute()

const laneChannelId = ref(null)
const appointmentChannelId = ref(null)
const frontChannelId = ref(null)
const rearChannelId = ref(null)
const ptz360ChannelId = ref(null)

const startTime = ref('')
const endTime = ref('')

onMounted(async () => {
  startTime.value = route.query.startTime || ''
  endTime.value = route.query.endTime || ''

  const res = await listChannels()
  if (res.code === 200 && res.data?.length >= 5) {
    const channels = res.data
    laneChannelId.value = channels.find(c => c.cameraType === 'lane')?.channel
    appointmentChannelId.value = channels.find(c => c.cameraType === 'appointment')?.channel
    frontChannelId.value = channels.find(c => c.cameraType === 'front')?.channel
    rearChannelId.value = channels.find(c => c.cameraType === 'rear')?.channel
    ptz360ChannelId.value = channels.find(c => c.cameraType === 'ptz360')?.channel
    document.title = '录像回放'
  }
})
</script>

<style scoped>
.video-playback-page {
  width: 100vw;
  height: 100vh;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  background: #0a0a0a;
}

.page-header {
  padding: 12px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-bottom: 2px solid #0f4c75;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  gap: 16px;
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

.video-split-container {
  display: flex;
  flex: 1;
  gap: 4px;
  padding: 4px;
  width: 100%;
  height: calc(100vh - 56px);
  box-sizing: border-box;
}

.split-left {
  flex: 0 0 20%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.lane-video {
  flex: 1;
  min-height: 0;
}

.split-right {
  flex: 0 0 80%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.split-right-top,
.split-right-bottom {
  display: flex;
  flex: 1;
  gap: 4px;
  min-height: 0;
}
</style>