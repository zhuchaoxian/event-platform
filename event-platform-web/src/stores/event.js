import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchEvents, getEventStats } from '@/api/event'

export var useEventStore = defineStore('event', function () {
  var events = ref([])
  var total = ref(0)
  var stats = ref(null)
  var loading = ref(false)

  function load(params) {
    loading.value = true
    return fetchEvents(params || {}).then(function (page) {
      events.value = page.records
      total.value = page.total
    }).finally(function () {
      loading.value = false
    })
  }

  function loadStats(params) {
    return getEventStats(params || {}).then(function (res) {
      stats.value = res
    })
  }

  return { events: events, total: total, stats: stats, loading: loading, load: load, loadStats: loadStats }
})
