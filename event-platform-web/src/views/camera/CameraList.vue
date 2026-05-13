<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;">
        <el-input v-model="filterCameraId" placeholder="相机ID" clearable style="width:160px;" />
        <el-input v-model="filterCameraName" placeholder="名称" clearable style="width:160px;" />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px;">
          <el-option label="在线" :value="1" />
          <el-option label="离线" :value="0" />
        </el-select>
        <el-button type="primary" @click="doSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="cameras" style="width:100%;">
        <el-table-column prop="cameraId" label="相机ID" width="120" />
        <el-table-column prop="cameraName" label="名称" width="140" />
        <el-table-column prop="longitude" label="经度" width="100" />
        <el-table-column prop="latitude" label="纬度" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button type="primary" size="small" link disabled>查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end;" layout="total, prev, pager, next"
        :total="total" :page-size="10" v-model:current-page="page" @current-change="doSearch" />
    </el-card>
  </div>
</template>

<script>
import { useCameraStore } from '@/stores/camera'
import { formatTime } from '@/utils'

export default {
  name: 'CameraList',
  layout: 'AppLayout',
  data: function () {
    return { filterCameraId: '', filterCameraName: '', filterStatus: '', page: 1 }
  },
  computed: {
    cameras: function () { return useCameraStore().cameras },
    total: function () { return useCameraStore().total },
    loading: function () { return useCameraStore().loading }
  },
  mounted: function () { this.doSearch() },
  methods: {
    formatTime: formatTime,
    doSearch: function () {
      var params = { page: this.page, size: 10 }
      if (this.filterCameraId) params.cameraId = this.filterCameraId
      if (this.filterCameraName) params.cameraName = this.filterCameraName
      if (this.filterStatus !== '') params.status = this.filterStatus
      useCameraStore().load(params)
    }
  }
}
</script>
