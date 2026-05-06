<template>
  <div class="video-monitor-page">
    <!-- 5路视频分屏容器: 左(1列) + 右(2列x2行) -->
    <div class="video-split-container">
      <!-- 左侧：车道（占1/3宽度，全高） -->
      <div class="split-left">
        <VideoWindow
          channel-key="lane"
          channel-name="车道"
          :channel-id="laneChannelId"
        />
      </div>
      <!-- 右侧：2x2分屏（占2/3宽度） -->
      <div class="split-right">
        <div class="split-right-top">
          <VideoWindow
            channel-key="appointment"
            channel-name="预约机"
            :channel-id="appointmentChannelId"
          />
          <VideoWindow
            channel-key="front"
            channel-name="车头"
            :channel-id="frontChannelId"
          />
        </div>
        <div class="split-right-bottom">
          <VideoWindow
            channel-key="rear"
            channel-name="车尾"
            :channel-id="rearChannelId"
          />
          <VideoWindow
            channel-key="ptz360"
            channel-name="360球机"
            :channel-id="ptz360ChannelId"
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

onMounted(async () => {
  const res = await listChannels()
  if (res.code === 200 && res.data?.length >= 5) {
    const channels = res.data
    laneChannelId.value = channels.find(c => c.cameraType === 'lane')?.channel
    appointmentChannelId.value = channels.find(c => c.cameraType === 'appointment')?.channel
    frontChannelId.value = channels.find(c => c.cameraType === 'front')?.channel
    rearChannelId.value = channels.find(c => c.cameraType === 'rear')?.channel
    ptz360ChannelId.value = channels.find(c => c.cameraType === 'ptz360')?.channel
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
}

/* 5路分屏容器 - 三列等宽 */
.video-split-container {
  display: flex;
  flex: 1;
  gap: 4px;
  padding: 4px;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

.split-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.split-right {
  flex: 1;
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