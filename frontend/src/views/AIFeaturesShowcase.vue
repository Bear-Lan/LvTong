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
        绿通AI智能测试平台
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
          <div class="orbit orbit-1"></div>
          <div class="orbit orbit-2"></div>
          <div class="orbit orbit-3"></div>
          <div class="particle-ring"></div>
          <div class="core-main">
            <div class="core-inner">
              <span class="core-text">绿通AI智能体</span>
            </div>
            <div class="core-pulse"></div>
          </div>
          <div class="core-glow"></div>
        </div>
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
      width="900px"
      class="feature-dialog"
      :close-on-click-modal="false"
      @closed="resetDialogState"
    >
      <!-- 顶部操作按钮区 -->
      <div class="dialog-toolbar" v-if="currentFeature && apiMapping[currentFeature.action]?.api">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :disabled="detecting"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-button size="large" :disabled="detecting" type="primary">
            <el-icon><UploadFilled /></el-icon>
            上传图片
          </el-button>
        </el-upload>
        <el-button
          size="large"
          type="primary"
          :disabled="!selectedFile || detecting"
          :loading="detecting"
          @click="handleDetect"
        >
          <el-icon v-if="!detecting"><MagicStick /></el-icon>
          开始AI检测
        </el-button>
      </div>

      <!-- 待开发提示 -->
      <div class="developing-tip" v-else-if="currentFeature">
        <el-icon class="tip-icon"><Clock /></el-icon>
        <span class="tip-text">{{ currentFeature?.name }} - 功能待开发</span>
      </div>

      <!-- 主体左右分栏 -->
      <div class="dialog-body" v-if="currentFeature && apiMapping[currentFeature.action]?.api">
        <!-- 左侧：图片预览 -->
        <div class="dialog-left">
          <el-tabs v-model="activeImageTab" class="image-tabs">
            <el-tab-pane label="原图" name="original"></el-tab-pane>
            <el-tab-pane label="检测结果图" name="result" :disabled="!detectionComplete || !resultImageUrl"></el-tab-pane>
          </el-tabs>
          <div
            class="image-container"
            :class="{ 'has-image': previewUrl, 'drag-over': isDragOver }"
            @dragover.prevent="isDragOver = true"
            @dragleave="isDragOver = false"
            @drop.prevent="handleDrop"
            @click="triggerUpload"
          >
            <el-image
              v-if="previewUrl"
              :src="activeImageTab === 'result' && resultImageUrl ? resultImageUrl : previewUrl"
              fit="contain"
              class="preview-image"
              :preview-src-list="previewSrcList"
              :initial-index="0"
              preview-teleported
            />
            <div v-if="!previewUrl" class="upload-hint">
              <el-icon class="hint-icon"><Picture /></el-icon>
              <span class="hint-text">点击或拖拽上传图片</span>
              <span class="hint-format">支持 jpg/png/jpeg/bmp</span>
            </div>
          </div>
        </div>

        <!-- 右侧：检测结果 -->
        <div class="dialog-right">
          <div class="result-header">
            <el-icon><List /></el-icon>
            <span>AI检测结果</span>
          </div>
          <div class="result-body">
            <!-- 车轴识别结果 -->
            <template v-if="detectionResult && currentFeature?.action === 'axle'">
              <div class="result-info">
                <div class="info-item">
                  <span class="info-label">轮胎总数</span>
                  <span class="info-value highlight">{{ detectionResult.wheel_count }}</span>
                </div>
                <div class="info-divider"></div>
                <div class="info-item">
                  <span class="info-label">车厢类型</span>
                  <span class="info-value">{{ detectionResult.cratetype }}</span>
                </div>
              </div>
              <div class="result-table">
                <div class="table-header">
                  <span>序号</span>
                  <span>类型</span>
                  <span>置信度</span>
                </div>
                <div
                  v-for="(item, index) in detectionResult.data"
                  :key="index"
                  class="table-row"
                >
                  <span>{{ index + 1 }}</span>
                  <span>{{ formatAxleLabel(item.label) }}</span>
                  <span>{{ (item.score * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </template>

            <!-- 车厢识别结果 -->
            <template v-else-if="detectionResult && currentFeature?.action === 'carriage'">
              <div class="result-info">
                <div class="info-item">
                  <span class="info-label">车厢类型</span>
                  <span class="info-value highlight">{{ detectionResult.cratetype_text || detectionResult.cratetype }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">车厢编码</span>
                  <span class="info-value">{{ detectionResult.cratetype }}</span>
                </div>
              </div>
              <div class="result-table" v-if="carriageFilteredData.length > 0">
                <div class="table-header">
                  <span>序号</span>
                  <span>类型</span>
                  <span>置信度</span>
                </div>
                <div
                  v-for="(item, index) in carriageFilteredData"
                  :key="index"
                  class="table-row"
                >
                  <span>{{ index + 1 }}</span>
                  <span>{{ formatAxleLabel(item.label) }}</span>
                  <span>{{ (item.score * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </template>

            <!-- 货物识别结果 -->
            <template v-else-if="detectionResult && currentFeature?.action === 'goods'">
              <div class="result-info">
                <div class="info-item">
                  <span class="info-label">识别货物数</span>
                  <span class="info-value highlight">{{ filteredGoods.length }}</span>
                </div>
              </div>
              <div class="result-table" v-if="filteredGoods.length > 0">
                <div class="table-header goods-header">
                  <span>序号</span>
                  <span>货物名称</span>
                  <span>品种</span>
                  <span>置信度</span>
                </div>
                <div
                  v-for="(item, index) in filteredGoods"
                  :key="index"
                  class="table-row goods-row"
                >
                  <span>{{ index + 1 }}</span>
                  <span>{{ item.chinese_name }}</span>
                  <span>{{ item.variety_name }}</span>
                  <span>{{ (item.score * 100).toFixed(1) }}%</span>
                </div>
              </div>
              <div v-else class="empty-result">
                <el-icon class="empty-icon"><Document /></el-icon>
                <span class="empty-text">未识别到有效货物</span>
              </div>
            </template>

            <!-- OCR行驶证识别结果 -->
            <template v-else-if="detectionResult && currentFeature?.action === 'license'">
              <div class="result-table" v-if="detectionResult.data && detectionResult.data.length > 0">
                <div class="table-header">
                  <span>序号</span>
                  <span>项目</span>
                  <span>内容</span>
                </div>
                <div
                  v-for="(item, index) in detectionResult.data"
                  :key="index"
                  class="table-row"
                >
                  <span>{{ index + 1 }}</span>
                  <span>{{ item.key }}</span>
                  <span>{{ item.value }}</span>
                </div>
              </div>
              <div v-else class="empty-result">
                <el-icon class="empty-icon"><Document /></el-icon>
                <span class="empty-text">未识别到有效信息</span>
              </div>
            </template>

            <!-- 待开发状态 -->
            <template v-else-if="detectionResult?.status === '待开发'">
              <div class="empty-result" style="min-height: 200px;">
                <el-icon class="empty-icon"><Clock /></el-icon>
                <span class="empty-text">待开发</span>
              </div>
            </template>
            <template v-else-if="detectionResult">
              <pre class="json-result">{{ JSON.stringify(detectionResult, null, 2) }}</pre>
            </template>

            <div v-else class="empty-result">
              <el-icon class="empty-icon"><Document /></el-icon>
              <span class="empty-text">暂无检测结果<br/>请先上传图片并开始检测</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="showDialog = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, MagicStick, Picture, List, Document, Clock } from '@element-plus/icons-vue'
import { detectGoods, detectAxle, detectCarriage, detectOCR } from '@/api/ai'

const showDialog = ref(false)
const currentFeature = ref(null)
const uploadRef = ref(null)

document.title = '绿通AI智能体'

// 弹窗状态
const selectedFile = ref(null)
const previewUrl = ref('')
const resultImageUrl = ref('')
const previewSrcList = ref([])
const detecting = ref(false)
const detectionComplete = ref(false)
const detectionResult = ref(null)
const activeImageTab = ref('original')
const isDragOver = ref(false)

// 已实现API的功能映射
const apiMapping = {
  goods: { api: detectGoods, hasBoxes: false },
  axle: { api: detectAxle, hasBoxes: true },
  carriage: { api: detectCarriage, hasBoxes: true },
  license: { api: detectOCR, hasBoxes: false }
}

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

// 车轴标签映射
const axleLabelMap = {
  'wheel': '轮胎',
  'locomotive': '车头',
  'box_truck_block': '车厢'
}

// 格式化车轴标签
const formatAxleLabel = (label) => {
  return axleLabelMap[label] || label
}

// 过滤货物数据（排除product_code为others的）
const filteredGoods = computed(() => {
  if (!detectionResult.value?.real_object_data) return []
  return detectionResult.value.real_object_data.filter(item =>
    item.product_code && item.product_code.toLowerCase() !== 'others'
  )
})

// 车厢识别过滤车轮数据，只显示车头和车厢
const carriageFilteredData = computed(() => {
  if (!detectionResult.value?.data) return []
  return detectionResult.value.data.filter(item =>
    item.label && item.label.toLowerCase() !== 'wheel'
  )
})

const handleCardClick = (item) => {
  currentFeature.value = item

  // 检查功能是否已实现
  const apiConfig = apiMapping[item.action]
  if (!apiConfig?.api) {
    // 未实现的功能，直接显示待开发状态
    detectionResult.value = { status: '待开发' }
    detectionComplete.value = true
    resultImageUrl.value = ''
    previewUrl.value = ''
    selectedFile.value = null
    activeImageTab.value = 'original'
  } else {
    // 已实现功能，重置检测状态
    resetDialogState()
  }

  showDialog.value = true
}

const handleFileChange = (file) => {
  const ext = file.name.toLowerCase().split('.').pop()
  if (!['jpg', 'jpeg', 'png', 'bmp'].includes(ext)) {
    ElMessage.error('仅支持 jpg/png/jpeg/bmp 格式图片')
    return
  }
  detectionResult.value = null
  detectionComplete.value = false
  resultImageUrl.value = ''
  activeImageTab.value = 'original'
  selectedFile.value = file.raw
  const url = URL.createObjectURL(file.raw)
  previewUrl.value = url
  previewSrcList.value = [url]
}

const handleFileRemove = () => {
  selectedFile.value = null
  previewUrl.value = ''
  resultImageUrl.value = ''
  detectionResult.value = null
  detectionComplete.value = false
  activeImageTab.value = 'original'
}

const handleDrop = (e) => {
  isDragOver.value = false
  const file = e.dataTransfer.files[0]
  if (file) {
    handleFileChange({ raw: file, name: file.name })
  }
}

const triggerUpload = () => {
  if (!selectedFile.value && !detecting.value) {
    uploadRef.value?.$refs['upload-inner']?.$refs.input.click()
  }
}

const handleDetect = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }

  detecting.value = true
  detectionResult.value = null
  resultImageUrl.value = ''
  activeImageTab.value = 'original'

  try {
    const action = currentFeature.value?.action
    const apiConfig = apiMapping[action]

    if (apiConfig?.api) {
      // 调用真实API
      const response = await apiConfig.api(selectedFile.value)
      // 兼容不同API返回格式（code=200或code=1）
      if (response.code === 200 || response.code === 1) {
        detectionResult.value = response.data
        // 提取结果图base64（仅已有结果图的API）
        if (apiConfig.hasBoxes && response.data?.result_image?.base64) {
          const format = response.data.result_image.format || 'jpg'
          resultImageUrl.value = `data:image/${format};base64,${response.data.result_image.base64}`
        }
      } else {
        ElMessage.error(response.message || '识别失败')
      }
    } else {
      // 未实现的功能，直接显示待开发
      detectionResult.value = { status: '待开发' }
    }

    detectionComplete.value = true
    activeImageTab.value = 'result'
  } catch (error) {
    console.error('检测失败:', error)
    ElMessage.error(error.message || '检测失败，请检查AI服务是否可用')
  } finally {
    detecting.value = false
  }
}

const resetDialogState = () => {
  selectedFile.value = null
  previewUrl.value = ''
  resultImageUrl.value = ''
  detecting.value = false
  detectionComplete.value = false
  detectionResult.value = null
  activeImageTab.value = 'original'
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
  gap: 100px;
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
  top: 50%;
  transform: translateY(-50%);
  height: 2px;
  opacity: 0.4;
  animation: dataFlow 2s ease-in-out infinite;
}

.left-line {
  left: 100%;
  width: 120px;
  background: linear-gradient(90deg, transparent, #64ffda);
}

.right-line {
  right: 100%;
  width: 120px;
  background: linear-gradient(270deg, transparent, #64ffda);
}

@keyframes dataFlow {
  0% { opacity: 0.2; }
  50% { opacity: 0.7; }
  100% { opacity: 0.2; }
}

.cards-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 24px;
}

.cards-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 24px;
}

.center-core {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.core-container {
  position: relative;
  width: 280px;
  height: 280px;
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
  width: 320px;
  height: 320px;
  animation-duration: 15s;
  border-style: dashed;
}

.orbit-2 {
  width: 380px;
  height: 380px;
  animation-duration: 20s;
  animation-direction: reverse;
  border-color: rgba(0, 191, 255, 0.2);
}

.orbit-3 {
  width: 440px;
  height: 440px;
  animation-duration: 25s;
  border-color: rgba(100, 255, 218, 0.15);
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.particle-ring {
  position: absolute;
  width: 320px;
  height: 320px;
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
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.core-inner {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0a192f, #071426);
  border: 3px solid rgba(100, 255, 218, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 40px rgba(100, 255, 218, 0.4),
    0 0 80px rgba(100, 255, 218, 0.25),
    inset 0 0 30px rgba(100, 255, 218, 0.15);
}

.core-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #64ffda, #00bfff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(100, 255, 218, 0.5);
}

.core-pulse {
  position: absolute;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: rgba(100, 255, 218, 0.25);
  animation: corePulse 2s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.3); opacity: 0; }
}

.core-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(100, 255, 218, 0.2) 0%, transparent 70%);
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

/* ======================= 弹窗样式 ======================= */

/* 弹窗整体 */
.feature-dialog {
  --el-dialog-bg-color: #fff;
  --el-text-color-primary: #000;
  border: 1px solid rgba(0, 191, 255, 0.3) !important;
  border-radius: 16px !important;
  box-shadow: 0 0 40px rgba(100, 255, 218, 0.15), 0 20px 60px rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(12px);
  max-width: 1100px;
}

.feature-dialog :deep(.el-dialog) {
  background: transparent !important;
}

.feature-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  padding: 16px 20px;
  margin-right: 0;
}

.feature-dialog :deep(.el-dialog__headerbtn) {
  top: 16px;
  right: 16px;
}

.feature-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(0, 0, 0, 0.5);
  font-size: 18px;
  transition: all 0.3s;
}

.feature-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #00bfff;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
}

.feature-dialog :deep(.el-dialog__title) {
  color: #000;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.feature-dialog :deep(.el-dialog__body) {
  padding: 20px;
  color: #000;
}

/* 顶部工具栏 */
.dialog-toolbar {
  display: flex;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
}

/* 工具栏按钮 */
.dialog-toolbar .el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s;
}

.dialog-toolbar .el-button--primary.is-plain {
  background: transparent !important;
  border: 1px solid rgba(0, 0, 0, 0.3) !important;
  color: #000 !important;
}

.dialog-toolbar .el-button--primary.is-plain:hover {
  background: rgba(0, 0, 0, 0.05) !important;
  border-color: rgba(0, 0, 0, 0.5) !important;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
}

.dialog-toolbar .el-button--primary {
  background: linear-gradient(135deg, #00bfff, #00bfff) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
}

.dialog-toolbar .el-button--primary:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.4);
  transform: translateY(-1px);
}

