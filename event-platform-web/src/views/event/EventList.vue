<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;">
        <el-input v-model="filterDeviceId" placeholder="设备ID" clearable style="width:160px;" />
        <el-select v-model="filterEventType" placeholder="事件类型" clearable style="width:160px;">
          <el-option v-for="t in eventTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-date-picker v-model="filterTimeRange" type="datetimerange" range-separator="~"
          start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss"
          value-format="x" style="width:380px;" />
        <el-button type="primary" @click="doSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="events" style="width:100%;">
        <el-table-column prop="deviceId" label="设备ID" width="120" />
        <el-table-column prop="eventType" label="事件类型" width="120" />
        <el-table-column prop="eventTimestamp" label="事件时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.eventTimestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="payloadJson" label="Payload" min-width="200">
          <template #default="scope">
            <el-popover placement="left" :width="300" trigger="click">
              <template #default>
                <pre style="max-height:300px;overflow:auto;font-size:12px;">{{ formatJson(scope.row.payloadJson) }}</pre>
              </template>
              <template #reference>
                <el-button type="text" size="small">查看详情</el-button>
              </template>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="记录时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end;" layout="total, prev, pager, next"
        :total="total" :page-size="10" v-model:current-page="page" @current-change="doSearch" />
    </el-card>
  </div>
</template>

<script>
import { useEventStore } from '@/stores/event'
import { formatTime, formatJson, EVENT_TYPES } from '@/utils'

export default {
  name: 'EventList',
  layout: 'AppLayout',
  data: function () {
    return {
      eventTypes: EVENT_TYPES,
      filterDeviceId: '',
      filterEventType: '',
      filterTimeRange: null,
      page: 1
    }
  },
  computed: {
    events: function () { return useEventStore().events },
    total: function () { return useEventStore().total },
    loading: function () { return useEventStore().loading }
  },
  mounted: function () { this.doSearch() },
  methods: {
    formatTime: formatTime,
    formatJson: formatJson,
    doSearch: function () {
      var params = { page: this.page, size: 10 }
      if (this.filterDeviceId) params.deviceId = this.filterDeviceId
      if (this.filterEventType) params.type = this.filterEventType
      if (this.filterTimeRange && this.filterTimeRange.length === 2) {
        params.startTime = this.filterTimeRange[0]
        params.endTime = this.filterTimeRange[1]
      }
      useEventStore().load(params)
    }
  }
}
</script>
