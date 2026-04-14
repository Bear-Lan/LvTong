<template>
    <!-- 顶部标题栏 -->
    <div class="header-bar">
        <div class="header-title">硚孝高速毛陈收费站X射线绿通快检试点项目智慧大屏</div>
        <div class="header-right">
            <span class="weather">{{ currentWeather }}</span>
            <span class="time">{{ currentTime }}</span>
        </div>
    </div>

    <div ref="threeContainer" class="three-container">

        <div class="main">
            <!-- 左列 -->
            <div class="col col-left">
                <div class="panel lfirst" v-show="!loading" style="height: 240px;">
                    <h3>关键指标</h3>
                    <div class="kpi-grid">
                        <StatCard img="cheliangshuliang" label="今日通行车辆" :value="kpis.tadaytotal" />
                        <StatCard img="lvtongche" label="总绿通车辆" :value="kpis.total" />
                        <StatCard img="youhui-jine" label="总通行金额(元)" :value="kpis.discount" />
                        <StatCard img="yichangcheliang" label="伪绿通车辆" :value="kpis.abnormal" />
                    </div>
                </div>
                <div class="panel lsecond" v-if="!loading" style="flex: 1;">
                    <SkyChart title="北斗卫星"></SkyChart>
                </div>
            </div>
            <!-- 中间列 - 在下半部分 -->
            <div class="col-middle">
                <div class="panel middleScroll" v-if="!loading">
                    <LatestPassRecords :list="records" :speed="0.5" :itemHeight="40" />
                </div>
            </div>
            <!-- 右列 -->
            <div class="col col-right">
                <div class="panel rfirst" v-if="!loading" style="flex: 1;">
                    <ChinaAirline title="始发地" :data="airlineData" />
                </div>
                <div class="panel rsecond" v-if="!loading" style="flex: 1;">
                    <RankList title="信用记录" :rows="genRanking" />
                </div>
                <div class="panel rthree" v-if="!loading" style="flex: 1;">
                    <Ciyun :data="goodsCount" title="货物类型" />
                </div>
            </div>
        </div>
    </div>

    <Teleport to="body">
        <div v-if="loading"
            style=" width: 100vw; height: 100vh; background: #515151; position: fixed; top: 0; left: 0; z-index: 999; ">
            <div class="loader"></div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { StatCard, SkyChart, RankList, ChinaAirline, Ciyun, LatestPassRecords } from '@/components/GreenChannel'
import { getKpiData, getPassRecords, getCreditRanking, getGoodsTypeCloud } from '@/api/datascreen'
import * as THREE from "three";
import { ThreeViewer } from "@/utils/GreenChannelTool";

// ============= 临时类型定义 =============
interface StationType {
  name?: string
  url?: string
  scale?: number
  position?: any
  autoPlay?: boolean
  animationIndex?: number
  initialFrame?: number
  fps?: number
  move?: any
  rotation?: number
}
interface ModelMoveInfoType {
  start?: number[]
  end?: number[]
  speed?: number
  movement?: string
  direction?: number
  degree?: number
}
// 模拟 globalstate 数据
const loading = ref(true)
const currentTime = ref('')
const currentWeather = ref('晴 25°C')

// 获取大屏关键指标数据
const fetchKpiData = async () => {
    try {
        console.log('正在获取大屏数据...')
        const res = await getKpiData()
        console.log('大屏数据响应:', res)
        if (res.code === 200 && res.data) {
            // passcode_fee 单位为分，需要转换为元
            const totalAmount = parseFloat(res.data.discount) || 0
            kpis.value = {
                total: res.data.total || 0,
                tadaytotal: res.data.tadaytotal || 0,
                discount: Math.round(totalAmount / 100),  // 转换为元
                abnormal: res.data.abnormal || 0
            }
            console.log('更新后的kpis:', kpis.value)
        }
    } catch (error) {
        console.error('获取大屏数据失败:', error)
    }
}

