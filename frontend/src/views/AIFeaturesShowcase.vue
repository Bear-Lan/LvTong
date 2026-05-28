<template>
  <div class="ai-showcase">
    <!-- 背景装饰 -->
    <div class="bg-gradient"></div>
    <div class="grid-overlay"></div>
    <div class="particles"></div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="main-title">
        <span class="title-icon">⚡</span>
        绿通AI智能检测平台
        <span class="title-sub">GREENTOANG AI DETECTION</span>
      </h1>
    </div>

    <!-- 主体内容区 -->
    <div class="main-container">
      <!-- 左侧功能卡片组（数据输入） -->
      <div class="cards-left">
        <div
          v-for="(item, index) in leftFeatures"
          :key="item.name"
          class="glass-card"
          :class="`card-${index + 1}`"
          @click="handleCardClick(item)"
        >
          <div class="card-glow"></div>
          <div class="card-content">
            <div class="card-icon" v-html="item.icon"></div>
            <div class="card-text">
              <h3 class="card-title">{{ item.name }}</h3>
              <p class="card-desc">{{ item.desc }}</p>
            </div>
          </div>
          <div class="data-line left-line" :class="`line-${index + 1}`"></div>
        </div>
      </div>

      <!-- 中心AI核心 -->
      <div class="center-core">
        <div class="core-container">
          <!-- 外层光环 -->
          <div class="orbit orbit-1"></div>
          <div class="orbit orbit-2"></div>
          <div class="orbit orbit-3"></div>
          <!-- 粒子层 -->
          <div class="particle-ring"></div>
          <!-- 核心主体 -->
          <div class="core-main">
            <div class="core-inner">
              <span class="core-text">绿通AI智能体</span>
            </div>
            <div class="core-pulse"></div>
          </div>
          <!-- 中心发光 -->
          <div class="core-glow"></div>
        </div>
        <p class="core-label">AI智能核心</p>
      </div>

      <!-- 右侧功能卡片组（分析输出） -->
      <div class="cards-right">
        <div
          v-for="(item, index) in rightFeatures"
          :key="item.name"
          class="glass-card"
          :class="`card-${index + 1}`"
          @click="handleCardClick(item)"
        >
          <div class="card-glow"></div>
          <div class="card-content">
            <div class="card-icon" v-html="item.icon"></div>
            <div class="card-text">
              <h3 class="card-title">{{ item.name }}</h3>
              <p class="card-desc">{{ item.desc }}</p>
            </div>
          </div>
          <div class="data-line right-line" :class="`line-${index + 1}`"></div>
        </div>
      </div>
    </div>

    <!-- 功能弹窗 -->
    <el-dialog
      v-model="showDialog"
      :title="currentFeature?.name"
      width="500px"
      class="feature-dialog"
      center
    >
      <div class="dialog-content">
        <div class="dialog-icon" v-html="currentFeature?.iconLarge"></div>
        <p class="dialog-desc">{{ currentFeature?.longDesc }}</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="showDialog = false">关闭</el-button>
          <el-button type="primary" size="large" @click="goToDetection">
            进入功能
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showDialog = ref(false)
const currentFeature = ref(null)

document.title = '绿通AI智能体'

