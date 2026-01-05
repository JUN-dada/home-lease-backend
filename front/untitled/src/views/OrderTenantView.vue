<script setup>
import { ref, reactive, onMounted } from 'vue'
import { orderApi, houseApi } from '../services/apiClient'

const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

const randomRecommended = ref([])
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

const terminationModal = reactive({
  open: false,
  orderId: null,
  reason: '',
  submitting: false,
  error: '',
})

async function loadOrders(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await orderApi.mine({ page: pageIndex, size: page.size })
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

async function loadRecommended() {
  try {
    randomRecommended.value = await houseApi.random(3)
  } catch (err) {
    console.warn('加载推荐失败', err)
  }
}

async function cancelOrder(orderId) {
  try {
    await orderApi.cancel(orderId)
    successMessage.value = '订单已取消'
    await loadOrders(page.current)
  } catch (err) {
    errorMessage.value = err.message || '取消失败'
  }
}

function openTerminationModal(orderId) {
  terminationModal.orderId = orderId
  terminationModal.reason = ''
  terminationModal.error = ''
  terminationModal.open = true
}

async function submitTerminationRequest() {
  if (!terminationModal.orderId) return
  terminationModal.submitting = true
  terminationModal.error = ''
  try {
    await orderApi.terminate(terminationModal.orderId, {
      reason: terminationModal.reason || null,
    })
    successMessage.value = '终止申请已提交'
    terminationModal.open = false
    await loadOrders(page.current)
  } catch (err) {
    terminationModal.error = err.message || '操作失败'
  } finally {
    terminationModal.submitting = false
  }
}

async function downloadContract(orderId) {
  try {
    const url = await orderApi.downloadContract(orderId)
    if (url) {
      window.open(url, '_blank')
    } else {
      successMessage.value = '暂未上传合同'
    }
  } catch (err) {
    errorMessage.value = err.message || '下载失败'
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
  loadRecommended()
})

function statusText(status) {
  return statusLabels[status] || status
}

function terminationStatusText(status) {
  return terminationStatusLabels[status] || status || '未申请'
}
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">我的租赁订单</h2>
        <span class="text-xs text-slate-500">共 {{ page.total }} 单</span>
      </div>

      <div v-if="loading" class="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        正在加载订单...
      </div>

      <div v-else-if="errorMessage" class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
        {{ errorMessage }}
      </div>

      <div v-else-if="!orders.length" class="mt-6 rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无订单记录。
      </div>

      <ul v-else class="mt-6 space-y-4">
        <li
          v-for="order in orders"
          :key="order.id"
          class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold text-slate-900">{{ order.houseTitle }}</p>
              <p class="text-xs text-slate-400">
                房东：{{ order.landlordName }} | 租期：{{ order.startDate }} 至 {{ order.endDate }}
              </p>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
              状态：{{ statusText(order.status) }}
            </span>
          </div>
          <div class="mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-2">
            <span>月租：{{ order.monthlyRent }} 元</span>
            <span>押金：{{ order.deposit }} 元</span>
          </div>
          <div
            v-if="order.terminationStatus && order.terminationStatus !== 'NONE'"
            class="mt-3 rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-700"
          >
            <p>终止状态：{{ terminationStatusText(order.terminationStatus) }}</p>
            <p v-if="order.terminationReason" class="mt-1">申请说明：{{ order.terminationReason }}</p>
            <p v-if="order.terminationFeedback" class="mt-1">处理意见：{{ order.terminationFeedback }}</p>
          </div>
          <div class="mt-4 flex flex-wrap gap-2 text-xs text-slate-600">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
              @click="downloadContract(order.id)"
            >
              查看合同
            </button>
            <button
              v-if="order.status === 'PENDING'"
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
              @click="cancelOrder(order.id)"
            >
              取消订单
            </button>
            <button
              v-if="order.status === 'ACTIVE' && (!order.terminationStatus || order.terminationStatus === 'NONE' || order.terminationStatus === 'REJECTED')"
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
              @click="openTerminationModal(order.id)"
            >
              申请终止
            </button>
          </div>
        </li>
      </ul>

      <div v-if="orders.length && pageCount() > 1" class="mt-4 flex items-center justify-center gap-4">
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

    <section v-if="randomRecommended.length" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">猜你喜欢</h2>
      <div class="mt-4 grid gap-4 md:grid-cols-3">
        <article
          v-for="house in randomRecommended"
          :key="house.id"
          class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600"
        >
          <p class="font-medium text-slate-900">{{ house.title }}</p>
          <p class="mt-1 text-xs text-slate-400">{{ house.regionName || '未指定地区' }}</p>
          <p class="mt-2 text-xs text-slate-500 line-clamp-2">{{ house.description || '暂无描述' }}</p>
        </article>
      </div>
    </section>

    <div v-if="successMessage" class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-600">
      {{ successMessage }}
    </div>

    <div
      v-if="terminationModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="terminationModal.open = false"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">申请终止租约</h3>
        <form class="mt-4 space-y-4" @submit.prevent="submitTerminationRequest">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">说明理由（选填）</label>
            <textarea
              v-model="terminationModal.reason"
              rows="3"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="例如：工作变动，需提前结束租约"
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
              {{ terminationModal.submitting ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
