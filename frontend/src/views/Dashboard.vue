<template>
  <!--
    Dashboard 首页概览（新版布局）
    布局结构：
      左侧（16栏）：信息总览卡片 + 时段分析图表 + 车型/货物类型图表
      右侧（8栏）：待办事项 + 文件通知 + 数据同步
  -->
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- 左侧区域（16栏） -->
      <el-col :span="16">
        <div class="left-section">
          <!-- 第一行：信息总览卡片 -->
          <div class="section-block">
            <div class="section-header">
              <span class="section-title">信息总览</span>
              <el-radio-group v-model="timeType" size="small" @change="handleTimeTypeChange">
                <el-radio-button value="day">日</el-radio-button>
                <el-radio-button value="month">月</el-radio-button>
                <el-radio-button value="year">年</el-radio-button>
              </el-radio-group>
            </div>
            <el-row :gutter="16">
              <!-- 卡片1：绿通车/收割机数量 + 查验车次 -->
              <el-col :span="8">
                <el-card class="stat-card" shadow="hover">
                  <div class="stat-inner">
                    <div class="stat-icon-wrap blue">
                      <el-icon size="28"><Van /></el-icon>
                    </div>
                    <div class="stat-body">
                      <div class="stat-value-row">
                        <span class="stat-value-item">
                          <span class="value">{{ infoOverview.greenVehicleCount }}</span>
                          <span class="label">绿通车</span>
                        </span>
                        <span class="stat-divider">|</span>
                        <span class="stat-value-item">
                          <span class="value">{{ infoOverview.harvesterCount }}</span>
                          <span class="label">收割机</span>
                        </span>
                      </div>
                      <div class="stat-sub">
                        查验车次：{{ infoOverview.inspectionCount }}
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>

              <!-- 卡片2：通行费用 -->
              <el-col :span="8">
                <el-card class="stat-card" shadow="hover">
                  <div class="stat-inner">
                    <div class="stat-icon-wrap green">
                      <el-icon size="28"><Money /></el-icon>
                    </div>
                    <div class="stat-body">
                      <div class="stat-value large">
                        {{ formatMoney(infoOverview.passFee) }}
                      </div>
                      <div class="stat-label">通行费用（元）</div>
                    </div>
                  </div>
                </el-card>
              </el-col>

              <!-- 卡片3：合格/不合格数量 + 上传记录数 -->
              <el-col :span="8">
                <el-card class="stat-card" shadow="hover">
                  <div class="stat-inner">
                    <div class="stat-icon-wrap amber">
                      <el-icon size="28"><CircleCheck /></el-icon>
                    </div>
                    <div class="stat-body">
                      <div class="stat-value-row">
                        <span class="stat-value-item success">
                          <span class="value">{{ infoOverview.passCount }}</span>
                          <span class="label">合格</span>
                        </span>
                        <span class="stat-divider">|</span>
                        <span class="stat-value-item danger">
                          <span class="value">{{ infoOverview.failCount }}</span>
                          <span class="label">不合格</span>
                        </span>
                      </div>
                      <div class="stat-sub">
                        上传记录：{{ infoOverview.uploadCount }}
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 第二行：时段分析（24小时折线图） -->
          <div class="section-block">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="panel-header">
                  <span class="panel-title">
                    <el-icon color="#409eff"><DataLine /></el-icon>
                    时段分析
                  </span>
                  <span class="panel-sub">查验量趋势</span>
                </div>
              </template>
              <div ref="lineChartRef" class="chart-container-line"></div>
            </el-card>
          </div>

          <!-- 第三行：车型分布 + 货物类别 -->
          <div class="section-block">
            <el-row :gutter="16">
              <!-- 车型分布（横向条形图） -->
              <el-col :span="12">
                <el-card class="chart-card" shadow="never">
                  <template #header>
                    <div class="panel-header">
                      <span class="panel-title">
                        <el-icon color="#67c23a"><Histogram /></el-icon>
                        车型分布
                      </span>
                    </div>
                  </template>
                  <div ref="barChartRef" class="chart-container-bar"></div>
                </el-card>
              </el-col>

              <!-- 货物类别（饼状图） -->
              <el-col :span="12">
                <el-card class="chart-card" shadow="never">
                  <template #header>
                    <div class="panel-header">
                      <span class="panel-title">
                        <el-icon color="#e6a23c"><PieChart /></el-icon>
                        货物类别
                      </span>
                    </div>
                  </template>
                  <div ref="pieChartRef" class="chart-container-pie"></div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-col>

      <!-- 右侧区域（8栏） -->
      <el-col :span="8">
        <div class="right-section">
          <!-- 第一部分：待办事项 -->
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span class="panel-title">
                  <el-icon color="#f56c6c"><List /></el-icon>
                  待办事项
                </span>
              </div>
            </template>
            <div v-if="todoItems.length === 0" class="empty-state">
              <el-icon size="32" color="#dcdfe6"><SuccessFilled /></el-icon>
              <span>暂无待办事项</span>
            </div>
            <div v-else class="todo-list">
              <div
                v-for="item in todoItems"
                :key="item.id"
                class="todo-item"
                :class="item.type"
              >
                <div class="todo-left">
                  <el-icon v-if="item.type === 'pending_review'" color="#faad14"><Clock /></el-icon>
                  <el-icon v-else-if="item.type === 'fake_green'" color="#f56c6c"><Warning /></el-icon>
                  <el-icon v-else color="#f56c6c"><WarningFilled /></el-icon>
                  <span class="todo-title">{{ item.title }}</span>
                </div>
                <el-tag size="small" :type="getTagType(item.type)">
                  {{ item.count }} 条
                </el-tag>
              </div>
            </div>
          </el-card>

          <!-- 第二部分：文件通知 -->
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span class="panel-title">
                  <el-icon color="#409eff"><Document /></el-icon>
                  文件通知
                </span>
              </div>
            </template>
            <div v-if="notices.length === 0" class="empty-state">
              <span>暂无通知</span>
            </div>
            <div v-else class="notice-list">
              <div
                v-for="item in notices"
                :key="item.id"
                class="notice-item"
              >
                <el-tag size="small" :type="getNoticeTagType(item.type)" effect="plain">
                  {{ getNoticeTypeText(item.type) }}
                </el-tag>
                <div class="notice-content">
                  <span class="notice-title">{{ item.title }}</span>
                  <span class="notice-date">{{ item.date }}</span>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 第三部分：数据同步 -->
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span class="panel-title">
                  <el-icon color="#67c23a"><PieChart /></el-icon>
                  数据同步
                </span>
              </div>
            </template>
            <div class="exempt-container">
              <div class="exempt-circle">
                <el-progress
                  type="circle"
                  :percentage="exemptRate.rate"
                  :width="100"
                  :stroke-width="8"
                  :color="getExemptColor(exemptRate.rate)"
                >
                  <template #default>
                    <span class="exempt-value">{{ exemptRate.rate }}%</span>
                  </template>
                </el-progress>
              </div>
              <div class="exempt-stats">
                <div class="exempt-stat">
                  <span class="stat-num">{{ exemptRate.total }}</span>
                  <span class="stat-label">总查验数</span>
                </div>
                <div class="exempt-stat">
                  <span class="stat-num success">{{ exemptRate.exempt }}</span>
                  <span class="stat-label">免检数</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <!-- 详情弹窗 -->
    <InspectionDetail
      v-model="detailVisible"
      :row="currentRow"
      @refresh="loadData"
    />
  </div>
