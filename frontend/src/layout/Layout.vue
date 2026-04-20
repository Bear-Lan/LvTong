<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <img src="/favicon.png" alt="logo" class="logo" />
        <span v-if="!isCollapsed" class="brand">绿通快检系统</span>
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
        <el-menu-item index="#" @click="openDatascreen">
          <el-icon><Monitor /></el-icon>
          <template #title>3D大屏</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-waves">
        <svg class="wave wave1" viewBox="0 0 220 65" preserveAspectRatio="none">
          <path d="M0,30 C35,4 75,56 110,30 C145,4 185,56 220,30 L220,65 L0,65 Z" />
        </svg>
        <svg class="wave wave2" viewBox="0 0 220 65" preserveAspectRatio="none">
          <path d="M0,36 C45,10 85,62 110,36 C135,10 175,62 220,36 L220,65 L0,65 Z" />
        </svg>
        <svg class="wave wave3" viewBox="0 0 220 65" preserveAspectRatio="none">
          <path d="M0,42 C40,16 80,64 110,42 C140,16 180,64 220,42 L220,65 L0,65 Z" />
        </svg>
        <svg class="wave wave4" viewBox="0 0 220 65" preserveAspectRatio="none">
          <path d="M0,50 C35,24 75,66 110,50 C145,24 185,66 220,50 L220,65 L0,65 Z" />
        </svg>
      </div>
    </aside>

    <!-- 主体区域 -->
    <div class="main-wrapper">
      <!-- 顶部栏 -->
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
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
  window.open('/greenchannel', '_blank')
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
  position: relative;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  flex-shrink: 0;
}

.brand {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
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
  background-color: rgba(255, 255, 255, 0.25) !important;
}

.sidebar-waves {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 180px;
  overflow: hidden;
  pointer-events: none;
}

.wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 200%;
  height: 65px;
}

.wave path {
  width: 100%;
  height: 100%;
}

.wave1 {
  bottom: 50px;
  animation: waveMove1 6s ease-in-out infinite;
}
.wave1 path {
  fill: rgba(30, 120, 220, 0.35);
}

.wave2 {
  bottom: 33px;
  animation: waveMove2 5s ease-in-out infinite;
}
.wave2 path {
  fill: rgba(40, 140, 240, 0.4);
}

.wave3 {
  bottom: 16px;
  animation: waveMove3 4.5s ease-in-out infinite;
}
.wave3 path {
  fill: rgba(50, 160, 255, 0.45);
}

.wave4 {
  bottom: 0;
  animation: waveMove4 4s ease-in-out infinite;
}
.wave4 path {
  fill: rgba(60, 180, 255, 0.5);
}

@keyframes waveMove1 {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(-25%); }
}

@keyframes waveMove2 {
  0%, 100% { transform: translateX(-10%); }
  50% { transform: translateX(-35%); }
}

@keyframes waveMove3 {
  0%, 100% { transform: translateX(-5%); }
  50% { transform: translateX(-20%); }
}

@keyframes waveMove4 {
  0%, 100% { transform: translateX(-15%); }
  50% { transform: translateX(-30%); }
}

/* 主体区域 */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
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
