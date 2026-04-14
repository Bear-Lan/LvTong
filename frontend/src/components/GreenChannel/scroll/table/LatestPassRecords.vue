<template>
  <!-- <div class="panel" style="height: 100%;"> -->
  <div class="seachHeader">
    <h3 class="title">绿通检测记录</h3>
  </div>

  <!-- 固定表头 -->
  <div class="table-header">
    <span class="th scrollID">序号</span>
    <span class="th plate">车牌号</span>
    <span class="th goodsType">货物类型</span>
    <span class="th goodsWeight">货物入口重量</span>
    <span class="th goodsWeight">货物出口重量</span>
    <!-- <span class="th originAddress">始发地</span> -->
    <span class="th amount">减免金额</span>
    <span class="th loadRate">满载率</span>
    <!-- <span class="th release">放行方式</span> -->
    <span class="th release">出站时间</span>
    <span class="th time">司机电话</span>
    <!-- <span class="th duration">通行时长</span> -->
  </div>

  <!-- 滚动区域 -->
  <div class="scroll-wrapper " ref="wrapper" @mouseenter="pauseScroll" @mouseleave="resumeScroll" @wheel="handleWheel">
    <div class="scroll-content" :style="{ transform: `translateY(-${scrollTop}px)` }" ref="list">
      <!-- 原始列表 -->
      <ul class="scroll-list">
        <li v-for="(item, index) in listData" :key="'a' + index" class="scroll-item"
          @click="handleCilckItem(item, index)" :class="{
            selected: isItemSelected(item, index),
            hovered: hoverIndex === index && !isItemSelected(item, index)
          }" @mouseenter="hoverIndex = index" @mouseleave="hoverIndex = -1">
          <span class="scrollID">{{ index + 1 }}</span>
          <span class="plate" :class="{ green: item.isGreen }">{{ item.plateNumber || '-' }}</span>
          <span class="goodsType">{{ item.goodsTypeName || '-' }}</span>
          <span class="goodsWeight">{{ item.passcodeExWeight || '-' }}</span>
          <span class="goodsWeight">{{ item.passcodeEnWeight || '-' }}</span>
          <span class="amount">{{ item.passcodeFee || '-' }}</span>
          <span class="loadRate">{{ item.loadRate || '-' }}</span>
          <span class="loadTime">{{ formatTime(item.passcodeExTime) }}</span>
          <span class="loadRate">{{ item.driverPhone || '-' }}</span>
          <!-- <span class="release" :class="item.release === '异常处理' ? 'yellowClass' : 'greenClass'">{{ item.release }}</span> -->
          <!-- <span class="release">{{ item.release }}</span>
          <span class="time">{{ item.time }}</span>
          <span class="duration">{{ item.duration }}s</span> -->
        </li>
      </ul>
      <!-- 复制一份列表用于无缝滚动 -->
      <ul class="scroll-list">
        <li v-for="(item, index) in listData" :key="'b' + index" class="scroll-item"
          @click="handleCilckItem(item, index)" :class="{
            selected: isItemSelected(item, index),
            hovered: hoverIndex === index && !isItemSelected(item, index)
          }" @mouseenter="hoverIndex = index" @mouseleave="hoverIndex = -1">
          <span class="scrollID">{{ index + 1 }}</span>
          <span class="plate" :class="{ green: item.isGreen }">{{ item.plateNumber || '-' }}</span>
          <span class="goodsType">{{ item.goodsTypeName || '-' }}</span>
          <span class="goodsWeight">{{ item.passcodeExWeight || '-' }}</span>
          <span class="goodsWeight">{{ item.passcodeEnWeight || '-' }}</span>
          <span class="amount">{{ item.passcodeFee || '-' }}</span>
          <span class="loadRate">{{ item.loadRate || '-' }}</span>
          <span class="loadTime">{{ formatTime(item.passcodeExTime) }}</span>
          <span class="loadRate">{{ item.driverPhone || '-' }}</span>
          <!-- <span class="release" :class="item.release === '异常处理' ? 'yellowClass' : 'greenClass'">{{ item.release }}</span> -->
          <!-- <span class="release">{{ item.release }}</span>
          <span class="time">{{ item.time }}</span>
          <span class="duration">{{ item.duration }}s</span> -->
        </li>
      </ul>
    </div>
  </div>

  <scrollport :visible="isShow" :rows="rowData" @close="handleScrollportClose" />
  <!-- </div> -->
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from "vue";
// 临时类型定义
interface VehicleInspection {
  id?: number
  plateNumber?: string
  [key: string]: any
}

import scrollport from '@/components/GreenChannel/teleport/scrollport.vue'

// 临时 globalstate 存根
const useGlobalStore = () => ({
  scrollPortVisible: ref(false),
  setScrollPortVisible: (v: boolean) => {}
})

import dayjs from 'dayjs'
const globalstate = useGlobalStore()

interface Props {
  list: VehicleInspection[];
  speed?: number; // 滚动速度(px/帧)
  itemHeight?: number; // 每行高度(px)
}


