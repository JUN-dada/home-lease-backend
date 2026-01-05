<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { supportApi } from '../services/apiClient'
import { useSupportStore } from '../stores/support'
import { useChatNotificationStore } from '../stores/chatNotifications'

const tickets = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const supportStore = useSupportStore()
const chatStore = useChatNotificationStore()

const pagination = reactive({
  page: 0,
  size: 10,
  total: 0,
})

const form = reactive({
  subject: '',
  category: '',
  message: '',
  submitting: false,
})

async function loadTickets(pageIndex = 0, { silent = false } = {}) {
  if (!silent) {
    loading.value = true
  }
  errorMessage.value = ''
  try {
    const response = await supportApi.list({ page: pageIndex, size: pagination.size })
    tickets.value = response.content || []
    pagination.page = response.number || 0
    pagination.total = response.totalElements || 0
    chatStore.syncSupportTickets(tickets.value)
  } catch (err) {
    errorMessage.value = err.message || '加载工单失败'
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

function pageCount() {
  return Math.ceil(pagination.total / pagination.size) || 1
}

function changePage(direction) {
  const next = pagination.page + direction
  if (next < 0 || next >= pageCount()) return
  loadTickets(next)
}

async function submitTicket() {
  if (!form.subject.trim() || !form.message.trim()) {
    errorMessage.value = '请填写主题和问题描述'
    return
  }
  form.submitting = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const ticket = await supportApi.create({
      subject: form.subject,
      category: form.category || null,
      message: form.message,
    })
    successMessage.value = '已提交客服工单'
    form.subject = ''
    form.category = ''
    form.message = ''
    await loadTickets(0)
    openChat(ticket)
  } catch (err) {
    errorMessage.value = err.message || '提交失败'
  } finally {
    form.submitting = false
  }
}

function openChat(ticket) {
  supportStore.openTicket(ticket)
}

onMounted(() => {
  loadTickets()
})

watch(
  () => supportStore.refreshToken,
  () => {
    loadTickets(pagination.page, { silent: true })
  },
)
</script>

<template>
  <section class="space-y-6">
    <header class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">联系客服</h2>
      <p class="mt-1 text-sm text-slate-500">遇到平台问题时，可向管理员创建工单并实时沟通。</p>
      <form class="mt-4 grid gap-4 md:grid-cols-2" @submit.prevent="submitTicket">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">主题</label>
          <input
            v-model="form.subject"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="如：账单问题、认证咨询等"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">分类（可选）</label>
          <input
            v-model="form.category"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="如：支付、认证、其他"
          />
        </div>
        <div class="md:col-span-2 space-y-2">
          <label class="text-sm font-medium text-slate-700">问题描述</label>
          <textarea
            v-model="form.message"
            rows="3"
            class="w-full rounded-lg border border-slate-200 px-4 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请详细描述问题，方便管理员快速处理"
          />
        </div>
        <div class="md:col-span-2 flex items-center gap-3">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
            :disabled="form.submitting"
          >
            {{ form.submitting ? '提交中...' : '提交工单' }}
          </button>
          <p v-if="successMessage" class="text-sm text-emerald-600">{{ successMessage }}</p>
          <p v-if="errorMessage" class="text-sm text-rose-600">{{ errorMessage }}</p>
        </div>
      </form>
    </header>

    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h3 class="text-sm font-semibold text-slate-900">我的工单</h3>
        <span class="text-xs text-slate-500">{{ pagination.total }} 条</span>
      </div>
      <div v-if="loading" class="mt-4 rounded-lg border border-slate-100 bg-slate-50 px-4 py-3 text-xs text-slate-500">
        加载中...
      </div>
      <div v-else-if="!tickets.length" class="mt-6 rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无工单，欢迎随时联系我们。
      </div>
      <ul v-else class="mt-4 space-y-3">
        <li
          v-for="ticket in tickets"
          :key="ticket.id"
          class="flex flex-col gap-2 rounded-2xl border border-slate-200 p-4 text-sm text-slate-600 transition hover:bg-slate-50"
        >
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="text-base font-semibold text-slate-900">{{ ticket.subject }}</p>
              <p class="text-xs text-slate-400">编号：{{ ticket.id }}</p>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">{{ ticket.status }}</span>
          </div>
          <p class="text-xs text-slate-500">最新回复：{{ ticket.latestMessage || '暂无' }}</p>
          <div class="flex items-center justify-between text-xs text-slate-400">
            <span>创建时间：{{ ticket.createdAt }}</span>
            <button
              type="button"
              class="rounded-lg border border-indigo-200 px-3 py-1.5 text-indigo-600 transition hover:bg-indigo-50"
              @click="openChat(ticket)"
            >
              打开对话
            </button>
          </div>
        </li>
      </ul>
      <div v-if="tickets.length && pageCount() > 1" class="mt-4 flex items-center justify-center gap-4">
        <button
          type="button"
          class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="pagination.page === 0"
          @click="changePage(-1)"
        >
          上一页
        </button>
        <span class="text-xs text-slate-500">第 {{ pagination.page + 1 }} / {{ pageCount() }} 页</span>
        <button
          type="button"
          class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="pagination.page >= pageCount() - 1"
          @click="changePage(1)"
        >
          下一页
        </button>
      </div>
    </div>

  </section>
</template>
