<template>
  <div class="user-mgmt-container">
    <!-- ==========================================
         左侧：用户列表（固定 320px）
    ========================================== -->
    <aside class="user-list-panel">
      <!-- 列表头部 -->
      <div class="list-header">
        <div class="list-header-top">
          <span class="list-title">用户列表</span>
          <span class="user-count">{{ filteredUsers.length }} 人</span>
        </div>
        <div class="search-wrapper">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户..."
            clearable
            size="large"
            :prefix-icon="Search"
          />
        </div>
        <div class="list-sort-bar">
          <el-select v-model="sortBy" size="default" placeholder="排序方式" style="width: 100%;">
            <el-option label="默认顺序" value="default" />
            <el-option label="最近登录 ↓" value="lastLoginDesc" />
            <el-option label="最近登录 ↑" value="lastLoginAsc" />
            <el-option label="按角色" value="role" />
            <el-option label="按班组" value="groupId" />
          </el-select>
        </div>
      </div>

      <!-- 新增按钮（管理员） -->
      <div class="list-add-btn" v-if="isAdmin">
        <el-button type="primary" style="width: 100%;" @click="openAddDialog">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
      </div>

      <!-- 用户列表 -->
      <div class="user-list" v-loading="loading">
        <div
          v-for="user in filteredUsers"
          :key="user.id"
          class="user-item"
          :class="{ active: selectedUser?.id === user.id }"
          @click="selectUser(user)"
        >
          <!-- 头像 -->
          <div class="user-avatar">
            {{ user.realName?.charAt(0) || user.username?.charAt(0) || 'U' }}
          </div>

          <!-- 用户信息 -->
          <div class="user-item-info">
            <div class="user-item-name">
              {{ user.realName || user.username }}
              <span v-if="user.id === userStore.userInfo.userId" class="self-badge">我</span>
            </div>
            <div class="user-item-meta">
              <span class="role-tag" :class="user.role === 0 ? 'role-admin' : 'role-user'">
                {{ user.roleText }}
              </span>
              <span class="group-tag" v-if="user.groupName">{{ user.groupName }}</span>
            </div>
            <div class="user-item-login">
              <el-icon><Clock /></el-icon>
              <span>{{ formatRelativeTime(user.lastLoginTime) }}</span>
            </div>
          </div>

          <!-- 状态 + 选中指示 -->
          <div class="user-item-right">
            <span class="status-dot" :class="user.status === 0 ? 'status-active' : 'status-disabled'">
              {{ user.status === 0 ? '正常' : '已禁用' }}
            </span>
            <el-icon v-if="selectedUser?.id === user.id" class="check-icon"><Check /></el-icon>
          </div>
        </div>

        <el-empty v-if="!loading && filteredUsers.length === 0" description="暂无用户" />
      </div>
    </aside>

    <!-- ==========================================
         右侧：用户详情
    ========================================== -->
    <main class="user-detail-panel" v-if="selectedUser">
      <!-- ===== 详情头部卡片 ===== -->
      <div class="detail-hero-card">
        <div class="hero-left">
          <!-- 头像 -->
          <div class="hero-avatar">
            {{ selectedUser.realName?.charAt(0) || selectedUser.username?.charAt(0) || 'U' }}
          </div>
          <!-- 用户名 + 标签 -->
          <div class="hero-info">
            <h2 class="hero-name">{{ selectedUser.realName || selectedUser.username }}</h2>
            <p class="hero-username" v-if="selectedUser.realName">@{{ selectedUser.username }}</p>
            <div class="hero-badges">
              <span class="role-tag" :class="selectedUser.role === 0 ? 'role-admin' : 'role-user'">
                {{ selectedUser.roleText }}
              </span>
              <span class="group-tag" v-if="selectedUser.groupName">{{ selectedUser.groupName }}</span>
              <span class="status-badge" :class="selectedUser.status === 0 ? 'badge-active' : 'badge-disabled'">
                {{ selectedUser.statusText }}
              </span>
            </div>
          </div>
        </div>

        <!-- 操作按钮组（右上角） -->
        <div class="hero-actions">
          <!-- 管理员操作栏 -->
          <template v-if="isAdmin">
            <el-button type="primary" @click="openEditDialog">
              <el-icon><Edit /></el-icon> 编辑信息
            </el-button>
            <el-button plain @click="confirmResetPassword" class="btn-ghost-primary">
              <el-icon><RefreshRight /></el-icon> 重置密码
            </el-button>
            <el-button class="btn-ghost-danger" @click="confirmDelete" :disabled="selectedUser.username === 'admin'">
              <el-icon><Delete /></el-icon> 删除用户
            </el-button>
          </template>
          <!-- 普通用户：只能编辑自己 -->
          <template v-else-if="selectedUser.id === userStore.userInfo.userId">
            <el-button type="primary" @click="openProfileDialog">
              <el-icon><Edit /></el-icon> 修改个人信息
            </el-button>
          </template>
        </div>
      </div>

      <!-- ===== 信息展示网格 ===== -->
      <div class="detail-content">

        <!-- 基本信息 -->
        <section class="info-section">
          <h3 class="section-title">基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">用户ID</span>
              <span class="info-value mono">{{ selectedUser.id }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ selectedUser.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">真实姓名</span>
              <span class="info-value">{{ selectedUser.realName || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属班组</span>
              <span class="info-value">{{ selectedUser.groupName || '未分配' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">角色</span>
              <span class="info-value">
                <span class="role-tag" :class="selectedUser.role === 0 ? 'role-admin' : 'role-user'">
                  {{ selectedUser.roleText }}
                </span>
              </span>
            </div>
            <div class="info-item" v-if="selectedUser.userTypeText">
              <span class="info-label">用户类型</span>
              <span class="info-value">{{ selectedUser.userTypeText }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">账号状态</span>
              <span class="info-value">
                <span class="status-badge" :class="selectedUser.status === 0 ? 'badge-active' : 'badge-disabled'">
                  {{ selectedUser.statusText }}
                </span>
              </span>
            </div>
          </div>
        </section>

        <div class="section-divider" />

        <!-- 联系方式 -->
        <section class="info-section">
          <h3 class="section-title">联系方式</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ selectedUser.phone || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ selectedUser.email || '—' }}</span>
            </div>
          </div>
        </section>

        <div class="section-divider" />

        <!-- 账号信息 -->
        <section class="info-section">
          <h3 class="section-title">账号信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(selectedUser.createdTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">最后登录</span>
              <span class="info-value">{{ formatDate(selectedUser.lastLoginTime) }}</span>
            </div>
          </div>
        </section>
      </div>
    </main>

    <!-- 无选中用户 -->
    <main class="user-detail-panel empty" v-else>
      <el-empty description="请从左侧选择一个用户" />
    </main>

    <!-- ==========================================
         弹窗区域
    ========================================== -->

    <!-- 管理员：编辑用户弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px" destroy-on-close class="form-dialog">
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="所属班组" prop="groupId">
          <el-select v-model="editForm.groupId" placeholder="请选择班组" clearable style="width: 100%;">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" style="width: 100%;">
            <el-option label="管理员" :value="0" />
            <el-option label="普通用户" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" style="width: 100%;">
            <el-option label="正常" :value="0" />
            <el-option label="已禁用" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="editForm.userType" multiple collapse-tags collapse-tags-tooltip placeholder="请选择用户类型" style="width: 100%;">
            <el-option v-for="t in userTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdminSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 普通用户：编辑个人信息弹窗 -->
    <el-dialog v-model="profileDialogVisible" title="修改个人信息" width="500px" destroy-on-close class="form-dialog">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileFormRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="profileForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-divider>修改密码（选填）</el-divider>
        <el-form-item label="原密码" prop="password">
          <el-input v-model="profileForm.password" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="profileForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleProfileSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 管理员：新增用户弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增用户" width="500px" destroy-on-close class="form-dialog">
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="所属班组" prop="groupId">
          <el-select v-model="addForm.groupId" placeholder="请选择班组" clearable style="width: 100%;">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="addForm.userType" multiple collapse-tags collapse-tags-tooltip placeholder="请选择用户类型" style="width: 100%;">
            <el-option v-for="t in userTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAddSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, Clock, Check, Edit, RefreshRight, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUser, deleteUser, updateProfile, register, resetPassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo.role === 0)

const loading = ref(false)
const submitLoading = ref(false)
const searchKeyword = ref('')
const sortBy = ref('default')
const users = ref([])
const groups = ref([])

// 用户类型选项：1=站长，2=班长，3=查验人员，4=复核人员
const userTypeOptions = [
  { value: '1', label: '站长' },
  { value: '2', label: '班长' },
  { value: '3', label: '查验人员' },
  { value: '4', label: '复核人员' }
]
const selectedUser = ref(null)

// 弹窗状态
const editDialogVisible = ref(false)
const profileDialogVisible = ref(false)
const addDialogVisible = ref(false)

// 表单 ref
const editFormRef = ref()
const profileFormRef = ref()
const addFormRef = ref()

// 管理员编辑表单
const editForm = ref({
  username: '', realName: '', email: '', phone: '',
  groupId: null, role: 1, status: 0, userType: ''
})

// 普通用户个人信息表单
const profileForm = ref({
  username: '', realName: '', email: '', phone: '',
  password: '', newPassword: ''
})

// 新增用户表单
const addForm = ref({
  username: '', password: '', realName: '', email: '', phone: '',
  groupId: null, userType: ''
})

// 表单验证规则
const editFormRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const profileFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const addFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 筛选 + 排序
const filteredUsers = computed(() => {
  let list = users.value

  // 关键词筛选
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(user =>
      user.username?.toLowerCase().includes(kw) ||
      user.realName?.toLowerCase().includes(kw) ||
      user.email?.toLowerCase().includes(kw) ||
      user.phone?.includes(kw) ||
      user.groupName?.toLowerCase().includes(kw)
    )
  }

  // 排序
  if (sortBy.value === 'lastLoginDesc') {
    list = [...list].sort((a, b) => {
      const aTime = a.lastLoginTime ? new Date(a.lastLoginTime).getTime() : 0
      const bTime = b.lastLoginTime ? new Date(b.lastLoginTime).getTime() : 0
      return bTime - aTime
    })
  } else if (sortBy.value === 'lastLoginAsc') {
    list = [...list].sort((a, b) => {
      const aTime = a.lastLoginTime ? new Date(a.lastLoginTime).getTime() : Infinity
      const bTime = b.lastLoginTime ? new Date(b.lastLoginTime).getTime() : Infinity
      return aTime - bTime
    })
  } else if (sortBy.value === 'role') {
    list = [...list].sort((a, b) => (a.role || 9) - (b.role || 9))
  } else if (sortBy.value === 'groupId') {
    list = [...list].sort((a, b) => (a.groupId || 999) - (b.groupId || 999))
  }

  return list
})

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList()
    users.value = res.data || []
    if (users.value.length > 0 && !selectedUser.value) {
      selectUser(users.value[0])
    }
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const loadGroups = async () => {
  // 班组：未分组（空）、1-5班对应1-5
  groups.value = [
    { id: 1, name: '班组1' },
    { id: 2, name: '班组2' },
    { id: 3, name: '班组3' },
    { id: 4, name: '班组4' },
    { id: 5, name: '班组5' }
  ]
}

const selectUser = (user) => {
  selectedUser.value = user
}

// 管理员：打开编辑弹窗
const openEditDialog = () => {
  editForm.value = {
    username: selectedUser.value.username,
    realName: selectedUser.value.realName || '',
    email: selectedUser.value.email || '',
    phone: selectedUser.value.phone || '',
    groupId: selectedUser.value.groupId ?? null,
    role: selectedUser.value.role ?? 1,
    status: selectedUser.value.status ?? 0,
    userType: userTypeToArray(selectedUser.value.userType)
  }
  editDialogVisible.value = true
}

// 管理员：提交编辑
const handleAdminSubmit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await updateUser(selectedUser.value.id, {
          realName: editForm.value.realName,
          email: editForm.value.email,
          phone: editForm.value.phone,
          groupId: editForm.value.groupId,
          role: editForm.value.role,
          status: editForm.value.status,
          userType: userTypeToString(editForm.value.userType)
        })
        ElMessage.success('更新成功')
        editDialogVisible.value = false
        await fetchUsers()
        const updated = users.value.find(u => u.id === selectedUser.value.id)
        if (updated) selectedUser.value = updated
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 普通用户：打开个人信息弹窗
const openProfileDialog = () => {
  profileForm.value = {
    username: selectedUser.value.username || '',
    realName: selectedUser.value.realName || '',
    email: selectedUser.value.email || '',
    phone: selectedUser.value.phone || '',
    password: '',
    newPassword: ''
  }
  profileDialogVisible.value = true
}

// 普通用户：提交个人信息修改
const handleProfileSubmit = async () => {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await updateProfile({
          username: profileForm.value.username,
          realName: profileForm.value.realName,
          email: profileForm.value.email,
          phone: profileForm.value.phone,
          password: profileForm.value.password || undefined,
          newPassword: profileForm.value.newPassword || undefined
        })
        ElMessage.success('更新成功')
        profileDialogVisible.value = false
        await fetchUsers()
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 管理员：打开新增弹窗
const openAddDialog = () => {
  addForm.value = { username: '', password: '', realName: '', email: '', phone: '', groupId: null }
  addDialogVisible.value = true
}

// 管理员：提交新增
const handleAddSubmit = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await register({
          username: addForm.value.username,
          password: addForm.value.password,
          realName: addForm.value.realName,
          email: addForm.value.email,
          phone: addForm.value.phone,
          groupId: addForm.value.groupId,
          userType: userTypeToString(addForm.value.userType)
        })
        ElMessage.success('新增成功')
        addDialogVisible.value = false
        await fetchUsers()
      } catch (error) {
        ElMessage.error(error.message || '新增失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除用户
const confirmDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定删除用户 "${selectedUser.value.username}" 吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteUser(selectedUser.value.id)
    ElMessage.success('删除成功')
    selectedUser.value = null
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 重置密码（管理员操作）
const confirmResetPassword = async () => {
  try {
    await ElMessageBox.confirm(
      `确定将用户 "${selectedUser.value.username}" 的密码重置为 "123456" 吗？`,
      '重置密码',
      { confirmButtonText: '确定重置', cancelButtonText: '取消', type: 'warning' }
    )
    await resetPassword(selectedUser.value.id)
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重置失败')
    }
  }
}

const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

const formatRelativeTime = (date) => {
  if (!date) return '从未登录'
  const now = new Date()
  const d = new Date(date)
  const diff = now - d
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'
  if (diff < 30 * day) return Math.floor(diff / (7 * day)) + '周前'
  return formatDate(date)
}

// userType 字符串转数组（用于多选框）
const userTypeToArray = (userType) => {
  if (!userType) return []
  return userType.split('|').filter(t => t)
}

// userType 数组转字符串（用于提交）
const userTypeToString = (arr) => {
  if (!arr || arr.length === 0) return ''
  return arr.join('|')
}

onMounted(() => {
  fetchUsers()
  loadGroups()
})
</script>

<style scoped>
/* ================================================
   用户管理主容器
   ================================================ */
.user-mgmt-container {
  display: flex;
  min-height: calc(100vh - 60px);
  background: var(--bg-page);
  padding-left: 16px;
  padding-top: 8px;
  padding-bottom: 8px;
  box-sizing: border-box;
  overflow: hidden;
}

/* ================================================
   1. 左侧用户列表面板（固定 320px）
   ================================================ */
.user-list-panel {
  width: 320px;
  flex-shrink: 0;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius);
}

/* 列表头部 */
.list-header {
  padding: 16px 16px 0;
  flex-shrink: 0;
}

.list-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.list-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-count {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 400;
}

.search-wrapper {
  margin-bottom: 8px;
}

.list-sort-bar {
  padding-bottom: 8px;
}

/* 新增按钮 */
.list-add-btn {
  padding: 0 16px 8px;
  flex-shrink: 0;
}

/* 用户列表 */
.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 12px;
}

/* 用户卡片 */
.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: background-color 0.15s ease, box-shadow 0.15s ease;
  position: relative;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.user-item:last-child {
  margin-bottom: 0;
}

.user-item:hover {
  background: var(--bg-page);
}

.user-item.active {
  background: var(--primary-lightest);
  border: 1px solid var(--primary);
  box-shadow: 0 0 0 1px var(--primary);
}

/* 头像 */
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
  letter-spacing: 0;
}

/* 用户信息 */
.user-item-info {
  flex: 1;
  min-width: 0;
}

.user-item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.self-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  height: 16px;
  background: var(--primary-lightest);
  color: var(--primary);
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-item-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 5px;
  flex-wrap: wrap;
}

.user-item-login {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}

.user-item-login .el-icon {
  font-size: 11px;
}

/* 角色/班组标签（浅色调） */
.role-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
}

