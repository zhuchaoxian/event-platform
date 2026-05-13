import request from './request'

export function fetchCameras(params) {
  return request.get('/query/cameras', { params: params })
}
