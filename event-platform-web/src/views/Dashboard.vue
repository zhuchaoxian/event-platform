<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <el-card class="stat-card" shadow="hover" :style="{ borderTop: '3px solid ' + card.color }">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card>
          <template #header><span>告警趋势（近7天）</span></template>
          <div ref="trendChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>事件类型分布</span></template>
          <div ref="pieChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px;">
      <template #header>
        <span>最近告警</span>
        <el-button type="primary" link style="float:right;" @click="$router.push('/alert/records')">查看全部</el-button>
      </template>
      <el-table :data="recentRecords" v-loading="tableLoading" empty-text="暂无告警数据" style="width:100%;">
        <el-table-column prop="deviceId" label="设备ID" width="120" />
        <el-table-column prop="eventType" label="事件类型" width="120" />
        <el-table-column prop="alertLevel" label="级别" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">
              {{ scope.row.alertLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" />
        <el-table-column prop="triggeredAt" label="触发时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { useAlertRecordStore } from '@/stores/alertRecord'
import * as echarts from 'echarts'

export default {
  name: 'Dashboard',
  layout: 'AppLayout',
  data: function () {
    return {
      statCards: [
        { key: 'active', label: '活跃告警', value: 0, color: '#e74c3c', icon: 'WarningFilled' },
        { key: 'acked', label: '已确认', value: 0, color: '#f39c12', icon: 'Check' },
        { key: 'resolved', label: '已消除', value: 0, color: '#27ae60', icon: 'CircleCheckFilled' },
        { key: 'total', label: '总事件数', value: '--', color: '#3498db', icon: 'DataLine' }
      ],
      recentRecords: [],
      tableLoading: false,
      trendChart: null,
      pieChart: null
    }
  },
  mounted: function () {
    this.initData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount: function () {
    window.removeEventListener('resize', this.handleResize)
    if (this.trendChart) this.trendChart.dispose()
    if (this.pieChart) this.pieChart.dispose()
  },
  methods: {
    initData: function () {
      var self = this
      var store = useAlertRecordStore()

      store.load({ status: 'ACTIVE', page: 1, size: 1 }).then(function () {
        self.statCards[0].value = store.total
      }).then(function () {
        return store.load({ status: 'ACKED', page: 1, size: 1 })
      }).then(function () {
        self.statCards[1].value = store.total
      }).then(function () {
        return store.load({ status: 'RESOLVED', page: 1, size: 1 })
      }).then(function () {
        self.statCards[2].value = store.total
      }).then(function () {
        return store.load({ page: 1, size: 5 })
      }).then(function () {
        self.recentRecords = store.records
        self.tableLoading = false
        self.initCharts()
      })
    },

    initCharts: function () {
      this.initTrendChart()
      this.initPieChart()
    },

    initTrendChart: function () {
      if (!this.$refs.trendChart) return
      this.trendChart = echarts.init(this.$refs.trendChart)
      var days = []
      var day = new Date()
      for (var i = 6; i >= 0; i--) {
        var d = new Date(day)
        d.setDate(d.getDate() - i)
        days.push((d.getMonth() + 1) + '/' + d.getDate())
      }
      var option = {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: days },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '告警数', type: 'line', smooth: true,
          data: [4, 7, 3, 9, 5, 12, 8],
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.35)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])},
          itemStyle: { color: '#409eff' },
          symbol: 'circle',
          symbolSize: 6
        }]
      }
      this.trendChart.setOption(option)
    },

    initPieChart: function () {
      if (!this.$refs.pieChart) return
      this.pieChart = echarts.init(this.$refs.pieChart)
      var option = {
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { fontSize: 12 } },
        series: [{
          name: '事件类型', type: 'pie', radius: ['45%', '75%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          data: [
            { value: 35, name: '违停' },
            { value: 20, name: '行人' },
            { value: 18, name: '拥堵' },
            { value: 15, name: '遗撒物' },
            { value: 12, name: '逆行' }
          ]
        }]
      }
      this.pieChart.setOption(option)
    },

    handleResize: function () {
      if (this.trendChart) this.trendChart.resize()
      if (this.pieChart) this.pieChart.resize()
    }
  }
}
</script>

<style scoped>
.dashboard { padding: 0; }
.stat-card { cursor: pointer; transition: transform 0.25s, box-shadow 0.25s; }
.stat-card:hover { transform: translateY(-3px); }
.stat-content { display: flex; align-items: center; gap: 16px; }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}
.stat-label { font-size: 13px; color: #999; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }
.chart-box { height: 320px; }
</style>
