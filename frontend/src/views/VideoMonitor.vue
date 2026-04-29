<template>
  <div class="video-monitor-page">
    <!-- 5路视频分屏容器 -->
    <div class="video-split-container">
      <!-- 左侧：车道（占1/3宽度） -->
      <div class="split-left">
        <VideoWindow
          ref="videoLaneRef"
          channel-key="lane"
          channel-name="车道"
          :channel-id="laneChannelId"
          @fullscreen="handleFullscreen('lane')"
          @screenshot="handleScreenshot('lane')"
        />
      </div>
      <!-- 右侧：2x2分屏（占2/3宽度） -->
      <div class="split-right">
        <div class="split-right-top">
          <VideoWindow
            ref="videoAppointmentRef"
            channel-key="appointment"
            channel-name="预约机"
            :channel-id="appointmentChannelId"
            @fullscreen="handleFullscreen('appointment')"
            @screenshot="handleScreenshot('appointment')"
          />
          <VideoWindow
            ref="videoFrontRef"
            channel-key="front"
            channel-name="车头"
            :channel-id="frontChannelId"
            @fullscreen="handleFullscreen('front')"
            @screenshot="handleScreenshot('front')"
          />
        </div>
        <div class="split-right-bottom">
          <VideoWindow
            ref="videoRearRef"
            channel-key="rear"
            channel-name="车尾"
            :channel-id="rearChannelId"
            @fullscreen="handleFullscreen('rear')"
            @screenshot="handleScreenshot('rear')"
          />
          <VideoWindow
            ref="video360Ref"
            channel-key="ptz360"
            channel-name="360球机"
            :channel-id="ptz360ChannelId"
            @fullscreen="handleFullscreen('ptz360')"
            @screenshot="handleScreenshot('ptz360')"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { listChannels } from '@/api/stream'

const VideoWindow = defineAsyncComponent(() => import('@/components/VideoWindow.vue'))

// 各分屏窗口对应的channel id
const laneChannelId = ref(null)
const appointmentChannelId = ref(null)
const frontChannelId = ref(null)
const rearChannelId = ref(null)
const ptz360ChannelId = ref(null)

// video elements refs
const videoLaneRef = ref(null)
const videoAppointmentRef = ref(null)
const videoFrontRef = ref(null)
const videoRearRef = ref(null)
const video360Ref = ref(null)

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

const handleFullscreen = (channelKey) => {
  const refMap = {
    lane: videoLaneRef,
    appointment: videoAppointmentRef,
    front: videoFrontRef,
    rear: videoRearRef,
    ptz360: video360Ref
  }
  const videoRef = refMap[channelKey]?.value
  if (videoRef) {
    videoRef.toggleFullscreen()
  }
}

const handleScreenshot = (channelKey) => {
  const refMap = {
    lane: videoLaneRef,
    appointment: videoAppointmentRef,
    front: videoFrontRef,
    rear: videoRearRef,
    ptz360: video360Ref
  }
  const videoRef = refMap[channelKey]?.value
  if (videoRef) {
    videoRef.captureScreenshot()
  }
}
</script>

<style scoped>
.video-monitor-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 5路分屏容器 */
.video-split-container {
  display: flex;
  flex: 1;
  gap: 8px;
  margin-bottom: 20px;
  min-height: 400px;
}

.split-left {
  flex: 0 0 33.33%;
  height: 100%;
}

.split-right {
  flex: 0 0 66.67%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.split-right-top,
.split-right-bottom {
  display: flex;
  gap: 8px;
  flex: 1;
}

.split-right-top > div,
.split-right-bottom > div {
  flex: 1;
}
</style>