</template>

<script setup>
/**
 * Dashboard 首页概览（新版布局）
 *
 * 【布局结构】
 *   左侧（16栏）：
 *     - 第一行：信息总览（3个卡片）+ 时间切换器
 *     - 第二行：时段分析（24小时折线图）
 *     - 第三行：车型分布（横向条形图）+ 货物类别（饼图）
 *   右侧（8栏）：
 *     - 待办事项
 *     - 文件通知
 *     - 数据同步
 *
 * 【数据来源】
 *   通过 /api/inspection/dashboard?timeType=day|month|year 加载统计数据
 */

import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import {
  Van, Money, CircleCheck, DataLine, Histogram, PieChart,
  List, Document, Clock, Warning, WarningFilled, SuccessFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/vehicleInspection'
import InspectionDetail from '@/components/InspectionDetail.vue'

// ================================================================
// 状态定义
// ================================================================

/** 时间类型：day=日, month=月, year=年 */
const timeType = ref('day')

/** 信息总览数据 */
const infoOverview = reactive({
  greenVehicleCount: 0,
  harvesterCount: 0,
  inspectionCount: 0,
  passFee: 0,
  passCount: 0,
  failCount: 0,
  uploadCount: 0
})

/** 时段分布数据 */
const hourlyDistribution = ref([])

/** 车型分布数据 */
const vehicleTypeStats = ref([])

/** 货物类别数据 */
const goodsTypeStats = ref([])

/** 待办事项 */
const todoItems = ref([])

/** 文件通知 */
const notices = ref([])

/** 数据同步 */
const exemptRate = reactive({
  total: 0,
  exempt: 0,
  rate: 0
})

// ================================================================
// 详情弹窗
// ================================================================

const detailVisible = ref(false)
const currentRow = ref({})

const showDetail = (item) => {
  currentRow.value = { ...item }
  detailVisible.value = true
}

// ================================================================
// 图表 DOM refs
// ================================================================

const lineChartRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)

