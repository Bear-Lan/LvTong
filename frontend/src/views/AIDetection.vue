<template>
  <div class="ai-detection">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><MagicStick /></el-icon>
        绿通AI智能体
      </h2>
    </div>

    <!-- Tab切换区域 -->
    <el-card class="detection-card" shadow="never">
      <el-tabs v-model="activeTab" class="detection-tabs">
        <!-- Tab 1: 车辆照片识别 -->
        <el-tab-pane label="车辆照片识别" name="vehicle">
          <div class="tab-content">
            <!-- 左右分栏布局 -->
            <el-row :gutter="20">
              <!-- 左侧：图片预览 + 操作按钮 -->
              <el-col :span="14">
                <div class="left-panel">
                  <!-- 图片预览区域 -->
                  <div class="preview-area" :class="{ 'has-image': vehiclePreviewUrl }">
                    <el-image
                      v-if="vehiclePreviewUrl"
                      :src="vehiclePreviewUrl"
                      fit="contain"
                      class="preview-image"
                      :preview-src-list="[vehiclePreviewUrl]"
                    />
                    <div v-else class="preview-placeholder">
                      <el-icon class="placeholder-icon"><Picture /></el-icon>
                      <span>请上传车辆图片</span>
                    </div>
                  </div>

                  <!-- 上传和识别按钮 -->
                  <div class="action-row">
                    <el-upload
                      ref="vehicleUploadRef"
                      :auto-upload="false"
                      :show-file-list="false"
                      :limit="1"
                      accept="image/*"
                      :on-change="(file) => handleFileChange(file, 'vehicle')"
                      :on-remove="(file) => handleFileRemove('vehicle')"
                    >
                      <el-button type="primary" plain size="large">
                        <el-icon><UploadFilled /></el-icon>
                        上传图片
                      </el-button>
                    </el-upload>
                    <el-button
                      type="primary"
                      size="large"
                      :loading="vehicleLoading"
                      :disabled="!selectedVehicleFile"
                      @click="handleVehicleDetect"
                    >
                      <el-icon v-if="!vehicleLoading"><MagicStick /></el-icon>
                      开始识别
                    </el-button>
                  </div>
                </div>
              </el-col>

              <!-- 右侧：识别结果 -->
              <el-col :span="10">
                <div class="right-panel">
                  <!-- 结果展示 -->
                  <div v-if="vehicleResult" class="result-section">
                    <!-- 基础信息卡片 -->
                    <el-card class="result-card" shadow="hover">
                      <template #header>
                        <div class="result-header">
                          <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                          <span>识别结果</span>
                        </div>
                      </template>
                      <el-descriptions :column="2" border>
                        <el-descriptions-item label="车厢类型">
                          <el-tag type="primary">{{ vehicleResult.cratetype_text || vehicleResult.cratetype || '未识别到' }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="车轮数量">
                          <el-tag type="success">{{ vehicleResult.wheel_count ?? '未识别到' }}</el-tag>
                        </el-descriptions-item>
                      </el-descriptions>
                    </el-card>

                    <!-- 详细检测结果表格 -->
                    <el-card class="result-card mt-16" shadow="hover">
                      <template #header>
                        <div class="result-header">
                          <el-icon color="#409eff"><List /></el-icon>
                          <span>检测详情</span>
                        </div>
                      </template>
                      <el-table :data="vehicleResult.data" stripe style="width: 100%" max-height="300">
                        <el-table-column prop="label" label="物体类别" width="120" />
                        <el-table-column prop="class_id" label="分类ID" width="80" />
                        <el-table-column prop="score" label="置信度" width="90">
                          <template #default="{ row }">
                            <el-tag :type="getScoreType(row.score)" size="small">
                              {{ (row.score * 100).toFixed(1) }}%
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column label="边界框" min-width="120">
                          <template #default="{ row }">
                            <span class="box-info">
                              {{ row.box?.x1?.toFixed(0) }},{{ row.box?.y1?.toFixed(0) }} -
                              {{ row.box?.x2?.toFixed(0) }},{{ row.box?.y2?.toFixed(0) }}
                            </span>
                          </template>
                        </el-table-column>
                      </el-table>
                    </el-card>
                  </div>

                  <!-- 空状态 -->
                  <div v-else class="empty-result">
                    <el-empty description="暂无识别结果" />
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 货物类型识别 -->
        <el-tab-pane label="货物类型识别" name="goods">
          <div class="tab-content">
            <el-row :gutter="20">
              <!-- 左侧：图片预览 + 操作按钮 -->
              <el-col :span="14">
                <div class="left-panel">
                  <!-- 图片预览区域 -->
                  <div class="preview-area" :class="{ 'has-image': goodsPreviewUrl }">
                    <el-image
                      v-if="goodsPreviewUrl"
                      :src="goodsPreviewUrl"
                      fit="contain"
                      class="preview-image"
                      :preview-src-list="[goodsPreviewUrl]"
                    />
                    <div v-else class="preview-placeholder">
                      <el-icon class="placeholder-icon"><Picture /></el-icon>
                      <span>请上传货物图片</span>
                    </div>
                  </div>

                  <!-- 上传和识别按钮 -->
                  <div class="action-row">
                    <el-upload
                      ref="goodsUploadRef"
                      :auto-upload="false"
                      :show-file-list="false"
                      :limit="1"
                      accept="image/*"
                      :on-change="(file) => handleFileChange(file, 'goods')"
                      :on-remove="(file) => handleFileRemove('goods')"
                    >
                      <el-button type="primary" plain size="large">
                        <el-icon><UploadFilled /></el-icon>
                        上传图片
                      </el-button>
                    </el-upload>
                    <el-button
                      type="primary"
                      size="large"
                      :loading="goodsLoading"
                      :disabled="!selectedGoodsFile"
                      @click="handleGoodsDetect"
                    >
                      <el-icon v-if="!goodsLoading"><MagicStick /></el-icon>
                      开始识别
                    </el-button>
                  </div>
                </div>
              </el-col>

              <!-- 右侧：识别结果 -->
              <el-col :span="10">
                <div class="right-panel">
                  <!-- 结果展示 -->
                  <div v-if="goodsResult && goodsResult.length > 0" class="result-section">
                    <el-card class="result-card" shadow="hover">
                      <template #header>
                        <div class="result-header">
                          <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                          <span>识别结果</span>
                        </div>
                      </template>
                      <el-table :data="goodsResult" stripe style="width: 100%" max-height="400">
                        <el-table-column prop="label" label="英文名称" width="130" />
                        <el-table-column prop="chinese_name" label="货物名称" width="100" />
                        <el-table-column prop="variety_name" label="品种" width="100" />
                        <el-table-column prop="class_id" label="分类ID" width="80" />
                        <el-table-column prop="score" label="置信度">
                          <template #default="{ row }">
                            <el-tag :type="getScoreType(row.score)" size="small">
                              {{ (row.score * 100).toFixed(1) }}%
                            </el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </el-card>
                  </div>

                  <!-- 无识别结果提示 -->
                  <div v-else-if="goodsResult && goodsResult.length === 0" class="empty-result">
                    <el-empty description="未识别到有效货物" />
                  </div>

                  <!-- 空状态 -->
                  <div v-else class="empty-result">
                    <el-empty description="暂无识别结果" />
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- Tab 3: 行驶证识别 -->
        <el-tab-pane label="行驶证识别" name="license">
          <div class="tab-content">
            <el-row :gutter="20">
              <!-- 左侧：图片预览 + 操作按钮 -->
              <el-col :span="14">
                <div class="left-panel">
                  <!-- 图片预览区域 -->
                  <div class="preview-area" :class="{ 'has-image': licensePreviewUrl }">
                    <el-image
                      v-if="licensePreviewUrl"
                      :src="licensePreviewUrl"
                      fit="contain"
                      class="preview-image"
                      :preview-src-list="[licensePreviewUrl]"
                    />
                    <div v-else class="preview-placeholder">
                      <el-icon class="placeholder-icon"><Picture /></el-icon>
                      <span>请上传行驶证图片</span>
                    </div>
                  </div>

                  <!-- 上传和识别按钮 -->
                  <div class="action-row">
                    <el-upload
                      ref="licenseUploadRef"
                      :auto-upload="false"
                      :show-file-list="false"
                      :limit="1"
                      accept="image/*"
                      :on-change="(file) => handleFileChange(file, 'license')"
                      :on-remove="(file) => handleFileRemove('license')"
                    >
                      <el-button type="primary" plain size="large">
                        <el-icon><UploadFilled /></el-icon>
                        上传图片
                      </el-button>
                    </el-upload>
                    <el-button
                      type="primary"
                      size="large"
                      :loading="licenseLoading"
                      :disabled="!selectedLicenseFile"
                      @click="handleLicenseDetect"
                    >
                      <el-icon v-if="!licenseLoading"><MagicStick /></el-icon>
                      开始识别
                    </el-button>
                  </div>
                </div>
              </el-col>

              <!-- 右侧：识别结果 -->
              <el-col :span="10">
                <div class="right-panel">
                  <!-- 结果展示 -->
                  <div v-if="licenseResult" class="result-section">
                    <el-card class="result-card" shadow="hover">
                      <template #header>
                        <div class="result-header">
                          <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                          <span>识别结果</span>
                        </div>
                      </template>
                      <div class="json-result">
                        <pre>{{ JSON.stringify(licenseResult, null, 2) }}</pre>
                      </div>
                    </el-card>
                  </div>

                  <!-- 空状态 -->
                  <div v-else class="empty-result">
                    <el-empty description="暂无识别结果" />
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, UploadFilled, SuccessFilled, List, Picture } from '@element-plus/icons-vue'
import { detectVehicle, detectGoods, detectDriverLicense } from '@/api/ai'