const props = withDefaults(defineProps<Props>(), {
  list: () => [],
  speed: 0.6,
  itemHeight: 40,
});



const scrollTop = ref(0);
let timer: number;
let listHeight = 0;
const list = ref<HTMLElement>();
const listData = ref<VehicleInspection[]>([])

const isShow = ref(false)
const dialogVisible = ref(false)

// ---- 监听数据变化 ----
watch(dialogVisible, (newValue, oldValue) => {
  console.log(newValue, oldValue);

})
watch(
  () => props.list,
  (newVal) => {
    console.log('props.list变化:', newVal ? newVal.length : 0)
    listData.value = newVal || []
    console.log('listData更新后:', listData.value.length)
  },
  { deep: true, immediate: true }
)

// 选中状态相关
const selectedIndex = ref(-1);
const selectedData = ref<VehicleInspection | null>(null)
const hoverIndex = ref(-1);
const rowData = ref<VehicleInspection | null>(null)

// 自动滚动控制
const isAutoScrolling = ref(true);
const isHovering = ref(false);
const isManualPaused = ref(false);
const isMounted = ref(false); // 添加mounted状态标志


// 计算是否选中当前项
const isItemSelected = (item: VehicleInspection, index: number): boolean => {
  // 如果当前项被选中，返回true
  if (selectedIndex.value === index) {
    return true;
  }

  // 如果有选中数据，对比数据内容
  if (selectedData.value) {
    // 使用唯一标识符比较，这里使用车牌号+出站时间
    return (
      item.plate_number === selectedData.value.plate_number &&
      item.passcode_ex_time === selectedData.value.passcode_ex_time
    );
  }

  return false;
};

// 修改startAutoScroll函数
const startAutoScroll = () => {
  if (!isAutoScrolling.value || isManualPaused.value || !isMounted.value) return;

  cancelAnimationFrame(timer);

  const loop = () => {
    // 只有在自动滚动启用、弹窗未显示、且不在悬停状态时才滚动
    if (isAutoScrolling.value && !isManualPaused.value && !isHovering.value && isMounted.value) {
      if (list.value) {
        // 确保listHeight有值
        if (listHeight === 0) {
          listHeight = list.value.scrollHeight / 2;
        }

        scrollTop.value += props.speed;

        // 当滚动到列表末尾时，重置到开头
        if (scrollTop.value >= listHeight) {
          scrollTop.value = 0;
        }
      }
    }
    timer = requestAnimationFrame(loop);
  };

  // 立即开始循环
  timer = requestAnimationFrame(loop);
};

// 初始化滚动
const initAutoScroll = async () => {
  await nextTick(); // 等待DOM更新

  // 确保元素存在
  if (!list.value) {
    console.warn('list元素未找到，重新尝试...');
    // 重新尝试获取
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();
  }

  if (list.value) {
    listHeight = list.value.scrollHeight / 2;
    console.log('listHeight:', listHeight, 'list内容高度:', list.value.scrollHeight);

    // 开始滚动
    startAutoScroll();
  } else {
    console.error('无法找到list元素');
  }
};


// 暂停自动滚动
const pauseAutoScroll = () => {
  isManualPaused.value = true;
  cancelAnimationFrame(timer);
};

// 恢复自动滚动
const resumeAutoScroll = () => {
  isManualPaused.value = false;
  if (isAutoScrolling.value && !isHovering.value) {
    startAutoScroll();
  }
};

// 鼠标进入时暂停滚动
const pauseScroll = () => {
  isHovering.value = true;
  cancelAnimationFrame(timer);
};

// 鼠标离开时恢复滚动
const resumeScroll = () => {
  isHovering.value = false;
  if (isAutoScrolling.value && !isManualPaused.value) {
    startAutoScroll();
  }
};


// 点击处理函数
const handleCilckItem = (data: VehicleInspection, index: number) => {
  console.log('点击行数据:', data, '索引:', index);

  // 如果点击的是同一项，取消选中
  if (selectedIndex.value === index && selectedData.value) {
    const currentId = `${selectedData.value.plate_number}_${selectedData.value.passcode_ex_time}`;
    const newId = `${data.plate_number}_${data.passcode_ex_time}`;

    if (currentId === newId) {
      clearSelection();
      return;
    }
  }

  // 设置选中状态
  selectedIndex.value = index;
  selectedData.value = { ...data };

  // 更新弹窗数据
  rowData.value = { ...data };

  // 显示弹窗
  globalstate.setScrollPortVisible(true);
  pauseAutoScroll()

  // 可选：滚动到选中项的位置
  // scrollToSelectedItem();
};


const formatTime = (timeString: string): string => {
  if (!timeString) return '--\n--'

  try {
    // 使用 dayjs
    const date = dayjs(timeString)
    if (!date.isValid()) return '--\n--'

    return `${date.format('YYYY-MM-DD')}\n${date.format('HH:mm:ss')}`

  } catch (e) {
    return '--\n--'
  }
}