// ECharts 实例
let lineChart = null
let barChart = null
let pieChart = null

// ================================================================
// 数据加载
// ================================================================

const loadData = async () => {
  try {
    const res = await getDashboardStats(timeType.value)
    if (res.code === 200) {
      const d = res.data

      // 信息总览
      const overview = d.infoOverview || {}
      infoOverview.greenVehicleCount = overview.greenVehicleCount || 0
      infoOverview.harvesterCount = overview.harvesterCount || 0
      infoOverview.inspectionCount = overview.inspectionCount || 0
      infoOverview.passFee = overview.passFee || 0
      infoOverview.passCount = overview.passCount || 0
      infoOverview.failCount = overview.failCount || 0
      infoOverview.uploadCount = overview.uploadCount || 0

      // 时段分布
      hourlyDistribution.value = d.hourlyDistribution || []

      // 车型分布
      vehicleTypeStats.value = d.vehicleTypeStats || []

      // 货物类别
      goodsTypeStats.value = d.goodsTypeStats || []

      // 待办事项
      todoItems.value = d.todoItems || []

      // 文件通知
      notices.value = d.notices || []

      // 数据同步
      const exempt = d.exemptRate || {}
      exemptRate.total = exempt.total || 0
      exemptRate.exempt = exempt.exempt || 0
      exemptRate.rate = exempt.rate || 0

      // 渲染图表
      await nextTick()
      renderLineChart()
      renderBarChart()
      renderPieChart()
    } else {
      ElMessage.error(res.message || '加载统计数据失败')
    }
  } catch {
    ElMessage.error('加载首页数据失败')
  }
}

// ================================================================
// 时间切换处理
// ================================================================

const handleTimeTypeChange = () => {
  loadData()
}

// ================================================================
// 工具函数
// ================================================================

