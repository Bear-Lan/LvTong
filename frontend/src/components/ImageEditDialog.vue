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

    <!-- ================== 整体布局 ================== -->
    <div class="detail-body">
        <!-- 证据链照片
             第一行：车头照、车尾照、行驶证、顶部照、通行凭证（5列）
             第二行：透视影像+车身照（占50%）、货物照（占50%） -->
        <div class="detail-section evidence-section">
          <!-- 第一行：5列等宽网格 -->
          <div class="evidence-grid-row-1">
            <!-- 车头照 -->
            <div class="evidence-item" :class="{ 'no-image': !row.headImagePath, 'excluded': excludedTypes.includes('11') }">
              <div class="evidence-img-box" v-if="row.headImagePath">
                <el-image
                  :src="formatImageUrl(row.headImagePath)"
                  fit="fill"
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
                  fit="fill"
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
                  fit="fill"
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
                  fit="fill"
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
                  fit="fill"
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

          <!-- 第二行：左侧50%（透视+车身2列），右侧50%（货物照） -->
          <div class="evidence-grid-row-2">
            <!-- 左侧50%：透视影像 + 车身照（2列，等宽） -->
            <div class="evidence-row-2-left">
              <!-- 透视影像 -->
              <div class="evidence-item" :class="{ 'no-image': !row.transparentImagePath, 'excluded': excludedTypes.includes('transparentImagePath') }">
                <div class="evidence-img-box" v-if="row.transparentImagePath">
                  <el-image
                    :src="formatImageUrl(row.transparentImagePath)"
                    fit="fill"
                    :preview-src-list="[formatImageUrl(row.transparentImagePath)]"
                    class="evidence-img"
                  />
                  <div class="delete-btn" @click.stop="toggleImage('transparentImagePath')">
                    <el-icon v-if="excludedTypes.includes('transparentImagePath')"><Plus /></el-icon>
                    <el-icon v-else><Close /></el-icon>
                  </div>
                </div>
                <div class="evidence-placeholder" v-else>
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="evidence-label">
                  <span class="type-tag">X光</span>透视影像
                  <span v-if="excludedTypes.includes('transparentImagePath')" class="excluded-tag">已排除</span>
                </div>
              </div>

              <!-- 车身照 -->
              <div class="evidence-item" :class="{ 'no-image': !row.bodyImagePath, 'excluded': excludedTypes.includes('bodyImagePath') }">
                <div class="evidence-img-box" v-if="row.bodyImagePath">
                  <el-image
                    :src="formatImageUrl(row.bodyImagePath)"
                    fit="fill"
                    :preview-src-list="[formatImageUrl(row.bodyImagePath)]"
                    class="evidence-img"
                  />
                  <div class="delete-btn" @click.stop="toggleImage('bodyImagePath')">
                    <el-icon v-if="excludedTypes.includes('bodyImagePath')"><Plus /></el-icon>
                    <el-icon v-else><Close /></el-icon>
                  </div>
                </div>
                <div class="evidence-placeholder" v-else>
                  <el-icon><Picture /></el-icon>
                </div>
                <div class="evidence-label">
                  <span class="type-tag">25</span>车身照
                  <span v-if="excludedTypes.includes('bodyImagePath')" class="excluded-tag">已排除</span>
                </div>
              </div>
            </div>

            <!-- 右侧50%：货物照 -->
            <div class="evidence-item evidence-col-right" :class="{ 'no-image': goodsImages.length === 0 }">
              <div class="goods-img-container" v-if="goodsImages.length > 0">
                <div
                  v-for="(img, idx) in goodsImages"
                  :key="idx"
                  class="goods-img-wrapper"
                  :class="{ 'excluded': excludedGoods.includes(idx) }"
                >
                  <el-image
                    :src="formatImageUrl(img)"
                    fit="fill"
                    :preview-src-list="goodsImages.map(p => formatImageUrl(p))"
                    :initial-index="idx"
                    class="evidence-img"
                  />
                  <div class="delete-btn" @click.stop="toggleGoodsImage(idx)">
                    <el-icon v-if="excludedGoods.includes(idx)"><Plus /></el-icon>
                    <el-icon v-else><Close /></el-icon>
                  </div>
                </div>
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">货物</span>货物照
                <span v-if="goodsImages.length > 0" class="goods-count">({{ goodsImages.length }}张)</span>
              </div>
            </div>
          </div>
        </div>

      <!-- 查验数据区域 -->
      <div class="detail-section data-section">
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
            <span class="result-label">验货员</span>
            <span class="result-value">{{ row.inspectorPhone || '-' }}</span>
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

/* ========== 区域通用样式 ========== */
.detail-section {
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

/* ========== 证据链照片区域（Grid网格布局） ========== */

.evidence-section {
  padding: 12px 16px;
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
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
  max-height: 100px;
  min-height: 100px;
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
}

/* 货物照容器：水平平铺所有图片 */
.goods-img-container {
  display: flex;
  flex-direction: row;
  flex: 1;
  width: 100%;
  gap: 4px;
  max-height: 100px;
  overflow: hidden;
}

.goods-img-container .evidence-img {
  flex: 1;
  object-fit: cover;
  width: 100%;
  height: 100%;
}

/* 照片卡片通用样式 */
.evidence-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  position: relative;
}

.evidence-item.excluded {
  opacity: 0.6;
}

.evidence-item .evidence-img-box {
  width: 100%;
  height: 100px;
  position: relative;
}

.evidence-item .evidence-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.evidence-item .evidence-placeholder {
  width: 100%;
  height: 60px;
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

.excluded-tag {
  display: inline-block;
  font-size: 10px;
  color: #fef0f0;
  margin-left: 2px;
}

/* 删除按钮 */
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
  z-index: 2;
}

.evidence-img-box:hover .delete-btn,
.goods-img-wrapper:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #f56c6c;
}

/* 货物照单个图片包装器 */
.goods-img-wrapper {
  position: relative;
  flex: 1;
  min-width: 0;
}

/* 无图片时的占位图标 */
.evidence-placeholder {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #dcdfe6;
  border-radius: 4px;
  width: 100%;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .data-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>