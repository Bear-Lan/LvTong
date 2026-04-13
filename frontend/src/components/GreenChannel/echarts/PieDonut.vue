<template>
  <div class="panel">
    <h3>{{ title }}</h3>
    <BaseChart :option="opt" />
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseChart from './BaseChart.vue'
const props = defineProps<{ title: string; data: { name: string; value: number }[] }>()

// 过滤掉 value 为 0 的数据
const filteredData = computed(() => { return props.data.filter(item => item.value > 0) })


const opt = computed(() => {
  const colorPalette = ['#5470c6B3', '#91cc75B3', '#fac858B3', '#ee6666B3', '#73c0deB3', '#3ba272B3', '#fc8452B3', '#9a60b4B3', '#ea7cccB3']
  return {
    color: colorPalette,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#fff' } },
    series: [{
      type: 'pie',
      radius: ['38%', '70%'],
      center: ['50%', '45%'],
      data: filteredData.value,
      itemStyle: {
        borderRadius: 10,
        borderWidth: 2,
        borderColor: 'rgba(0,0,0,0)'
      },
      label: {
        show: true,
        formatter: ['{name|{b}}', '{value|{d}%}'].join('\n'),
        rich: {
          name: { fontSize: 14, color: '#fff', padding: [2, 0] },
          value: { fontSize: 16, fontWeight: 'bold', color: '#2ed573', padding: [2, 0] }
        }
      },
      animationDuration: 800
    }]
  }
})
</script>
