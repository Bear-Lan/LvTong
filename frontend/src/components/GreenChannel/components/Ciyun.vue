<template>
    <div class="panel">
        <h3>{{ title }}</h3>
        <div ref="chartRef" class="chart"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'

// ---- props ----
const props = defineProps<{
    title: string
    data: { name: string; count: number }[]
}>()

// ---- ECharts DOM 引用 ----
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

// ---- 处理词云数据 ----
const wordCloudData = computed(() => {
    // 先按 count 排序取前五
    const sortedData = [...props.data].sort((a, b) => b.count - a.count).slice(0, 70);
    
    return props.data.map(item => {
        const isTop5 = sortedData.some(topItem => topItem.name === item.name);
        
        return {
            name: item.name,
            value: item.count,
            textStyle: {
                color: isTop5 ? getSolidColor() : getRandomColor()
            }
        }
    })
})

// 前五数据使用不透明颜色
const getSolidColor = () => {
    const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']
    return colors[Math.floor(Math.random() * colors.length)]
}

// 其他数据使用稍透明的颜色
const getRandomColor = () => {
    const colors = ['#5470c6aa', '#91cc75aa', '#fac858aa', '#ee6666aa', '#73c0deaa', '#3ba272aa', '#fc8452aa', '#9a60b4aa', '#ea7cccaa']
    return colors[Math.floor(Math.random() * colors.length)]
}

// ---- 初始化图表 ----
const initChart = () => {
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    chart.setOption(getOption())
}

// ---- 词云配置 ----
const getOption = () => ({
    backgroundColor: "transparent",
    tooltip: {
        show: true,
        formatter: (params: any) => {
            return `${params.data.name}: ${params.data.value}`
        }
    },
    series: [{
        type: 'wordCloud',
        shape: 'circle', // 词云形状：'circle', 'cardioid', 'diamond', 'triangle-forward', 'triangle', 'pentagon', 'star'
        left: 'center',
        top: 'center',
        width: '90%',
        height: '90%',
        right: null,
        bottom: null,
        sizeRange: [12, 26], // 文字大小范围
        rotationRange: [-45, 45], // 旋转角度范围
        rotationStep: 45,
        gridSize: 8,
        drawOutOfBound: false,
        textStyle: {
            fontFamily: 'monospace',
            fontWeight: 'bold',
            color: () => {
                return getRandomColor()
            }
        },
        emphasis: {
            focus: 'self',
            textStyle: {
                shadowBlur: 10,
                shadowColor: '#333'
            }
        },
        data: wordCloudData.value
    }]
})

// ---- 监听数据变化 ----
watch(
    () => props.data,
    () => {
        chart?.setOption(getOption(), true)
    },
    { deep: true }
)

// ---- 窗口自适应 ----
const resizeChart = () => chart?.resize()

// ---- 生命周期 ----
onMounted(() => {
    initChart()
    window.addEventListener("resize", resizeChart)
})

onUnmounted(() => {
    window.removeEventListener("resize", resizeChart)
    chart?.dispose()
})
</script>

<style scoped>
.chart {
    width: 100%;
    background: transparent;
}
</style>