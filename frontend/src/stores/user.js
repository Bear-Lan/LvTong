import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const login = async (username, password) => {
    const res = await apiLogin({ username, password })
    if (res.data.token) {
      setToken(res.data.token)
      setUserInfo(res.data)
    }
    return res
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const isAuthenticated = () => {
    return !!token.value
  }

  // 是否是管理员
  const isAdmin = () => {
    return userInfo.value.role === 0
  }

  return {
    token,
    userInfo,
    login,
    logout,
    setToken,
    setUserInfo,
    isAuthenticated,
    isAdmin
  }
})
