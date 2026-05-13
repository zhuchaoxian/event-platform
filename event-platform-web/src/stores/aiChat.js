import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chat, createSession, getMessages, reindexKnowledge } from '@/api/aiChat'

export var useAiChatStore = defineStore('aiChat', function () {
  var sessionId = ref('')
  var messages = ref([])
  var loading = ref(false)

  function newSession() {
    return createSession().then(function (res) {
      sessionId.value = res.sessionId
      messages.value = []
    })
  }

  function send(text) {
    var p = sessionId.value ? Promise.resolve() : newSession()
    return p.then(function () {
      messages.value.push({ type: 'USER', text: text })
      loading.value = true
      return chat({ sessionId: sessionId.value, message: text }).then(function (res) {
        messages.value.push({ type: 'AI', data: res })
      }).finally(function () {
        loading.value = false
      })
    })
  }

  function loadHistory(sid) {
    return getMessages(sid).then(function (msgs) {
      messages.value = msgs
    })
  }

  function reindex() {
    return reindexKnowledge()
  }

  return {
    sessionId: sessionId, messages: messages, loading: loading,
    newSession: newSession, send: send, loadHistory: loadHistory, reindex: reindex
  }
})
