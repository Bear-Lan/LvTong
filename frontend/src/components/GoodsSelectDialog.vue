<template>
  <!--
    货物选择弹窗组件（可复用）
    用于 HistoricalRecords 搜索区和 InspectionDetail 详情页

    【使用方式】
    <GoodsSelectDialog v-model="dialogVisible" v-model:selected="selectedProducts" @confirm="onConfirm" />
  -->
  <el-dialog
    v-model="visible"
    title="选择货物类型"
    width="1000px"
    destroy-on-close
    class="goods-select-dialog"
    append-to-body
  >
    <!-- 筛选工具栏 -->
    <div class="goods-dialog-toolbar">
      <el-select
        v-model="filterProductType"
        placeholder="按产品大类筛选"
        clearable
        size="default"
        style="width: 180px;"
      >
        <el-option
          v-for="pt in productTypeOptions"
          :key="pt"
          :label="pt"
          :value="pt"
        />
      </el-select>
      <el-select
        v-model="filterCategory"
        placeholder="按类别筛选"
        clearable
        size="default"
        style="width: 180px;"
        :disabled="!filterProductType"
      >
        <el-option
          v-for="cat in categoryOptions"
          :key="cat"
          :label="cat"
          :value="cat"
        />
      </el-select>
      <el-input
        v-model="filterVarietyName"
        placeholder="搜索品种名称"
        clearable
        size="default"
        style="flex: 1; min-width: 160px;"
      />
    </div>

    <!-- 已选品种标签 -->
    <div v-if="tempSelected.length > 0" class="temp-selected">
      <span class="temp-label">已选：</span>
      <el-tag
        v-for="code in tempSelected"
        :key="code"
        closable
        type="success"
        size="small"
        @close="removeTemp(code)"
      >{{ getVarietyName(code) }}</el-tag>
    </div>

    <!-- 品种卡片网格 -->
    <div class="variety-card-grid">
      <div
        v-for="v in displayedVarieties"
        :key="v.productCode"
        class="variety-card"
        :class="{ selected: tempSelected.includes(v.productCode) }"
        @click="toggleTemp(v.productCode)"
      >
        <div class="card-left">
          <div class="card-type">{{ v.productType }}</div>
          <div class="card-name">{{ v.varietyName }}</div>
          <div v-if="getAliasesText(v.aliases)" class="card-aliases">{{ getAliasesText(v.aliases) }}</div>
        </div>
        <div class="card-icon-wrapper">
          <img
            v-if="getVarietyImage(v.varietyName)"
            :src="getVarietyImage(v.varietyName)"
            :alt="v.varietyName"
            class="variety-img"
          />
          <el-icon v-else size="20" color="#c0c4cc"><Picture /></el-icon>
        </div>
      </div>
      <div v-if="displayedVarieties.length === 0" class="no-data">
        暂无匹配的品种
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-inner">
        <span class="selected-count">已选 {{ tempSelected.length }} 个品种</span>
        <div style="display: flex; gap: 10px;">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection">确定</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * GoodsSelectDialog 货物选择弹窗组件
 *
 * 【Props】
 * modelValue (v-model): 控制弹窗显示/隐藏
 * selected: 当前已选中的品种编码数组（用于回显）
 *
 * 【Emits】
 * update:modelValue: 关闭时通知父组件更新 v-model
 * confirm: 用户点击确定时返回选中的品种编码数组
 */

import { ref, computed, watch, onMounted } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { getProductList } from '@/api/vehicleInspection'
import { ElMessage } from 'element-plus'

// ================================================================
// Props & Emits
// ================================================================

