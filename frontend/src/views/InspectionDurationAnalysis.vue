<template>
  <!--
    查验时长分析页面
    - 上半部分：查询表单（参照车辆查验页面）
    - 下半部分：柱状图（横坐标=查验记录，纵坐标=查验时长）
  -->
  <div class="duration-analysis-page">
    
    <!-- ========== 查询卡片 ========== -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" label-position="left" class="search-form">
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
              </div>
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>
    </el-card>

    <!-- ========== 图表卡片 ========== -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">
            <el-icon color="#409eff"><Histogram /></el-icon>
            查验时长分布
          </span>
          <span class="panel-sub">柱状图展示每条查验记录的时长</span>
        </div>
      </template>
      <div ref="barChartRef" class="chart-container"></div>
    </el-card>

  </div>
</template>

<script setup>
/**
 * 查验时长分析页面
 *
 * 【页面结构】
 *   - 上半部分：查询表单（参照车辆查验页面）
 *   - 下半部分：柱状图（横坐标=查验记录，纵坐标=查验时长）
 */

import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { Timer, Histogram } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getInspectionExport } from '@/api/vehicleInspection'

// ================================================================
// 状态定义
// ================================================================

/** 查询表单 */
const searchForm = reactive({
  plateNumber: '',
  driverPhone: '',
  reviewerPhone: '',
  resultStatus: null,
  manualReviewState: null,
  toTransportdeptState: null
})

/** 日期范围 */
const dateRangeStart = ref('')
const dateRangeEnd = ref('')

/** 复核员列表 */
const reviewerPhones = ref([])

/** 加载状态 */
const loading = ref(false)

/** 图表数据 */
const chartData = ref([])

// ================================================================
// 图表 DOM
// ================================================================

const barChartRef = ref(null)
let barChart = null

// ================================================================
// 数据加载
// ================================================================

/**
 * 加载查验记录数据
 */