// 左侧功能（数据输入）
const leftFeatures = [
  {
    name: '雷达车头识别',
    desc: '精准定位车头轮廓',
    longDesc: '通过雷达扫描技术，精准识别货车车头轮廓与位置信息，适用于车辆入场自动识别场景。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="9"/><path d="M12 3v2M12 19v2M3 12h2M19 12h2"/><circle cx="12" cy="12" r="4"/><path d="M12 8v8M8 12h8"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><circle cx="32" cy="32" r="24"/><circle cx="32" cy="32" r="16"/><circle cx="32" cy="32" r="8"/><path d="M32 8v8M32 48v8M8 32h8M48 32h8"/></svg>',
    action: 'vehicle'
  },
  {
    name: '雷达车高识别',
    desc: '自动测量车辆高度',
    longDesc: '采用雷达测距技术，自动测量货车高度，适用于限高场景的智能检测与预警。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="4" y="6" width="16" height="12" rx="2"/><path d="M4 12h16M12 6v12"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><rect x="12" y="16" width="40" height="32" rx="4"/><path d="M12 24h40M32 16v32M20 16v10M44 16v10"/></svg>',
    action: 'height'
  },
  {
    name: '雷达车长宽高识别',
    desc: '三维尺寸综合测量',
    longDesc: '融合多维雷达感知技术，实现车辆长、宽、高的精准三维测量，为超限检测提供数据支撑。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><path d="M32 8L8 20v24l24 12 24-12V20L32 8z"/><path d="M32 8v36M8 20l24 12M56 20l-24 12"/></svg>',
    action: 'dimension'
  },
  {
    name: '货物实物照片识别',
    desc: 'AI拍照智能识别货物',
    longDesc: '基于深度学习的图像识别技术，对货物实物照片进行智能分类与识别，支持多种货物品类。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><rect x="8" y="8" width="48" height="48" rx="6"/><circle cx="20" cy="20" r="4"/><path d="M56 40L40 24 16 48 8 48 8 56 16 56 56 16 56 40"/></svg>',
    action: 'goods'
  },
  {
    name: 'OCR行驶证识别',
    desc: '证件自动识别与裁切',
    longDesc: '光学字符识别技术，自动识别行驶证上的文字信息，并智能裁切证件区域，便于电子化存档。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4M14 15h4"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><rect x="8" y="16" width="48" height="32" rx="4"/><path d="M8 26h48M16 34h12M32 34h12M16 42h20"/></svg>',
    action: 'license'
  }
]

// 右侧功能（AI分析输出）
const rightFeatures = [
  {
    name: '车轴识别',
    desc: '智能检测车轮车轴',
    longDesc: '通过AI视觉分析技术，精准识别货车车轴数量与位置，为轴重检测提供准确数据。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><path d="M6 15V6M18 15V6M6 6h12"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><circle cx="16" cy="48" r="8"/><circle cx="48" cy="48" r="8"/><path d="M16 40V16M48 40V16"/><path d="M16 16h32"/></svg>',
    action: 'axle'
  },
  {
    name: '车厢识别',
    desc: 'AI视觉车厢建模',
    longDesc: '基于计算机视觉技术，对货车车厢进行三维建模与识别，获取车厢结构信息。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 4h16v16H4z"/><path d="M4 4l4 4v12l-4-4M20 4l-4 4v12l4-4"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><path d="M8 12h48v40H8z"/><path d="M8 12l8 8v32l-8-8M56 12l-8 8v32l8-8M8 20h48M8 32h48M8 44h48"/></svg>',
    action: 'carriage'
  },
  {
    name: '车厢混装识别',
    desc: '智能检测货物混装',
    longDesc: 'AI图像分析技术，智能检测车厢内货物是否存在混装情况，确保货物分类合规。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 3v18"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><rect x="8" y="8" width="48" height="48" rx="4"/><path d="M8 24h48M24 8v48M40 8v48"/></svg>',
    action: 'mixed'
  },
  {
    name: '车厢货物装载率识别',
    desc: '实时计算装载效率',
    longDesc: '通过3D视觉分析技术，实时计算车厢内货物装载率，为配载优化提供数据依据。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="10" width="20" height="10" rx="2"/><path d="M4 10V6a2 2 0 012-2h12a2 2 0 012 2v4"/><path d="M6 14h4M14 14h4"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><rect x="8" y="24" width="48" height="24" rx="4"/><path d="M8 24V16a4 4 0 014-4h40a4 4 0 014 4v8"/><path d="M16 32h10M38 32h10M16 40h10M38 40h10"/></svg>',
    action: 'loading'
  },
  {
    name: '货物透视图识别',
    desc: 'X光透视货物检测',
    longDesc: '模拟X光透视效果，对车厢内货物进行深层扫描分析，获取货物内部结构信息。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="9"/><path d="M12 3v18M3 12h18M5.6 5.6l12.8 12.8M18.4 5.6L5.6 18.4"/></svg>',
    iconLarge: '<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80"><circle cx="32" cy="32" r="24"/><path d="M32 8v48M8 32h48M14.4 14.4l35.2 35.2M49.6 14.4L14.4 49.6"/></svg>',
    action: 'xray'
  }
]

const handleCardClick = (item) => {
  currentFeature.value = item
  showDialog.value = true
}

const goToDetection = () => {
  showDialog.value = false
  router.push('/ai-detection')
}
</script>

<style scoped>
.ai-showcase {
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  background: #071426;
}

