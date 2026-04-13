<template>
    <Teleport to="body">
        <div v-if="scrollPortVisible && rows.bodyImages.length > 0" class="teleport-box">
            <el-icon style="position: absolute; color: black; right: 5px; top: 5px; font-size: 26px;"
                @click="handleClose()">
                <Close />
            </el-icon>
            <div>
                <div>
                    <el-divider content-position="left" style="background: #f0fdf4;">详情照片</el-divider>
                    <div style="display: flex;justify-content: space-between;">
                        <el-image ref="imageRef" style="width: 200px; height: 200px" v-for="item in srcList"
                            :src="item[0]" :preview-src-list="item" fit="cover" show-progress>
                            <template #error>
                                <div class="image-viewer-slot image-err-slot"> 暂无图片 </div>
                            </template>
                            <template #toolbar="{ actions, prev, next, reset, activeIndex, setActiveItem }">
                                <el-icon @click="actions('zoomOut')">
                                    <ZoomOut />
                                </el-icon>
                                <el-icon @click="actions('zoomIn', { enableTransition: false, zoomRate: 2 })">
                                    <ZoomIn />
                                </el-icon>
                                <el-icon @click="
                                    actions('clockwise', { rotateDeg: 180, enableTransition: false })
                                    ">
                                    <RefreshRight />
                                </el-icon>
                                <el-icon @click="actions('anticlockwise')">
                                    <RefreshLeft />
                                </el-icon>
                                <el-icon @click="reset">
                                    <Refresh />
                                </el-icon>
                                <!-- <el-icon @click="download(activeIndex)">
                                    <Download />
                                </el-icon> -->
                            </template>
                        </el-image>
                    </div>
                </div>
                <div>
                    <el-divider content-position="left" style="background: #f0fdf3;">侧面照片</el-divider>
                    <div style="display: flex;justify-content: space-between;">
                        <el-image ref="imageRef" style="width: 49%; height: 200px" v-for="item in ceshengSrcList"
                            :src="item[0]" :preview-src-list="item" fit="cover" show-progress>
                            <template #error>
                                <div class="image-viewer-slot image-err-slot"> 暂无图片 </div>
                            </template>
                            <template #toolbar="{ actions, prev, next, reset, activeIndex, setActiveItem }">
                                <el-icon @click="actions('zoomOut')">
                                    <ZoomOut />
                                </el-icon>
                                <el-icon @click="actions('zoomIn', { enableTransition: false, zoomRate: 2 })">
                                    <ZoomIn />
                                </el-icon>
                                <el-icon @click="actions('clockwise', { rotateDeg: 180, enableTransition: false })">
                                    <RefreshRight />
                                </el-icon>
                                <el-icon @click="actions('anticlockwise')">
                                    <RefreshLeft />
                                </el-icon>
                                <el-icon @click="reset">
                                    <Refresh />
                                </el-icon>
                                <!-- <el-icon @click="download(activeIndex)">
                                    <Download />
                                </el-icon> -->
                            </template>
                        </el-image>
                    </div>
                </div>
                <div style="display: flex; flex-wrap: wrap;justify-content: space-around;margin: 13px 0px;">
                    <div class="aaa"><label for="">入口站编号</label> <input type="text" class="valueInput"
                            :value="Tools.getStation(rows.passcode_en_station_id)" disabled>
                    </div>
                    <div class="aaa"><label for="">满载率</label> <input type="text" class="valueInput"
                            :value="rows.load_rate" disabled>
                    </div>
                    <div class="aaa"><label for="">车牌号码</label> <input type="text" class="valueInput"
                            :value="rows.plate_number" disabled>
                    </div>
                    <div class="aaa"><label for="">出口站编号</label> <input type="text" class="valueInput"
                            :value="Tools.getStation(rows.passcode_ex_station_id)" disabled>
                    </div>
                    <div class="aaa"><label for="">货物名称</label> <input type="text" class="valueInput"
                            :value="rows.cvarietyname" disabled>
                    </div>
                    <div class="aaa"><label for="">交易支付方式</label> <input type="text" class="valueInput"
                            :value="rows.passcode_trans_pay_type" disabled>
                    </div>
                    <div class="aaa"><label for="">出口站交易时间</label> <input type="text" class="valueInput"
                            :value="rows.passcode_ex_time" disabled>
                    </div>
                    <div class="aaa"><label for="">入口重量</label> <input type="text" class="valueInput"
                            :value="rows.passcode_en_weight" disabled>
                    </div>
                    <div class="aaa"><label for="">车辆状态标识</label> <input type="text" class="valueInput"
                            :value="rows.passcode_vehicle_sign" disabled>
                    </div>
                    <div class="aaa"><label for="">总交易金额</label> <input type="text" class="valueInput"
                            :value="rows.passcode_fee" disabled>
                    </div>
                    <div class="aaa"><label for="">出口重量</label> <input type="text" class="valueInput"
                            :value="rows.passcode_ex_weight" disabled>
                    </div>
                    <div class="aaa"><label for="">查验依据</label> <input type="text" class="valueInput" value="" disabled>
                    </div>
                    <!-- <div class="aaa"><label for="">车货长宽高</label> <input type="text" class="valueInput" value="" disabled>
                    </div> -->
                    <div class="aaa"><label for="">应收金额</label> <input type="text" class="valueInput"
                            :value="rows.passcode_pay_fee" disabled>
                    </div>
                    <div class="aaa"><label for="">货箱类型</label> <input type="text" class="valueInput"
                            :value="rows.dtypename" disabled>
                    </div>
                    <div class="aaa"><label for="">通过省份个数</label> <input type="text" class="valueInput"
                            :value="rows.passcode_province_count" disabled>
                    </div>
                    <div class="aaa"><label for="">司机电话</label> <input type="text" class="valueInput"
                            :value="rows.driver_phone" disabled>
                    </div>
                    <div class="aaa"><label for="">通行介质</label> <input type="text" class="valueInput"
                            :value="rows.passcode_media_type" disabled>
                    </div>
                    <div class="aaa"><label for="">出口交易编号</label> <input type="text" class="valueInput"
                            :value="rows.passcode_transaction_id" disabled>
                    </div>
                    <!-- <div class="aaa"><label for="">通行标识ID</label> <input type="text" class="valueInput" value="" disabled>
                    </div>
                    <div class="aaa"><label for="">备注类容</label> <input type="text" class="valueInput" value="" disabled>
                    </div> -->
                </div>
                <footer style="text-align: right;">
                    <el-button @click="globalstate.setScrollPortVisible(true)">取消</el-button>
                    <el-button type="primary" @click="handleClose()">确定</el-button>
                </footer>
            </div>
        </div>
    </Teleport>
