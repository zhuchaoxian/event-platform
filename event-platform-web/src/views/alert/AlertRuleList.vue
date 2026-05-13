<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center;">
        <el-select v-model="filterEventType" placeholder="事件类型" clearable style="width:180px;" @change="doSearch">
          <el-option v-for="t in eventTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="openCreate">新建规则</el-button>
      </div>

      <el-table v-loading="loading" :data="rules" style="width:100%;">
        <el-table-column prop="ruleName" label="名称" min-width="140" />
        <el-table-column prop="eventType" label="事件类型" width="120" />
        <el-table-column prop="ruleType" label="规则类型" width="100">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.ruleType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cooldownS" label="冷却(秒)" width="90" />
        <el-table-column prop="enabled" label="状态" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
              {{ scope.row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDisable(scope.row.id)">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end;" layout="total, prev, pager, next"
        :total="total" :page-size="10" v-model:current-page="page" @current-change="doSearch" />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="ruleForm.eventType" style="width:100%;">
            <el-option v-for="t in eventTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="ruleForm.ruleType" style="width:100%;">
            <el-option label="THRESHOLD" value="THRESHOLD" />
            <el-option label="FREQUENCY" value="FREQUENCY" />
            <el-option label="DURATION" value="DURATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值字段" prop="thresholdField">
          <el-input v-model="ruleForm.thresholdField" placeholder="如: count" />
        </el-form-item>
        <el-form-item label="运算符" prop="thresholdOp">
          <el-select v-model="ruleForm.thresholdOp" style="width:100%;">
            <el-option label="大于 (gt)" value="gt" />
            <el-option label="小于 (lt)" value="lt" />
            <el-option label="等于 (eq)" value="eq" />
            <el-option label="大于等于 (gte)" value="gte" />
            <el-option label="小于等于 (lte)" value="lte" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue">
          <el-input-number v-model="ruleForm.thresholdValue" :min="0" />
        </el-form-item>
        <el-form-item label="冷却(秒)" prop="cooldownS">
          <el-input-number v-model="ruleForm.cooldownS" :min="0" :max="3600" />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-input v-model="ruleForm.deviceId" placeholder="留空表示全局" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { useAlertRuleStore } from '@/stores/alertRule'
import { ElMessageBox } from 'element-plus'
import { EVENT_TYPES } from '@/utils'

export default {
  name: 'AlertRuleList',
  layout: 'AppLayout',
  data: function () {
    return {
      eventTypes: EVENT_TYPES,
      filterEventType: '',
      page: 1,
      dialogVisible: false,
      dialogTitle: '新建规则',
      editId: null,
      saving: false,
      editing: false,
      ruleForm: { ruleName: '', eventType: '', ruleType: 'THRESHOLD', thresholdField: '', thresholdOp: 'gt', thresholdValue: 0, cooldownS: 300, deviceId: '' },
      formRules: {
        ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
        ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
        thresholdField: [{ required: true, message: '请输入阈值字段', trigger: 'blur' }],
        thresholdOp: [{ required: true, message: '请选择运算符', trigger: 'change' }]
      }
    }
  },
  computed: {
    rules: function () { return useAlertRuleStore().rules },
    total: function () { return useAlertRuleStore().total },
    loading: function () { return useAlertRuleStore().loading }
  },
  mounted: function () { this.doSearch() },
  methods: {
    doSearch: function () {
      var params = { page: this.page, size: 10 }
      if (this.filterEventType) params.eventType = this.filterEventType
      useAlertRuleStore().load(params)
    },
    openCreate: function () {
      this.dialogTitle = '新建规则'
      this.editId = null
      this.ruleForm = { ruleName: '', eventType: '', ruleType: 'THRESHOLD', thresholdField: '', thresholdOp: 'gt', thresholdValue: 0, cooldownS: 300, deviceId: '' }
      this.dialogVisible = true
    },
    openEdit: function (row) {
      this.dialogTitle = '编辑规则'
      this.editId = row.id
      var tf = '', to = 'gt', tv = 0
      try { var t = JSON.parse(row.threshold); tf = t.field || ''; to = t.operator || 'gt'; tv = t.value || 0 } catch (e) {}
      this.ruleForm = {
        ruleName: row.ruleName, eventType: row.eventType, ruleType: row.ruleType,
        thresholdField: tf, thresholdOp: to, thresholdValue: tv,
        cooldownS: row.cooldownS, deviceId: row.deviceId || ''
      }
      this.dialogVisible = true
    },
    resetForm: function () {
      this.$refs.ruleFormRef && this.$refs.ruleFormRef.resetFields()
    },
    handleSave: function () {
      var self = this
      this.$refs.ruleFormRef.validate(function (valid) {
        if (!valid) return
        self.saving = true
        var store = useAlertRuleStore()
        var data = {
          ruleName: self.ruleForm.ruleName,
          eventType: self.ruleForm.eventType,
          ruleType: self.ruleForm.ruleType,
          threshold: JSON.stringify({ field: self.ruleForm.thresholdField, operator: self.ruleForm.thresholdOp, value: self.ruleForm.thresholdValue }),
          cooldownS: self.ruleForm.cooldownS,
          deviceId: self.ruleForm.deviceId
        }
        var p = self.editId
          ? store.update(self.editId, data)
          : store.create(data)
        p.then(function () {
          self.dialogVisible = false
          self.doSearch()
        }).finally(function () {
          self.saving = false
        })
      })
    },
    handleDisable: function (id) {
      var self = this
      ElMessageBox.confirm('确定禁用该规则?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消' }).then(function () {
        useAlertRuleStore().disable(id).then(function () { self.doSearch() })
      }).catch(function () {})
    }
  }
}
</script>
