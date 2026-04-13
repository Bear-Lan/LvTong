<template>
  <div ref="el" :style="{ height: baseHeight }" class="chart" />
</template>
<script setup lang="ts">
import * as echarts from "echarts";
import { ref, onMounted, onUnmounted, watch, type PropType } from "vue";
const props = defineProps({
  option: {
    type: Object as PropType<echarts.EChartsCoreOption>,
    required: true,
  },
  chartHeight: {
    type: String
  }
});

const baseHeight = ref()


const clientHeight = () => {
  if (window.innerHeight == 1440) {
    baseHeight.value = '260px'
  } else if (window.innerHeight == 1080) {
    baseHeight.value = '350px'
  } else if (window.innerHeight < 1080) {
    baseHeight.value = '260px'
  } else {
    baseHeight.value = '255px'
  }
}
// clientHeight()
// window.addEventListener('resize', () => { clientHeight() })

const el = ref<HTMLDivElement>();
let chart: echarts.ECharts | null = null;
const init = () => {
  if (!el.value) return;
  chart = echarts.init(el.value);
  chart.setOption(props.option);
};
const resize = () => chart?.resize();
watch(
  () => props.option,
  (v) => chart?.setOption(v, true),
  { deep: true }
);
onMounted(() => {
  init();
  window.addEventListener("resize", resize);
});
onUnmounted(() => {
  window.removeEventListener("resize", resize);
  chart?.dispose();
});
</script>
<style scoped>
.chart {
  width: 100% ;
  height: 100% ;
}
</style>