// 获取大屏最近通行记录
const fetchRecords = async () => {
    try {
        const res = await getPassRecords()
        if (res.code === 200 && res.data) {
            records.value = res.data
            console.log('更新后的records:', records.value)
        }
    } catch (error) {
        console.error('获取通行记录失败:', error)
    }
}

// 获取大屏信用记录排行
const fetchCreditRanking = async () => {
    try {
        const res = await getCreditRanking()
        if (res.code === 200 && res.data) {
            genRanking.value = res.data.map((item: any, index: number) => ({
                rank: index + 1,
                plate: item.plateNumber,
                count: item.passCount,
                goodsWeight: item.creditScore
            }))
            console.log('更新后的genRanking:', genRanking.value)
        }
    } catch (error) {
        console.error('获取信用排行失败:', error)
    }
}

// 获取大屏货物类型词云数据
const fetchGoodsTypeCloud = async () => {
    try {
        const res = await getGoodsTypeCloud()
        if (res.code === 200 && res.data) {
            goodsCount.value = res.data.map((item: any) => ({
                name: item.name,
                count: item.count
            }))
            console.log('更新后的goodsCount:', goodsCount.value)
        }
    } catch (error) {
        console.error('获取货物类型词云失败:', error)
    }
}

// 更新时间
const updateTime = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = String(now.getHours()).padStart(2, '0')
    const minute = String(now.getMinutes()).padStart(2, '0')
    const second = String(now.getSeconds()).padStart(2, '0')
    currentTime.value = `${year}-${month}-${day} ${hour}:${minute}:${second}`
}
const kpis = ref<any>({ total: 0, tadaytotal: 0, discount: 0, abnormal: 0 })
const typeShare = ref<any>([])
const genRanking = ref<any>([])
const goodsCount = ref<any>([])
const lvtongStations = ref({ railingMachine: false, lightSource: false })
const lvtongDetection = ref<any>({})
// 模拟 globalstate 方法
const globalstate: any = {
  loading,
  lvtongStations,
  lvtongDetection,
  kpis,
  typeShare,
  genRanking,
  goodsCount,
  setLoading: (v: boolean) => { loading.value = v },
  setMockData: (data: any) => {
    if (data.kpis) kpis.value = data.kpis
    if (data.typeShare) typeShare.value = data.typeShare
    if (data.genRanking) genRanking.value = data.genRanking
    if (data.goodsCount) goodsCount.value = data.goodsCount
  },
  seedWsData: (data: any) => {}
}
interface VehicleInspection {
  id?: number
  plateNumber?: string
  [key: string]: any
}
// =========================================

let timer: number | ReturnType<typeof setTimeout>

// ✅ 类型声明
const threeContainer = ref<HTMLDivElement | null>(null);
let viewer: ThreeViewer;
// ✅ 创建添加的模型集合
const models: THREE.Group<THREE.Object3DEventMap>[] = [];

let modelsInfo: StationType[] = [];
let scarInfo: StationType[] = []

const initModelsData = async () => {
    try {
        const response = await fetch('/GreenChannel/data/threeModels.json');
        const threeModels = await response.json();

        threeModels.projectModel.forEach((item: StationType) => {
            if (item.position) {
                const [x, y, z] = item.position
                item.position = new THREE.Vector3(x, y, z)
            }
            if (item.name == "guangyuan") {
                item.rotation = Math.PI
            }
            modelsInfo.push(item)
        })

        scarInfo = threeModels.smallCarModes
        return modelsInfo;  // 返回处理后的数据
    } catch (error) {
        console.error('加载模型数据失败:', error);
        return [];
    }
}


