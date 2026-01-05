<script setup>
import { ref, reactive, onMounted } from 'vue'
import { orderApi, mediaApi } from '../services/apiClient'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})
const statusLabels = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  ACTIVE: '履约中',
  CANCELLED: '已取消',
  COMPLETED: '已完成',
  TERMINATED: '已终止',
}
const terminationStatusLabels = {
  NONE: '未申请',
  REQUESTED: '待处理',
  APPROVED: '已同意',
  REJECTED: '已驳回',
}

const contractModal = reactive({
  open: false,
  orderId: null,
  contractUrl: '',
  submitting: false,
  uploading: false,
  error: '',
})
const contractFileInput = ref(null)

const terminationModal = reactive({
  open: false,
  orderId: null,
  action: 'approve',
  feedback: '',
  reason: '',
  submitting: false,
  error: '',
})

async function loadOrders(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await orderApi.landlord({ page: pageIndex, size: page.size })
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

async function confirm(orderId) {
  try {
    await orderApi.confirm(orderId)
    successMessage.value = '订单已确认'
    await loadOrders(page.current)
  } catch (err) {
    errorMessage.value = err.message || '确认失败'
  }
}

async function activate(orderId) {
  try {
    await orderApi.activate(orderId)
    successMessage.value = '订单已激活'
    await loadOrders(page.current)
  } catch (err) {
    errorMessage.value = err.message || '激活失败'
  }
}

function openUploadModal(order) {
  contractModal.orderId = order.id
  contractModal.contractUrl = order.contractUrl || ''
  contractModal.open = true
  contractModal.error = ''
}

function triggerContractUpload() {
  contractFileInput.value?.click()
}

async function handleContractFileChange(event) {
  const [file] = event.target.files || []
  event.target.value = ''
  if (!file) return
  contractModal.uploading = true
  contractModal.error = ''
  try {
    const response = await mediaApi.upload(file)
    contractModal.contractUrl = response.url
  } catch (err) {
    contractModal.error = err.message || '上传失败'
  } finally {
    contractModal.uploading = false
  }
}

async function submitContract() {
  if (!contractModal.orderId) return
  if (!contractModal.contractUrl) {
    contractModal.error = '请填写合同链接'
    return
  }
  contractModal.submitting = true
  try {
    await orderApi.uploadContract(contractModal.orderId, { contractUrl: contractModal.contractUrl })
    successMessage.value = '合同链接已保存'
    contractModal.open = false
    await loadOrders(page.current)
  } catch (err) {
    contractModal.error = err.message || '上传失败'
  } finally {
    contractModal.submitting = false
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

function statusText(status) {
  return statusLabels[status] || status
}

function terminationStatusText(status) {
  return terminationStatusLabels[status] || status || '未申请'
}

function openTerminationModal(order, action) {
  terminationModal.orderId = order.id
  terminationModal.action = action
  terminationModal.reason = order.terminationReason || '申请人未填写说明'
  terminationModal.feedback = ''
  terminationModal.error = ''
  terminationModal.open = true
}

async function submitTerminationDecision() {
  if (!terminationModal.orderId) return
  terminationModal.submitting = true
  terminationModal.error = ''
  try {
    if (terminationModal.action === 'approve') {
      await orderApi.approveTermination(terminationModal.orderId, {
        feedback: terminationModal.feedback || null,
      })
      successMessage.value = '已同意终止'
    } else {
      await orderApi.rejectTermination(terminationModal.orderId, {
        feedback: terminationModal.feedback || null,
      })
      successMessage.value = '已驳回终止申请'
    }
    terminationModal.open = false
    await loadOrders(page.current)
  } catch (err) {
    terminationModal.error = err.message || '操作失败'
  } finally {
    terminationModal.submitting = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">房东订单</h2>
      <span class="text-xs text-slate-500">共 {{ page.total }} 单</span>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载订单...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!orders.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      暂无订单。
    </div>

    <ul v-else class="space-y-4">
      <li
        v-for="order in orders"
        :key="order.id"
        class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-sm font-semibold text-slate-900">{{ order.houseTitle }}</p>
            <p class="text-xs text-slate-400">租客：{{ order.tenantName }}</p>
          </div>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
            状态：{{ statusText(order.status) }}
          </span>
        </div>
        <div class="mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-2">
          <span>租期：{{ order.startDate }} 至 {{ order.endDate }}</span>
          <span>租金：{{ order.monthlyRent }} 元 / 押金：{{ order.deposit }} 元</span>
        </div>
        <div
          v-if="order.terminationStatus && order.terminationStatus !== 'NONE'"
          class="mt-3 rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-700"
        >
          <p>终止状态：{{ terminationStatusText(order.terminationStatus) }}</p>
          <p v-if="order.terminationRequesterName" class="mt-1">申请人：{{ order.terminationRequesterName }}</p>
          <p v-if="order.terminationReason" class="mt-1">申请说明：{{ order.terminationReason }}</p>
          <p v-if="order.terminationFeedback" class="mt-1 text-amber-800">处理意见：{{ order.terminationFeedback }}</p>
        </div>
        <div class="mt-4 flex flex-wrap gap-2 text-xs text-slate-600">
          <button
            v-if="order.status === 'PENDING'"
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
            @click="confirm(order.id)"
          >
            确认订单
          </button>
          <button
            v-if="order.status === 'CONFIRMED'"
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
            @click="activate(order.id)"
          >
            激活订单
          </button>
          <button
            v-if="order.status === 'CONFIRMED' || order.status === 'ACTIVE'"
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
            @click="openUploadModal(order)"
          >
            {{ order.contractUrl ? '更新合同' : '上传合同' }}
          </button>
          <a
            v-if="order.contractUrl"
            :href="order.contractUrl"
            target="_blank"
            class="inline-flex items-center rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-indigo-600 transition hover:bg-slate-100"
          >
            查看合同
          </a>
          <button
            v-if="order.terminationStatus === 'REQUESTED'"
            type="button"
            class="rounded-lg border border-rose-200 px-3 py-1.5 text-rose-600 transition hover:bg-rose-50"
            @click="openTerminationModal(order, 'approve')"
          >
            同意终止
          </button>
          <button
            v-if="order.terminationStatus === 'REQUESTED'"
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
            @click="openTerminationModal(order, 'reject')"
          >
            驳回终止
          </button>
        </div>
      </li>
    </ul>

    <div v-if="successMessage" class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-600">
      {{ successMessage }}
    </div>

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

    <div
      v-if="contractModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="contractModal.open = false"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">上传合同</h3>
        <form class="mt-4 space-y-4" @submit.prevent="submitContract">
          <div class="flex flex-wrap items-center gap-3">
            <input
              ref="contractFileInput"
              type="file"
              class="hidden"
              accept="image/*,application/pdf"
              @change="handleContractFileChange"
            />
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed"
              :disabled="contractModal.uploading"
              @click="triggerContractUpload"
            >
              {{ contractModal.uploading ? '上传中...' : '上传图片/文件' }}
            </button>
            <span class="text-xs text-slate-400">支持 jpg/png，上传后自动生成合同链接。</span>
          </div>
          <div v-if="contractModal.contractUrl" class="rounded-xl border border-slate-200 bg-slate-50 p-3 text-center">
            <p class="text-xs text-slate-500">当前合同预览</p>
            <img
              v-if="/\\.(png|jpg|jpeg|gif|webp)$/i.test(contractModal.contractUrl)"
              :src="contractModal.contractUrl"
              alt="合同图片"
              class="mt-2 max-h-64 w-full rounded-lg object-contain"
            />
            <div v-else class="mt-2 rounded-lg bg-white px-4 py-6 text-xs text-slate-500">
              已上传文件：{{ contractModal.contractUrl }}
            </div>
            <a
              :href="contractModal.contractUrl"
              target="_blank"
              class="mt-2 inline-flex items-center justify-center rounded-lg border border-slate-200 px-3 py-1 text-xs text-indigo-600 hover:bg-slate-100"
            >
              查看原文件
            </a>
          </div>
          <p v-else class="rounded-lg border border-dashed border-slate-200 px-4 py-6 text-center text-xs text-slate-400">
            暂未选择合同图片，点击上方按钮上传
          </p>
          <p v-if="contractModal.error" class="rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
            {{ contractModal.error }}
          </p>
          <div class="flex items-center justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
              @click="contractModal.open = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="contractModal.submitting || !contractModal.contractUrl"
            >
              {{ contractModal.submitting ? '保存中...' : '保存合同' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="terminationModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="terminationModal.open = false"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">
          {{ terminationModal.action === 'approve' ? '同意终止' : '驳回终止' }}
        </h3>
        <form class="mt-4 space-y-4" @submit.prevent="submitTerminationDecision">
          <div class="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <p class="font-medium text-slate-900">申请说明</p>
            <p class="mt-1">{{ terminationModal.reason }}</p>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">处理意见（选填）</label>
            <textarea
              v-model="terminationModal.feedback"
              rows="3"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <p v-if="terminationModal.error" class="rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
            {{ terminationModal.error }}
          </p>
          <div class="flex items-center justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
              @click="terminationModal.open = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="terminationModal.submitting"
            >
              {{ terminationModal.submitting ? '处理中...' : '确认' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
