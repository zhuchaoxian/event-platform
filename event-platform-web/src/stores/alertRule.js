import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRules, createRule, updateRule, disableRule } from '@/api/alertRule'

export var useAlertRuleStore = defineStore('alertRule', function () {
  var rules = ref([])
  var total = ref(0)
  var loading = ref(false)

  function load(params) {
    loading.value = true
    return fetchRules(params || {}).then(function (page) {
      rules.value = page.records
      total.value = page.total
    }).finally(function () {
      loading.value = false
    })
  }

  function create(data) {
    return createRule(data).then(function () { return load() })
  }

  function update(id, data) {
    return updateRule(id, data).then(function () { return load() })
  }

  function disable(id) {
    return disableRule(id).then(function () { return load() })
  }

  return { rules: rules, total: total, loading: loading, load: load, create: create, update: update, disable: disable }
})