// 滚轮事件处理
const handleWheel = (event: WheelEvent) => {
  // 按住Ctrl键滚动滚轮触发搜索
  if (event.ctrlKey) {
    event.preventDefault();

    // 如果是向下滚动，激活搜索
    // if (event.deltaY > 0 && !isWheelSearchActive.value) {
    //   activateWheelSearch();
    // }
  } else {
    // 普通滚轮控制滚动速度
    event.preventDefault();
    const scrollAmount = event.deltaY > 0 ? 30 : -30;
    scrollTop.value = Math.max(0, scrollTop.value + scrollAmount);

    // 如果滚动到底部，重置到顶部
    if (list.value && scrollTop.value >= list.value.scrollHeight / 2) {
      scrollTop.value = 0;
    }
  }
};


// 处理弹窗关闭事件
const handleScrollportClose = () => {
  console.log('弹窗关闭，恢复自动滚动');

  // 恢复自动滚动
  resumeAutoScroll();

  // 可选：清除选中状态
  // clearSelection();
};

// 清除选中状态
const clearSelection = () => {
  selectedIndex.value = -1;
  selectedData.value = null;
  // rowData.value = undefined;
  globalstate.setScrollPortVisible(false);
};


onMounted(async () => {
  console.log('组件挂载');
  isMounted.value = true;
  // 初始化滚动
  await initAutoScroll();

  // 添加调试信息
  console.log('props.list长度:', props.list.length);
  console.log('scrollTop初始值:', scrollTop.value);

  // 如果初始化失败，延迟重试
  if (!timer) {
    setTimeout(() => {
      console.log('延迟重试初始化滚动');
      initAutoScroll();
    }, 500);
  }
});
onBeforeUnmount(() => {
  console.log('组件卸载');
  isMounted.value = false;
  pauseAutoScroll();

  // 清理定时器
  if (timer) {
    cancelAnimationFrame(timer);
  }
});
</script>

<style scoped>
.record-wrapper {
  /* background: rgba(0, 20, 40, 0.8); */
  border-radius: 12px;
  padding: 10px;
  border: 1px solid rgba(0, 255, 255, 0.3);
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2);
  color: #fff;
  width: 100%;
  height: 100%;
}

.seachHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 1.5rem;
  margin-bottom: 8px;
  /* color: #00f0ff; */
  /* text-shadow: 0 0 10px #00f0ff; */
}

/* 表头样式 */
.table-header {
  display: flex;
  justify-content: space-between;
  /* background: rgba(0, 128, 0, 0.3); */
  /* color: #00ffb3; */
  font-weight: bold;
  font-size: 1.5rem;
  padding: 0 12px;
  height: 40px;
  line-height: 40px;
  /* border-bottom: 1px solid rgba(0, 255, 0, 0.2); */
}

.th {
  text-align: center;
}

/* 滚动区域 */
.scroll-wrapper {
  flex: 1;
  /* 占据剩余空间 */
  overflow: hidden;
  position: relative;
  min-height: 0;
  /* 重要：防止flex item溢出 */
  height: calc(100% - 70px);
  /* 减去标题30px + 表头40px */
  margin-top: 0;
  /* 移除可能的margin */
}

.scroll-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
}

.scroll-list {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 1.3rem;
}

.scroll-item {
  display: flex;
  justify-content: space-between;
  /* grid-template-columns: 100px 100px 120px 80px; */
  text-align: center;
  align-items: center;
  height: 40px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 0 12px;
  /* background: rgba(0, 255, 0, 0.05); */
  cursor: pointer;
}

/* 悬停效果 */
.scroll-item.hovered {
  background: rgba(0, 240, 255, 0.1);
  border-left: 3px solid rgba(0, 240, 255, 0.5);
}

/* 选中效果 - 高亮样式 */
.scroll-item.selected {
  background: linear-gradient(90deg,
      rgba(0, 240, 255, 0.15) 0%,
      rgba(0, 240, 255, 0.3) 50%,
      rgba(0, 240, 255, 0.15) 100%);
  border-left: 4px solid #00f0ff;
  border-right: 4px solid #00f0ff;
  box-shadow:
    inset 0 0 15px rgba(0, 240, 255, 0.3),
    0 0 10px rgba(0, 240, 255, 0.2);
  transform: scale(1.01);
  z-index: 1;
}

.scrollID {
  width: 4rem;
}

.plate,
.goodsType,
.goodsWeight,
.originAddress,
.amount,
.time,
.loadRate,
.release {
  width: 11rem;
}

.duration {
  /* color: #73c2fb; */
  text-align: right;
  width: 8rem;
}

.release {
  padding: 2px 6px;
  border-radius: 2px;
}

.loadTime {
  white-space: pre-line;
  /* 关键：保留换行符 */
  display: inline-block;
  text-align: center;
}

.yellowClass {
  /* margin:  10px; */
  color: #ffc107;
  background-color: #fff3cd;
}

.greenClass {
  color: #28a745;
  background-color: #d4edda;
}

.teleport-box {
  width: 500px;
  height: 500px;
  background: skyblue;
  z-index: 100;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
