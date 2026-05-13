import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getMe } from '@/api/auth'

export var useAuthStore = defineStore('auth', function () {
  var token = ref(localStorage.getItem('token') || '')
  var username = ref('')
  var isLoggedIn = computed(function () { return !!token.value })

  function login(user, pwd) {
    return loginApi(user, pwd).then(function (res) {
      token.value = res.token
      localStorage.setItem('token', res.token)
      return fetchMe().then(function () {
        return res
      })
    })
  }

  function fetchMe() {
    return getMe().then(function (res) {
      username.value = res.username
    }).catch(function () {})
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
  }

  return { token: token, username: username, isLoggedIn: isLoggedIn, login: login, fetchMe: fetchMe, logout: logout }
})