onMounted(async () => {
    // 启动时间更新定时器
    updateTime()
    setInterval(updateTime, 1000)

    // 获取大屏关键指标数据
    fetchKpiData()
    setInterval(fetchKpiData, 30000)  // 每30秒刷新一次

    // 获取大屏最近通行记录
    fetchRecords()
    setInterval(fetchRecords, 30000)  // 每10秒刷新一次

    // 获取大屏信用记录排行
    fetchCreditRanking()

    // 获取大屏货物类型词云数据
    fetchGoodsTypeCloud()

    viewer = await new ThreeViewer(threeContainer!);
    // 隐藏加载提示
    //globalstate.setLoading(false);

    // 启用 3D 模型加载
    await initModelsData()
    // showData()
    await addStaticModel()
    await addAllModels(modelsInfo);
    const guangyuanModel = models.find((m) => m.name === "guangyuan") as THREE.Group;

    const truck4Model = models.find((m) => m.name === "truck4") as THREE.Group;
    setTimeout(() => {
        movetruck(truck4Model);
    }, 50)
    // console.log(lvtongDetection.value, 'lvtongDetection', globalstate.lvtongDetection);
    })


const addStaticModel = async () => {
    await viewer.addAnimatedGLTFModel('/GreenChannel/allModel/cj.glb', { scale: 1, position: new THREE.Vector3(0, 0, 0), autoPlay: false }, (model, mixer, clips) => {
        model.name = 'staticScene';
        model.traverse((child) => {
            if (child.name.includes('MERGED_挤压')) {  // 底部
                if (child instanceof THREE.Mesh) {
                    // 创建支持顶点颜色的材质
                    const material = new THREE.MeshStandardMaterial({
                        vertexColors: true,  // 启用顶点颜色
                        roughness: 0.3,
                        metalness: 0.1,
                        flatShading: true,
                    });

                    // 获取几何体位置数据
                    const geometry = child.geometry;
                    const positions = geometry.attributes.position.array;
                    const vertexCount = positions.length / 3;

                    // 创建颜色数组
                    const colors = [];

                    for (let i = 0; i < vertexCount; i++) {
                        const x = positions[i * 3];     // X坐标
                        const y = positions[i * 3 + 1]; // Y坐标（如果需要）
                        const z = positions[i * 3 + 2]; // Z坐标（如果需要）
                        // console.log(x, '555555555555555555');

                        // 基于X轴方向决定颜色
                        if (x < 6500) {
                            colors.push(0.3176, 0.3176, 0.3176);
                        } else {
                            colors.push(0.0, 1.0, 0.0);
                        }
                    }

                    // 设置颜色属性
                    geometry.setAttribute('color',
                        new THREE.Float32BufferAttribute(colors, 3));

                    // 应用材质
                    child.material = material;

                    // console.log('已应用X轴颜色渐变');
                }
            }

            if (child instanceof THREE.Mesh) {

                // }
                if (child.name.includes('MERGED_材质24')) { // 水码
                    changeColor(child, 0xc0c0c0, 1)
                }
                if (child.name.includes('MERGED_材质8')) { // 水码
                    changeColor(child, 0xffff00, 0.5)
                }
                if (child.name.includes('MERGED_材质11')) { // 水码
                    changeColor(child, 0xff0000, 0.5)
                }
                if (child.name.includes('MERGED_材质12')) {  // 方向地标
                    changeColor(child, 0xffff00, 0.1)
                }
                if (child.name.includes('MERGED_fallback_Material_132')) {  // 白色路线
                    changeColor(child, 0xffffff, 0.1)
                }
            }
        });

        if (scarInfo && scarInfo.length > 0) {
            scarInfo.forEach((item: StationType) => {
                const { name, url, scale, autoPlay, position, animationIndex, initialFrame, fps, move } = item
                const [x, y, z] = position as [number, number, number]
                const moveInfo = move as ModelMoveInfoType
                if (!moveInfo.start || !moveInfo.end) return
                const { start, end, speed, movement, direction, degree } = moveInfo
                const [sx, sy, sz] = start
                const [ex, ey, ez] = end
                viewer.addAnimatedGLTFModel(url, { scale, autoPlay, position: new THREE.Vector3(x, y, z), animationIndex, initialFrame, fps }, (model, mixer, clips) => {
                    model.name = name;
                    model.rotation.y = Math.PI / (degree || 1);
                    viewer.moveModel(model, speed || 0, new THREE.Vector3(sx, sy, sz), new THREE.Vector3(ex, ey, ez), movement, direction);
                });
            })
        }
        showData();
    });
}

