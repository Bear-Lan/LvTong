<template>
  <el-dialog
    v-model="visible"
    title="上报前图片管理"
    width="95vw"
    top="0vh"
    destroy-on-close
    class="inspection-detail-dialog"
  >
    <!-- 提示 -->
    <div class="dialog-tip">
      <el-icon color="#e6a23c"><WarningFilled /></el-icon>
      <span>请确认上报图片，可排除（删除）不需要的图片后上报交通部</span>
    </div>

    <!-- 左右两栏整体布局 -->
    <div class="detail-body">

      <!-- 左侧栏：证据链照片 + 查验数据 -->
      <div class="detail-left">

        <!-- 证据链照片区域 -->
        <div class="detail-section evidence-section">
          <div class="section-header">
            <span class="section-title">证据链照片</span>
            <span class="section-sub">点击图片可放大查看，点击×排除该图片</span>
          </div>
          <!-- 第一行：5列等宽网格 -->
          <div class="evidence-grid">
            <!-- 车头照 -->
            <div class="evidence-item" :class="{ 'no-image': !row.headImagePath, 'excluded': excludedTypes.includes('11') }">
              <div class="evidence-img-box" v-if="row.headImagePath">
                <el-image
                  :src="formatImageUrl(row.headImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.headImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('11')">
                  <el-icon v-if="excludedTypes.includes('11')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">11</span>车头照
                <span v-if="excludedTypes.includes('11')" class="excluded-tag">已排除</span>
              </div>
            </div>

            <!-- 车尾照 -->
            <div class="evidence-item" :class="{ 'no-image': !row.tailImagePath, 'excluded': excludedTypes.includes('12') }">
              <div class="evidence-img-box" v-if="row.tailImagePath">
                <el-image
                  :src="formatImageUrl(row.tailImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.tailImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('12')">
                  <el-icon v-if="excludedTypes.includes('12')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">12</span>车尾照
                <span v-if="excludedTypes.includes('12')" class="excluded-tag">已排除</span>
              </div>
            </div>

            <!-- 行驶证 -->
            <div class="evidence-item" :class="{ 'no-image': !row.licenseImagePath, 'excluded': excludedTypes.includes('13') }">
              <div class="evidence-img-box" v-if="row.licenseImagePath">
                <el-image
                  :src="formatImageUrl(row.licenseImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.licenseImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('13')">
                  <el-icon v-if="excludedTypes.includes('13')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">13</span>行驶证
                <span v-if="excludedTypes.includes('13')" class="excluded-tag">已排除</span>
              </div>
            </div>

            <!-- 顶部照 -->
            <div class="evidence-item" :class="{ 'no-image': !row.topImagePath, 'excluded': excludedTypes.includes('99') }">
              <div class="evidence-img-box" v-if="row.topImagePath">
                <el-image
                  :src="formatImageUrl(row.topImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.topImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('99')">
                  <el-icon v-if="excludedTypes.includes('99')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">26</span>顶部照
                <span v-if="excludedTypes.includes('99')" class="excluded-tag">已排除</span>
              </div>
            </div>

            <!-- 通行凭证 -->
            <div class="evidence-item" :class="{ 'no-image': !row.passcodeImagePath, 'excluded': excludedTypes.includes('passcodeImagePath') }">
              <div class="evidence-img-box" v-if="row.passcodeImagePath">
                <el-image
                  :src="formatImageUrl(row.passcodeImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.passcodeImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('passcodeImagePath')">
                  <el-icon v-if="excludedTypes.includes('passcodeImagePath')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">凭证</span>通行凭证
                <span v-if="excludedTypes.includes('passcodeImagePath')" class="excluded-tag">已排除</span>
              </div>
            </div>
          </div>

          <!-- 第二行：透视影像 + 车身照 -->
          <div class="evidence-grid evidence-grid-2col">
            <!-- 透视影像 -->
            <div class="evidence-item evidence-item-horizontal" :class="{ 'no-image': !row.transparentImagePath, 'excluded': excludedTypes.includes('transparentImagePath') }">
              <div class="evidence-label-left">
                <span class="type-tag">X光</span>透视影像
                <span v-if="excludedTypes.includes('25')" class="excluded-tag">已排除</span>
              </div>
              <div class="evidence-img-box" v-if="row.transparentImagePath">
                <el-image
                  :src="formatImageUrl(row.transparentImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.transparentImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('transparentImagePath')">
                  <el-icon v-if="excludedTypes.includes('25')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
            </div>

            <!-- 车身照 -->
            <div class="evidence-item evidence-item-horizontal" :class="{ 'no-image': !row.bodyImagePath, 'excluded': excludedTypes.includes('bodyImagePath') }">
              <div class="evidence-label-left">
                <span class="type-tag">25</span>车身照
                <span v-if="excludedTypes.includes('25_body')" class="excluded-tag">已排除</span>
              </div>
              <div class="evidence-img-box" v-if="row.bodyImagePath">
                <el-image
                  :src="formatImageUrl(row.bodyImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.bodyImagePath)]"
                  class="evidence-img"
                />
                <div class="delete-btn" @click.stop="toggleImage('bodyImagePath')">
                  <el-icon v-if="excludedTypes.includes('25_body')"><Plus /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 查验数据区域 -->
        <div class="detail-section data-section">
          <div class="section-header">
            <span class="section-title">查验数据</span>
          </div>
          <div class="data-grid">
            <!-- 第一列：基础通行信息 -->
            <div class="data-col">
              <div class="data-row">
                <span class="data-label">入口站名称</span>
                <span class="data-value">{{ row.passcodeEnStationId || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">出口站名称</span>
                <span class="data-value">{{ row.passcodeExStationId || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">出口交易时间</span>
                <span class="data-value">{{ row.passcodeExTime || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">总交易金额(元)</span>
                <span class="data-value">{{ row.passcodeFee || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">出口交易编号</span>
                <span class="data-value mono">{{ row.passcodeTransactionId || '-' }}</span>
              </div>
            </div>

            <!-- 第二列：车辆与货物信息 -->
            <div class="data-col">
              <div class="data-row">
                <span class="data-label">货车类型</span>
                <span class="data-value primary">{{ row.vehicleTypeText || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">货箱类型</span>
                <span class="data-value">{{ row.vehicleContainerTypeText || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">满载率(%)</span>
                <span class="data-value">{{ row.loadRate != null ? row.loadRate + '%' : '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">货物名称</span>
                <span class="data-value">{{ row.goodsTypeName || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">入口重量(KG)</span>
                <span class="data-value">{{ row.passcodeEnWeight || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">出口重量(KG)</span>
                <span class="data-value">{{ row.passcodeExWeight || '-' }}</span>
              </div>
            </div>

            <!-- 第三列：载重与费用信息 -->
            <div class="data-col">
              <div class="data-row">
                <span class="data-label">应收金额(元)</span>
                <span class="data-value">{{ row.passcodePayFee || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">货车长宽高</span>
                <span class="data-value mono">{{ formatVehicleSize(row.vehicleSize) }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">通过省份个数</span>
                <span class="data-value">{{ row.passcodeProvinceCount || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">司机电话</span>
                <span class="data-value">{{ row.driverPhone || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">通行标识ID</span>
                <span class="data-value mono">{{ row.passcodePassId || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">通行介质</span>
                <span class="data-value">{{ row.passcodeMediaTypeText || '-' }}</span>
              </div>
            </div>

            <!-- 第四列：车牌与状态信息 -->
            <div class="data-col">
              <div class="data-row">
                <span class="data-label">车牌号码</span>
                <span class="data-value plate">{{ row.plateNumber || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">挂车号码</span>
                <span class="data-value">{{ row.plateNumberGc || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">交易支付方式</span>
                <span class="data-value">{{ row.passcodeTransPayTypeText || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">车辆状态标识</span>
                <span class="data-value">{{ row.passcodeVehicleSignText || '-' }}</span>
              </div>
              <div class="data-row">
                <span class="data-label">查验依据</span>
                <span class="data-value">{{ row.inspectionBasis || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部结论栏 -->
        <div class="result-bar">
          <div class="result-bar-main">
            <el-tag :type="getResultTagType(row.resultStatus)" size="large" effect="dark" class="result-badge">
              {{ row.resultStatusText || '-' }}
            </el-tag>
          </div>
          <div class="result-bar-items">
            <div class="result-item">
              <span class="result-label">查验员</span>
              <span class="result-value">{{ row.operatorName || '-' }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">复核员</span>
              <span class="result-value">{{ row.reviewerPhone || '-' }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">班组</span>
              <span class="result-value">{{ row.groupId || '-' }}</span>
            </div>
            <div class="result-item" v-if="row.resultStatus === 2">
              <span class="result-label">不合格类型</span>
              <span class="result-value danger">{{ row.nopassTypeText || row.nopassType || '-' }}</span>
            </div>
          </div>
        </div>

      </div><!-- /detail-left -->

      <!-- 右侧栏：货物照片 -->
      <div class="detail-right">
        <div class="detail-section goods-section">
          <div class="section-header">
            <span class="section-title">货物照</span>
            <span class="section-sub" v-if="goodsImages.length > 0">共 {{ goodsImages.length }} 张</span>
          </div>
          <div class="goods-photo-area">
            <div v-if="goodsImages.length > 0" class="goods-photo-list">
              <div
                v-for="(img, idx) in goodsImages"
                :key="idx"
                class="goods-photo-item"
                :class="{ 'excluded': excludedGoods.includes(idx) }"
              >
                <div class="goods-img-wrapper">
                  <el-image
                    :src="formatImageUrl(img)"
                    fit="contain"
                    class="goods-photo-img"
                    :preview-src-list="goodsImages.map(p => formatImageUrl(p))"
                    :initial-index="idx"
                  />
                  <div class="delete-btn" @click.stop="toggleGoodsImage(idx)">
                    <el-icon v-if="excludedGoods.includes(idx)"><Plus /></el-icon>
                    <el-icon v-else><Close /></el-icon>
                  </div>
                </div>
                <div class="goods-photo-label">
                  货物照 {{ idx + 1 }}
                  <span v-if="excludedGoods.includes(idx)" class="excluded-tag">已排除</span>
                </div>
              </div>
            </div>
            <div class="goods-photo-placeholder" v-else>
              <el-icon size="40" color="#dcdfe6"><Picture /></el-icon>
              <span>暂无货物照</span>
            </div>
          </div>
        </div>
      </div><!-- /detail-right -->

    </div><!-- /detail-body -->

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="uploading">
          确认上报（已排除 {{ excludedCount }} 张图片）
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, Picture, Close, Plus } from '@element-plus/icons-vue'
import { uploadSingleWithExclude } from '@/api/transportDept'

const props = defineProps({
  modelValue: Boolean,
  row: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const uploading = ref(false)

// 排除的图片类型
const excludedTypes = ref([])

// 排除的货物照索引
const excludedGoods = ref([])

// 货物照片数组
const goodsImages = computed(() => {
  if (!props.row.goodsImagePath) return []
  return props.row.goodsImagePath
    .split(/[,，]/)
    .map(p => p.trim())
    .filter(p => p && (p.startsWith('/') || /^[A-Za-z]:/.test(p)))
})

// 已排除的图片数量
const excludedCount = computed(() => {
  return excludedTypes.value.length + excludedGoods.value.length
})

// 监听 row 变化，初始化数据
watch(() => props.row, () => {
  excludedTypes.value = []
  excludedGoods.value = []
}, { immediate: true })

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  return `/api/images?path=${encodeURIComponent(path)}`
}

// 格式化货车长宽高
const formatVehicleSize = (size) => {
  if (!size) return '-'
  const parts = size.split('|')
  if (parts.length === 3) {
    return `长:${(parseFloat(parts[0]) / 1000).toFixed(2)}m × 宽:${(parseFloat(parts[1]) / 1000).toFixed(2)}m × 高:${(parseFloat(parts[2]) / 1000).toFixed(2)}m`
  }
  return size
}

// 查验结果标签类型
const getResultTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

// 切换证据链图片的排除状态
const toggleImage = (typeId) => {
  const idx = excludedTypes.value.indexOf(typeId)
  if (idx >= 0) {
    excludedTypes.value.splice(idx, 1)
  } else {
    excludedTypes.value.push(typeId)
  }
}

// 切换货物照片的排除状态
const toggleGoodsImage = (idx) => {
  const goodsIdx = excludedGoods.value.indexOf(idx)
  if (goodsIdx >= 0) {
    excludedGoods.value.splice(goodsIdx, 1)
  } else {
    excludedGoods.value.push(idx)
  }
}

// 构建最终的排除列表
const buildExcludeList = () => {
  const excludeList = [...excludedTypes.value]
  // 货物照使用下标格式 goods_0, goods_1, ...
  for (const idx of excludedGoods.value) {
    excludeList.push('goods_' + idx)
  }
  return excludeList
}

// 确认上报
const handleConfirm = async () => {
  uploading.value = true
  try {
    const excludeList = buildExcludeList()
    const res = await uploadSingleWithExclude(props.row.id, excludeList)

    if (res.code === 200) {
      ElMessage.success('上报成功')
      visible.value = false
      emit('success')
    } else {
      ElMessage.error(res.message || '上报失败')
    }
  } catch {
    ElMessage.error('上报失败，请重试')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.inspection-detail-dialog :deep(.el-dialog) {
  max-height: 90vh;
}

.inspection-detail-dialog :deep(.el-dialog__body) {
  padding: 0 24px 20px;
}

.dialog-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #e6a23c;
}

/* ========== 左右两栏布局 ========== */
.detail-body {
  display: flex;
  align-items: stretch;
  gap: 0;
}

.detail-left {
  flex: 1;
  min-width: 0;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.detail-right {
  flex: 0 0 18%;
  min-width: 0;
  max-width: 18%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* ========== 区域通用样式 ========== */
.detail-section {
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}

.section-sub {
  font-size: 12px;
  color: #909399;
}

/* ========== 证据链照片 ========== */
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0;
}

.evidence-grid-2col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-top: 1px solid #ebeef5;
}

.evidence-item {
  border-right: 1px solid #f0f0f0;
  text-align: center;
  position: relative;
}

.evidence-item:last-child {
  border-right: none;
}

.evidence-item.excluded {
  opacity: 0.6;
}

.evidence-img-box {
  height: 80px;
  overflow: hidden;
  background: #f5f7fa;
  cursor: zoom-in;
  position: relative;
}

.evidence-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.evidence-placeholder {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #dcdfe6;
}

.evidence-label {
  padding: 10px;
  font-size: 13px;
  color: #606266;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  font-weight: 500;
}

.type-tag {
  display: inline-block;
  background: #409eff;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  margin-right: 4px;
  font-weight: 700;
}

.excluded-tag {
  display: inline-block;
  font-size: 11px;
  color: #f56c6c;
  margin-left: 4px;
}

.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.evidence-img-box:hover .delete-btn,
.goods-img-wrapper:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #f56c6c;
}

/* 第二行横向布局 */
.evidence-item-horizontal {
  display: flex;
  align-items: center;
  border-right: 1px solid #f0f0f0;
  padding: 10px 12px;
  gap: 12px;
}

.evidence-item-horizontal:last-child {
  border-right: none;
}

.evidence-label-left {
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.evidence-item-horizontal .evidence-img-box {
  flex: 1;
  height: 80px;
  min-width: 0;
}

.evidence-item-horizontal .evidence-placeholder {
  flex: 1;
  height: 80px;
}

/* ========== 数据网格 ========== */
.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.data-col {
  border-right: 1px solid #ebeef5;
  padding: 0;
}

.data-col:last-child {
  border-right: none;
}

.data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  border-bottom: 1px solid #f5f5f5;
  min-height: 44px;
}

.data-row:last-child {
  border-bottom: none;
}

.data-label {
  font-size: 14px;
  color: #505050;
  flex-shrink: 0;
  font-weight: 600;
}

.data-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
  text-align: right;
  word-break: break-all;
  margin-left: 8px;
}

.data-value.primary {
  color: #409eff;
  font-weight: 700;
}

.data-value.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #606266;
}

.data-value.plate {
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', monospace;
  letter-spacing: 1px;
}

/* ========== 底部结论栏 ========== */
.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3e1 100%);
  border-top: 3px solid #67c23a;
  margin-top: 12px;
}

.result-bar-main {
  flex-shrink: 0;
}

.result-badge {
  font-size: 20px;
  font-weight: 800;
  padding: 10px 28px;
  letter-spacing: 3px;
}

.result-bar-items {
  display: flex;
  align-items: center;
  gap: 40px;
  flex: 1;
  justify-content: flex-end;
}

.result-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.result-label {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.result-value {
  font-size: 16px;
  color: #303133;
  font-weight: 700;
}

.result-value.danger {
  color: #f56c6c;
}

/* ========== 货物照片区域 ========== */
.goods-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-photo-area {
  flex: 1;
  max-height: 690px;
  padding: 16px;
  overflow-y: auto;
}

.goods-photo-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.goods-photo-item {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.goods-photo-item.excluded {
  opacity: 0.6;
}

.goods-img-wrapper {
  position: relative;
}

.goods-photo-img {
  width: 100%;
  height: 150px;
  display: block;
  cursor: zoom-in;
  background: #f5f7fa;
}

.goods-photo-label {
  text-align: center;
  font-size: 13px;
  color: #909399;
  padding: 8px 0;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
  font-weight: 500;
}

.goods-photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 200px;
  color: #d0d0d0;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .detail-body {
    flex-direction: column;
  }

  .detail-right {
    max-width: 100%;
    width: 100%;
    border-top: 1px solid #e4e7ed;
    border-right: none;
  }

  .evidence-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .data-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