.role-admin {
  background: #FEE2E2;
  color: #991B1B;
}

.role-user {
  background: #E0E7FF;
  color: #4338CA;
}

.group-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  font-size: 11px;
  font-weight: 500;
  background: #F1F5F9;
  color: #475569;
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  font-size: 11px;
  font-weight: 500;
}

.status-tag.status-active {
  background: #D1FAE5;
  color: #065F46;
}

.status-tag.status-disabled {
  background: #F1F5F9;
  color: #64748B;
}

/* 右侧：状态点 + 选中图标 */
.user-item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.status-dot {
  font-size: 11px;
  font-weight: 500;
}

.status-active {
  color: #10B981;
}

.status-disabled {
  color: #94A3B8;
}

.check-icon {
  color: var(--primary);
  font-size: 14px;
  font-weight: 700;
}

/* ================================================
   2. 右侧详情区
   ================================================ */
.user-detail-panel {
  flex: 1;
  padding-left: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.user-detail-panel.empty {
  justify-content: center;
  align-items: center;
}

/* ===== 详情头部卡片 ===== */
.detail-hero-card {
  background: var(--bg-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  min-width: 0;
}

.hero-avatar {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--primary), #818CF8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
  letter-spacing: 0;
  box-shadow: 0 4px 12px rgb(79 70 229 / 0.3);
}

.hero-info {
  min-width: 0;
}

.hero-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-username {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 10px;
}

.hero-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 600;
}

