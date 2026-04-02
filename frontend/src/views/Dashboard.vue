<template>
  <!--
    首页概览 Dashboard
    布局结构：
      第一行（四列）：核心指标卡片
      第二行（三栏）：预警看板 + 实时流水
      第三行（三栏）：时段分布折线图 + 货物类别饼图 + 查验结果环形图
  -->
  <div class="dashboard">

    <!-- ================================================================
         第一行：核心指标卡片（顶部实时状态）
         4列等宽，el-col:span=6
    ================================================================ -->
    <el-row :gutter="20" class="stat-row">

      <!-- 今日查验总数 -->
      <el-col :span="6">
        <el-card class="stat-card pass-card" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon-wrap blue">
              <el-icon size="28"><Van /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">今日查验总数</div>
              <div class="stat-sub">累计快检设备通过车辆</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 合格数 | 不合格数（对比展示） -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon-wrap green">
              <el-icon size="28"><CircleCheck /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value contrast-row">
                <span class="pass-num">{{ stats.passCount }}</span>
                <span class="separator">|</span>
                <span class="fail-num">{{ stats.failCount }}</span>
              </div>
              <div class="stat-label">合格 | 不合格</div>
              <div class="stat-sub">今日查验结果对比</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 待复核数（需人工确认） -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon-wrap amber">
              <el-icon size="28"><Bell /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value" :class="{ 'text-amber': stats.pendingReviewCount > 0 }">
                {{ stats.pendingReviewCount }}
              </div>
              <div class="stat-label">待复核数</div>
              <div class="stat-sub">需人工确认的非规则数据</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 实时在线设备（模拟状态） -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon-wrap" :class="deviceOnline === true ? 'green' : deviceOnline === false ? 'red' : 'gray'">
              <el-icon size="28"><Monitor /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value device-status">
                <span v-if="deviceOnline !== null" class="status-dot" :class="deviceOnline ? 'online' : 'offline'"></span>
                {{ deviceOnline === null ? '检测中' : deviceOnline ? '在线' : '离线' }}
              </div>
              <div class="stat-label">快检设备状态</div>
              <div class="stat-sub">
                <template v-if="deviceOnline === null">正在检测设备状态...</template>
                <template v-else-if="deviceOnline === true">设备运行正常</template>
                <template v-else>设备连接异常，请检查</template>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ================================================================
         第二行：预警看板（左）+ 实时动态流水（右），等高布局
    ================================================================ -->
    <el-row :gutter="20" class="content-row">

      <!-- 左：预警看板（16宽） -->
      <el-col :span="16">
        <el-card class="panel-card equal-height" shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">
                <el-icon color="#f56c6c"><WarningFilled /></el-icon>
                预警看板
              </span>
            </div>
          </template>

          <!-- 待复核清单 -->
          <div class="alert-section">
            <div class="alert-section-title">
              <el-icon color="#faad14"><Bell /></el-icon>
              待复核清单
              <el-tag size="small" type="warning" style="margin-left:8px">
                {{ pendingReviews.length }} 条
              </el-tag>
            </div>
            <div v-if="pendingReviews.length === 0" class="empty-state">
              <el-icon size="40" color="#dcdfe6"><SuccessFilled /></el-icon>
              <span>当前暂无待复核车辆</span>
            </div>
            <div v-else class="alert-list">
              <div
                v-for="item in pendingReviews"
                :key="item.id"
                class="alert-item"
                @click="showDetail(item)"
              >
                <div class="alert-left">
                  <el-tag size="small" type="warning" effect="dark">待复核</el-tag>
                  <span class="alert-plate">{{ item.plateNumber }}</span>
                  <el-tag v-if="item.plateNumberGc" size="small">{{ item.plateNumberGc }}</el-tag>
                </div>
                <div class="alert-right">
                  <span class="alert-time">{{ item.inspectionTime || '-' }}</span>
                  <span class="alert-arrow">›</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 假冒绿通黑名单预警 -->
          <div class="alert-section" style="margin-top:16px">
            <div class="alert-section-title">
              <el-icon color="#f56c6c"><WarningFilled /></el-icon>
              假冒绿通预警
              <el-tag size="small" type="danger" effect="dark" style="margin-left:8px">
                {{ fakeGreenAlerts.length }} 条
              </el-tag>
            </div>
            <div v-if="fakeGreenAlerts.length === 0" class="empty-state">
              <el-icon size="40" color="#dcdfe6"><CircleCheck /></el-icon>
              <span>当前暂无假冒绿通预警</span>
            </div>
            <div v-else class="alert-list">
              <div
                v-for="item in fakeGreenAlerts"
                :key="item.id"
                class="alert-item danger"
                @click="showDetail(item)"
              >
                <div class="alert-left">
                  <el-tag size="small" type="danger" effect="dark">假冒绿通</el-tag>
                  <span class="alert-plate danger">{{ item.plateNumber }}</span>
                  <el-tag v-if="item.plateNumberGc" size="small">{{ item.plateNumberGc }}</el-tag>
                </div>
                <div class="alert-right">
                  <span class="alert-reason">{{ item.nopassTypeText }}</span>
                  <span class="alert-arrow">›</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：实时动态流水（8宽） -->
      <el-col :span="8">
        <el-card class="panel-card equal-height" shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">
                <el-icon color="#409eff"><Timer /></el-icon>
                实时动态
              </span>
              <span class="panel-sub">最近查验记录</span>
            </div>
          </template>

          <div v-if="recentRecords.length === 0" class="empty-state" style="padding:40px 0">
            <el-icon size="40" color="#dcdfe6"><Document /></el-icon>
            <span>暂无查验记录</span>
          </div>
          <div v-else class="stream-list">
            <div
              v-for="item in recentRecords"
              :key="item.id"
              class="stream-item"
              @click="showDetail(item)"
            >
              <div class="stream-top">
                <span class="stream-plate">{{ item.plateNumber }}</span>
                <el-tag
                  size="small"
                  :type="item.resultStatus === 1 ? 'success' : item.resultStatus === 2 ? 'danger' : 'info'"
                  effect="dark"
                >
                  {{ item.resultStatusText }}
                </el-tag>
              </div>
              <div class="stream-bottom">
                <span class="stream-goods">{{ item.goodsTypeName || item.goodsType || '-' }}</span>
                <span class="stream-time">{{ item.inspectionTime ? item.inspectionTime.slice(11, 16) : '-' }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ================================================================
         第三行：查验时段分布折线图 + 查验结果环形图
         折线图16宽放左侧，环形图8宽放右侧
    ================================================================ -->
    <el-row :gutter="20" class="content-row">

      <!-- 左：查验时段分布折线图（16宽） -->
      <el-col :span="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">
                <el-icon color="#409eff"><DataLine /></el-icon>
                查验时段分布
              </span>
              <span class="panel-sub">24小时查验量走势</span>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 右：查验结果环形图（8宽） -->
      <el-col :span="8">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">
                <el-icon color="#e6a23c"><DataAnalysis /></el-icon>
                查验结果分布
              </span>
              <span class="panel-sub">合格 vs 不合格</span>
            </div>
          </template>
          <div ref="ringChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ================================================================
         详情弹窗
    ================================================================ -->
    <InspectionDetail
      v-model="detailVisible"
      :row="currentRow"
      @refresh="loadData"
    />
  </div>
</template>

<script setup>
/**
 * Dashboard 首页概览
 *
 * 【布局结构】
 *   第一行（4列）：核心指标卡片
 *   第二行（16+8）：预警看板 + 实时动态流水
 *   第三行（16+8）：时段分布折线图 + 货物类别饼图
 *   第四行（8）  ：查验结果环形图
 *
 * 【色彩语义化规范】
 *   合格/正常：#52c41a（绿色）
 *   待办/预警：#faad14（琥珀色）
 *   不合格/离线：#f5222d（红色）
 *   信息/蓝    ：#409eff（蓝色）
 *
 * 【数据来源】
 *   通过 /api/inspection/dashboard 一次性加载全部统计数据，
 *   减少请求次数，提升页面加载速度。
 *
 * 【图表库】
 *   使用 ECharts（echarts npm 包），通过 ref 获取 DOM 实例，
 *   在 onMounted 后初始化图表，数据更新时调用 setOption() 刷新。
 */

import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import {
  Van, CircleCheck, Bell, Monitor, WarningFilled,
  SuccessFilled, Timer, Document, DataLine,
  DataAnalysis
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/vehicleInspection'
import InspectionDetail from '@/components/InspectionDetail.vue'

// ================================================================
// 状态定义
// ================================================================

/** 今日统计数据 */
const stats = reactive({
  total: 0,
  passCount: 0,
  failCount: 0,
  pendingReviewCount: 0
})

/** 待复核清单 */
const pendingReviews = ref([])

/** 假冒绿通预警列表 */
const fakeGreenAlerts = ref([])

/** 最近查验记录（实时流水） */
const recentRecords = ref([])

/** 图表数据 */
const hourlyDistribution = ref([])  // 时段分布原始数据

/** 设备在线状态（null=未知/检测中，true=在线，false=离线） */
const deviceOnline = ref(null)

// ================================================================
// 详情弹窗
// ================================================================

const detailVisible = ref(false)
const currentRow = ref({})

/**
 * 点击预警项或流水项，显示详情弹窗
 */
const showDetail = (item) => {
  currentRow.value = { ...item }
  detailVisible.value = true
}

// ================================================================
// 图表 DOM refs
// ================================================================

const lineChartRef = ref(null)
const ringChartRef = ref(null)

// ECharts 实例（避免重复创建）
let lineChart = null
let ringChart = null

// ================================================================
// 数据加载
// ================================================================

/**
 * loadData：加载 Dashboard 全部统计数据
 *
 * 后端 /api/inspection/dashboard 一次性返回所有数据，
 * 避免前端多次请求。
 */
const loadData = async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200) {
      const d = res.data

      // 今日统计
      const t = d.todayStats || {}
      stats.total            = t.total            || 0
      stats.passCount         = t.passCount         || 0
      stats.failCount         = t.failCount         || 0
      stats.pendingReviewCount = t.pendingReviewCount || 0

      // 预警数据
      pendingReviews.value  = d.pendingReviews  || []
      fakeGreenAlerts.value = d.fakeGreenAlerts || []

      // 实时流水
      recentRecords.value = d.recentRecords || []

      // 图表原始数据
      hourlyDistribution.value = d.hourlyDistribution || []

      // 渲染图表
      await nextTick()
      renderLineChart()
      renderRingChart()
    } else {
      ElMessage.error(res.message || '加载统计数据失败')
    }
  } catch {
    ElMessage.error('加载首页数据失败')
  }
}

