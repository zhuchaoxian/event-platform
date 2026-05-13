<template>
  <div class="login-page">
    <div class="login-brand">
      <el-icon :size="56" class="brand-icon"><Monitor /></el-icon>
      <h1 class="brand-title">事件中台管理平台</h1>
      <p class="brand-desc">Event Platform Management</p>
    </div>
    <el-card class="login-card">
      <template #header>
        <h2 class="card-title">登录</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%;" size="large" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <p class="login-footer">&copy; 2025 Event Platform</p>
  </div>
</template>

<script>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  setup: function () {
    return {
      auth: useAuthStore(),
      router: useRouter()
    }
  },
  data: function () {
    var saved = {}
    try { saved = JSON.parse(localStorage.getItem('loginCred') || '{}') } catch (e) {}
    return {
      form: { username: saved.username || '', password: saved.password || '' },
      rememberMe: saved.rememberMe || false,
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    handleLogin: function () {
      var self = this
      this.$refs.formRef.validate(function (valid) {
        if (!valid) return
        self.loading = true
        self.auth.login(self.form.username, self.form.password).then(function () {
          if (self.rememberMe) {
            localStorage.setItem('loginCred', JSON.stringify({ username: self.form.username, password: self.form.password, rememberMe: true }))
          } else {
            localStorage.removeItem('loginCred')
          }
          self.router.push('/').catch(function (err) {
            console.error('路由跳转失败', err)
          })
        }).catch(function () {
          self.loading = false
          ElMessage.error('登录失败，请检查用户名和密码')
        })
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}
.login-page::before {
  content: '';
  position: absolute;
  width: 360px; height: 360px;
  border-radius: 50%;
  background: rgba(255,255,255,0.03);
  top: -120px; right: -120px;
}
.login-page::after {
  content: '';
  position: absolute;
  width: 480px; height: 480px;
  border-radius: 50%;
  background: rgba(255,255,255,0.02);
  bottom: -180px; left: -180px;
}
.login-brand {
  text-align: center;
  margin-bottom: 32px;
  z-index: 1;
}
.brand-icon {
  color: #409eff;
}
.brand-title {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  margin: 12px 0 4px;
  letter-spacing: 2px;
}
.brand-desc {
  color: rgba(255,255,255,0.45);
  font-size: 13px;
  margin: 0;
  letter-spacing: 1px;
}
.login-card {
  width: 400px;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.35);
  z-index: 1;
}
.card-title {
  text-align: center;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.login-footer {
  margin-top: 24px;
  color: rgba(255,255,255,0.35);
  font-size: 12px;
  text-align: center;
  z-index: 1;
}
</style>
