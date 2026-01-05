<script setup>
import { ref, reactive, onMounted } from 'vue'
import { contactApi } from '../services/apiClient'
import { getContactStatusLabel } from '../utils/statusLabels'

const contacts = ref([])
const loading = ref(false)
const errorMessage = ref('')

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

async function loadContacts(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await contactApi.admin({ page: pageIndex, size: page.size })
    contacts.value = response.content || []
    page.current = response.number || 0
    page.size = response.size || page.size
    page.total = response.totalElements || contacts.value.length
  } catch (err) {
    errorMessage.value = err.message || '加载数据失败'
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
  loadContacts(next)
}

onMounted(() => {
  loadContacts()
})
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">全站联系记录</h2>
      <span class="text-xs text-slate-500">共 {{ page.total }} 条</span>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!contacts.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      暂无记录。
    </div>

    <table v-else class="min-w-full divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th class="px-4 py-3 text-left">房源</th>
          <th class="px-4 py-3 text-left">租客</th>
          <th class="px-4 py-3 text-left">房东</th>
          <th class="px-4 py-3 text-left">状态</th>
          <th class="px-4 py-3 text-left">提交时间</th>
          <th class="px-4 py-3 text-left">备注</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100 text-sm text-slate-700">
        <tr v-for="contact in contacts" :key="contact.id" class="hover:bg-slate-50">
          <td class="px-4 py-3">
            <p class="font-medium text-slate-900">{{ contact.houseTitle }}</p>
            <p class="text-xs text-slate-400">ID: {{ contact.id }}</p>
          </td>
          <td class="px-4 py-3 text-xs text-slate-600">
            {{ contact.tenantName }}
          </td>
          <td class="px-4 py-3 text-xs text-slate-600">
            {{ contact.landlordName }}
          </td>
          <td class="px-4 py-3 text-xs">
            <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">{{ getContactStatusLabel(contact.status) }}</span>
          </td>
          <td class="px-4 py-3 text-xs text-slate-500">{{ contact.preferredVisitTime || '—' }}</td>
          <td class="px-4 py-3 text-xs text-slate-400">{{ contact.remarks || '暂无' }}</td>
        </tr>
      </tbody>
    </table>

    <div v-if="contacts.length && pageCount() > 1" class="flex items-center justify-center gap-4">
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