// ================================================================
// 图表渲染
// ================================================================

/**
 * 渲染查验时段分布折线图
 * X轴：00:00 ~ 23:00
 * Y轴：查验数量
 * 当前小时之前的区域用渐变填充
 */
const renderLineChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  const data = hourlyDistribution.value
  const hours = data.map(d => d.label)          // ["00:00", "01:00", ...]
  const values = data.map(d => d.count || 0)    // [3, 0, 5, ...]
  const currentHour = new Date().getHours()

  lineChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => `${params[0].axisValue}<br/>查验量：<b>${params[0].data}</b> 辆`
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: {
        interval: 2,
        fontSize: 10,
        color: '#909399'
      },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLine: { show: false },
      axisTick: { show: false }
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
        // 渐变填充：当前小时之前蓝色，之后灰色
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: currentHour / 23, color: 'rgba(64,158,255,0.1)' },
          { offset: 1, color: 'rgba(200,200,200,0.05)' }
        ])
      }
    }]
  })
}

/**
 * 渲染查验结果环形图（合格 vs 不合格）
 */
const renderRingChart = () => {
  if (!ringChartRef.value) return
  if (!ringChart) ringChart = echarts.init(ringChartRef.value)

  const passCount = stats.passCount
  const failCount = stats.failCount
  const pendingCount = Math.max(0, stats.total - passCount - failCount)

  ringChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}<br/>${p.value} 辆（${p.percent}%）`
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12, color: '#606266' }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: [
        { name: '合格', value: passCount, itemStyle: { color: '#52c41a' } },
        { name: '不合格', value: failCount, itemStyle: { color: '#f5222d' } },
        ...(pendingCount > 0 ? [{ name: '待查验', value: pendingCount, itemStyle: { color: '#dcdfe6' } }] : [])
      ]
    }]
  })
}

// ================================================================
// 生命周期
// ================================================================

/**
 * onMounted：组件挂载后初始化
 * - 加载统计数据
 * - 初始化 ECharts 实例
 */
onMounted(() => {
  loadData()

  // 监听窗口大小变化，图表自适应
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // 清理事件监听，防止内存泄漏
  window.removeEventListener('resize', handleResize)
  // 销毁 ECharts 实例，释放内存
  lineChart?.dispose()
  ringChart?.dispose()
})

/** 窗口大小变化处理（供 onMounted / onUnmounted 注册/移除） */
const handleResize = () => {
  lineChart?.resize()
  ringChart?.resize()
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.dashboard {
  padding: 20px 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
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

/* 等高布局：两列卡片内容区最大高度一致，超出滚动 */
.panel-card.equal-height :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  max-height: 420px;
  overflow-y: auto;
}

/* ========== 核心指标行 ========== */
.stat-row { margin-bottom: 16px; }

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
.stat-icon-wrap.green { background: linear-gradient(135deg, #52c41a, #73d13d); }
.stat-icon-wrap.amber { background: linear-gradient(135deg, #faad14, #ffc53d); }
.stat-icon-wrap.red   { background: linear-gradient(135deg, #f5222d, #ff4d4f); }
.stat-icon-wrap.gray  { background: linear-gradient(135deg, #909399, #a6a9ad); }

.stat-body { flex: 1; min-width: 0; }

.stat-value {
  font-size: 30px;
  font-weight: 800;
  color: #303133;
  line-height: 1.2;
}

/* 合格/不合格对比展示 */
.contrast-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pass-num { color: #52c41a; font-size: 28px; }
.separator { color: #dcdfe6; font-size: 20px; }
.fail-num { color: #f5222d; font-size: 28px; }

.stat-label {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
  margin-top: 4px;
}
.stat-sub {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 设备状态 */
.device-status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.status-dot.online {
  background: #52c41a;
  box-shadow: 0 0 6px #52c41a;
  animation: pulse 2s infinite;
}
.status-dot.offline {
  background: #f5222d;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.text-amber { color: #faad14 !important; }

/* ========== 内容行 ========== */
.content-row { margin-bottom: 16px; }

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
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.panel-sub {
  font-size: 12px;
  color: #909399;
}

/* ========== 预警看板 ========== */

/* 分区标题 */
.alert-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

/* 空状态 */
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

/* 预警列表 */
.alert-list { display: flex; flex-direction: column; gap: 6px; }

.alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
}
.alert-item:hover { background: #ffe7ba; transform: translateX(2px); }
.alert-item.danger {
  background: #fff1f0;
  border-color: #ffa39e;
}
.alert-item.danger:hover { background: #ffcdc7; }

.alert-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.alert-plate {
  font-weight: 700;
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: #303133;
}
.alert-plate.danger { color: #f5222d; }

.alert-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.alert-time {
  font-size: 11px;
  color: #909399;
  font-family: 'Consolas', monospace;
}
.alert-reason {
  font-size: 11px;
  color: #f5222d;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.alert-arrow {
  font-size: 16px;
  color: #dcdfe6;
  font-weight: bold;
}

/* ========== 实时动态流水 ========== */
.stream-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  /* 固定高度，超出滚动 */
  max-height: 400px;
  overflow-y: auto;
  /* 美化滚动条 */
  scrollbar-width: thin;
  overflow-x: hidden;  /* ← 新增：隐藏水平溢出 */
  scrollbar-color: #c0c4cc transparent;
}
.stream-list::-webkit-scrollbar { width: 4px; }
.stream-list::-webkit-scrollbar-track { background: transparent; }
.stream-list::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 2px; }
.stream-list::-webkit-scrollbar-thumb:hover { background: #a8abb2; }

.stream-item {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  /* 防止被压缩 */
  flex-shrink: 0;
}
.stream-item:hover {
  background: #ecf5ff;
  transform: translateX(2px);
}

.stream-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.stream-plate {
  font-weight: 700;
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: #303133;
}
.stream-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stream-goods {
  font-size: 11px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}
.stream-time {
  font-size: 11px;
  color: #c0c4cc;
  font-family: 'Consolas', monospace;
  flex-shrink: 0;
}

/* ========== 图表区域 ========== */
.chart-container {
  height: 220px;
  width: 100%;
}
</style>
