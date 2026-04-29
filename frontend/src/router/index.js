import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    meta: { title: '登录' },
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/greenchannel',
    name: 'GreenChannel',
    meta: { title: '绿通校验' },
    component: () => import('@/views/GreenChannel.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        meta: { title: '首页概览' },
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'users',
        name: 'UserManagement',
        meta: { title: '用户管理' },
        component: () => import('@/views/UserManagement.vue')
      },
      {
        path: 'inspection',
        name: 'VehicleInspection',
        meta: { title: '历史记录' },
        component: () => import('@/views/HistoricalRecords.vue')
      },
      {
        path: 'video',
        name: 'VideoMonitor',
        meta: { title: '视频监控' },
        component: () => import('@/views/VideoMonitor.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isAuthenticated()) {
    next('/login')
  } else if (to.path === '/login' && userStore.isAuthenticated()) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router