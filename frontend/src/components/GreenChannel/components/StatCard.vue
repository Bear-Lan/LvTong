<template>
  <div class="kpi">
    <div class="kpi-icon">
      <img v-if="img.startsWith('/') || img.startsWith('http') || img.startsWith('@')" :src="img" style="width:100%;height:100%;padding:5px;border-radius:50%;" />
      <svg v-else class="icon" aria-hidden="true">
        <use :xlink:href="`#icon-${img}`"></use>
      </svg>
    </div>
    <div class="kpi-content">
      <div class="label">{{ label }}</div>
      <div class="val">
        <NumberTween :value="Number(value)" />
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import NumberTween from "./NumberTween.vue";
defineProps<{ label: string; value: string | number; img: string }>();
</script>
