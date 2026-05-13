import request from './request'

export function fetchRules(params) {
  return request.get('/alert/rules', { params: params })
}

export function getRule(id) {
  return request.get('/alert/rules/' + id)
}

export function createRule(data) {
  return request.post('/alert/rules', data)
}

export function updateRule(id, data) {
  return request.put('/alert/rules/' + id, data)
}

export function disableRule(id) {
  return request.delete('/alert/rules/' + id)
}
