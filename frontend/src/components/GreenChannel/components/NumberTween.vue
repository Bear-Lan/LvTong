<template>
  <span>{{ display }}</span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  value: number
  duration?: number
  formatter?: (n: number) => string
}

const props = withDefaults(defineProps<Props>(), {
  duration: 800,
  formatter: (n: number) => Math.round(n).toLocaleString()
})

const display = ref(props.formatter(0))

let startTime = 0
let fromValue = 0
let toValue = 0
let animationFrameId = 0

// 清理数字中的逗号，支持国际化数字格式
const parseDisplayValue = (value: string): number => {
  // 移除所有非数字字符（逗号、空格等），只保留数字、小数点、负号
  const numericString = value.replace(/[^\d.-]/g, '')
  return Number(numericString) || 0
}

const animate = (timestamp: number) => {
  if (!startTime) startTime = timestamp
  
  const progress = Math.min((timestamp - startTime) / props.duration, 1)
  
  // 使用缓动函数让动画更自然
  const easedProgress = easeOutQuart(progress)
  const currentValue = fromValue + (toValue - fromValue) * easedProgress
  
  display.value = props.formatter(currentValue)
  
  if (progress < 1) {
    animationFrameId = requestAnimationFrame(animate)
  }
}

// 缓动函数 - 让动画更自然
const easeOutQuart = (x: number): number => {
  return 1 - Math.pow(1 - x, 4)
}

const startAnimation = (newValue: number) => {
  cancelAnimationFrame(animationFrameId)
  
  startTime = 0
  fromValue = parseDisplayValue(display.value)
  toValue = newValue
  
  // 如果值没有变化，直接更新显示
  if (fromValue === toValue) {
    display.value = props.formatter(toValue)
    return
  }
  
  animationFrameId = requestAnimationFrame(animate)
}

// 清理动画帧
const cleanup = () => {
  cancelAnimationFrame(animationFrameId)
}

watch(() => props.value, startAnimation, { immediate: true })

onMounted(() => {
  // 初始值动画
  startAnimation(props.value)
})

onUnmounted(cleanup)
</script>