const loadData = async () => {
  loading.value = true
  try {
    // 默认获取当天数据
    let startDate = dateRangeStart.value
    let endDate = dateRangeEnd.value
    if (!startDate && !endDate) {
      // 都没有选择，默认当天
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      startDate = `${year}-${month}-${day}`
      endDate = `${year}-${month}-${day}`
    } else if (!startDate) {
      // 只有结束日期，设为当天
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      startDate = `${year}-${month}-${day}`
    } else if (!endDate) {
      // 只有开始日期，设为当天
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      endDate = `${year}-${month}-${day}`
    }

    const params = {}

    // 时间范围（格式：yyyy-MM-dd HH:mm:ss）
    if (startDate && endDate) {
      params.startTime = `${startDate} 00:00:00`
      params.endTime = `${endDate} 23:59:59`
    }

    // 添加查询条件
    if (searchForm.plateNumber) {
      params.plateNumber = searchForm.plateNumber
    }
    if (searchForm.driverPhone) {
      params.driverPhone = searchForm.driverPhone
    }
    if (searchForm.reviewerPhone) {
      params.operatorName = searchForm.reviewerPhone
    }
    if (searchForm.resultStatus) {
      params.resultStatus = searchForm.resultStatus
    }
    if (searchForm.manualReviewState !== null) {
      params.manualReviewState = searchForm.manualReviewState
    }
    if (searchForm.toTransportdeptState !== null) {
      params.toTransportdeptState = searchForm.toTransportdeptState
    }

    const res = await getInspectionExport(params)
    if (res.code === 200) {
      console.log("查验时间范围：", params.startTime, "至", params.endTime)
      console.log('查验记录列表：', res.data)
      // 处理图表数据
      chartData.value = (res.data || []).map((item, index) => {
        // 时间字段列表（按顺序）
        const timeFields = [
          { key: 'btnPrebookTime', label: '司机预约' },
          { key: 'acceptanceTime', label: '受理' },
          { key: 'opengateTime', label: '抬杆放行' },
          { key: 'openlightscreenTime', label: '光闸打开' },
          { key: 'closelightscreenTime', label: '光闸关闭' },
          { key: 'cdPhotoTime', label: 'CD拍照' },
          { key: 'inspectionTime', label: '检测结束' }
        ]

        // 判断是否为新格式（7个时间字段都有值）
        const isNewFormat = timeFields.every(f => item[f.key])

        let totalDuration = 0
        const segments = []

        if (!isNewFormat) {
          // 旧格式：用 acceptanceTime → inspectionTime，整体显示为紫色
          const start = item.acceptanceTime ? new Date(item.acceptanceTime) : null
          const end = item.inspectionTime ? new Date(item.inspectionTime) : null
          if (start && end && !isNaN(start.getTime()) && !isNaN(end.getTime())) {
            const diff = end.getTime() - start.getTime()
            if (diff > 0) {
              totalDuration = Math.floor(diff / 1000)
            }
          }
          segments.push({ duration: totalDuration, label: '查验总时长', color: '#9b59b6' })
        } else {
          // 新格式：计算每段的时长
          const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452']
          for (let i = 0; i < timeFields.length - 1; i++) {
            const start = new Date(item[timeFields[i].key])
            const end = new Date(item[timeFields[i + 1].key])
            const diff = end.getTime() - start.getTime()
            const duration = diff > 0 ? Math.floor(diff / 1000) : 0
            totalDuration += duration
            segments.push({
              duration,
              color: colors[i],
              label: `${timeFields[i].label} → ${timeFields[i + 1].label}`
            })
          }
        }

        return {
          index: index + 1,
          plateNumber: item.plateNumber || '-',
          acceptanceTime: item.acceptanceTime || '',
          totalDuration,
          segments,
          isOldFormat: !isNewFormat
        }
      })

      // 按 acceptanceTime 排序并重新编号
      chartData.value.sort((a, b) => {
        if (!a.acceptanceTime && !b.acceptanceTime) return 0
        if (!a.acceptanceTime) return 1
        if (!b.acceptanceTime) return -1
        return new Date(a.acceptanceTime) - new Date(b.acceptanceTime)
      })
      chartData.value.forEach((item, idx) => { item.index = idx + 1 })

      // 渲染图表
      await nextTick()
      renderBarChart()
    } else {
      ElMessage.error(res.message || '加载数据失败')
    }
  } catch (e) {
    console.error('加载数据失败', e)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 加载复核员列表
 */
const loadReviewers = async () => {
  // 如果有专门的复核员列表接口，调用它
  // 目前复用车辆查验页面的方式
}

// ================================================================
// 查询与重置
// ================================================================

const handleQuery = () => {
  loadData()
}

const handleReset = () => {
  searchForm.plateNumber = ''
  searchForm.driverPhone = ''
  searchForm.reviewerPhone = ''
  searchForm.resultStatus = null
  searchForm.manualReviewState = null
  searchForm.toTransportdeptState = null
  dateRangeStart.value = ''
  dateRangeEnd.value = ''
}

// ================================================================
// 图表渲染
// ================================================================

/**
 * 渲染查验时长柱状图
 * - 横坐标：查验记录（序号或车牌号）
 * - 纵坐标：查验时长（秒）
 */
const renderBarChart = () => {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const data = chartData.value
  if (data.length === 0) return

  const formatDuration = (seconds) => {
    if (!seconds || seconds === 0) return '0秒'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return mins === 0 ? `${secs}秒` : `${mins}分${secs}秒`
  }

  const labels = data.map(d => d.acceptanceTime ? d.acceptanceTime.replace('T', ' ').slice(0, 16) : '#' + d.index)

  let series = []
  // 统一用堆叠图，新格式分段，旧格式只有第一段有值（=总时长），其余为0
  const segmentCount = 6
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
  const defaultLabels = ['司机预约→受理', '受理→抬杆放行', '抬杆放行→光幕打开', '光幕打开→光幕关闭', '光幕关闭→CD拍照', 'CD拍照→检测结束']

  const stackData = []
  for (let i = 0; i < segmentCount; i++) {
    stackData.push({
      name: defaultLabels[i],
      color: colors[i],
      data: data.map(item => {
        const seg = item.segments[i]
        return seg ? { value: seg.duration, itemStyle: { color: seg.color || colors[i] } } : { value: 0, itemStyle: { color: colors[i] } }
      })
    })
  }
  series = stackData.map((s, i) => ({
    name: s.name,
    type: 'bar',
    stack: 'total',
    data: s.data,
    barWidth: 24,
    itemStyle: {
      color: s.color,
      borderRadius: i === stackData.length - 1 ? [4, 4, 0, 0] : 0
    }
  }))

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        const item = data[idx]
        const timeStr = item.acceptanceTime ? item.acceptanceTime.replace('T', ' ').slice(0, 19) : '-'
        let tip = `时间：${timeStr}<br/>车牌：${item.plateNumber}<br/>总时长：<b>${formatDuration(item.totalDuration)}</b><br/><br/>`
        if (item.isOldFormat) {
          tip += `<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#9b59b6;margin-right:5px;"></span>查验总时长：${formatDuration(item.segments[0]?.duration)}`
        } else {
          tip += `<div style="line-height:18px;">`
          item.segments.forEach(seg => {
            if (seg.duration > 0) {
              tip += `<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${seg.color};margin-right:5px;"></span>${seg.label}：${formatDuration(seg.duration)}<br/>`
            }
          })
          tip += `</div>`
        }
        return tip
      }
    },
    legend: { top: 10, data: series.map(s => s.name) },
    grid: { left: 80, right: 40, top: 50, bottom: 60 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 10, color: '#909399', rotate: 30 },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      name: '查验时长',
      nameTextStyle: { fontSize: 12, color: '#606266' },
      axisLabel: { fontSize: 11, color: '#909399', formatter: v => formatDuration(v) },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLine: { show: true, lineStyle: { color: '#e4e7ed' } }
    },
    series,
    dataZoom: [{ type: 'inside', start: 0, end: 100 }]
  })
}

// ================================================================
// 生命周期
// ================================================================

onMounted(() => {
  // 初始化默认当天日期
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const todayStr = `${year}-${month}-${day}`
  dateRangeStart.value = todayStr
  dateRangeEnd.value = todayStr

  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
})

const handleResize = () => {
  barChart?.resize()
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.duration-analysis-page {
  padding: 20px 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

/* ========== 页面头部 ========== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 查询卡片 ========== */
.search-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.date-range-split {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  color: #909399;
  font-size: 14px;
}

.btn-group-right {
  display: flex;
  gap: 8px;
}

/* ========== 图表卡片 ========== */
.chart-card {
  border-radius: 10px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.panel-sub {
  font-size: 12px;
  color: #909399;
}

.chart-container {
  height: 400px;
  width: 100%;
}
</style>