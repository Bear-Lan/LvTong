<template>
  <!--
    查验详情弹窗
    布局：证据链照片、查验数据、底部结论
  -->
  <el-dialog
    :model-value="visible"
    width="95vw"
    top="1vh"
    destroy-on-close
    class="inspection-detail-dialog"
    :show-close="true"
    :close-on-click-modal="true"
    append-to-body
  >

    <!-- ================== 整体布局 ================== -->
    <div class="detail-body">
      <!-- 标题：右上角 -->
      <div class="detail-title">查验详情</div>
        <!-- 第一区域：证据链照片
             第一行：车头照、车尾照、行驶证、顶部照、通行凭证（5列）
             第二行：透视影像+车身照（占50%）、货物照（占50%） -->
        <div class="detail-section evidence-section">
          <!-- 第一行：5列等宽网格 -->
          <div class="evidence-grid-row-1">
            <!-- 车头照（type-tag=11） -->
            <div class="evidence-item">
              <div class="evidence-img-box" v-if="rows.headImagePath">
                <el-image
                  :src="formatImageUrl(rows.headImagePath)"
                  fit="fill"
                  :preview-src-list="[formatImageUrl(rows.headImagePath)]"
                  class="evidence-img"
                  :initial-index="0"
                />
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">11</span>车头照
              </div>
            </div>

            <!-- 车尾照（type-tag=12） -->
            <div class="evidence-item">
              <div class="evidence-img-box" v-if="rows.tailImagePath">
                <el-image
                  :src="formatImageUrl(rows.tailImagePath)"
                  fit="fill"
                  :preview-src-list="[formatImageUrl(rows.tailImagePath)]"
                  class="evidence-img"
                  :initial-index="1"
                />
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">12</span>车尾照
              </div>
            </div>

            <!-- 行驶证（type-tag=13） -->
            <div class="evidence-item">
              <div class="evidence-img-box" v-if="rows.licenseImagePath">
                <el-image
                  :src="formatImageUrl(rows.licenseImagePath)"
                  fit="fill"
                  :preview-src-list="[formatImageUrl(rows.licenseImagePath)]"
                  class="evidence-img"
                  :initial-index="2"
                />
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">13</span>行驶证
              </div>
            </div>

            <!-- 顶部照 -->
            <div class="evidence-item">
              <div class="evidence-img-box" v-if="rows.topImagePath">
                <el-image
                  :src="formatImageUrl(rows.topImagePath)"
                  fit="fill"
                  :preview-src-list="[formatImageUrl(rows.topImagePath)]"
                  class="evidence-img"
                  :initial-index="3"
                />
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">26</span>顶部照
              </div>
            </div>

            <!-- 通行凭证 -->
            <div class="evidence-item">
              <div class="evidence-img-box" v-if="rows.passcodeImagePath">
                <el-image
                  :src="formatImageUrl(rows.passcodeImagePath)"
                  fit="fill"
                  :preview-src-list="[formatImageUrl(rows.passcodeImagePath)]"
                  class="evidence-img"
                  :initial-index="4"
                />
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">凭证</span>通行凭证
              </div>
            </div>
          </div>

          <!-- 第二行：左侧50%（透视+车身2列），右侧50%（货物照） -->
          <div class="evidence-grid-row-2">
            <!-- 左侧50%：透视影像 + 车身照（2列，等宽） -->
            <div class="evidence-row-2-left">
              <!-- 透视影像（ETC X光扫描） -->
              <div class="evidence-item">
                <div class="evidence-img-box" v-if="rows.transparentImagePath">
                  <el-image
                    :src="formatImageUrl(rows.transparentImagePath)"
                    fit="fill"
                    :preview-src-list="[formatImageUrl(rows.transparentImagePath)]"
                    class="evidence-img"
                    :initial-index="5"
                  />
                </div>
                <div class="evidence-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="evidence-label">
                  <span class="type-tag">X光</span>透视影像
                </div>
              </div>

              <!-- 车身照（type-tag=25） -->
              <div class="evidence-item">
                <div class="evidence-img-box" v-if="rows.bodyImagePath">
                  <el-image
                    :src="formatImageUrl(rows.bodyImagePath)"
                    fit="fill"
                    :preview-src-list="[formatImageUrl(rows.bodyImagePath)]"
                    class="evidence-img"
                    :initial-index="6"
                  />
                </div>
                <div class="evidence-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="evidence-label">
                  <span class="type-tag">25</span>车身照
                </div>
              </div>
            </div>

            <!-- 右侧50%：货物照，根据张数使用网格或横向滚动 -->
            <div class="evidence-item evidence-col-right">
              <div
                class="goods-img-container"
                :class="{ 'scroll-mode': useScrollMode }"
                v-if="goodsImages.length > 0"
                :style="useScrollMode ? {} : { gridTemplateColumns: `repeat(${goodsPerRow}, ${goodsColWidth})` }"
              >
                <div
                  v-for="(img, idx) in goodsImages"
                  :key="idx"
                  class="goods-img-wrapper"
                >
                  <el-image
                    :src="formatImageUrl(img)"
                    fit="fill"
                    :preview-src-list="goodsImages.map(p => formatImageUrl(p))"
                    :initial-index="idx"
                    class="evidence-img"
                  />
                </div>
              </div>
              <div class="evidence-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">货物</span>货物照
                <span v-if="goodsImages.length > 0" class="goods-count">({{ goodsImages.length }}张)</span>
              </div>
            </div>
          </div>
        </div>

      <!-- 第二区域：数据网格（3列） -->
      <div class="detail-section data-section">
        <div class="data-grid">
          <!-- 第一列：基础通行信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">入口站名称</span>
              <span class="data-value">{{ rows.passcodeEnStationId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口站名称</span>
              <span class="data-value">{{ rows.passcodeExStationId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口交易时间</span>
              <span class="data-value">{{ rows.passcodeExTime || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">总交易金额(元)</span>
              <span class="data-value">{{ rows.passcodeFee || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通行介质</span>
              <span class="data-value">{{ rows.passcodeMediaTypeText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通过省份个数</span>
              <span class="data-value">{{ rows.passcodeProvinceCount || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口交易编号</span>
              <span class="data-value mono">{{ rows.passcodeTransactionId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通行标识ID</span>
              <span class="data-value mono">{{ rows.passcodePassId || '-' }}</span>
            </div>
          </div>

          <!-- 第二列：车辆与货物信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">货车类型</span>
              <span class="data-value primary">{{ rows.vehicleTypeText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">货箱类型</span>
              <span class="data-value">{{ rows.vehicleContainerTypeText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">货物名称</span>
              <span class="data-value">{{ rows.goodsTypeName || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">入口重量(KG)</span>
              <span class="data-value">{{ rows.passcodeEnWeight || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口重量(KG)</span>
              <span class="data-value">{{ rows.passcodeExWeight || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">应收金额(元)</span>
              <span class="data-value">{{ rows.passcodePayFee || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">交易支付方式</span>
              <span class="data-value">{{ rows.passcodeTransPayTypeText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">车辆状态标识</span>
              <span class="data-value">{{ rows.passcodeVehicleSignText || '-' }}</span>
            </div>
          </div>

          <!-- 第三列：车牌与查验信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">车牌号码</span>
              <span class="data-value plate">{{ rows.plateNumber || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">货车长宽高(m)</span>
              <span class="data-value mono">{{ formatVehicleSize(rows.vehicleSize) }}</span>
            </div>
            <!-- 满载率：可编辑时显示输入框 -->
            <div class="data-row">
              <span class="data-label">满载率(%)</span>
            </div>
            <div class="data-row">
              <span class="data-label">满载率(%)</span>
              <span class="data-value">{{ rows.loadRate != null ? rows.loadRate + '%' : '-' }}</span>
            </div>
            <!-- 司机电话：可编辑时显示输入框 -->
            <div class="data-row">
              <span class="data-label">司机电话</span>
            </div>
            <div class="data-row">
              <span class="data-label">司机电话</span>
              <span class="data-value">{{ rows.driverPhone || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">验货员</span>
              <span class="data-value">{{ rows.inspectorPhone || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">复核员</span>
              <span class="data-value">{{ rows.reviewerPhone || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">班组</span>
            </div>
            <!-- 备注内容：可编辑时显示输入框 -->
            <div class="data-row">
              <span class="data-label">备注内容</span>
            </div>
            <div class="data-row">
              <span class="data-label">备注内容</span>
              <span class="data-value">{{ rows.historyRecord || '-' }}</span>
            </div>    

          </div>

        </div>
      </div>

      <!-- 底部结果与操作区（横向排列） -->
      <div class="bottom-result-section" :class="{ 'is-success': (rows.resultStatus) === 1, 'is-danger': (rows.resultStatus) === 2 }">
        <!-- 左侧：查验结果、不合格类型、复核结果 -->
        <div class="result-group">
          <!-- 查验结果 -->
          <div class="result-item">
            <span class="result-label">查验结果</span>
            <el-tag :type="getResultTagType(rows.resultStatus)" size="small" effect="dark">
              {{ rows.resultStatusText || '-' }}
            </el-tag>
          </div>

          <!-- 不合格类型：仅在查验结果为不合格时显示 -->
          <div class="result-item" v-if="rows.resultStatus === 2">
            <span class="result-label">不合格类型</span>
            <span class="result-value danger">{{ rows.nopassTypeText || rows.nopassType || '-' }}</span>
          </div>

          <!-- 复核结果 -->
          <div class="result-item">
            <span class="result-label">复核结果</span>
            <span class="result-value">{{ rows.manualReviewText || '未审核' }}</span>
          </div>
        </div>

        <!-- 右侧：操作按钮 -->
        <div class="result-actions">
          <el-button type="primary" @click="emit('close')">关闭</el-button>
        </div>
      </div>

    </div><!-- /detail-body -->

  </el-dialog>
</template>

<script setup>
/**
 * 绿通检测记录详情弹窗（大屏只读版本）
 *
 * 【数据来源】
 * 父组件（LatestPassRecords.vue）通过 rows 传入当前选中行的数据。
 * 只读显示，不支持编辑。
 *
 * 【v-model 说明】
 * visible 控制弹窗显示/隐藏，通过 emit('close') 通知父组件关闭。
 */

import { computed, ref } from 'vue'
import { Picture } from '@element-plus/icons-vue'

// ================================================================
// Props & Emits
// ================================================================

// 大屏绿通记录详情弹窗
const props = defineProps({
  visible: Boolean,
  rows: { type: Object, default: () => ({}) }
})

// 关闭弹窗
const emit = defineEmits(['close'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('close', val)
})

// ================================================================
// 样式辅助函数
// ================================================================

/**
 * getResultTagType：根据查验结果返回 el-tag 类型
 */
const getResultTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

// ================================================================
// 图片路径转换
// ================================================================

/**
 * formatImageUrl：将本地路径转换为后端图片接口 URL
 *
 * 【背景】
 * 数据库中图片路径以 D:\xxx\xxx.jpg 格式存储（Windows 本地路径）。
 * 浏览器无法直接通过 file:// 协议访问，需通过后端 /api/images 接口转发。
 *
 * 【处理逻辑】
 * 1. 空路径直接返回空字符串（el-image 收到空字符串不显示）
 * 2. 对路径进行 URL 编码后拼接到 /api/images?path=
 * 3. axios 请求会自动附上 JWT Token，接口鉴权通过
 *
 * @param {string|null|undefined} path 原始文件路径
 * @returns {string} 后端图片接口 URL
 *
 * @example
 * formatImageUrl('D:\\photos\\car.jpg')  → '/api/images?path=D%3A%5Cphotos%5Ccar.jpg'
 * formatImageUrl(null)                   → ''
 */
const formatImageUrl = (path) => {
  if (!path) return ''
  return `/api/images?path=${encodeURIComponent(path)}`
}

const rows = computed(() => props.rows)

/** 货物照数组：支持中文逗号、英文逗号分隔多张图片路径 */
const goodsImages = computed(() => {
  if (!rows.value.goodsImagePath) return []
  return rows.value.goodsImagePath
    .split(/[,，]/)
    .map(p => p.trim())
    .filter(p => p && (p.startsWith('/') || /^[A-Za-z]:/.test(p)))
})

/**
 * 根据货物图片数量计算每行显示的列数
 * 1-4张: 每行显示全部(1-4列)
 * 5张: 每行3列(3+2布局)
 * 6张: 每行3列(3+3布局)
 * 7张: 每行4列(4+3布局)
 * 8张: 每行4列(4+4布局)
 * 9张: 每行3列(3+3+3布局)
 * 10张及以上: 保持一行横向滚动
 */
const goodsPerRow = computed(() => {
  const count = goodsImages.value.length
  if (count <= 4) return count
  if (count === 5) return 3
  if (count === 6) return 3
  if (count === 7) return 4
  if (count === 8) return 4
  if (count === 9) return 3
  return count
})

/**
 * 计算列宽：10张及以上使用最小宽度保证横向滚动，否则使用等宽
 */
const goodsColWidth = computed(() => {
  return goodsImages.value.length >= 10 ? 'minmax(70px, 1fr)' : '1fr'
})

/**
 * 判断是否使用横向滚动模式（10张及以上）
 */
const useScrollMode = computed(() => goodsImages.value.length >= 10)

/**
 * formatVehicleSize：格式化货车长宽高
 * 数据库中格式为"长|宽|高"（单位mm），如 "8870|2260|3730"
 * 转换为 "长:8.87m × 宽:2.26m × 高:3.73m"
 */
const formatVehicleSize = (size) => {
  if (!size) return '-'
  const parts = size.split('|')
  if (parts.length === 3) {
    return `长${(parseFloat(parts[0]) / 1000).toFixed(2)}m|宽${(parseFloat(parts[1]) / 1000).toFixed(2)}m|高${(parseFloat(parts[2]) / 1000).toFixed(2)}m`
  }
  return size
}
</script>

<style>
.inspection-detail-dialog .el-dialog__header {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}
.inspection-detail-dialog .el-dialog__body {
  padding: 10px 20px 20px !important;
}

</style>

<style scoped>
/* ========== 弹窗样式 ========== */
.inspection-detail-dialog {
  max-height: 95vh;
}
.detail-title {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 18px; /* 这里改大一点，比如 18px */
  color: #121212;
  font-weight: 600;
  padding: 4px 8px;
  z-index: 1;
}

/* ========== 区域通用样式 ========== */
.detail-section {
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}



/* ========== 证据链照片区域（Grid网格布局） ========== */

.evidence-section {
  padding: 2px 2px;
}

/* 第一行：5列等宽网格 */
.evidence-grid-row-1 {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  width: 100%;
  margin-bottom: 8px;
}

/* 第二行：左侧50%（透视+车身2列），右侧50%（货物照） */
.evidence-grid-row-2 {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 8px;
  width: 100%;
}

/* 左侧50%容器：包含透视影像和车身照，2列等宽 */
.evidence-row-2-left {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 4px;
}

.evidence-col-right {
  display: flex;
  flex-direction: column;
  min-height: 160px;
  height: 160px;
  min-width: 0;
  /* 10张及以上时横向滚动 */
  overflow-x: auto;
  overflow-y: hidden;
}

/* 货物照容器：默认flex横向滚动，网格模式时使用grid */
.goods-img-container {
  display: flex;
  gap: 8px;
  width: 100%;
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  align-items: center;
}

/* 网格模式（10张以下）：使用grid布局 */
.goods-img-container:not(.scroll-mode) {
  display: grid;
  overflow-x: hidden;
  align-items: start;
}

.goods-img-container .evidence-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 货物照单个图片包装器 */
.goods-img-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
}

/* 滚动模式下使用固定宽度 */
.goods-img-container.scroll-mode .goods-img-wrapper {
  width: 120px;
  flex-shrink: 0;
}

/* 照片卡片通用样式 */
.evidence-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  position: relative;
}
.evidence-grid-row-1 .evidence-item .evidence-img-box {
  width:100%;
  height: 160px;
}

.evidence-grid-row-2 .evidence-item .evidence-img-box {
  width: 100%;
  height: 150px;
}

.evidence-item .evidence-img {
  width: 100%;
  height: 100%;
}

.evidence-item .evidence-placeholder {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #dcdfe6;
  border-radius: 4px;
}

.evidence-item .evidence-label {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(64, 158, 255, 0.9);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 0 6px 0 4px;
  font-weight: 500;
  text-align: right;
  z-index: 1;
  white-space: nowrap;
}

.evidence-item .type-tag {
  display: inline-block;
  padding: 1px 4px;
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
  border-radius: 3px;
  font-size: 9px;
  margin-right: 2px;
}

/* ========== 数据网格区域（3列） ========== */
.data-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

/* 每列：右边框分隔，内容从上到下排列 */
.data-col {
  border-right: 1px solid #ebeef5;
  padding: 0;
}

.data-col:last-child {
  border-right: none;
}

/* 每行：label 靠左，value 靠右，中间有间距 */
.data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 14px;
  border-bottom: 1px solid #f5f5f5;
  min-height: 36px;
}

.data-row:last-child {
  border-bottom: none;
}

/* 标签：深色加粗 */
.data-label {
  font-size: 14px;
  color: #505050;
  flex-shrink: 0;
  font-weight: 600;
}

/* 数值：深色粗体 */
.data-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
  text-align: right;
  word-break: break-all;
  margin-left: 8px;
  flex: 1;
}

/* 单位后缀：更小的灰色字 */
.data-value .unit {
  font-size: 13px;
  color: #909399;
}

/* 车种名称：蓝色高亮 */
.data-value.primary {
  color: #409eff;
  font-weight: 700;
}

/* 编号/ID：等宽字体 */
.data-value.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #606266;
}

/* 车牌号：等宽粗体突出 */
.data-value.plate {
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', monospace;
  letter-spacing: 1px;
  color: #303133;
}

/* ========== 响应式：小屏适配 ========== */
@media (max-width: 900px) {
  .data-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ========== 货物类型字段 ========== */
.goods-type-field {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}

/* ========== 货物类型弹窗 ========== */
.goods-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 8px;
}

.goods-dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.temp-selected {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 4px;
  margin-bottom: 12px;
  max-height: 80px;
  overflow-y: auto;
}

.temp-label {
  font-size: 12px;
  color: #67c23a;
  font-weight: 600;
  margin-right: 4px;
  flex-shrink: 0;
}

.variety-card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  max-height: 480px;
  overflow-y: auto;
  padding: 4px;
}

.variety-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  display: flex;
  align-items: center;
}

.variety-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.variety-card.selected {
  border-color: #67c23a;
  background: #f0f9eb;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.card-type {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.card-aliases {
  font-size: 11px;
  color: #a0a8b6;
  line-height: 1.4;
}

.card-left {
  flex: 1;
  min-width: 0;
}

.card-icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #f5f7fa;
  margin-left: 8px;
  overflow: hidden;
}

.variety-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.no-data {
  grid-column: 1 / -1;
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
}

.dialog-footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.selected-count {
  font-size: 13px;
  color: #909399;
}

/* ========== 货车长宽高弹框样式 ========== */
.vehicle-size-input {
  cursor: pointer;
}

.vehicle-size-input:hover {
  border-color: #409eff;
}

/* ========== 底部结果与操作区 ========== */
.bottom-result-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3e1 100%);
  border-top: 3px solid #67c23a;
  flex-wrap: wrap;
  transition: all 0.3s ease;
}

.bottom-result-section.is-success {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3e1 100%);
  border-top-color: #67c23a;
}

.bottom-result-section.is-danger {
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
  border-top-color: #f56c6c;
}

.bottom-result-section .result-group {
  display: flex;
  align-items: center;
  gap: 24px;
}

.bottom-result-section .result-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.bottom-result-section .result-label {
  font-size: 18px;
  color: #606266;
  font-weight: 600;
  white-space: nowrap;
}

.bottom-result-section .result-value {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.bottom-result-section .result-value.danger {
  color: #f56c6c;
}

.result-actions {
  display: flex;
  gap: 10px;
  margin-left: 20px;
}

/* ========== 货车长宽高弹窗样式 ========== */
.vehicle-size-dialog-content {
  padding: 10px 0;
}

.vehicle-size-input-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vehicle-size-input-group .input-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vehicle-size-input-group .input-label {
  width: 50px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
</style>