/** 格式化金额 */
const formatMoney = (value) => {
  if (!value) return '0.00'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 获取待办事项标签类型 */
const getTagType = (type) => {
  switch (type) {
    case 'pending_review': return 'warning'
    case 'fake_green': return 'danger'
    case 'upload_failed': return 'danger'
    default: return 'info'
  }
}

/** 获取通知标签类型 */
const getNoticeTagType = (type) => {
  switch (type) {
    case 'important': return 'danger'
    case 'policy': return 'primary'
    case 'report': return 'success'
    default: return 'info'
  }
}

/** 获取通知类型文本 */
const getNoticeTypeText = (type) => {
  switch (type) {
    case 'important': return '重要'
    case 'policy': return '政策'
    case 'report': return '报告'
    default: return '通知'
  }
}

/** 获取数据同步颜色 */
const getExemptColor = (rate) => {
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
}

// ================================================================
// 图表渲染
// ================================================================

/**
 * 渲染时段分析折线图
 */
const renderLineChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  const data = hourlyDistribution.value

  // 判断是按小时还是按天显示
  const isHourly = data.length > 0 && data[0].hour !== undefined

  let labels, values
  if (isHourly) {
    // 按小时：生成 0-23 小时标签
    const hourMap = {}
    data.forEach(d => {
      hourMap[d.hour] = d.count || 0
    })
    labels = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
    values = labels.map((_, i) => hourMap[i] || 0)
  } else {
    // 按天
    labels = data.map(d => d.label || d.date)
    values = data.map(d => d.count || 0)
  }

  // 确保有数据
  if (labels.length === 0) {
    labels = isHourly ? Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`) : ['暂无数据']
    values = isHourly ? Array(24).fill(0) : [0]
  }

  lineChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => `${params[0].axisValue}<br/>查验量：<b>${params[0].data}</b> 辆`
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        interval: isHourly ? 2 : 'auto',
        fontSize: 10,
        color: '#909399',
        rotate: !isHourly ? 30 : 0
      },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      name: '查验量',
      nameTextStyle: { fontSize: 10, color: '#909399' },
      axisLabel: { fontSize: 10, color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLine: { show: true, lineStyle: { color: '#e4e7ed' } }
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: 1, color: 'rgba(64,158,255,0.05)' }
        ])
      }
    }]
  })
}

/**
 * 渲染车型分布横向条形图
 */
const renderBarChart = () => {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const data = vehicleTypeStats.value
  const barData = data.map(item => ({
    name: item.type || '未知',
    value: item.count || 0
  }))

  // 车型名称映射
  const typeMap = {
    '11': '一型货车',
    '12': '二型货车',
    '13': '三型货车',
    '14': '四型货车',
    '15': '五型货车',
    '16': '六型货车'
  }
  barData.forEach(item => {
    if (typeMap[item.name]) {
      item.name = typeMap[item.name]
    }
  })

  // 确保有数据
  if (barData.length === 0) {
    barData.push({ name: '暂无数据', value: 0 })
  }

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}: {c} 辆'
    },
    grid: { left: 80, right: 20, top: 10, bottom: 10 },
    xAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    yAxis: {
      type: 'category',
      data: barData.map(d => d.name).reverse(),
      axisLabel: { fontSize: 11, color: '#606266' },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    series: [{
      type: 'bar',
      data: barData.map(d => d.value).reverse(),
      barWidth: 16,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#67c23a' },
          { offset: 1, color: '#85ce61' }
        ])
      }
    }]
  })
}

/**
 * 渲染货物类别饼图
 */
const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  const data = goodsTypeStats.value
  const pieData = data.map(item => ({
    name: item.goodsTypeName || item.name || '未知',
    value: item.count || 0
  }))

  // 饼图颜色配置
  const colors = [
    '#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
    '#c71585', '#ff8c00', '#00ced1', '#9370db', '#20b2aa',
    '#ff69b4', '#32cd32', '#daa520', '#4682b4', '#cd5c5c'
  ]

  // 确保有数据
  if (pieData.length === 0) {
    pieData.push({ name: '暂无数据', value: 0 })
  }

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}<br/>${p.value} 辆（${p.percent}%）`
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 5,
      top: 10,
      bottom: 10,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 6,
      textStyle: { fontSize: 11, color: '#606266' },
      pageIconSize: 12,
      pageIconColor: '#909399',
      pageIconInactiveColor: '#dcdfe6',
      pageTextStyle: { fontSize: 10, color: '#909399' }
    },
    series: [{
      type: 'pie',
      radius: ['30%', '65%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold' }
      },
      data: pieData.map((item, index) => ({
        ...item,
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  })
}

// ================================================================
// 生命周期
// ================================================================

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  barChart?.dispose()
  pieChart?.dispose()
})