// 当前激活的Tab
const activeTab = ref('vehicle')

// 文件相关状态
const selectedVehicleFile = ref(null)
const selectedGoodsFile = ref(null)
const selectedLicenseFile = ref(null)

const vehiclePreviewUrl = ref('')
const goodsPreviewUrl = ref('')
const licensePreviewUrl = ref('')

// 加载状态
const vehicleLoading = ref(false)
const goodsLoading = ref(false)
const licenseLoading = ref(false)

// 识别结果
const vehicleResult = ref(null)
const goodsResult = ref(null)
const licenseResult = ref(null)

// Upload组件引用
const vehicleUploadRef = ref(null)
const goodsUploadRef = ref(null)
const licenseUploadRef = ref(null)

// 处理文件选择
const handleFileChange = (file, type) => {
  const url = URL.createObjectURL(file.raw)

  if (type === 'vehicle') {
    selectedVehicleFile.value = file.raw
    vehiclePreviewUrl.value = url
    vehicleResult.value = null
  } else if (type === 'goods') {
    selectedGoodsFile.value = file.raw
    goodsPreviewUrl.value = url
    goodsResult.value = null
  } else if (type === 'license') {
    selectedLicenseFile.value = file.raw
    licensePreviewUrl.value = url
    licenseResult.value = null
  }
}

