import request from './request'

export function login(username, password) {
  return request.post('/auth/login', { username: username, password: password })
}

export function getMe() {
  return request.get('/auth/me')
}
