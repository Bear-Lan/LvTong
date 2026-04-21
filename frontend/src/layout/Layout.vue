<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <img src="/favicon.png" alt="logo" class="logo" />
        <div v-if="!isCollapsed" class="brand-wrapper">
          <span class="brand">绿通快检系统</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#03D5CD"
        text-color="#e0f7f6"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/inspection">
          <el-icon><Van /></el-icon>
          <template #title>车辆查验</template>
        </el-menu-item>
        <el-menu-item index="#" @click="openDatascreen" v-if="userStore.userInfo.role === 0">
          <el-icon><Monitor /></el-icon>
          <template #title>3D大屏</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-waves">
  <svg class="wave wave1" viewBox="0 0 1200 600" preserveAspectRatio="none">
    <path d="M0,450 C200,420 400,550 600,350 C800,150 1000,250 1200,80 L1200,600 L0,600 Z" />
  </svg>
  <svg class="wave wave2" viewBox="0 0 1200 600" preserveAspectRatio="none">
    <path d="M0,480 C250,440 450,580 650,380 C850,180 1050,280 1200,120 L1200,600 L0,600 Z" />
  </svg>
  <svg class="wave wave3" viewBox="0 0 1200 600" preserveAspectRatio="none">
    <path d="M0,430 C150,400 350,520 550,320 C750,120 950,220 1200,50 L1200,600 L0,600 Z" />
  </svg>
  <svg class="wave wave4" viewBox="0 0 1200 600" preserveAspectRatio="none">
    <path d="M0,500 C300,460 500,600 700,400 C900,200 1100,300 1200,150 L1200,600 L0,600 Z" />
  </svg>
</div>
    </aside>

    <!-- 主体区域 -->
    <div class="main-wrapper" :class="{ collapsed: isCollapsed }">
      <!-- 顶部栏 -->
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">绿通快检系统</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item.meta?.title || item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" style="background: linear-gradient(135deg, #a855f7, #6d28d9);">
                {{ userStore.userInfo.realName?.charAt(0) || userStore.userInfo.username?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo.realName || userStore.userInfo.username }}</span>
              <el-tag size="small" :type="userStore.userInfo.role === 0 ? 'danger' : 'primary'">
                {{ userStore.userInfo.role === 0 ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, HomeFilled, User, SwitchButton, Van, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.filter(item => item.path !== '/')
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// 新窗口打开3D大屏
const openDatascreen = () => {
  window.open('/#/greenchannel', '_blank')
}
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  background: #03D5CD;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 100;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar.collapsed + .main-wrapper,
.main-wrapper.collapsed {
  margin-left: 64px;
}

.sidebar-header {
  height: auto;
  min-height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 10px;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  width: 50%;
  height: auto;
  object-fit: contain;
  flex-shrink: 0;
}

.brand-wrapper {
  text-align: center;
}

.brand {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
  line-height: 1.3;
  white-space: nowrap;
}

:deep(.el-menu) {
  border: none;
  flex: 1;
}

:deep(.el-menu-item) {
  height: 56px;
  line-height: 56px;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.15) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #5BA6A2 !important;
}

.sidebar-waves {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 360px; /* 容器调高，以展示巨大的坡度 */
  overflow: hidden;
  pointer-events: none;
  /* 如果你的背景不是渐变的，建议加上这个背景色，会让波浪更亮 */

}

.wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 250%; /* 宽度拉长，动画会更平滑，坡度更自然 */
  height: 100%;
}

.wave path {
  fill: rgba(144, 255, 252, 0.4); 
}

/* 调整各层的动画和明度，制造出图中的层叠感 */
.wave1 {
  animation: waveMove 15s linear infinite;
  opacity: 0.3;
}

.wave2 {
  animation: waveMove 12s linear infinite;
  opacity: 0.5;
  margin-left: -50px; /* 错开起始位置 */
}

.wave3 {
  animation: waveMove 10s linear infinite;
  opacity: 0.2; /* 淡淡的叠层 */
}

.wave4 {
  animation: waveMove 8s linear infinite;
  opacity: 0.6; /* 这一层最亮，突出主要坡度 */
}

/* 统一采用大范围平移，让波浪产生流动感 */
@keyframes waveMove {
  0% { transform: translateX(0); }
  50% { transform: translateX(-15%); } /* 加上一点点回弹感 */
  100% { transform: translateX(0); }
}

/* 主体区域 */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  margin-left: 220px;
  min-height: 100vh;
}

.main-wrapper.collapsed {
  margin-left: 64px;
}

/* 顶部栏 */
.header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.el-breadcrumb {
  font-size: 14px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.collapse-btn:hover {
  color: #a855f7;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 内容区 */
.content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
</style>