.bg-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at 50% 30%, rgba(0, 191, 255, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 80%, rgba(100, 255, 218, 0.1) 0%, transparent 40%),
    radial-gradient(ellipse at 80% 80%, rgba(0, 191, 255, 0.08) 0%, transparent 40%),
    linear-gradient(180deg, #0a192f 0%, #071426 50%, #030912 100%);
  z-index: 0;
}

.grid-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(100, 255, 218, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(100, 255, 218, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  z-index: 1;
  pointer-events: none;
}

.particles {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(1px 1px at 10% 20%, rgba(100, 255, 218, 0.4), transparent),
    radial-gradient(1px 1px at 30% 60%, rgba(0, 191, 255, 0.3), transparent),
    radial-gradient(1px 1px at 50% 30%, rgba(100, 255, 218, 0.5), transparent),
    radial-gradient(1px 1px at 70% 70%, rgba(0, 191, 255, 0.4), transparent),
    radial-gradient(1px 1px at 90% 40%, rgba(100, 255, 218, 0.3), transparent);
  background-size: 200px 200px;
  animation: particleFloat 20s linear infinite;
  z-index: 1;
  pointer-events: none;
}

@keyframes particleFloat {
  0% { transform: translateY(0); }
  100% { transform: translateY(-200px); }
}

.page-header {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 30px 20px 20px;
}

.main-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 0 30px rgba(100, 255, 218, 0.5);
  margin: 0;
}

.title-icon {
  font-size: 32px;
  animation: pulse 2s ease-in-out infinite;
}