const handleResize = () => {
  lineChart?.resize()
  barChart?.resize()
  pieChart?.resize()
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.dashboard {
  padding: 20px 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

/* ========== 左侧区域 ========== */
.left-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 右侧区域 ========== */
.right-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== 区块 ========== */
.section-block {
  margin-bottom: 0;
}

/* ========== 区块标题 ========== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* ========== 通用卡片 ========== */
.stat-card, .panel-card, .chart-card {
  border-radius: 10px;
  border: none;
  transition: box-shadow 0.3s;
}
.stat-card:hover, .panel-card:hover, .chart-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important;
}

/* 信息总览卡片固定高度，保证三卡片一致 */
.stat-card :deep(.el-card__body) {
  min-height: 120px;
  display: flex;
  align-items: center;
  overflow: visible;
}

/* 通行费用金额，预留13位数空间 */
.stat-card .stat-value.large {
  font-size: 24px;
  word-break: break-all;
  word-wrap: break-word;
}

/* ========== 核心指标行 ========== */
.stat-row { margin-bottom: 0; }

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 图标圆形背景 */
.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-icon-wrap.blue  { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon-wrap.green { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon-wrap.amber { background: linear-gradient(135deg, #e6a23c, #f0c78a); }

.stat-body { flex: 1; min-width: 0; }

/* 多值展示行 */
.stat-value-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.stat-value-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-value-item .value {
  font-size: 26px;
  font-weight: 800;
  color: #303133;
  line-height: 1.2;
}
.stat-value-item .label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.stat-value-item.success .value { color: #67c23a; }
.stat-value-item.danger .value { color: #f56c6c; }

.stat-divider {
  color: #dcdfe6;
  font-size: 20px;
}

/* 单值展示 */
.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #303133;
  line-height: 1.2;
}
.stat-value.large {
  font-size: 32px;
}

.stat-label {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  text-align: center;
}

/* ========== 面板标题 ========== */
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

/* ========== 图表容器 ========== */
.chart-container-line {
  height: 240px;
  width: 100%;
}

.chart-container-bar {
  height: 220px;
  width: 100%;
}

.chart-container-pie {
  height: 220px;
  width: 100%;
}

/* ========== 待办事项 ========== */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
}
.todo-item.pending_review { background: #fdf6ec; border: 1px solid #faecd8; }
.todo-item.fake_green { background: #fef0f0; border: 1px solid #fde2e2; }

.todo-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-title {
  font-size: 13px;
  color: #303133;
}

/* ========== 文件通知 ========== */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}
.notice-item:last-child { border-bottom: none; }

.notice-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.notice-title {
  font-size: 13px;
  color: #303133;
  line-height: 1.4;
}

.notice-date {
  font-size: 11px;
  color: #909399;
}

/* ========== 数据同步 ========== */
.exempt-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.exempt-circle {
  display: flex;
  align-items: center;
  justify-content: center;
}

.exempt-value {
  font-size: 24px;
  font-weight: 800;
  color: #303133;
}

.exempt-stats {
  display: flex;
  gap: 24px;
}

.exempt-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.exempt-stat .stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}
.exempt-stat .stat-num.success {
  color: #67c23a;
}

.exempt-stat .stat-label {
  font-size: 12px;
  color: #909399;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: #909399;
  font-size: 13px;
}
</style>