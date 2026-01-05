<script setup>
import { ref, reactive, onMounted } from 'vue'
import { orderApi } from '../services/apiClient'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

const terminationStatusLabels = {
  NONE: '未申请',
  REQUESTED: '待处理',
  APPROVED: '已同意',
  REJECTED: '已驳回',
}

async function loadOrders(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await orderApi.admin({ page: pageIndex, size: page.size })
    orders.value = response.content || []
    page.current = response.number || 0
    page.size = response.size || page.size
    page.total = response.totalElements || orders.value.length
  } catch (err) {
    errorMessage.value = err.message || '加载订单失败'
  } finally {
    loading.value = false
  }
}

function pageCount() {
  return Math.ceil(page.total / page.size)
}

function changePage(direction) {
  const next = page.current + direction
  if (next < 0 || next >= pageCount()) return
  loadOrders(next)
}

onMounted(() => {
  loadOrders()
})

function terminationStatusText(status) {
  return terminationStatusLabels[status] || status || '未申请'
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">全站订单</h2>
      <span class="text-xs text-slate-500">共 {{ page.total }} 单</span>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!orders.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      暂无订单。
    </div>

    <table v-else class="min-w-full divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th class="px-4 py-3 text-left">房源</th>
          <th class="px-4 py-3 text-left">租客</th>
          <th class="px-4 py-3 text-left">房东</th>
          <th class="px-4 py-3 text-left">租期</th>
          <th class="px-4 py-3 text-left">费用</th>
          <th class="px-4 py-3 text-left">状态</th>
          <th class="px-4 py-3 text-left">终止</th>
          <th class="px-4 py-3 text-left">合同</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100 text-sm text-slate-700">
        <tr v-for="order in orders" :key="order.id" class="hover:bg-slate-50">
          <td class="px-4 py-3">
            <p class="font-medium text-slate-900">{{ order.houseTitle }}</p>
            <p class="text-xs text-slate-400">ID: {{ order.id }}</p>
          </td>
          <td class="px-4 py-3 text-xs text-slate-600">{{ order.tenantName }}</td>
          <td class="px-4 py-3 text-xs text-slate-600">{{ order.landlordName }}</td>
          <td class="px-4 py-3 text-xs text-slate-500">{{ order.startDate }} - {{ order.endDate }}</td>
          <td class="px-4 py-3 text-xs text-slate-500">
            月租 {{ order.monthlyRent }} 元 <br />
            押金 {{ order.deposit }} 元
          </td>
          <td class="px-4 py-3 text-xs">
            <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">{{ order.status }}</span>
          </td>
          <td class="px-4 py-3 text-xs text-slate-500">
            <span class="font-medium text-slate-700">{{ terminationStatusText(order.terminationStatus) }}</span>
            <p v-if="order.terminationReason" class="mt-1 text-slate-400 line-clamp-2">按 {{ order.terminationReason }}</p>
          </td>
          <td class="px-4 py-3 text-xs">
            <a
              v-if="order.contractUrl"
              :href="order.contractUrl"
              target="_blank"
              class="text-indigo-600 hover:underline"
            >
              查看合同
            </a>
            <span v-else class="text-slate-400">未上传</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="orders.length && pageCount() > 1" class="flex items-center justify-center gap-4">
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="page.current === 0"
        @click="changePage(-1)"
      >
        上一页
      </button>
      <span class="text-xs text-slate-500">第 {{ page.current + 1 }} / {{ pageCount() }} 页</span>
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="page.current >= pageCount() - 1"
        @click="changePage(1)"
      >
        下一页
      </button>
    </div>
  </section>
</template>