.badge-active {
  background: #D1FAE5;
  color: #065F46;
}

.badge-disabled {
  background: #F1F5F9;
  color: #64748B;
}

/* 操作按钮组 */
.hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

/* Ghost 主色按钮（描边） */
.btn-ghost-primary {
  color: var(--primary);
  border: 1px solid var(--primary) !important;
  background: transparent;
  font-weight: 500;
}

.btn-ghost-primary:hover {
  background: var(--primary-lightest) !important;
  color: var(--primary) !important;
}

/* Ghost 危险按钮 */
.btn-ghost-danger {
  color: #991B1B;
  border: 1px solid #FCA5A5 !important;
  background: #FEF2F2;
  font-weight: 500;
}

.btn-ghost-danger:hover {
  background: #FEE2E2 !important;
  color: #7F1D1D !important;
  border-color: #FCA5A5 !important;
}

/* ================================================
   3. 信息展示网格
   ================================================ */
.detail-content {
  background: var(--bg-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.info-section {
  padding: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 12px;
}

/* 2列网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  line-height: 1.5;
}

.info-value.mono {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}

/* 区块分隔线 */
.section-divider {
  height: 1px;
  background: var(--border-light);
  margin: 0;
}

/* ================================================
   4. 弹窗覆盖样式
   ================================================ */
.form-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-light);
  padding: 20px 24px;
  margin: 0;
}

.form-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 16px;
}

.form-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.form-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid var(--border-light);
  padding: 16px 24px;
}

/* ================================================
   5. 响应式
   ================================================ */
@media (max-width: 1100px) {
  .user-mgmt-container {
    flex-direction: column;
    height: auto;
    overflow: auto;
  }

  .user-list-panel {
    width: 100%;
    max-height: 400px;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }

  .user-detail-panel {
    min-height: 500px;
  }

  .detail-hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
