<template>
  <!--
    车辆查验记录页面（历史记录列表）
    本页面是查验模块的主入口，集成了搜索、新增、列表、分页、详情、编辑、删除功能。
  -->
  <div class="history-page">


    <!-- ========== 搜索卡片 ========== -->
    <!-- el-card 提供阴影和圆角效果，shadow="hover" 悬停时显示阴影 -->
    <el-card class="search-card" shadow="hover">

      <!-- ========== 页面标题 + 新增按钮 ========== -->
      <div class="page-header">
        <h2 class="page-title">
          <el-icon class="title-icon"><Van /></el-icon>
          车辆查验记录
        </h2>
      </div>
      <el-form :model="searchForm" label-position="inline" class="search-form">
        <el-row :gutter="20">

          <!-- 车牌号 -->
          <el-col :span="6">
            <el-form-item label="车牌">
              <el-input
                v-model="searchForm.plateNumber"
                placeholder="车牌号"
                clearable
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>

          <!-- 司机电话 -->
          <el-col :span="6">
            <el-form-item label="电话">
              <el-input
                v-model="searchForm.driverPhone"
                placeholder="电话"
                clearable
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>

          <!-- 复核员 -->
          <el-col :span="6">
            <el-form-item label="复核员">
              <el-select
                v-model="searchForm.reviewerPhone"
                placeholder="请选择"
                clearable
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="r in reviewerPhones"
                  :key="r.phone"
                  :label="r.realName + ' ' + r.phone"
                  :value="r.phone"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 查验结果 -->
          <el-col :span="6">
            <el-form-item label="查验结果">
              <el-select
                v-model="searchForm.resultStatus"
                placeholder="请选择"
                clearable
                style="width: 100%;"
              >
                <el-option label="合格" :value="1" />
                <el-option label="不合格" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>

        </el-row>

        <el-row :gutter="8" style="margin-top: 12px;">

          <!-- 时间范围 -->
          <el-col :span="10">
            <el-form-item label="时间">
              <div class="date-range-split">
                <el-date-picker
                  v-model="dateRangeStart"
                  type="date"
                  placeholder="开始"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 48%;"
                />
                <span class="date-separator">至</span>
                <el-date-picker
                  v-model="dateRangeEnd"
                  type="date"
                  placeholder="结束"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 48%;"
                />
              </div>
            </el-form-item>
          </el-col>

          <!-- 复核状态 -->
          <el-col :span="4">
            <el-form-item label="复核结果">
              <el-select
                v-model="searchForm.manualReviewState"
                placeholder="请选择"
                clearable
                style="width: 100%;"
              >
                <el-option label="待审核" :value="0" />
                <el-option label="审核通过" :value="1" />
                <el-option label="审核未通过" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 上传状态 -->
          <el-col :span="4">
            <el-form-item label="上传状态">
              <el-select
                v-model="searchForm.toTransportdeptState"
                placeholder="请选择"
                clearable
                style="width: 100%;"
              >
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="2" />
                <el-option label="未上传" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 按钮组 -->
          <el-col :span="6">
            <el-form-item label=" ">
              <div class="btn-group-right">
                <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
                <el-button type="success" :loading="uploadLoading" @click="handleUpload">上报</el-button>
                <el-button type="warning" :loading="exportLoading" @click="handleExport">导出</el-button>
              </div>
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>
    </el-card>

    <!-- ========== 列表卡片 ========== -->
    <el-card class="table-card" shadow="never">

      <!-- 分页摘要信息 -->
      <div class="table-summary">
        <span class="summary-text">
          共 <strong>{{ pagination.total }}</strong> 条记录，
          当前第 <strong>{{ pagination.page }}/{{ totalPages }}</strong> 页
        </span>
      </div>

      <!--
        数据表格
        - border：显示竖线边框
        - v-loading：数据加载中时显示 loading 遮罩
        - @row-click：点击行时高亮选中行
        - highlight-current-row：选中行高亮背景
      -->
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        class="data-table"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600', fontSize: '14px' }"
        @row-click="handleRowClick"
        @row-dblclick="handleRowDblClick"
        :row-class-name="getRowClassName"
      >
        <!-- 车牌号码：显示车牌+颜色背景 -->
        <el-table-column label="车牌号码" width="110" align="center">
          <template #default="{ row }">
            <div class="plate-wrapper">
              <el-checkbox
                :model-value="selectedRadio === row.id"
                size="small"
                @click.stop
                @change="(val) => handleCheckboxChange(row, val)"
              />
              <div
                class="plate-cell"
                :class="{ 'row-selected': selectedRadio === row.id }"
                :style="{
                  background: getPlateColor(row.passcodeVehicleColorName).bg,
                  color: getPlateColor(row.passcodeVehicleColorName).text
                }"
              >
                <span class="plate-num">{{ row.plateNumber }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 司机电话 -->
        <el-table-column label="电话" width="100" align="center">
          <template #default="{ row }">
            <span class="mono-text">{{ row.driverPhone || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 车辆类型：显示车种标签 -->
        <el-table-column label="车型" width="70" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getVehicleClassTagType(row.vehicleType)"
              size="small"
              effect="light"
              class="vehicle-class-tag"
            >
              {{ row.vehicleTypeText || '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 货物类型（显示品种名称，而非产品编码） -->
        <el-table-column label="货物类型" min-width="120" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.goodsTypeName || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 货箱类型 -->
        <el-table-column label="货箱类型" width="100" align="center">
          <template #default="{ row }">
            <span class="container-type">{{ row.vehicleContainerTypeText || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 满载率：颜色分级显示 -->
        <el-table-column label="满载率" width="70" align="center">
          <template #default="{ row }">
            <span :class="getLoadRateClass(row.loadRate)">
              {{ row.loadRate != null ? row.loadRate + '%' : '-' }}
            </span>
          </template>
        </el-table-column>

        <!-- 查验员 -->
        <el-table-column label="查验员" width="75" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.operatorName || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 受理时间 -->
        <el-table-column label="受理时间" width="110" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.acceptanceTime || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 放行时间 -->
        <el-table-column label="放行时间" width="110" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.inspectionTime || '-' }}</span>
          </template>
        </el-table-column>



        <!-- 查验结果：合格/不合格标签 -->
        <el-table-column label="查验结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getResultTagType(row.resultStatus)" size="small" effect="dark">
              {{ row.resultStatusText }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 复核结果 -->
        <el-table-column label="复核结果" width="85" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.manualReviewState === 1" type="success" size="small">审核通过</el-tag>
            <el-tag v-else-if="row.manualReviewState === 2" type="danger" size="small">审核未通过</el-tag>
            <el-tag v-else type="info" size="small">待审核</el-tag>
          </template>
        </el-table-column>

        <!-- 上传状态 -->
        <el-table-column label="上传状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.toTransportdeptState === 1" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="row.toTransportdeptState === 2" type="danger" size="small">失败</el-tag>
            <el-tag v-else-if="row.toTransportdeptState === 0" type="info" size="small">未上传</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <!-- 上传备注 -->
        <el-table-column label="上传备注" min-width="86" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.toTransportdeptComment || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 上传时间 -->
        <el-table-column label="上传时间" width="130" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.toTransportdeptTime || '-' }}</span>
          </template>
        </el-table-column>


        <!-- 操作列：固定在右侧，查看/删除 -->
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <!-- .stop 阻止冒泡，避免触发行的 @row-click -->
              <el-button link type="primary" size="small" @click.stop="handleView(row)">
                <el-icon><View /></el-icon> 详情
              </el-button>
            </div>
          </template>
        </el-table-column>

      </el-table>

      <!-- 分页器 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- ========== 详情弹窗（可编辑） ========== -->
    <!-- 点击详情直接进入编辑模式 -->
    <InspectionDetail
      v-model="detailVisible"
      :row="currentRow"
      :editable="true"
      @refresh="loadData"
    />

    <!-- 图片编辑弹窗：上报前管理图片 -->
    <ImageEditDialog
      v-model="imageEditVisible"
      :row="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
/**
 * HistoricalRecords 车辆查验记录页面
 *
 * 【页面职责】
 * 作为查验模块的主页面，集成了列表展示、多条件搜索、分页、
 * 新增、编辑、删除、详情等全部功能。
 *
 * 【子组件通信】
 * - InspectionDetail：详情弹窗，通过 currentRow 传入选中行数据
 * - InspectionDetail：详情弹窗（可编辑），通过 currentRow 传入选中行数据
 *
 * 【数据流】
 * 1. onMounted → loadData() → 请求列表 API → 更新 tableData
 * 2. handleQuery → 重置页码为1 → loadData() → 带搜索条件请求
 * 3. handleReset → 清空搜索表单 → loadData()
 * 5. 弹窗提交成功 → emit('refresh') → loadData() 刷新列表
 */

import { ref, reactive, computed, onMounted, watch } from 'vue'
import * as XLSX from 'xlsx'
import { useRoute } from 'vue-router'
import {Search, RefreshLeft, Van, View, Edit, Upload} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInspectionList, getInspectionExport } from '@/api/vehicleInspection'
import { getUserPhoneList } from '@/api/user'
import InspectionDetail from '@/components/InspectionDetail.vue'
import ImageEditDialog from '@/components/ImageEditDialog.vue'

// ================================================================
// 状态定义
// ================================================================

/** 表格数据加载状态（控制 loading 遮罩） */
const loading = ref(false)

/** 路由对象，用于接收跳转参数 */
const route = useRoute()

/** 表格数据列表 */
const tableData = ref([])

/** 当前选中行（用于详情/编辑/行高亮） */
const currentRow = ref({})

/** 当前选中的记录ID（用于上报） */
const selectedRadio = ref(null)

/** 上报按钮 loading */
const uploadLoading = ref(false)

/** 导出按钮 loading */
const exportLoading = ref(false)

/** 详情弹窗显示状态 */
const detailVisible = ref(false)

/** 图片编辑弹窗显示状态 */
const imageEditVisible = ref(false)

/** 开始日期 */
const dateRangeStart = ref('')

/** 结束日期 */
const dateRangeEnd = ref('')

/** 日期范围选择器绑定的值，格式：[开始日期, 结束日期] */
const dateRange = ref([])

// 监听两个独立日期的变化，同步更新 dateRange
watch([dateRangeStart, dateRangeEnd], ([start, end]) => {
  if (start && end) {
    dateRange.value = [start, end]
  } else {
    dateRange.value = []
  }
})

// ================================================================
// 搜索表单
// ================================================================

/**
 * searchForm：搜索条件表单
 * 各字段初始值均为空/null，表示不作为查询条件。
 */
const searchForm = reactive({
  plateNumber: '',
  driverPhone: '',
  reviewerPhone: null,
  resultStatus: null,
  manualReviewState: null,
  toTransportdeptState: null
})

/** 核验员下拉选项（所有用户电话） */
const reviewerPhones = ref([])

// ================================================================
// 分页状态
// ================================================================

const pagination = reactive({
  page: 1,       // 当前页码（从1开始）
  pageSize: 10,   // 每页条数
  total: 0        // 总记录数
})

/** 总页数（计算属性） */
const totalPages = computed(() => Math.ceil(pagination.total / pagination.pageSize) || 1)

// ================================================================
// 数据加载
// ================================================================

/** 初始化日期范围为当前时间往前一个月 */
const initDateRange = () => {
  const now = new Date()
  const oneMonthAgo = new Date()
  oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1)
  // 格式化为 yyyy-MM-dd
  const fmt = (d) => {
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  dateRangeStart.value = fmt(oneMonthAgo)
  dateRangeEnd.value = fmt(now)
}

/** 加载核验员下拉选项（所有用户电话） */
const loadUserPhones = async () => {
  try {
    const res = await getUserPhoneList()
    if (res.code === 200) {
      reviewerPhones.value = res.data || []
    }
  } catch {
    // 加载失败不影响主流程
  }
}

/**
 * loadData：从后端加载查验记录列表
 *
 * 【分页参数】
 * page=1/pageSize=10 由 pagination 对象提供，
 * 由 el-pagination 的 v-model:current-page/v-model:page-size 自动双向绑定。
 *
 * 【搜索参数处理】
 * 只有非空字段才加入请求参数：
 * - plateNumber：模糊查询，可空
 * - driverPhone：精确查询，可空
 * - reviewerPhone：精确查询，可空（对应 reviewer_phone）
 * - resultStatus：精确查询，可空（注意用 !== null 判断，而非 !resultStatus）
 * - startTime/endTime：时间范围，dateRange 为空数组时不传
 *
 * 【后端排序】
 * 后端固定按 inspectionTime DESC 排序，无需前端指定。
 *
 * 【响应数据】
 * 后端返回 { records: [...], total: N }，
 * 分页信息（page/pageSize）直接复用前端当前值。
 */
const loadData = async () => {
  loading.value = true
  try {
    // 构建请求参数
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    // 仅添加有值的搜索条件
    if (searchForm.plateNumber)   params.plateNumber   = searchForm.plateNumber
    if (searchForm.driverPhone)    params.driverPhone   = searchForm.driverPhone
    if (searchForm.reviewerPhone) params.reviewerPhone = searchForm.reviewerPhone
    // 注意：resultStatus=0 时也是有意义的值，要用 !== null 判断
    if (searchForm.resultStatus !== null) params.resultStatus = searchForm.resultStatus
    // 时间范围：有值且长度为2时才加入
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime   = dateRange.value[1] + ' 23:59:59'
    }
    // 注意：manualReviewState 有值时要用 !== null 判断
    if (searchForm.manualReviewState !== null) params.manualReviewState = searchForm.manualReviewState
    // 注意：toTransportdeptState 有值时要用 !== null 判断
    if (searchForm.toTransportdeptState !== null) params.toTransportdeptState = searchForm.toTransportdeptState

    const res = await getInspectionList(params)

    if (res.code === 200) {
      tableData.value   = res.data.records || []
      pagination.total  = res.data.total || 0
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ================================================================
// 搜索操作
// ================================================================

/**
 * handleQuery：点击"查询"按钮
 * 重置页码为1，然后重新加载（带搜索条件）
 */
const handleQuery = () => {
  pagination.page = 1
  loadData()
}

/**
 * handleReset：点击"重置"按钮
 * 清空所有搜索条件，重置页码，重新加载
 */
const handleReset = () => {
  searchForm.plateNumber   = ''
  searchForm.driverPhone    = ''
  searchForm.reviewerPhone  = null
  searchForm.resultStatus   = null
  searchForm.manualReviewState = null
  searchForm.toTransportdeptState = null
  initDateRange()
  pagination.page = 1
  loadData()
}

/**
 * handleUpload：打开图片编辑弹窗
 */
const handleUpload = async () => {
  if (!selectedRadio.value) {
    ElMessage.warning('请先选择要上报的记录')
    return
  }

  // 找到选中的行数据
  const selectedRow = tableData.value.find(row => row.id === selectedRadio.value)
  if (!selectedRow) {
    ElMessage.warning('找不到选中的记录')
    return
  }

  // 检查是否已上传成功
  if (selectedRow.toTransportdeptState === 1) {
    ElMessage.warning('该记录已上传成功，不能重复上传')
    return
  }

  // 不合格记录禁止上传
  if (selectedRow.resultStatus === 2) {
    ElMessageBox.alert('不合格记录禁止上传，请仔细复核', '警告', {
      confirmButtonText: '确定',
      type: 'warning'
    })
    return
  }

  // 设置当前行数据，打开图片编辑弹窗
  currentRow.value = { ...selectedRow }
  imageEditVisible.value = true
}

/**
 * handleExport：导出Excel表格
 * 调用全量导出API获取所有符合条件的数据
 */
const handleExport = async () => {
  exportLoading.value = true
  try {
    // 构建请求参数（与loadData保持一致）
    const params = {}
    if (searchForm.plateNumber) params.plateNumber = searchForm.plateNumber
    if (searchForm.driverPhone) params.driverPhone = searchForm.driverPhone
    if (searchForm.reviewerPhone) params.reviewerPhone = searchForm.reviewerPhone
    if (searchForm.resultStatus !== null) params.resultStatus = searchForm.resultStatus
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }
    if (searchForm.manualReviewState !== null) params.manualReviewState = searchForm.manualReviewState
    if (searchForm.toTransportdeptState !== null) params.toTransportdeptState = searchForm.toTransportdeptState

    // 调用全量导出API
    const res = await getInspectionExport(params)
    if (res.code !== 200) {
      ElMessage.error(res.message || '获取导出数据失败')
      return
    }
    const exportDataList = res.data || []
    if (!exportDataList.length) {
      ElMessage.warning('没有可导出的数据')
      return
    }

    // 准备表头
    const headers = [
      { key: 'plateNumber', title: '车牌号码' },
      { key: 'driverPhone', title: '司机电话' },
      { key: 'vehicleTypeText', title: '车辆类型' },
      { key: 'goodsTypeName', title: '货物类型' },
      { key: 'vehicleContainerTypeText', title: '货箱类型' },
      { key: 'loadRate', title: '满载率' },
      { key: 'operatorName', title: '查验员' },
      { key: 'acceptanceTime', title: '受理时间' },
      { key: 'inspectionTime', title: '放行时间' },
      { key: 'duration', title: '受理时长' },
      { key: 'resultStatusText', title: '查验结果' },
      { key: 'manualReviewStateText', title: '复核结果' },
      { key: 'toTransportdeptStateText', title: '上传状态' },
      { key: 'toTransportdeptComment', title: '上传备注' },
      { key: 'toTransportdeptTime', title: '上传时间' }
    ]

    // 计算受理时长（放行时间 - 受理时间）
    const calcDuration = (row) => {
      if (!row.acceptanceTime || !row.inspectionTime) return ''
      const start = new Date(row.acceptanceTime)
      const end = new Date(row.inspectionTime)
      if (isNaN(start.getTime()) || isNaN(end.getTime())) return ''
      const diffMs = end.getTime() - start.getTime()
      if (diffMs <= 0) return ''
      const totalSeconds = Math.floor(diffMs / 1000)
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60
      if (hours > 0) {
        return `${hours}小时${minutes}分${seconds}秒`
      }
      if (minutes > 0) {
        return `${minutes}分${seconds}秒`
      }
      return `${seconds}秒`
    }

    // 转换数据格式
    const exportData = exportDataList.map(row => {
      const obj = {}
      headers.forEach(h => {
        let val = row[h.key]
        // 特殊字段处理
        if (h.key === 'loadRate') {
          val = val != null ? val + '%' : ''
        } else if (h.key === 'duration') {
          val = calcDuration(row)
        } else if (h.key === 'manualReviewStateText') {
          if (row.manualReviewState === 1) val = '审核通过'
          else if (row.manualReviewState === 2) val = '审核未通过'
          else val = '待审核'
        } else if (h.key === 'toTransportdeptStateText') {
          if (row.toTransportdeptState === 1) val = '成功'
          else if (row.toTransportdeptState === 2) val = '失败'
          else if (row.toTransportdeptState === 0) val = '未上传'
          else val = ''
        }
        obj[h.title] = val ?? ''
      })
      return obj
    })

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(exportData)

    // 设置列宽
    ws['!cols'] = [
      { wch: 12 }, { wch: 12 }, { wch: 10 }, { wch: 14 }, { wch: 10 },
      { wch: 8 }, { wch: 10 }, { wch: 18 }, { wch: 18 }, { wch: 12 },
      { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 16 }, { wch: 18 }
    ]

    XLSX.utils.book_append_sheet(wb, ws, '查验记录')

    // 生成文件名（当前日期）
    const now = new Date()
    const fileName = `查验记录_${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}.xlsx`

    // 导出文件
    XLSX.writeFile(wb, fileName)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// ================================================================
// 分页操作
// ================================================================

/**
 * handleSizeChange：切换每页条数时触发
 * el-pagination 会自动更新 pagination.pageSize，
 * 此处只需重置页码并重新加载。
 *
 * 【为什么需要手动重置 page？】
 * 切换 pageSize 后，若当前页码已超出新的总页数，会显示空白。
 * 统一重置为第1页，保证始终有数据显示。
 */
const handleSizeChange = () => {
  pagination.page = 1
  loadData()
}

/**
 * handlePageChange：切换页码时触发
 * el-pagination 会自动更新 pagination.page，
 * 此处直接调用 loadData() 加载新页数据。
 */
const handlePageChange = () => {
  loadData()
}

// ================================================================
// 行交互
// ================================================================

// ================================================================
// CRUD 操作
// ================================================================

/**
 * handleView：查看详情
 * 复制选中行数据（浅拷贝），打开详情弹窗。
 * 使用浅拷贝避免弹窗中修改数据影响原列表。
 */
const handleView = (row) => {
  currentRow.value = { ...row }
  detailVisible.value = true
}

/**
 * handleRowClick：点击行选中/取消选中
 * - 点击已选中行：取消选中
 * - 点击未选中行：选中
 */
const handleRowClick = (row) => {
  const id = row.id
  if (selectedRadio.value === id) {
    selectedRadio.value = null
  } else {
    selectedRadio.value = id
  }
}

/**
 * handleRowDblClick：双击行显示详情
 */
const handleRowDblClick = (row) => {
  currentRow.value = { ...row }
  detailVisible.value = true
}

/**
 * handleCheckboxChange：勾选框状态变化时触发
 * @param {Object} row - 当前行数据
 * @param {Boolean} val - 勾选状态
 */
const handleCheckboxChange = (row, val) => {
  if (val) {
    selectedRadio.value = row.id
  } else {
    selectedRadio.value = null
  }
}

/**
 * getRowClassName：行样式类名，用于高亮选中行
 */
const getRowClassName = ({ row }) => {
  return selectedRadio.value === row.id ? 'selected-row' : ''
}

// ================================================================
// 样式辅助函数
// ================================================================

/**
 * getVehicleClassTagType：根据车辆类型返回 el-tag 类型
 * 11-16（一至六型货车）→ success（绿通车），其他 → warning（联合收割机）
 */
const getVehicleClassTagType = (vehicleType) => {
  if (!vehicleType) return 'info'
  // 11-16 是一至六型货车（绿通车）
  if (['11', '12', '13', '14', '15', '16'].includes(vehicleType)) return 'success'
  return 'warning'
}

/**
 * getResultTagType：根据查验结果返回 el-tag 类型
 * 合格(1) → success，不合格(2) → danger，其他 → info
 */
const getResultTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

/**
 * getPlateColor：根据通行颜色返回车牌背景色和字体颜色
 * 0-蓝色，1-黄色，2-黑色，3-白色，4-渐变绿色，5-黄绿双拼色，
 * 6-蓝白渐变色，7-临时牌照（灰色），11-绿色，12-红色
 */
const getPlateColor = (colorName) => {
  const colorMap = {
    '0': { bg: '#2980b9', text: '#ffffff' },   // 蓝色 → 白字
    '1': { bg: '#f1c40f', text: '#2c3e50' },   // 黄色 → 黑字
    '2': { bg: '#2c3e50', text: '#ecf0f1' },   // 黑色 → 白字
    '3': { bg: '#ecf0f1', text: '#2c3e50' },  // 白色 → 黑字
    '4': { bg: '#27ae60', text: '#ffffff' },  // 绿色 → 白字
    '5': { bg: '#16a085', text: '#ffffff' },  // 蓝绿 → 白字
    '6': { bg: '#16a085', text: '#ffffff' },  // 蓝绿渐变 → 白字
    '7': { bg: '#8e44ad', text: '#ffffff' },  // 灰色 → 白字
    '11': { bg: '#27ae60', text: '#ffffff' }, // 绿色 → 白字
    '12': { bg: '#ff0000', text: '#ffffff' }, // 红色 → 白字
  }
  return colorMap[colorName] || { bg: '#7f8c8d', text: '#ffffff' }
}

/**
 * getLoadRateClass：根据满载率返回样式类名
 * ≥80% → load-high（绿色，深绿色），表示绿通合格
 * ≥60% → load-mid（橙色），需要注意
 * <60% → load-low（灰色），可能不符合要求
 * null → text-muted（浅灰）
 */
const getLoadRateClass = (loadRate) => {
  if (loadRate == null) return 'text-muted'
  if (loadRate >= 80) return 'load-high'
  if (loadRate >= 60) return 'load-mid'
  return 'load-low'
}

// ================================================================
// 生命周期
// ================================================================

/**
 * onMounted：组件挂载完成后自动加载数据
 * 这是 Vue 3 Composition API 的生命周期钩子，
 * 等同于 Vue 2 的 mounted。
 */
onMounted(async () => {
  initDateRange()
  await loadUserPhones()
  // 读取 URL 查询参数并应用搜索条件
  applyQueryParams()
  loadData()
})

/** 应用 URL 查询参数到搜索表单 */
const applyQueryParams = () => {
  if (route.query.manualReviewState !== undefined) {
    searchForm.manualReviewState = Number(route.query.manualReviewState)
  }
  if (route.query.resultStatus !== undefined) {
    searchForm.resultStatus = Number(route.query.resultStatus)
  }
  if (route.query.toTransportdeptState !== undefined) {
    searchForm.toTransportdeptState = Number(route.query.toTransportdeptState)
  }
}

// 监听路由变化，重新应用查询参数
watch(() => route.query, () => {
  applyQueryParams()
  loadData()
})
</script>

<style scoped>
/* ========== 页面容器 ========== */
.history-page {
  padding: 20px 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  overflow-x: hidden;
}

/* ========== 页面标题 ========== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 22px;
  color: #409eff;
}

/* ========== 搜索卡片 ========== */
.search-card {
  margin-bottom: 20px;
  border-radius: 8px;
  width: 100%;
}

.search-form {
  width: 100%;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  width: 100%;
}

.search-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  padding-bottom: 6px !important;
}

.search-form :deep(.el-input__wrapper),
.search-form :deep(.el-select__wrapper) {
  border-radius: 6px;
}

/* 按钮组列：保持与查询条件对齐 */
.btn-group-col {
  display: flex;
  align-items: flex-start;
  padding-top: 0px;
}

.btn-group-col :deep(.el-form-item) {
  margin-top: 0;
}

.btn-group {
  display: flex;
  gap: 8px;
}

.btn-group-right {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ========== 列表卡片 ========== */
.table-card {
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

/* ========== 表格样式 ========== */
.data-table {
  border-radius: 6px;
  width: 100%;
}

/* 行悬停效果 */
.data-table :deep(.el-table__row) {
  cursor: pointer;
  transition: background 0.2s;
  height: 46px;
}

.data-table :deep(.el-table__row:hover) {
  background: #ecf5ff !important;
}

/* 单元格内边距 */
.data-table :deep(.el-table__cell) {
  padding: 6px 0;
}

/* ========== 单元格内容样式 ========== */

/* 车牌单元格：居中 + 间距 + 圆角背景 */
.plate-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0px;
}

.plate-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 6px;
  width: fit-content;
  margin: 0 auto;
}

.plate-cell :deep(.el-checkbox) {
  margin-right: 0;
}


/* el-table 选中行高亮 */
:deep(.selected-row td) {
  background-color: var(--el-color-primary-light-9) !important;
}

.plate-num {
  font-weight: 700;
  color: inherit;
  font-family: 'Consolas', 'Monaco', monospace;
  letter-spacing: 1px;
  font-size: 12px;
}

/* 挂车号码 */
.trailer-num {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

.plate-color-tag {
  font-size: 10px;
  padding: 0 2px;
  height: 16px;
  line-height: 14px;
}

/* 车辆类型标签 */
.vehicle-class-tag {
  font-size: 11px;
}

/* 货箱类型 */
.container-type {
  font-size: 11px;
  color: #606266;
}

/* 等宽字体（电话号码） */
.mono-text {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #606266;
}

/* 时间文本 */
.time-text {
  font-size: 11px;
  color: #909399;
}

/* 分开的日期选择器布局 */
.date-range-split {
  display: flex;
  align-items: center;
  width: 100%;
}

.date-separator {
  margin: 0 8px;
  color: #909399;
}

/* 满载率颜色分级 */
.load-high {
  color: #67c23a;  /* 绿色：合格 */
  font-weight: 600;
  font-size: 12px;
}

.load-mid {
  color: #e6a23c;  /* 橙色：需关注 */
  font-weight: 500;
  font-size: 11px;
}

.load-low {
  color: #909399;  /* 灰色：不合格 */
}

.text-muted {
  color: #c0c4cc;  /* 空值占位 */
}

/* ========== 操作按钮 ========== */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.action-btns .el-button {
  padding: 2px 6px;
  font-size: 12px;
}

.action-btns .el-icon {
  margin-right: 2px;
}

/* ========== 分页器 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.pagination-wrapper :deep(.el-pagination__total) {
  font-size: 13px;
  color: #909399;
}
</style>
