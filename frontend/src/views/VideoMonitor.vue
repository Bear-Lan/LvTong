<template>
  <div class="video-monitor-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><VideoCamera /></el-icon>
        视频监控
      </h2>
      <div class="header-actions">
        <el-select v-model="selectedChannel" placeholder="选择通道" style="width: 120px" @change="selectChannel">
          <el-option v-for="ch in channels" :key="ch.channel" :label="ch.name" :value="ch.channel" />
        </el-select>
        <el-tag :type="isPlaying ? 'success' : 'info'">{{ isPlaying ? '播放中' : '未播放' }}</el-tag>
      </div>
    </div>

    <!-- 视频播放区域 -->
    <div class="video-player-container" v-if="currentChannel">
      <video
        ref="videoElement"
        class="webrtc-video"
        autoplay
        playsinline
        controls
      ></video>
    </div>
    <div class="video-empty" v-else>
      <el-icon><VideoCamera /></el-icon>
      <p>点击下方通道开始播放</p>
    </div>

    <!-- 通道列表 -->
    <div class="channel-grid">
      <div
        v-for="ch in channels"
        :key="ch.channel"
        class="channel-card"
        :class="{ active: currentChannel?.channel === ch.channel }"
        @click="selectChannel(ch.channel)"
      >
        <el-icon><VideoCamera /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getStreamUrl, listChannels } from '@/api/stream'

const MEDIA_SERVER = 'http://127.0.0.1:8889'

const videoElement = ref(null)
const channels = ref([])
const selectedChannel = ref(null)
const currentChannel = ref(null)
const isPlaying = ref(false)
let peerConnection = null

onMounted(async () => {
  await loadChannels()
})

onUnmounted(() => {
  stopStream()
})

const loadChannels = async () => {
  const res = await listChannels()
  if (res.code === 200) {
    channels.value = res.data || []
  }
}

const selectChannel = async (channel) => {
  try {
    ElMessage.info('正在连接视频流...')
    selectedChannel.value = channel

    const res = await getStreamUrl(channel)
    if (res.code !== 200) {
      ElMessage.error(res.message || '获取流地址失败')
      return
    }

    currentChannel.value = {
      channel: res.data.channel,
      name: res.data.channelName
    }

    await startWebRTC(channel)
    isPlaying.value = true
    ElMessage.success('开始播放: ' + res.data.channelName)
  } catch (e) {
    ElMessage.error('播放失败: ' + e.message)
  }
}

const startWebRTC = async (channel) => {
  stopStream()

  peerConnection = new RTCPeerConnection()

  peerConnection.ontrack = (event) => {
    if (videoElement.value && event.streams[0]) {
      videoElement.value.srcObject = event.streams[0]
    }
  }

  peerConnection.oniceconnectionstatechange = () => {
    if (peerConnection.iceConnectionState === 'connected') {
      isPlaying.value = true
    }
  }

  const offer = await peerConnection.createOffer({
    offerToReceiveAudio: 1,
    offerToReceiveVideo: 1
  })

  await peerConnection.setLocalDescription(offer)
  await new Promise(r => setTimeout(r, 1000))

  const whepUrl = `${MEDIA_SERVER}/channel_${channel}/whep`
  const response = await fetch(whepUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/sdp' },
    body: peerConnection.localDescription.sdp
  })

  if (!response.ok) {
    const errText = await response.text()
    throw new Error(`WHEP请求失败: ${response.status} - ${errText}`)
  }

  const answerSdp = await response.text()
  await peerConnection.setRemoteDescription(new RTCSessionDescription({
    type: 'answer',
    sdp: answerSdp
  }))
}

const stopStream = () => {
  if (peerConnection) {
    peerConnection.close()
    peerConnection = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
  isPlaying.value = false
}
</script>

<style scoped>
.video-monitor-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.video-player-container {
  width: 100%;
  aspect-ratio: 16/9;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
  position: relative;
}

.webrtc-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 14px;
}

.video-empty {
  width: 100%;
  aspect-ratio: 16/9;
  background: #1a1a1a;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
  margin-bottom: 20px;
}

.video-empty .el-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.channel-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.channel-card:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.channel-card.active {
  background: #f0f9eb;
  border-color: #67c23a;
}

.channel-card .el-icon {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 8px;
}

.channel-card.active .el-icon {
  color: #67c23a;
}

.channel-name {
  display: block;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}
</style>