.dialog-toolbar .el-button.is-disabled {
  background: rgba(0, 0, 0, 0.1) !important;
  border-color: rgba(0, 0, 0, 0.2) !important;
  color: rgba(0, 0, 0, 0.4);
  box-shadow: none !important;
}

/* 待开发提示 */
.developing-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 20px;
  color: rgba(0, 0, 0, 0.7);
}

.tip-icon {
  font-size: 64px;
  color: rgba(0, 0, 0, 0.3);
  animation: pulse 2s ease-in-out infinite;
}

.tip-text {
  font-size: 18px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
  letter-spacing: 1px;
}

/* 主体左右分栏 */
.dialog-body {
  display: flex;
  gap: 20px;
  min-height: 420px;
}

.dialog-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dialog-right {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

/* ======================= 图片切换标签 ======================= */
.image-tabs {
  margin-bottom: 12px;
}

.image-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  border-bottom: 1px solid rgba(100, 255, 218, 0.15);
}

.image-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.image-tabs :deep(.el-tabs__item) {
  color: #333;
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
  transition: all 0.3s;
}

.image-tabs :deep(.el-tabs__item:hover) {
  color: #000;
}

.image-tabs :deep(.el-tabs__item.is-disabled) {
  color: rgba(0, 0, 0, 0.25);
  cursor: not-allowed;
}

