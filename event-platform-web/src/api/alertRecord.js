import request from './request'

export function fetchRecords(params) {
  return request.get('/alert/records', { params: params })
}

export function getRecord(id) {
  return request.get('/alert/records/' + id)
}

export function ackRecord(id) {
  return request.post('/alert/records/' + id + '/ack')
}

export function resolveRecord(id) {
  return request.post('/alert/records/' + id + '/resolve')
}
