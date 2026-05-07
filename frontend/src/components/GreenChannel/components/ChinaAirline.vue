<template>
    <div class="container">
        <h3>{{ title }}</h3>
        <div ref="chartRef" class="chart"></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
    title: string
    data: { name: string; count: number }[]
}>()

const wuhan = [114.305392, 30.593098]

const cityCoords: Record<string, number[]> = {
    北京市: [116.407526, 39.90403],
    天津市: [117.201587, 39.084158],
    河北省: [114.51486, 38.04228],
    山西省: [112.549248, 37.857014],
    内蒙古自治区: [111.765617, 40.817498],
    辽宁省: [123.431474, 41.805698],
    吉林省: [125.324636, 43.886841],
    黑龙江省: [126.642464, 45.756967],
    上海市: [121.473701, 31.230416],
    江苏省: [118.796877, 32.060255],
    浙江省: [120.15507, 30.274084],
    安徽省: [117.284124, 31.86119],
    福建省: [119.296472, 26.074507],
    江西省: [115.858098, 28.682892],
    山东省: [120.416612, 36.066407],
    河南省: [113.274379, 34.445213],
    湖北省: [114.305392, 30.593098],
    湖南省: [112.98324, 28.112444],
    广东省: [113.264385, 23.129112],
    广西壮族自治区: [108.327546, 22.817532],
    海南省: [110.19989, 20.044194],
    重庆市: [106.551556, 29.563009],
    四川省: [104.066541, 30.572269],
    贵州省: [106.70739, 26.598194],
    云南省: [102.832891, 24.880095],
    西藏自治区: [91.140856, 29.645554],
    陕西省: [108.93977, 34.341574],
    甘肃省: [103.826308, 36.059539],
    青海省: [101.780199, 36.620901],
    宁夏回族自治区: [106.27839, 38.466372],
    新疆维吾尔自治区: [87.616848, 43.825592],
    台湾省: [121.50906, 25.044903],
    香港特别行政区: [114.171126, 22.277526],
    澳门特别行政区: [113.549708, 22.192921]
}

const topColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']

const linesData = computed(() => {
    const sortedData = [...props.data].filter(item => cityCoords[item.name] && item.name !== '湖北省').sort((a, b) => b.count - a.count).slice(0, 5)

    return sortedData.map((item, index) => ({
        coords: [cityCoords[item.name], wuhan],
        fromName: item.name,
        toName: "武汉",
        value: item.count,
        lineStyle: {
            width: Math.max(1, item.count / 50),
            color: topColors[index]
        }
    }))
})

const cityData = computed(() => {
    const sortedData = [...props.data].filter(item => cityCoords[item.name] && item.name !== '湖北省').sort((a, b) => b.count - a.count).slice(0, 5)

    return sortedData.map((item, index) => ({
        name: item.name,
        value: cityCoords[item.name],
        count: item.count,
        symbolSize: Math.min(20, Math.max(8, item.count / 15)),
        itemStyle: {
            color: topColors[index]
        }
    }))
})

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const initChart = () => {
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    chart.setOption(getOption())
}

const getOption = () => {
    const top5Data = cityData.value
    const visualPieces = top5Data.map((item: any, index: number) => ({
        min: index + 1,
        max: index + 1,
        label: `第${index + 1}:${item.name}`,
        textStyle: { color: topColors[index] }
    }))

    return {
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
            inverse: true,
            pieces: visualPieces,
            textStyle: { color: '#fff', align: 'right' },
            itemSymbol: 'roundRect',
            seriesIndex: 1
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
                        return params.data.lineStyle.color
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
                    color: '#fff',
                    fontSize: 12,
                    fontWeight: 'bold',
                    backgroundColor: 'rgba(0, 0, 0, 0.6)',
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
                        scale: true
                    }
                }
            },
            {
                type: "effectScatter",
                coordinateSystem: "geo",
                data: [{ name: "毛陈", value: wuhan }],
                symbolSize: 18,
                rippleEffect: {
                    brushType: "stroke",
                    scale: 3
                },
                label: {
                    show: true,
                    position: 'right',
                    formatter: '毛陈',
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
    }
}

watch(
    () => props.data,
    () => { chart?.setOption(getOption(), true) },
    { deep: true }
)

const resizeChart = () => chart?.resize()

onMounted(() => {
    fetch('/GreenChannel/data/china.json').then(res => res.json()).then(chinaGeoJson => {
        echarts.registerMap('china', chinaGeoJson)
        initChart()
        window.addEventListener("resize", resizeChart)
    })
})

onUnmounted(() => {
    window.removeEventListener("resize", resizeChart)
    chart?.dispose()
})
</script>

<style scoped>
.container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
}
</style>
