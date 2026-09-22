<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">我的申请</h1>
          <p class="arch-sub">在案的各项服务申请记录。</p>
        </div>
        <span class="arch-mark n-rec">FORM · 在册 {{ applications.length }} 份</span>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <div class="px-6 py-4 border-b border-line">
        <el-radio-group v-model="activeTab" @change="fetchData">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="PROCESSING">审批中</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
          <el-radio-button value="WITHDRAWN">已撤回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="applications" v-loading="loading" empty-text="暂无申请记录">
        <el-table-column prop="applicationNo" label="申请编号" width="180" />
        <el-table-column prop="serviceName" label="事项名称" min-width="150" />
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column prop="currentNode" label="当前节点" width="150" />
        <el-table-column label="审批状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="approval" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/application/detail/${row.id}`)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="px-6 py-4 border-t border-line flex justify-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getMyApplications } from '@/api/application'
import type { ServiceApplication } from '@/types/application'
import StatusTag from '@/components/common/StatusTag.vue'

const loading = ref(false)
const activeTab = ref('all')
const applications = ref<ServiceApplication[]>([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: pagination.page, pageSize: pagination.pageSize }
    if (activeTab.value !== 'all') params.status = activeTab.value
    const res = await getMyApplications(params)
    applications.value = res.data.records
    pagination.total = res.data.total
  } catch {} finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