function changeColor(material: { name: string | string[]; material: THREE.MeshStandardMaterial }, color: any, opacity: any) {
    const newMaterial = new THREE.MeshStandardMaterial({
        color,   // 绿色
        roughness: 0.3,
        transparent: true,
        opacity,
    });

    material.material = newMaterial;
}

const addAllModels = async (info: any[]) => {
    // 创建所有模型加载的 Promise 数组
    const modelPromises: Promise<any>[] = [];

    // 添加所有模型
    if (info.length > 0) {
        await info.forEach((item: any) => {
            const { name, url, scale, autoPlay, animationIndex, initialFrame, position, rotation, fps, } = item;
            modelPromises.push(new Promise((resolve) => {
                viewer.addAnimatedGLTFModel(url, { scale, autoPlay, position, animationIndex, initialFrame, fps }, (model, mixer, clips) => {
                    model.name = name;
                    if (name == 'guangyuan') {
                        model.rotation.y = Math.PI;
                    }
                    if (name == 'truck4') {
                        model.rotation.y = Math.PI / 11.5;
                    }
                    if (name == 'taiganqi') {
                        // model.rotation.y = Math.PI;
                        model.traverse((child) => {
                            if (child instanceof THREE.Mesh && child.name.includes("立方体")) {
                                child.visible = false
                            }
                        })
                    }
                    models.push(model);
                    resolve(model);
                });
            }));
        });
    }
    // 等待所有模型加载完成
    await Promise.all(modelPromises);
};
// ===============================
// 模拟车辆通行状态机
// ===============================
const movetruck = (model: THREE.Group) => {
    const taiganqiModel = models.find((m) => m.name === "taiganqi") as THREE.Group;
    const guangyuanModel = models.find((m) => m.name === "guangyuan") as THREE.Group;

    const startPos = new THREE.Vector3(0, 0, 0);
    const directionVector = new THREE.Vector3(0, 0, 1).normalize();
    const endPos = startPos.clone().add(directionVector.clone().multiplyScalar(10));
    const speed = 3;

    // 状态机
    let state: | "APPROACHING" | "WAITING" | "GATE_OPENING" | "PASSING" | "GATE_CLOSING" | "GO_ON" | "RESETTING" = "APPROACHING";

    viewer.moveModel(model, speed, startPos, endPos, "continuous", 1, "ceshi", async (currentPosition: THREE.Vector3) => {
        // 计算距离
        const truckBox = new THREE.Box3().setFromObject(model);
        const barrierBox = new THREE.Box3().setFromObject(taiganqiModel);
        const guangyuanBox = new THREE.Box3().setFromObject(guangyuanModel);
        const barrierCenter = barrierBox.getCenter(new THREE.Vector3());
        const guangyuanCenter = guangyuanBox.getCenter(new THREE.Vector3());
        const barrierDistance = truckBox.distanceToPoint(barrierCenter);
        const lightDistance = truckBox.distanceToPoint(guangyuanCenter);

        const { railingMachine, lightSource } = lvtongStations.value;

        // ─── 状态机逻辑 ───
        switch (state) {
            case "APPROACHING":
                if (barrierDistance <= 3) {
                    viewer.pauseModel(model, 0);
                    state = "WAITING";
                    // console.log("🚗 等待开杆指令中…");
                }
                break;

            case "WAITING":
                // 收到抬杆指令
                if (railingMachine) {
                    // console.log("🚗 抬杆器抬杆中…");
                    state = "GATE_OPENING"; // 此时改变状态 防止再次触发动画
                    await viewer.playSmoothTransition(taiganqiModel, 78, 89, 0, 24, 1.5);
                    // console.log("🚗 抬杆完成，车辆移动中…");
                    viewer.pauseModel(model, 1);
                }
                break;

            case "PASSING":
                // 收到通过指令
                if (barrierDistance > 0) {
                    state = "GATE_CLOSING";
                    await viewer.playSmoothTransition(taiganqiModel, 120, 128, 0, 24, 1.5);
                    // console.log("🚗 落杆完成");
                }
                break;
            case "GATE_CLOSING":
                // 收到关杆指令  当车通过以后 关闭光源
                if (lightDistance > 0) {
                    globalstate.seedWsData({ type: "update_lvtong_json", data: { field: "lightSource", value: false }, timestamp: Date.now(), });
                    state = "GO_ON";
                }
                break;
            case "GO_ON":
                setTimeout(() => {
                    state = "RESETTING";
                }, 5000)
                break;

            case "RESETTING":
                // // 重置所有状态
                viewer.resetModel(model, new THREE.Vector3(-29, 4.65, -30));
                state = "APPROACHING";
                break;
        }

        if (lightSource) {
            guangyuanModel.visible = true;
        } else {
            guangyuanModel.visible = false;
        }

        // 车辆通过栏杆后，进入PASSING状态
        if (barrierDistance === 0) {
            state = "PASSING";
        }
    });
};