</template>

<script setup lang="ts">
import { ElIcon } from 'element-plus'
import { Back, DArrowRight, Download, Close, Refresh, RefreshLeft, RefreshRight, Right, ZoomIn, ZoomOut, } from '@element-plus/icons-vue'
import { Picture as IconPicture } from '@element-plus/icons-vue'
// 临时类型定义
interface VehicleInspection {
  id?: number
  plateNumber?: string
  [key: string]: any
}
interface ImageItem {
  url?: string
  [key: string]: any
}

import { computed, onMounted, ref, watch } from 'vue';
// 临时 globalstate 存根
import { storeToRefs } from 'pinia'
const useGlobalStore = () => ({
  scrollPortVisible: ref(false),
  setScrollPortVisible: (v: boolean) => {}
})
// 临时 Tools 存根
const Tools = {
  getStation: (id: any) => id || '-'
}

const globalstate = useGlobalStore()
const { scrollPortVisible } = storeToRefs(globalstate)

const props = defineProps<{
    visible: boolean
    rows?: VehicleInspection
}>()
// 使用计算属性来响应式更新图片数据
const srcList = computed(() => {
    const images = props.rows?.fullImages || [];
    // 使用 Array.from 创建固定长度的数组
    const result = Array.from({ length: 6 }, (_, index) =>
        images[index]?.url || ''
    );
    return result;
});

const ceshengSrcList = computed(() => {
    const images = props.rows?.bodyImages || [];
    // 使用 Array.from 创建固定长度的数组
    const result = Array.from({ length: 2 }, (_, index) =>
        images[index]?.url || ''
    );
    return result;
});

// 声明emits
const emit = defineEmits<{
    (e: 'close'): void;
}>();

// 关闭处理函数
const handleClose = () => {
    emit('close');
    globalstate.setScrollPortVisible(false)
};
</script>

<style scoped>
.teleport-box {
    width: 70%;
    height: 90%;
    background: #f0fdf4;
    z-index: 100;
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 8px;
    padding: 8px 12px;
}

@media (min-height: 1080px) {
    .teleport-box {
        height: 80%;
    }
}

label {
    color: #052e16;
    font-size: 1.8rem;
    display: flex;
    width: 30%;
    align-items: center;
}

.valueInput {
    border: none;
    background: transparent;
    border-bottom: 1px solid #dcdcdc;
    outline: none;
    height: 4rem;
    line-height: 4rem;
    font-size: 1.8rem;
    width: 65%;
}

.image-err-slot {
    color: #a8abb2;
    height: 100%;
    align-items: center;
    display: flex;
    justify-content: center;
    background: #dcdcdc;
    font-family: emoji;
    font-size: 12px;
}

.aaa {
    width: 33%;
    display: flex;
}

:deep(.el-divider__text) {
    background: #f0fdf4;
}
</style>