// 处理文件移除
const handleFileRemove = (type) => {
  if (type === 'vehicle') {
    selectedVehicleFile.value = null
    vehiclePreviewUrl.value = ''
    vehicleResult.value = null
  } else if (type === 'goods') {
    selectedGoodsFile.value = null
    goodsPreviewUrl.value = ''
    goodsResult.value = null
  } else if (type === 'license') {
    selectedLicenseFile.value = null
    licensePreviewUrl.value = ''
    licenseResult.value = null
  }
}

// 车辆识别
const handleVehicleDetect = async () => {
  if (!selectedVehicleFile.value) {
    ElMessage.warning('请先选择车辆图片')
    return
  }

  vehicleLoading.value = true
  vehicleResult.value = null

  try {
    const response = await detectVehicle(selectedVehicleFile.value)
    if (response.code === 200) {
      vehicleResult.value = response.data
      ElMessage.success('识别成功')
    } else {
      ElMessage.error(response.message || '识别失败')
    }
  } catch (error) {
    console.error('车辆识别失败:', error)
    ElMessage.error(error.message || '车辆识别失败，请检查AI服务是否可用')
  } finally {
    vehicleLoading.value = false
  }
}

// 货物识别
const handleGoodsDetect = async () => {
  if (!selectedGoodsFile.value) {
    ElMessage.warning('请先选择货物图片')
    return
  }

  goodsLoading.value = true
  goodsResult.value = null

  try {
    const response = await detectGoods(selectedGoodsFile.value)
    if (response.code === 200) {
      goodsResult.value = response.data.real_object_data || []
      ElMessage.success('识别成功')
    } else {
      ElMessage.error(response.message || '识别失败')
    }
  } catch (error) {
    console.error('货物识别失败:', error)
    ElMessage.error(error.message || '货物识别失败，请检查AI服务是否可用')
  } finally {
    goodsLoading.value = false
  }
}

// 行驶证识别
const handleLicenseDetect = async () => {
  if (!selectedLicenseFile.value) {
    ElMessage.warning('请先选择行驶证图片')
    return
  }

  licenseLoading.value = true
  licenseResult.value = null

  try {
    const response = await detectDriverLicense(selectedLicenseFile.value)
    if (response.code === 200) {
      licenseResult.value = response.data
      ElMessage.success('识别成功')
    } else {
      ElMessage.error(response.message || '识别失败')
    }
  } catch (error) {
    console.error('行驶证识别失败:', error)
    ElMessage.error(error.message || '行驶证识别失败，请检查AI服务是否可用')
  } finally {
    licenseLoading.value = false
  }
}

// 根据置信度返回tag类型
const getScoreType = (score) => {
  if (score >= 0.9) return 'success'
  if (score >= 0.7) return 'warning'
  return 'info'
}
</script>

<style scoped>
.ai-detection {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.detection-card {
  border-radius: 12px;
}

.tab-content {
  padding: 20px 0;
}

.tab-content .el-row {
  align-items: flex-start;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-area {
  width: 100%;
  height: 400px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s;
}

.preview-area.has-image {
  border-style: solid;
  background: #fff;
}

.preview-image {
  width: 100%;
  height: 100%;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;
}

.placeholder-icon {
  font-size: 64px;
}

.action-row {
  display: flex;
  gap: 12px;
}

.right-panel {
  height: 400px;
  overflow-y: auto;
}

.empty-result {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 12px;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mt-16 {
  margin-top: 16px;
}

.result-card {
  border-radius: 12px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.json-result {
  max-height: 350px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.json-result pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.box-info {
  font-size: 11px;
  color: #606266;
}
</style>

<style>
.detection-tabs .el-tabs__header {
  margin-bottom: 0;
}

.detection-tabs .el-tabs__nav-wrap::after {
  height: 1px;
}

.detection-tabs .el-tabs__item {
  font-size: 15px;
  font-weight: 500;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
}

.detection-tabs .el-tabs__item.is-active {
  color: var(--el-color-primary);
}

.detection-tabs .el-tabs__active-bar {
  height: 3px;
  border-radius: 3px;
}
</style>