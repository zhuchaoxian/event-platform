import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRecords, ackRecord, resolveRecord } from '@/api/alertRecord'

export var useAlertRecordStore = defineStore('alertRecord', function () {
  var records = ref([])
  var total = ref(0)
  var loading = ref(false)

  function load(params) {
    loading.value = true
    return fetchRecords(params || {}).then(function (page) {
      records.value = page.records
      total.value = page.total
    }).finally(function () {
      loading.value = false
    })
  }

  function ack(id) {
    return ackRecord(id).then(function () { return load() })
  }

  function resolve(id) {
    return resolveRecord(id).then(function () { return load() })
  }

  return { records: records, total: total, loading: loading, load: load, ack: ack, resolve: resolve }
})
