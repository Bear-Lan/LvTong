<template>
  <div class="video-monitor-page">
    <div class="page-header">
      <h1 class="page-title">硚孝高速毛陈收费站X射线绿通快检试点项目视频监控</h1>
    </div>
    <!-- 5路视频分屏容器: 左(1列) + 右(2列x2行) -->
    <div class="video-split-container">
      <!-- 左侧：车道（占1/3宽度，全高） -->
      <div class="split-left">
        <VideoWindow
          class="lane-video"
          channel-key="lane"
          channel-name="车道"
          :channel-id="laneChannelId"
          :media-server-url="mediaServerUrl"
          :is-rotated="true"
        />
      </div>
      <!-- 右侧：2x2分屏（占2/3宽度） -->
      <div class="split-right">
        <div class="split-right-top">
          <VideoWindow
            channel-key="ptz360"
            channel-name="360球机"
            :channel-id="ptz360ChannelId"
            :media-server-url="mediaServerUrl"
          />
          <VideoWindow
            channel-key="appointment"
            channel-name="预约机"
            :channel-id="appointmentChannelId"
            :media-server-url="mediaServerUrl"
          />
        </div>
        <div class="split-right-bottom">
          <VideoWindow
            channel-key="rear"
            channel-name="车尾"
            :channel-id="rearChannelId"
            :media-server-url="mediaServerUrl"
          />
          
          <VideoWindow
            channel-key="front"
            channel-name="车头"
            :channel-id="frontChannelId"
            :media-server-url="mediaServerUrl"
          />
          
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import VideoWindow from '@/components/VideoWindow.vue'
import { listChannels } from '@/api/stream'

// 通道ID (1-5)
const laneChannelId = ref(null)
const appointmentChannelId = ref(null)
const frontChannelId = ref(null)
const rearChannelId = ref(null)
const ptz360ChannelId = ref(null)
const mediaServerUrl = ref('http://127.0.0.1:8889')

onMounted(async () => {
  const res = await listChannels()
  if (res.code === 200 && res.data?.length >= 5) {
    const channels = res.data
    laneChannelId.value = channels.find(c => c.cameraType === 'lane')?.channel
    appointmentChannelId.value = channels.find(c => c.cameraType === 'appointment')?.channel
    frontChannelId.value = channels.find(c => c.cameraType === 'front')?.channel
    rearChannelId.value = channels.find(c => c.cameraType === 'rear')?.channel
    ptz360ChannelId.value = channels.find(c => c.cameraType === 'ptz360')?.channel
    mediaServerUrl.value = channels[0]?.mediaServerUrl || mediaServerUrl.value
    document.title = '绿通视频监控'
  }
})
</script>

<style scoped>
.video-monitor-page {
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
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  text-align: center;
  letter-spacing: 2px;
}

/* 5路分屏容器 - 三列等宽 */
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