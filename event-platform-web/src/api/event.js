import request from './request'

export function fetchEvents(params) {
  return request.get('/query/events', { params: params })
}

export function getEventStats(params) {
  return request.get('/query/events/stats', { params: params })
}
