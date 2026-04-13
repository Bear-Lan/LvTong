<template>
    <div class="panel">
        <h3>{{ title }}</h3>
        <div class="chart-container">
            <div class="skyplot-wrapper">
                <canvas ref="canvasRef" class="skyplot"></canvas>
            </div>
            <div class="compass">
                <div class="compass-arrow north">北</div>
                <div class="compass-arrow east">东</div>
                <div class="compass-arrow south">南</div>
                <div class="compass-arrow west">西</div>
                <div class="compass-center"></div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, onUnmounted } from 'vue'

const props = defineProps<{ title: string }>()

const canvasRef = ref<HTMLCanvasElement | null>(null)

// 卫星图片映射
const satelliteIcons = {
    'C': new URL('@/assets/GreenChannel/satellite/beidou.png', import.meta.url).href,      // 北斗
    'G': new URL('@/assets/GreenChannel/satellite/gps.png', import.meta.url).href,         // GPS
    'R': new URL('@/assets/GreenChannel/satellite/glonass.png', import.meta.url).href,     // GLONASS
    'E': new URL('@/assets/GreenChannel/satellite/galileo.png', import.meta.url).href      // GALILEO
}

// 卫星数据类型
interface Satellite {
    id: string;
    azimuth: number;    // 方位角 0-360°
    elevation: number;  // 高度角 0-90°
    snr?: number;       // 信噪比（可选）
    inUse?: boolean;    // 是否在用（可选）
}

// 动态生成卫星数据
const generateSatelliteData = (): Satellite[] => {
    const baseSatellites = [
        // 北斗卫星
        { id: 'C01', azimuth: 30, elevation: 75 },
        { id: 'C03', azimuth: 120, elevation: 35 },
        { id: 'C11', azimuth: 210, elevation: 50 },
        { id: 'C19', azimuth: 300, elevation: 65 },
        { id: 'C23', azimuth: 45, elevation: 80 },
        { id: 'C30', azimuth: 150, elevation: 40 },
        { id: 'C38', azimuth: 270, elevation: 55 },
        { id: 'C45', azimuth: 330, elevation: 70 },

        // GPS卫星
        { id: 'G10', azimuth: 270, elevation: 55 },
        { id: 'G15', azimuth: 90, elevation: 45 },
        { id: 'G22', azimuth: 180, elevation: 60 },
        { id: 'G28', azimuth: 0, elevation: 35 },

        // GLONASS卫星
        { id: 'R05', azimuth: 200, elevation: 25 },
        { id: 'R12', azimuth: 320, elevation: 50 },
        { id: 'R18', azimuth: 140, elevation: 65 },

        // GALILEO卫星
        { id: 'E09', azimuth: 340, elevation: 60 },
        { id: 'E15', azimuth: 100, elevation: 40 },
        { id: 'E21', azimuth: 250, elevation: 55 }
    ];

    // 添加随机变化
    return baseSatellites.map(sat => ({
        ...sat,
        azimuth: (sat.azimuth + Math.random() * 10 - 5) % 360,
        elevation: Math.max(5, Math.min(85, sat.elevation + Math.random() * 6 - 3)),
        snr: 25 + Math.random() * 25,
        inUse: Math.random() > 0.1
    }));
};

const satellites = ref<Satellite[]>(generateSatelliteData());

// 预加载所有卫星图标
const loadedImages = ref<Record<string, HTMLImageElement>>({})

// 加载图片
const loadSatelliteIcons = async (): Promise<void> => {
    const promises = Object.entries(satelliteIcons).map(([system, iconSrc]) => {
        return new Promise<void>((resolve) => {
            const img = new Image()
            img.src = iconSrc
            img.onload = () => {
                loadedImages.value[system] = img
                resolve()
            }
            img.onerror = () => {
                console.warn(`Failed to load ${system} satellite icon`)
                // 创建备用图片
                const fallbackImg = new Image()
                fallbackImg.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIxMiIgY3k9IjEyIiByPSIxMCIgZmlsbD0iIzMzMzMzMyIvPjwvc3ZnPg=='
                loadedImages.value[system] = fallbackImg
                resolve()
            }
        })
    })

    await Promise.all(promises)
}

// 绘制卫星点
const drawSatellite = (ctx: CanvasRenderingContext2D, x: number, y: number, satellite: Satellite) => {
    const { id, inUse } = satellite
    const system = id[0] as keyof typeof satelliteIcons

    // 获取对应系统的图标
    const icon = loadedImages.value[system]
    if (!icon) return

    // 图标大小
    const iconSize = 14
    const halfSize = iconSize / 2

    // 保存上下文状态
    ctx.save()

    // 绘制图标
    ctx.drawImage(icon, x - halfSize, y - halfSize, iconSize, iconSize)

    // 如果不在使用中，添加灰色覆盖层
    if (inUse === false) {
        ctx.fillStyle = 'rgba(128, 128, 128, 0.6)'
        ctx.beginPath()
        ctx.arc(x, y, halfSize - 2, 0, Math.PI * 2)
        ctx.fill()

        // 添加禁用符号
        ctx.strokeStyle = '#ff0000'
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.moveTo(x - 8, y - 8)
        ctx.lineTo(x + 8, y + 8)
        ctx.moveTo(x + 8, y - 8)
        ctx.lineTo(x - 8, y + 8)
        ctx.stroke()
    }

    ctx.restore()

    // 绘制卫星ID标签（不同系统使用不同颜色）
    let labelColor = '#fff'
    switch (system) {
        case 'C': labelColor = '#FFDE00' // 北斗金色
            break
        case 'G': labelColor = '#FFD700' // GPS金色
            break
        case 'R': labelColor = '#FF7F50' // GLONASS珊瑚色
            break
        case 'E': labelColor = '#9A66E4' // GALILEO紫色
            break
    }

    ctx.fillStyle = labelColor
    ctx.font = 'bold 10px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'top'
    ctx.shadowColor = 'rgba(0, 0, 0, 0.8)'
    ctx.shadowBlur = 3
    ctx.fillText(id, x, y + halfSize + 4)
    ctx.shadowBlur = 0
}

