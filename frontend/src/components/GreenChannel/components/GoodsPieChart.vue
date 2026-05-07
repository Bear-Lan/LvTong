<template>
    <div class="panel">
        <h3>{{ title }}</h3>
        <div ref="chartRef" class="chart"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
    title: string
    data: { name: string; count: number }[]
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const colors = [
    '#5470c6B3', '#91cc75B3', '#fac858B3', '#ee6666B3', '#73c0deB3', '#3ba272B3', '#fc8452B3', '#9a60b4B3', '#ea7cccB3'
]

const initChart = () => {
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    chart.setOption(getOption())
}

const getOption = () => {
    const pieData = props.data.slice(0, 8).map((item, index) => ({
        name: item.name,
        value: item.count,
        itemStyle: { color: colors[index % colors.length] }
    }))

    if (pieData.length === 0) {
        pieData.push({ name: '暂无数据', value: 0, itemStyle: { color: '#666' } })
    }

    return {
        backgroundColor: 'transparent',
        tooltip: {
            trigger: 'item',
            formatter: (p: any) => `${p.name}<br/>${p.value} 辆（${p.percent}%）`
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
            textStyle: { color: '#cfe', fontSize: 11 },
            pageIconSize: 12,
            pageIconColor: '#909399',
            pageIconInactiveColor: '#dcdfe6',
            pageTextStyle: { color: '#cfe', fontSize: 10 }
        },
        series: [{
            type: 'pie',
            radius: ['30%', '65%'],
            center: ['35%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 4 },
            label: { show: false },
            emphasis: {
                label: { show: true, fontSize: 12, fontWeight: 'bold', color: '#fff' }
            },
            data: pieData
        }]
    }
}

watch(
    () => props.data,
    () => { chart?.setOption(getOption(), true) },
    { deep: true }
)

const resizeChart = () => chart?.resize()

onMounted(() => {
    initChart()
    window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
    chart?.dispose()
})
</script>

<style scoped>
.panel {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
}
h3 {
    margin: 0 0 8px 0;
    color: #fff;
    font-size: 14px;
    font-weight: bold;
}
.chart {
    flex: 1;
    width: 100%;
    min-height: 0;
}
</style>
