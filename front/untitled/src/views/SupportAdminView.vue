<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { supportApi } from '../services/apiClient'
import { useSupportStore } from '../stores/support'
import { useChatNotificationStore } from '../stores/chatNotifications'

const tickets = ref([])
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({
  status: '',
  page: 0,
  size: 10,
  total: 0,
})
const supportStore = useSupportStore()
const chatStore = useChatNotificationStore()

async function loadTickets(pageIndex = 0, { silent = false } = {}) {
  if (!silent) {
    loading.value = true
  }
  errorMessage.value = ''
  try {
    const response = await supportApi.adminList({
      page: pageIndex,
      size: filters.size,
      status: filters.status || undefined,
    })
    tickets.value = response.content || []
    filters.page = response.number || 0
    filters.total = response.totalElements || 0
    chatStore.syncSupportTickets(tickets.value)
  } catch (err) {
    errorMessage.value = err.message || '加载工单失败'
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

function openChat(ticket) {
  supportStore.openTicket(ticket)
}

function pageCount() {
  return Math.ceil(filters.total / filters.size) || 1
}

function changePage(direction) {
  const next = filters.page + direction
  if (next < 0 || next >= pageCount()) return
  loadTickets(next)
}

onMounted(() => {
  loadTickets()
})

watch(
  () => supportStore.refreshToken,
  () => {
    loadTickets(filters.page, { silent: true })
  },
)
</script>

<template>
  <section class="space-y-6">
    <header class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <h2 class="text-base font-semibold text-slate-900">客服工单管理</h2>
        <div class="flex items-center gap-3 text-xs">
          <label class="text-slate-500">筛选状态</label>
          <select
            v-model="filters.status"
            class="rounded-lg border border-slate-200 px-3 py-1.5 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            @change="loadTickets(0)"
          >
            <option value="">全部</option>
            <option value="OPEN">进行中</option>
            <option value="IN_PROGRESS">处理中</option>
            <option value="RESOLVED">已解决</option>
            <option value="CLOSED">已关闭</option>
          </select>
        </div>
      </div>
      <div v-if="loading" class="mt-4 rounded-lg border border-slate-100 bg-slate-50 px-4 py-3 text-xs text-slate-500">
        正在加载工单...
      </div>
      <div v-else-if="errorMessage" class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-xs text-rose-600">
        {{ errorMessage }}
      </div>
      <div v-else-if="!tickets.length" class="mt-6 rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无相关工单。
      </div>
      <div v-else class="mt-6 space-y-3">
        <article
          v-for="ticket in tickets"
          :key="ticket.id"
          class="rounded-2xl border border-slate-200 p-4 text-sm text-slate-600 transition hover:bg-slate-50"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-base font-semibold text-slate-900">{{ ticket.subject }}</p>
              <p class="text-xs text-slate-400">
                编号：{{ ticket.id }} · 提交人：{{ ticket.requesterName || '未知' }}
              </p>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">{{ ticket.status }}</span>
          </div>
          <p class="mt-2 text-xs text-slate-500">最新消息：{{ ticket.latestMessage || '暂无' }}</p>
          <div class="mt-3 flex items-center justify-between text-xs text-slate-400">
            <span>创建：{{ ticket.createdAt }}</span>
            <button
              type="button"
              class="rounded-lg border border-indigo-200 px-3 py-1.5 text-indigo-600 transition hover:bg-indigo-50"
              @click="openChat(ticket)"
            >
              打开对话
            </button>
          </div>
        </article>
        <div class="flex items-center justify-center gap-4">
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="filters.page === 0"
            @click="changePage(-1)"
          >
            上一页
          </button>
          <span class="text-xs text-slate-500">第 {{ filters.page + 1 }} / {{ pageCount() }} 页</span>
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="filters.page >= pageCount() - 1"
            @click="changePage(1)"
          >
            下一页
          </button>
        </div>
      </div>
    </header>

  </section>
</template>
