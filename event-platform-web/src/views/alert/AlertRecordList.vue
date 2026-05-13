<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;">
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:140px;" @change="doSearch">
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="ACKED" value="ACKED" />
          <el-option label="RESOLVED" value="RESOLVED" />
        </el-select>
        <el-select v-model="filterEventType" placeholder="事件类型" clearable style="width:160px;" @change="doSearch">
          <el-option v-for="t in eventTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-input v-model="filterDeviceId" placeholder="设备ID" clearable style="width:160px;" @change="doSearch" @clear="doSearch" />
        <el-button type="primary" @click="doSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" style="width:100%;">
        <el-table-column prop="deviceId" label="设备ID" width="120" />
        <el-table-column prop="eventType" label="事件类型" width="120" />
        <el-table-column prop="alertLevel" label="级别" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">
              {{ scope.row.alertLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggeredAt" label="触发时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'ACTIVE'" type="primary" size="small" @click="handleAck(scope.row.id)">确认</el-button>
            <el-button v-if="scope.row.status === 'ACKED'" type="success" size="small" @click="handleResolve(scope.row.id)">消除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end;" layout="total, prev, pager, next"
        :total="total" :page-size="10" v-model:current-page="page" @current-change="doSearch" />
    </el-card>
  </div>
</template>

<script>
import { useAlertRecordStore } from '@/stores/alertRecord'
import { EVENT_TYPES } from '@/utils'

export default {
  name: 'AlertRecordList',
  layout: 'AppLayout',
  data: function () {
    return {
      eventTypes: EVENT_TYPES,
      filterStatus: '',
      filterEventType: '',
      filterDeviceId: '',
      page: 1
    }
  },
  computed: {
    records: function () { return useAlertRecordStore().records },
    total: function () { return useAlertRecordStore().total },
    loading: function () { return useAlertRecordStore().loading }
  },
  mounted: function () { this.doSearch() },
  methods: {
    doSearch: function () {
      var params = { page: this.page, size: 10 }
      if (this.filterStatus) params.status = this.filterStatus
      if (this.filterEventType) params.eventType = this.filterEventType
      if (this.filterDeviceId) params.deviceId = this.filterDeviceId
      useAlertRecordStore().load(params)
    },
    statusTagType: function (status) {
      return { ACTIVE: 'danger', ACKED: 'warning', RESOLVED: 'info' }[status] || 'info'
    },
    handleAck: function (id) {
      var self = this
      useAlertRecordStore().ack(id).then(function () { self.doSearch() })
    },
    handleResolve: function (id) {
      var self = this
      useAlertRecordStore().resolve(id).then(function () { self.doSearch() })
    }
  }
}
</script>