const props = defineProps({
  modelValue: Boolean,
  selected: { type: Array, default: () => [] },
  singleSelect: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

// ================================================================
// 弹窗显示状态
// ================================================================

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// ================================================================
// 筛选状态
// ================================================================

const tempSelected = ref([])
const filterProductType = ref('')
const filterCategory = ref('')
const filterVarietyName = ref('')

// ================================================================
// 品种数据
// ================================================================

let allVarieties = []
const productTypeOptions = ref([])
const allCategories = ref([])

// 品种图片（从 variety_img 文件夹加载）
const varietyImages = import.meta.glob('@/assets/variety_img/*.png', { eager: true })

const getVarietyImage = (varietyName) => {
  if (!varietyName) return null
  let imagePath = varietyImages[`/src/assets/variety_img/${varietyName}.png`]?.default
  if (imagePath) return imagePath
  for (const path in varietyImages) {
    const fileName = path.split('/').pop().replace('.png', '')
    if (fileName.includes(varietyName) || varietyName.includes(fileName)) {
      return varietyImages[path].default
    }
  }
  return null
}

const categoryOptions = computed(() => {
  if (!filterProductType.value) return allCategories.value
  return [...new Set(
    allVarieties
      .filter(v => v.productType === filterProductType.value)
      .map(v => v.category)
      .filter(c => c)
  )].sort()
})

const displayedVarieties = computed(() => {
  let list = allVarieties
  if (filterProductType.value) {
    list = list.filter(v => v.productType === filterProductType.value)
  }
  if (filterCategory.value) {
    list = list.filter(v => v.category === filterCategory.value)
  }
  if (filterVarietyName.value.trim()) {
    const kw = filterVarietyName.value.trim().toLowerCase()
    list = list.filter(v => {
      if (v.varietyName.toLowerCase().includes(kw)) return true
      if (v.varietyNamePinyin && v.varietyNamePinyin.toLowerCase().includes(kw)) return true
      if (v.aliasesPinyin) {
        try {
          const aliasesPinyinArr = JSON.parse(v.aliasesPinyin)
          if (Array.isArray(aliasesPinyinArr) && aliasesPinyinArr.some(a => a.toLowerCase().includes(kw))) {
            return true
          }
        } catch {}
      }
      return false
    })
  }
  return list
})

const getVarietyName = (code) => {
  const v = allVarieties.find(v => v.productCode === code)
  return v ? v.varietyName : code
}

const getAliasesText = (aliasesJson) => {
  if (!aliasesJson) return ''
  try {
    const arr = JSON.parse(aliasesJson)
    return Array.isArray(arr) ? arr.join('、') : ''
  } catch {
    return ''
  }
}

// ================================================================
// 交互操作
// ================================================================

const toggleTemp = (code) => {
  if (props.singleSelect) {
    // 单选模式：直接设置选中的品种
    tempSelected.value = [code]
  } else {
    const idx = tempSelected.value.indexOf(code)
    if (idx >= 0) {
      tempSelected.value.splice(idx, 1)
    } else {
      tempSelected.value.push(code)
    }
  }
}

const removeTemp = (code) => {
  tempSelected.value = tempSelected.value.filter(c => c !== code)
}

const confirmSelection = () => {
  if (props.singleSelect) {
    emit('confirm', tempSelected.value[0] || null)
  } else {
    emit('confirm', [...tempSelected.value])
  }
  visible.value = false
}

const loadProducts = async () => {
  try {
    const res = await getProductList()
    if (res.code === 200) {
      allVarieties = res.data.varieties || []
      productTypeOptions.value = res.data.productTypes || []
      allCategories.value = res.data.categories || []
    }
  } catch {
    ElMessage.error('货物类型加载失败')
  }
}

// 监听 visible 变为 true 时，同步已选中数据到临时选中
watch(visible, (val) => {
  if (val) {
    // 单选模式：selected 可能是字符串，转为数组；多选模式直接展开
    if (props.singleSelect && props.selected) {
      tempSelected.value = [props.selected]
    } else if (props.singleSelect) {
      tempSelected.value = []
    } else {
      tempSelected.value = Array.isArray(props.selected) ? [...props.selected] : []
    }
    filterProductType.value = ''
    filterCategory.value = ''
    filterVarietyName.value = ''
  }
})

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.goods-select-dialog :deep(.el-dialog__body) {
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
</style>