import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

var routes = [
  {
    path: '/login',
    name: 'Login',
    component: function () { return import('@/views/Login.vue') },
    meta: { public: true, layout: 'none', title: '登录' }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: function () { return import('@/views/Dashboard.vue') },
    meta: { title: '仪表盘' }
  },
  {
    path: '/alert/rules',
    name: 'AlertRules',
    component: function () { return import('@/views/alert/AlertRuleList.vue') },
    meta: { title: '告警规则' }
  },
  {
    path: '/alert/records',
    name: 'AlertRecords',
    component: function () { return import('@/views/alert/AlertRecordList.vue') },
    meta: { title: '告警记录' }
  },
  {
    path: '/events',
    name: 'Events',
    component: function () { return import('@/views/event/EventList.vue') },
    meta: { title: '事件查询' }
  },
  {
    path: '/cameras',
    name: 'Cameras',
    component: function () { return import('@/views/camera/CameraList.vue') },
    meta: { title: '相机查询' }
  },
  {
    path: '/ai',
    name: 'AiChat',
    component: function () { return import('@/views/ai/AiChat.vue') },
    meta: { title: 'AI助手' }
  }
]

var router = createRouter({
  history: createWebHistory(),
  routes: routes
})

router.beforeEach(function (to, from, next) {
  var auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return next('/login')
  }
  next()
})

router.afterEach(function (to) {
  document.title = (to.meta.title || '仪表盘') + ' - 事件中台管理平台'
})

export default router