.title-sub {
  font-size: 12px;
  font-weight: 500;
  color: rgba(100, 255, 218, 0.7);
  letter-spacing: 2px;
  margin-left: 8px;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.main-container {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 80px;
  padding: 20px 60px 60px;
  min-height: calc(100vh - 120px);
}

.glass-card {
  position: relative;
  width: 220px;
  padding: 20px;
  background: rgba(10, 25, 47, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(100, 255, 218, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: visible;
}

.glass-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(100, 255, 218, 0.1) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-5px);
  border-color: rgba(100, 255, 218, 0.5);
  box-shadow:
    0 0 20px rgba(100, 255, 218, 0.2),
    0 10px 40px rgba(0, 0, 0, 0.3);
}

.glass-card:hover::before {
  opacity: 1;
}

.card-glow {
  position: absolute;
  top: -1px;
  left: -1px;
  right: -1px;
  bottom: -1px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(100, 255, 218, 0.3), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.glass-card:hover .card-glow {
  opacity: 1;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 2;
}

.card-icon {
  width: 44px;
  height: 44px;
  padding: 8px;
  background: rgba(0, 191, 255, 0.15);
  border-radius: 10px;
  color: #64ffda;
  flex-shrink: 0;
}

.card-icon :deep(svg) {
  width: 100%;
  height: 100%;
}

.card-text {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 4px;
  white-space: nowrap;
}

.card-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
  line-height: 1.3;
}

.data-line {
  position: absolute;
  height: 2px;
  top: 50%;
  background: linear-gradient(90deg, transparent, #64ffda, transparent);
  opacity: 0.4;
  animation: dataFlow 2s ease-in-out infinite;
}

.left-line {
  right: 100%;
  margin-right: 20px;
  background: linear-gradient(90deg, transparent, #64ffda);
}

.right-line {
  left: 100%;
  margin-left: 20px;
  background: linear-gradient(270deg, transparent, #64ffda);
}

.cards-left .glass-card {
  margin-bottom: 20px;
}

.cards-left .card-1 { margin-bottom: 30px; }
.cards-left .card-2 { margin-bottom: 10px; }
.cards-left .card-3 { margin-left: 40px; margin-bottom: 10px; }
.cards-left .card-4 { margin-bottom: 10px; }
.cards-left .card-5 { margin-left: 40px; }

.cards-right .glass-card {
  margin-bottom: 20px;
}

.cards-right .card-1 { margin-bottom: 30px; }
.cards-right .card-2 { margin-bottom: 10px; }
.cards-right .card-3 { margin-right: 40px; margin-bottom: 10px; }
.cards-right .card-4 { margin-bottom: 10px; }
.cards-right .card-5 { margin-right: 40px; }

@keyframes dataFlow {
  0% { opacity: 0.2; }
  50% { opacity: 0.6; }
  100% { opacity: 0.2; }
}

.left-line.line-1 { width: 80px; animation-delay: 0s; }
.left-line.line-2 { width: 100px; animation-delay: 0.3s; }
.left-line.line-3 { width: 120px; animation-delay: 0.6s; }
.left-line.line-4 { width: 100px; animation-delay: 0.9s; }
.left-line.line-5 { width: 80px; animation-delay: 1.2s; }

.right-line.line-1 { width: 80px; animation-delay: 0s; }
.right-line.line-2 { width: 100px; animation-delay: 0.3s; }
.right-line.line-3 { width: 120px; animation-delay: 0.6s; }
.right-line.line-4 { width: 100px; animation-delay: 0.9s; }
.right-line.line-5 { width: 80px; animation-delay: 1.2s; }

.center-core {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.core-container {
  position: relative;
  width: 180px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(100, 255, 218, 0.3);
  animation: rotate 10s linear infinite;
}

.orbit-1 {
  width: 200px;
  height: 200px;
  animation-duration: 15s;
  border-style: dashed;
}

.orbit-2 {
  width: 240px;
  height: 240px;
  animation-duration: 20s;
  animation-direction: reverse;
  border-color: rgba(0, 191, 255, 0.2);
}

.orbit-3 {
  width: 280px;
  height: 280px;
  animation-duration: 25s;
  border-color: rgba(100, 255, 218, 0.15);
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.particle-ring {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background-image:
    radial-gradient(2px 2px at 50% 0%, rgba(100, 255, 218, 0.6), transparent),
    radial-gradient(2px 2px at 100% 50%, rgba(100, 255, 218, 0.6), transparent),
    radial-gradient(2px 2px at 50% 100%, rgba(100, 255, 218, 0.6), transparent),
    radial-gradient(2px 2px at 0% 50%, rgba(100, 255, 218, 0.6), transparent);
  animation: rotate 8s linear infinite;
}

.core-main {
  position: relative;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.core-inner {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0a192f, #071426);
  border: 2px solid rgba(100, 255, 218, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 30px rgba(100, 255, 218, 0.3),
    0 0 60px rgba(100, 255, 218, 0.2),
    inset 0 0 20px rgba(100, 255, 218, 0.1);
}

.core-text {
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #64ffda, #00bfff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(100, 255, 218, 0.5);
}

.core-pulse {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(100, 255, 218, 0.2);
  animation: corePulse 2s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.3); opacity: 0; }
}

.core-glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(100, 255, 218, 0.15) 0%, transparent 70%);
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.core-label {
  font-size: 14px;
  color: rgba(100, 255, 218, 0.8);
  font-weight: 500;
  letter-spacing: 2px;
  margin: 0;
  text-shadow: 0 0 10px rgba(100, 255, 218, 0.3);
}

.feature-dialog {
  --el-dialog-bg-color: rgba(10, 25, 47, 0.95);
  --el-text-color-primary: #fff;
  border: 1px solid rgba(100, 255, 218, 0.3);
  border-radius: 16px !important;
}

.feature-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(100, 255, 218, 0.2);
  padding-bottom: 16px;
}

.feature-dialog :deep(.el-dialog__title) {
  color: #64ffda;
  font-weight: 600;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
}

.dialog-icon {
  width: 80px;
  height: 80px;
  padding: 16px;
  background: rgba(0, 191, 255, 0.1);
  border-radius: 16px;
  color: #64ffda;
}

.dialog-icon :deep(svg) {
  width: 100%;
  height: 100%;
}

.dialog-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  text-align: center;
  margin: 0;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .main-container {
    gap: 40px;
    padding: 20px 40px 40px;
  }

  .glass-card {
    width: 180px;
    padding: 16px;
  }

  .core-container {
    width: 140px;
    height: 140px;
  }

  .orbit-1 { width: 160px; height: 160px; }
  .orbit-2 { width: 190px; height: 190px; }
  .orbit-3 { width: 220px; height: 220px; }

  .particle-ring {
    width: 160px;
    height: 160px;
  }
}

@media (max-width: 1024px) {
  .main-container {
    flex-direction: column;
    gap: 40px;
    padding: 20px;
  }

  .cards-left, .cards-right {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 16px;
  }

  .glass-card {
    margin: 0 !important;
  }

  .data-line {
    display: none;
  }

  .center-core {
    order: -1;
  }
}
</style>

<style>
.el-overlay-dialog {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}
</style>