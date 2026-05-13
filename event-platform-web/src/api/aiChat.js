import request from './request'

export function chat(data) {
  return request.post('/ai/chat', data)
}

export function createSession() {
  return request.post('/ai/sessions')
}

export function getMessages(sessionId) {
  return request.get('/ai/sessions/' + sessionId + '/messages')
}

export function reindexKnowledge() {
  return request.post('/ai/knowledge/reindex')
}