// 模型加载完成后 加载数据
const showData = () => {
    globalstate.setLoading(false);
};

onUnmounted(() => {
    // 清理 ThreeViewer 资源
    if (viewer) {
        viewer.dispose()
    }
})

const airlineData = ref([
    { name: '北京', count: 120 },
    { name: '西藏', count: 340 },
    { name: '广州', count: 80 },
    { name: '深圳', count: 200 },
    { name: '重庆', count: 90 },
    { name: '西安', count: 150 },
    { name: '武汉', count: 150 },
    { name: '吐鲁番', count: 150 },
    { name: '乌鲁木齐', count: 150 },
    { name: '沈阳', count: 150 },
    { name: '哈尔滨', count: 150 },
    { name: '昆明', count: 150 },
    { name: '成都', count: 150 }
])

const ciyunData = ref([
    { name: '大白菜', count: 8724 },


    { name: '蟹类', count: 5984 },
    { name: '海带', count: 7196 },
    { name: '紫菜', count: 5473 },
    { name: '海蜇', count: 3876 },
    { name: '海参', count: 4589 },
    { name: '仔猪', count: 8237 },
    { name: '蜜蜂（转地放蜂）', count: 2984 },
    { name: '新鲜的鸡蛋', count: 9568 },
    { name: '新鲜的鸭蛋', count: 6742 },
    { name: '新鲜的鹅蛋', count: 5289 },
    { name: '新鲜的鹌鹑蛋', count: 4837 },
    { name: '新鲜的鸽蛋', count: 4196 },
    { name: '新鲜的家禽肉', count: 8765 },
    { name: '新鲜的家畜肉', count: 9348 },
    { name: '生鲜乳', count: 7823 }
])

const records = ref<VehicleInspection[]>([]);



</script>

<style>
@import '@/components/GreenChannel/layout.css';

.icon {
  width: 70%;
  height: 70%;
  vertical-align: -0.15em;
  align-items: center;
  fill: currentColor;
  overflow: hidden;
}

/* 加载动画 */
.loader {
  width: fit-content;
  font-weight: bold;
  font-family: sans-serif;
  font-size: 30px;
  padding-bottom: 8px;
  background: linear-gradient(currentColor 0 0) 0 100%/0% 3px no-repeat;
  animation: l2 2s linear infinite;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.loader:before {
  content: "场景加载中..."
}

@keyframes l2 {
  to {
    background-size: 100% 3px
  }
}
</style>
