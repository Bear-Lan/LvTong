<template>
  <div class="panel">
    <h3>{{ title }}</h3>
    <table class="table">
      <thead>
        <tr>
          <th style="width:60px">排名</th>
          <th>车牌号</th>
          <th>通行次数</th>
          <th>信用评分(10分)</th>
          <!-- <th style="text-align:right">优惠金额(元)</th> -->
        </tr>
      </thead>
      <tbody>
        <tr v-for="r, index in topThreeRows" :key="index">
          <td><span class="rank">{{ index + 1 }}</span></td>
          <!-- <td><span :style="getPlateType(r.plate)" class="plate">{{ r.plate }}</span></td> -->
          <td><span class="plate">{{ r.plate }}</span></td>
          <td>{{ r.count }}</td>
          <td>{{ r.goodsWeight.toFixed(2) }}</td>
          <!-- <td style="text-align:right">{{ r.amount.toLocaleString() }}</td> -->
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { color } from 'echarts'
import { computed } from 'vue'

interface RowData {
  rank: number
  // name: string
  plate: string
  goodsWeight: number
  count: number
}
// let ceshi = "#000"
const props = defineProps<{
  title: string
  rows: RowData[]
}>()

// 计算属性：只返回前三条数据
const topThreeRows = computed(() => {
  return props.rows.slice(0, 5)
})

// 车牌类型判断函数
function getPlateType(plate: string) {
  // 新能源车牌（绿牌）：第3位是D或F，且总长度8位
  if (plate.length === 8 && (plate[2] === 'D' || plate[2] === 'F')) {
    return {
      background: '#00a950',
      color: '#000'
    } // 绿牌
  }
  // 黄牌：大型车，通常是货车、客车等
  // 黄牌：通常最后一位是字母，或者有特定特征
  else if (plate.length === 7 && /[A-Z]$/.test(plate)) {
    return {
      background: '#cc9900',
      color: '#000'
    }; // 黄牌
  }
  // 蓝牌：通常是5位数字或4位数字+1位字母
  else if (plate.length === 7) {
    return {
      background: '#0066cc',
      color: '#fff',
    }; // 蓝牌
  }
  return 'unknown';
}
</script>

<style scoped>
.plate {
  border-radius: 4px;
  padding: 6px 12px;
  font-weight: 800;
}
</style>