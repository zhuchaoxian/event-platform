<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '200px'" class="aside">
      <div class="logo">
        <el-icon :size="20" style="vertical-align:middle;margin-right:4px;"><Monitor /></el-icon>
        <span v-show="!collapsed" class="logo-text">事件中台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="collapsed"
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#409eff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/alert/rules">
          <el-icon><Setting /></el-icon>
          <span>告警规则</span>
        </el-menu-item>
        <el-menu-item index="/alert/records">
          <el-icon><Bell /></el-icon>
          <span>告警记录</span>
        </el-menu-item>
        <el-menu-item index="/events">
          <el-icon><List /></el-icon>
          <span>事件查询</span>
        </el-menu-item>
        <el-menu-item index="/cameras">
          <el-icon><VideoCamera /></el-icon>
          <span>相机查询</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI助手</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon :size="22" class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="0" :hidden="true" style="margin-right:20px;">
            <el-icon :size="20" style="cursor:pointer;"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user">{{ username }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

export default {
  name: 'AppLayout',
  data: function () {
    return { collapsed: false }
  },
  computed: {
    activeMenu: function () {
      return this.$route.path
    },
    currentTitle: function () {
      return this.$route.meta.title || ''
    },
    username: function () {
      var auth = useAuthStore()
      return auth.username || '未登录'
    }
  },
  methods: {
    handleCommand: function (command) {
      if (command === 'logout') {
        var auth = useAuthStore()
        var router = useRouter()
        auth.logout()
        router.push('/login')
      }
    }
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1e293b; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-size: 18px; font-weight: bold; background: #1a2332; white-space: nowrap; }
.logo-text { display: inline-block; vertical-align: middle; }
.header { background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-right { display: flex; align-items: center; }
.collapse-btn { cursor: pointer; color: #666; }
.collapse-btn:hover { color: #409eff; }
.user { cursor: pointer; color: #666; white-space: nowrap; }
.el-main { background: #f0f2f5; padding: 20px; }

@media (max-width: 768px) {
  .aside { width: 64px !important; }
  .logo { font-size: 14px; }
  .header { padding: 0 12px; }
  .header-left { gap: 6px; }
  .el-main { padding: 12px; }
}
</style>
