<template>
  <div class="login-container">
    <!-- Left Panel -->
    <div class="left-panel">
      <!-- Decorative circle -->
      <div class="deco-circle"></div>

      <div class="left-content">
        <h1 class="brand-name">绿通快检系统</h1>

        <p class="top-tagline">X透视成像·AI大数据模型·创新查验机制</p>

        <div class="feature-list">
          <div class="feature-item">货物智能识别，自动标定</div>
          <div class="feature-item">车头智能识别，安全避让</div>
          <div class="feature-item">北斗卫星系统，授时定位</div>
          <div class="feature-item">查验机制优化，提升效率</div>
        </div>

        <div class="keywords">
          <span class="keyword">立体化核查</span>
          <span class="arrow">--></span>
          <span class="keyword">精细化管理</span>
          <span class="arrow">--></span>
          <span class="keyword">智能化决策</span>
        </div>
      </div>

      <div class="left-bottom">
        @2026达生智能 绿通检测系统 ·版本v1.1.5
      </div>
    </div>

    <!-- Right Panel -->
    <div class="right-panel">
      <div class="login-card">
        <div class="right-header">
          <img src="/favicon.png" alt="logo" class="logo" />
          <h1 class="right-title">绿通快检系统</h1>
        </div>

        <h2 class="login-title">欢迎登录</h2>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          size="large"
        >
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
          </div>

          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="bottom-links">
          <a class="contact-link" href="mailto:admin@lvtong.com">联系管理员? 点击这里</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm.username, loginForm.password)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
}

/* ==================== Left Panel ==================== */
.left-panel {
  flex: 1.2;
  background: linear-gradient(135deg, #a855f7 0%, #6d28d9 50%, #5b21b6 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  box-sizing: border-box;
}

.deco-circle {
  position: absolute;
  width: 350px;
  height: 350px;
  top: -120px;
  left: -120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.left-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: white;
  max-width: 420px;
  text-align: center;
  padding: 50px 0;
  box-sizing: border-box;
}

.brand-name {
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin: 0;
  letter-spacing: 6px;
  line-height: 1;
}

.top-tagline {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 2px;
  margin: 0;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 50px 0 0;
}

.feature-item {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 1px;
}

.keywords {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 50px 0 0;
}

.keyword {
  font-size: 15px;
  color: white;
  letter-spacing: 2px;
}

.arrow {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.left-bottom {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin: 50px 0 0;
}

/* ==================== Right Panel ==================== */
.right-panel {
  flex: 0.8;
  min-width: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 60px 40px;
  box-sizing: border-box;
}

.login-card {
  width: 320px;
}

.right-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  margin-right: 12px;
}

.right-title {
  font-size: 36px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 3px;
}

.login-title {
  font-size: 26px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 32px;
  text-align: center;
}

.login-form {
  margin-bottom: 0;
}

.form-options {
  margin: 0 0 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  background: linear-gradient(135deg, #a855f7 0%, #6d28d9 100%);
  border: none;
  border-radius: 8px;
  color: #fff;
  letter-spacing: 4px;
}

.login-btn:hover {
  background: linear-gradient(135deg, #9333ea 0%, #5b21b6 100%);
}

.bottom-links {
  text-align: center;
  margin: 20px 0 16px;
}

.contact-link {
  font-size: 13px;
  color: #a855f7;
  text-decoration: none;
}

.contact-link:hover {
  color: #9333ea;
}

.register-tip {
  text-align: center;
  color: #999;
  font-size: 14px;
}

.register-link {
  color: #a855f7;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  color: #9333ea;
}

@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
  }

  .left-panel {
    flex: 0 0 auto;
    padding: 50px 30px 40px;
  }

  .brand-name {
    font-size: 36px;
    letter-spacing: 4px;
  }

  .feature-list {
    display: none;
  }

  .right-panel {
    flex: 1;
    min-width: 0;
    padding: 40px 30px;
  }
}
</style>
