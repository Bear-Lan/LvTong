<template>
    <div class="panel">
        <h3>{{ title }}</h3>
        <div ref="chartRef" class="chart"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'

// ---- props ----
const props = defineProps<{
    title: string
    data: { name: string; count: number }[]
}>()

// ---- 武汉坐标 ----
const wuhan = [114.305392, 30.593098]

// ---- 全国城市坐标 ----
const cityCoords: Record<string, number[]> = {
    北京: [116.407526, 39.90403],
    上海: [121.473701, 31.230416],
    广州: [113.264385, 23.129112],
    深圳: [114.057868, 22.543099],
    成都: [104.066541, 30.572269],
    西安: [108.93977, 34.341574],
    重庆: [106.551556, 29.563009],
    杭州: [120.15507, 30.274084],
    南京: [118.796877, 32.060255],
    沈阳: [123.431474, 41.805698],
    哈尔滨: [126.642464, 45.756967],
    昆明: [102.832891, 24.880095],
    乌鲁木齐: [87.616848, 43.825592],
    吐鲁番: [89.189651, 42.951301],
    青海: [101.780199, 36.620901],
    西藏: [91.140856, 29.645554]
}

const topColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
// const topColors = [
//     '#052e16', // 红色 - 第1名
//     '#166534', // 橙色 - 第2名
//     '#16a34a', // 黄色 - 第3名
//     '#4ade80', // 紫色 - 第4名
//     '#bbf7d0', // 蓝色 - 第5名
//     '#f0fdf4'  // 绿色 - 其他
// ]
// const topColors = [
//     '#ff0000', // 红色 - 第1名
//     '#ff6b00', // 橙色 - 第2名
//     '#ffd500', // 黄色 - 第3名
//     '#a855f7', // 紫色 - 第4名
//     '#3b82f6', // 蓝色 - 第5名
//     '#4ade80'  // 绿色 - 其他
// ]

// ---- 修正后的飞线数据（按数量排序并分配颜色） ----
const linesData = computed(() => {
    // 先按数量排序
    const sortedData = [...props.data].filter(item => cityCoords[item.name]).sort((a, b) => b.count - a.count)

    return sortedData.map((item, index) => ({
        coords: [cityCoords[item.name], wuhan],
        fromName: item.name,
        toName: "武汉",
        value: item.count,
        lineStyle: {
            width: Math.max(1, item.count / 50),
            color: index < 5 ? topColors[index] : topColors[5] // 前5名用不同颜色
        }
    }))
})

// ---- 城市数据（用于显示城市名称） ----
const cityData = computed(() => {
    const sortedData = [...props.data].filter(item => cityCoords[item.name]).sort((a, b) => b.count - a.count)

    return sortedData.map((item, index) => ({
        name: item.name,
        value: cityCoords[item.name],
        count: item.count,
        symbolSize: Math.min(20, Math.max(8, item.count / 15)),
        itemStyle: {
            color: index < 5 ? topColors[index] : topColors[5]
        }
    }))
})

// ---- ECharts DOM 引用 ----
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

// ---- 初始化图表 ----
const initChart = () => {
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    chart.setOption(getOption())
}

// ---- 动态 option ----
const getOption = () => ({
    backgroundColor: "transparent",
    geo: {
        map: "china",
        roam: true,
        zoom: 1.2,
        layoutSize: '110%',
        itemStyle: { areaColor: "#184b70B3", borderColor: "#4ea3ff" },
        emphasis: { label: { color: "#ff0" }, itemStyle: { areaColor: "#0d2238B3" } }
    },
    visualMap: {
        type: 'piecewise',
        show: true,
        orient: 'vertical',
        right: 5,
        // top: 'center',
        inverse: true,
        pieces: [
            { min: 1, max: 1, label: '第1', color: topColors[0] },
            { min: 2, max: 2, label: '第2', color: topColors[1] },
            { min: 3, max: 3, label: '第3', color: topColors[2] },
            { min: 4, max: 4, label: '第4', color: topColors[3] },
            { min: 5, max: 5, label: '第5', color: topColors[4] },
            { min: 6, label: '其他', color: topColors[5] }
        ],
        textStyle: { color: '#fff', align: 'right' },
        itemSymbol: 'roundRect',
        seriesIndex: 1 // 关联到城市系列
    },
    series: [
        {
            type: "lines",
            coordinateSystem: "geo",
            zlevel: 2,
            effect: {
                show: true,
                period: 2 + Math.random() * 3,
                trailLength: 0.15,
                symbol: "arrow",
                symbolSize: 7,
            },
            lineStyle: {
                opacity: 0.7,
                curveness: 0.2,
                color: function (params: any) {
                    // 动态设置线条颜色
                    const sortedData = [...props.data].filter(item => cityCoords[item.name]).sort((a, b) => b.count - a.count)
                    const index = sortedData.findIndex(item => item.name === params.data.fromName)
                    return index < 5 ? topColors[index] : topColors[5]
                }
            },
            data: linesData.value
        },
        {
            type: "effectScatter",
            coordinateSystem: "geo",
            data: cityData.value,
            symbolSize: function (val: any) {
                return val.symbolSize
            },
            rippleEffect: {
                brushType: "stroke",
                scale: 2.5
            },
            label: {
                show: true,
                position: 'right',
                formatter: '{b}',
                color: '#fff', // 白色文字
                fontSize: 12,
                fontWeight: 'bold',
                backgroundColor: 'rgba(0, 0, 0, 0.6)', // 半透明黑色背景
                padding: [2, 6],
                borderRadius: 3,
                borderColor: 'rgba(255, 255, 255, 0.3)',
                borderWidth: 1
            },
            itemStyle: {
                color: function (params: any) {
                    return params.data.itemStyle.color
                }
            },
            emphasis: {
                label: {
                    show: true,
                    fontSize: 14,
                    // backgroundColor: 'rgba(0, 0, 0, 0.8)'
                },
                scale: true
            }
        },
        {
            type: "effectScatter",
            coordinateSystem: "geo",
            data: [{ name: "武汉", value: wuhan }],
            symbolSize: 18,
            rippleEffect: {
                brushType: "stroke",
                scale: 3
            },
            label: {
                show: true,
                position: 'right',
                formatter: '武汉',
                color: '#fff',
                fontSize: 14,
                fontWeight: 'bold',
                backgroundColor: 'rgba(220, 38, 38, 0.8)',
                padding: [4, 8],
                borderRadius: 4
            },
            itemStyle: { color: "#ff0000" }
        }
    ]
})

// ---- 监听 props 数据变化 ----
watch(
    () => props.data,
    () => { chart?.setOption(getOption(), true) },
    { deep: true }
)

// ---- 窗口自适应 ----
const resizeChart = () => chart?.resize()

// ---- onMounted ----
onMounted(() => {
    fetch('/GreenChannel/data/china.json').then(res => res.json()).then(chinaGeoJson => {
        echarts.registerMap('china', chinaGeoJson)
        initChart()
        window.addEventListener("resize", resizeChart)
    })
})

// ---- onUnmounted ----
onUnmounted(() => {
    window.removeEventListener("resize", resizeChart)
    chart?.dispose()
})
</script>

<style scoped></style>