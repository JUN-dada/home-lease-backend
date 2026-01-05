<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminAlertStore } from '../stores/adminAlerts'
import { useSupportStore } from '../stores/support'

const alertStore = useAdminAlertStore()
const supportStore = useSupportStore()
const router = useRouter()
const toasts = computed(() => alertStore.toasts)

function formatTime(value) {
  if (!value) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function handleAction(toast) {
  if (toast.type === 'support' && toast.ticketId) {
    supportStore.openTicketById(toast.ticketId, toast.subject || toast.requester || '')
    alertStore.dismissToast(toast.id)
    return
  }
  const targetQuery = toast.certificationId ? { review: toast.certificationId } : {}
  router
    .push({ path: '/certification/admin', query: targetQuery })
    .catch(() => {})
    .finally(() => alertStore.dismissToast(toast.id))
}
</script>

<template>
  <div class="pointer-events-none fixed right-4 top-44 z-40 flex flex-col gap-3">
    <transition-group name="toast" tag="div">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="pointer-events-auto w-80 rounded-2xl border border-amber-200 bg-white/95 p-4 text-sm shadow-lg shadow-amber-100 ring-1 ring-black/5"
      >
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="text-xs font-medium text-amber-600">
              {{ toast.type === 'support' ? '新的客服工单' : '房东认证待审核' }}
            </p>
            <template v-if="toast.type === 'support'">
              <p class="mt-0.5 text-base font-semibold text-slate-900">工单：{{ toast.subject }}</p>
              <p class="text-xs text-slate-500">提交人：{{ toast.requester }}</p>
              <p class="text-xs text-slate-500">状态：{{ toast.status }}</p>
            </template>
            <template v-else>
              <p class="mt-0.5 text-base font-semibold text-slate-900">
                申请号：{{ toast.certificationId }}
              </p>
              <p class="text-xs text-slate-500">申请人：{{ toast.applicant }}</p>
              <p class="text-xs text-slate-500">状态：{{ toast.status }}</p>
            </template>
            <p class="text-xs text-slate-400">时间：{{ formatTime(toast.createdAt) }}</p>
          </div>
          <button
            type="button"
            class="rounded-full p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            @click="alertStore.dismissToast(toast.id)"
          >
            ✕
          </button>
        </div>
        <button
          v-if="toast.type !== 'support'"
          type="button"
          class="mt-3 inline-flex w-full items-center justify-center rounded-xl bg-amber-500/90 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-amber-500"
          @click="handleAction(toast)"
        >
          前往审核
        </button>
        <button
          v-else
          type="button"
          class="mt-3 inline-flex w-full items-center justify-center rounded-xl bg-amber-500/90 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-amber-500"
          @click="handleAction(toast)"
        >
          查看工单
        </button>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
