import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchCameras } from '@/api/camera'

export var useCameraStore = defineStore('camera', function () {
  var cameras = ref([])
  var total = ref(0)
  var loading = ref(false)

  function load(params) {
    loading.value = true
    return fetchCameras(params || {}).then(function (page) {
      cameras.value = page.records
      total.value = page.total
    }).finally(function () {
      loading.value = false
    })
  }

  return { cameras: cameras, total: total, loading: loading, load: load }
})