// 绘制天空图
const drawSkyplot = () => {
    const canvas = canvasRef.value
    if (!canvas) return

    const ctx = canvas.getContext('2d')!
    const width = canvas.width
    const height = canvas.height
    const cx = width / 2
    const cy = height / 2
    const R = Math.min(width, height) * 0.4

    // 清除画布
    ctx.clearRect(0, 0, width, height)

    // 设置样式
    ctx.strokeStyle = '#dcfce71a'
    ctx.fillStyle = '#dcfce7'
    ctx.lineWidth = 1
    ctx.font = '10px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    // 绘制同心圆 (仰角圈)
    for (let e = 0; e <= 90; e += 15) {
        const r = R * (90 - e) / 90
        ctx.beginPath()
        ctx.arc(cx, cy, r, 0, Math.PI * 2)
        ctx.stroke()

        // 绘制仰角标签
        ctx.fillText(`${e}°`, cx, cy - r - 8)
    }

    // 绘制方位角刻度线和标签
    for (let az = 0; az < 360; az += 30) {
        const rad = (az - 90) * Math.PI / 180
        const x = cx + R * Math.cos(rad)
        const y = cy + R * Math.sin(rad)

        // 绘制刻度线
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.lineTo(x, y)
        ctx.stroke()

        // 绘制方位角标签
        const labelX = cx + (R + 15) * Math.cos(rad)
        const labelY = cy + (R + 15) * Math.sin(rad)
        ctx.fillText(`${az}°`, labelX, labelY)
    }

    // 绘制卫星点
    satellites.value.forEach((satellite) => {
        const { azimuth, elevation } = satellite
        const r = R * (90 - elevation) / 90
        const rad = (azimuth - 90) * Math.PI / 180
        const x = cx + r * Math.cos(rad)
        const y = cy + r * Math.sin(rad)

        drawSatellite(ctx, x, y, satellite)
    })
}

// 初始化Canvas尺寸
const initCanvas = async () => {
    const canvas = canvasRef.value
    if (!canvas) return

    const container = canvas.parentElement
    if (!container) return

    const size = Math.min(container.clientWidth, container.clientHeight)
    canvas.width = size
    canvas.height = size

    // 确保所有图标已加载
    if (Object.keys(loadedImages.value).length === 0) {
        await loadSatelliteIcons()
    }

    drawSkyplot()
}

// 定时更新数据
let dataTimer: number
onMounted(async () => {
    await initCanvas()

    // 每3秒更新一次卫星数据
    dataTimer = window.setInterval(() => {
        satellites.value = generateSatelliteData()
        drawSkyplot()
    }, 3000)

    // 监听窗口大小变化
    window.addEventListener('resize', initCanvas)
})

onUnmounted(() => {
    window.removeEventListener('resize', initCanvas)
    clearInterval(dataTimer)
})

// 监听卫星数据变化，重新绘制
watch(satellites, () => {
    drawSkyplot()
}, { deep: true })
</script>

<style scoped>
.chart-container {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 94%;
    position: relative;
}

.skyplot-wrapper {
    position: relative;
    width: 24rem;
    height: 24rem;
}

.skyplot {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    box-shadow: 0 0 20px #4ade80;
    background: rgba(22, 101, 52, 0.3);
}

/* 方向指针样式 */
.compass {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 65px;
    height: 65px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.3);
    display: flex;
    justify-content: center;
    align-items: center;
    background: rgba(0, 0, 0, 0.5);
}

.compass-center {
    width: 8px;
    height: 8px;
    background: #ff0000cc;
    border-radius: 50%;
    position: absolute;
    z-index: 10;
}

.compass-arrow {
    position: absolute;
    font-size: 12px;
    font-weight: bold;
    color: #dcfce7;
    text-shadow: 0 0 2px rgba(0, 0, 0, 0.8);
}

.compass-arrow.north {
    top: 5px;
    left: 50%;
    transform: translateX(-50%);
}

.compass-arrow.east {
    right: 5px;
    top: 50%;
    transform: translateY(-50%);
}

.compass-arrow.south {
    bottom: 5px;
    left: 50%;
    transform: translateX(-50%);
}

.compass-arrow.west {
    left: 5px;
    top: 50%;
    transform: translateY(-50%);
}

/* 添加罗盘指针线 */
.compass::before,
.compass::after {
    content: '';
    position: absolute;
    background: rgba(255, 255, 255, 0.5);
    left: 50%;
    top: 50%;
    transform-origin: center;
}

.compass::before {
    width: 2px;
    height: 60%;
    transform: translate(-50%, -50%);
}

.compass::after {
    width: 60%;
    height: 2px;
    transform: translate(-50%, -50%);
}
</style>