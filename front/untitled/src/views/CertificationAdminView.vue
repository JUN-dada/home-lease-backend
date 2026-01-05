<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { certificationApi } from '../services/apiClient'
import { CERTIFICATION_STATUS_OPTIONS, getCertificationStatusLabel } from '../utils/statusLabels'

const certifications = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const route = useRoute()
const router = useRouter()

const filters = reactive({
  status: 'PENDING',
  page: 0,
  size: 10,
})

const reviewModal = reactive({
  open: false,
  certificationId: null,
  status: 'APPROVED',
  reason: '',
  submitting: false,
})

const pendingRouteReviewId = ref(extractReviewId(route.query.review))

async function loadCertifications(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await certificationApi.list({
      page: pageIndex,
      size: filters.size,
      status: filters.status || undefined,
    })
    certifications.value = response.content || []
    filters.page = response.number || 0
    maybeOpenPendingReview()
  } catch (err) {
    errorMessage.value = err.message || '加载认证申请失败'
  } finally {
    loading.value = false
  }
}

function openReviewModal(certificationId) {
  reviewModal.certificationId = certificationId
  reviewModal.status = 'APPROVED'
  reviewModal.reason = ''
  reviewModal.open = true
}

async function submitReview() {
  if (!reviewModal.certificationId) return
  reviewModal.submitting = true
  try {
    await certificationApi.review(reviewModal.certificationId, {
      status: reviewModal.status,
      reason: reviewModal.reason,
    })
    successMessage.value = '审核结果已提交'
    reviewModal.open = false
    await loadCertifications(filters.page)
  } catch (err) {
    errorMessage.value = err.message || '审核失败'
  } finally {
    reviewModal.submitting = false
  }
}

onMounted(() => {
  loadCertifications()
})

watch(
  () => route.query.review,
  (value) => {
    pendingRouteReviewId.value = extractReviewId(value)
    if (!loading.value) {
      maybeOpenPendingReview()
    }
  },
)

function extractReviewId(value) {
  if (Array.isArray(value)) {
    return value[0] ?? null
  }
  return value ?? null
}

function maybeOpenPendingReview() {
  if (!pendingRouteReviewId.value) return
  const target = certifications.value.find((item) => String(item.id) === String(pendingRouteReviewId.value))
  if (!target) return
  openReviewModal(target.id)
  pendingRouteReviewId.value = null
  clearReviewQuery()
}

function clearReviewQuery() {
  if (!route.query.review) return
  const nextQuery = { ...route.query }
  delete nextQuery.review
  router.replace({ query: nextQuery }).catch(() => {})
}
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <h2 class="text-base font-semibold text-slate-900">房东认证审核</h2>
        <div class="flex items-center gap-3 text-xs">
          <label class="text-slate-500">筛选状态</label>
          <select
            v-model="filters.status"
            class="rounded-lg border border-slate-200 px-3 py-1.5 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            @change="loadCertifications(0)"
          >
            <option
              v-for="option in CERTIFICATION_STATUS_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        正在加载申请...
      </div>

      <div v-else-if="errorMessage" class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
        {{ errorMessage }}
      </div>

      <div v-else-if="!certifications.length" class="mt-6 rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无相关申请。
      </div>

      <ul v-else class="mt-6 space-y-4">
        <li
          v-for="item in certifications"
          :key="item.id"
          class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold text-slate-900">申请编号：{{ item.id }}</p>
              <p class="text-xs text-slate-400">
                提交时间：{{ item.createdAt || '未知' }} | 审核时间：{{ item.reviewedAt || '未审核' }}
              </p>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
              当前状态：{{ getCertificationStatusLabel(item.status) }}
            </span>
          </div>
          <div class="mt-3 space-y-3 text-sm text-slate-600">
            <div>
              <p class="text-slate-500">资料附件：</p>
              <div v-if="item.documentUrls && item.documentUrls.length" class="mt-2 grid gap-3 sm:grid-cols-3 lg:grid-cols-4">
                <a
                  v-for="(url, index) in item.documentUrls"
                  :key="`${item.id}-${index}`"
                  :href="url"
                  target="_blank"
                  class="group relative overflow-hidden rounded-xl border border-slate-200"
                >
                  <img :src="url" alt="认证资料" class="h-28 w-full object-cover transition group-hover:scale-105" />
                  <span class="absolute bottom-2 right-2 rounded-full bg-black/60 px-2 py-0.5 text-[10px] text-white opacity-0 transition group-hover:opacity-100">
                    查看原图
                  </span>
                </a>
              </div>
              <p v-else class="text-xs text-slate-400">申请人未上传附件</p>
            </div>
            <p>申请说明：{{ item.reason || '无' }}</p>
          </div>
          <div class="mt-4">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100"
              @click="openReviewModal(item.id)"
            >
              审核
            </button>
          </div>
        </li>
      </ul>
    </section>

    <div v-if="successMessage" class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-600">
      {{ successMessage }}
    </div>

    <div
      v-if="reviewModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="reviewModal.open = false"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">审核申请</h3>
        <form class="mt-4 space-y-4" @submit.prevent="submitReview">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">审核结果</label>
            <select
              v-model="reviewModal.status"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="APPROVED">通过</option>
              <option value="REJECTED">驳回</option>
            </select>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">审核意见</label>
            <textarea
              v-model="reviewModal.reason"
              rows="3"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="填写审核意见，驳回时必填"
            />
          </div>
          <div class="flex items-center justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
              @click="reviewModal.open = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="reviewModal.submitting"
            >
              提交
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