.image-tabs :deep(.el-tabs__item.is-active) {
  color: #00bfff;
  font-weight: 600;
}

.image-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  background: linear-gradient(90deg, #00bfff, #00bfff);
  box-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
}

/* ======================= 图片容器 ======================= */
.image-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(240, 240, 240, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  min-height: 340px;
}

.image-container:hover {
  border-color: rgba(0, 191, 255, 0.4);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
}

.image-container.has-image {
  background: rgba(0, 0, 0, 0.05);
  border-style: solid;
}

.image-container.drag-over {
  border-color: #00bfff;
  background: rgba(0, 191, 255, 0.05);
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: rgba(0, 0, 0, 0.4);
  text-align: center;
}

.hint-icon {
  font-size: 56px;
  color: rgba(0, 0, 0, 0.2);
  transition: all 0.3s;
}

.image-container:hover .hint-icon {
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
}

.hint-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.5);
}

.hint-format {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.3);
}

/* ======================= 结果区域 ======================= */
.result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(100, 255, 218, 0.12), rgba(0, 191, 255, 0.08));
  border: 1px solid rgba(100, 255, 218, 0.25);
  border-radius: 10px 10px 0 0;
  color: #000;
  font-weight: 600;
  font-size: 14px;
}

.result-header .el-icon {
  font-size: 16px;
  color: #000;
}

