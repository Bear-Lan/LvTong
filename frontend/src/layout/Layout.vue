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
        background-color="#1a1a2e"
        text-color="#a0a0b0"
        active-text-color="#a855f7"
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
      </el-menu>
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
import { Fold, Expand, HomeFilled, User, SwitchButton, Van, Document, Grid } from '@element-plus/icons-vue'
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
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  background: #1a1a2e;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  background-color: rgba(168, 85, 247, 0.1) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(168, 85, 247, 0.15) !important;
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