.result-body {
  flex: 1;
  background: #ffffff;
  border: 1px solid rgba(100, 255, 218, 0.2);
  border-top: none;
  border-radius: 0 0 10px 10px;
  overflow: auto;
  min-height: 300px;
}

/* 滚动条样式 */
.result-body::-webkit-scrollbar {
  width: 6px;
}

.result-body::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.result-body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.result-body::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.4);
}

.json-result {
  margin: 0;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #000;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-result {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: rgba(0, 0, 0, 0.4);
  min-height: 300px;
  padding: 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: rgba(0, 0, 0, 0.3);
}

.empty-text {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

/* ======================= 结果信息展示 ======================= */
.result-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(100, 255, 218, 0.06);
  border: 1px solid rgba(100, 255, 218, 0.12);
  border-radius: 8px;
  transition: all 0.3s;
}

.info-item:hover {
  background: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.15);
}

.info-label {
  color: #333;
  font-size: 13px;
}

.info-value {
  color: #000;
  font-size: 14px;
  font-weight: 500;
}

.info-value.highlight {
  color: #000;
  font-size: 22px;
  font-weight: 700;
}

.info-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(100, 255, 218, 0.2), transparent);
}

/* ======================= 结果表格 ======================= */
.result-table {
  margin: 0 16px 16px;
  border: 1px solid rgba(100, 255, 218, 0.2);
  border-radius: 8px;
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 50px 1fr 90px;
  gap: 8px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(100, 255, 218, 0.15), rgba(0, 191, 255, 0.1));
  font-size: 12px;
  font-weight: 600;
  color: #000;
}

.table-row {
  display: grid;
  grid-template-columns: 50px 1fr 90px;
  gap: 8px;
  padding: 12px 14px;
  font-size: 13px;
  color: #000;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.table-row:hover {
  background: rgba(0, 0, 0, 0.03);
}

.goods-header {
  grid-template-columns: 50px 1fr 90px 90px !important;
}

.goods-row {
  grid-template-columns: 50px 1fr 90px 90px !important;
}

/* ======================= 弹窗底部 ======================= */
.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  margin-top: 16px;
}

.dialog-footer .el-button {
  min-width: 120px;
  height: 42px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(0, 0, 0, 0.3);
  background: transparent;
  color: #000;
  transition: all 0.3s;
}

.dialog-footer .el-button:hover {
  background: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
}

/* 响应式 */
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

  .dialog-body {
    flex-direction: column;
    min-height: auto;
  }

  .dialog-left {
    min-height: 300px;
  }

  .dialog-right {
    width: 100%;
  }

  .dialog-toolbar {
    flex-wrap: wrap;
  }

  .feature-dialog {
    max-width: 95vw;
    margin: 20px;
  }
}
</style>

<style>
.el-overlay-dialog {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}
</style>