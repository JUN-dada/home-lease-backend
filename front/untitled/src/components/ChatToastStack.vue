<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatNotificationStore } from '../stores/chatNotifications'
import { useSupportStore } from '../stores/support'

const chatStore = useChatNotificationStore()
const supportStore = useSupportStore()
const route = useRoute()
const toasts = computed(() => chatStore.toasts)
const suppressRoutes = new Set(['contacts-mine', 'contacts-landlord'])
const shouldHide = computed(() => suppressRoutes.has(route.name))

onMounted(() => {
  chatStore.bootstrap()
})

function open(toast) {
  if (toast.type === 'support' && toast.ticketId) {
    supportStore.openTicketById(toast.ticketId, toast.subject || toast.preview || '')
    chatStore.dismissToast(toast.id)
    return
  }
  if (toast.contactId) {
    chatStore.openConversation(toast.contactId)
  }
  chatStore.dismissToast(toast.id)
}

function formatTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<template>
  <div v-if="!shouldHide" class="pointer-events-none fixed right-4 top-4 z-40 flex flex-col gap-3">
    <transition-group name="toast" tag="div">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="pointer-events-auto w-72 rounded-2xl border border-indigo-100 bg-white/95 p-4 text-sm shadow-lg shadow-indigo-100 ring-1 ring-black/5"
      >
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="text-xs font-medium text-indigo-500">
              {{ toast.type === 'support' ? '客服提醒' : '聊天消息' }} · {{ formatTime(toast.createdAt) || '刚刚' }}
            </p>
            <p class="mt-1 text-sm font-semibold text-slate-900">{{ toast.senderName }}</p>
            <p class="mt-0.5 text-xs text-slate-500">
              {{ toast.preview || (toast.type === 'support' ? '新的客服消息' : '') }}
            </p>
          </div>
          <button
            type="button"
            class="rounded-full p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            @click="chatStore.dismissToast(toast.id)"
          >
            ✕
          </button>
        </div>
        <button
          type="button"
          class="mt-3 inline-flex w-full items-center justify-center rounded-xl bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-500"
          @click="open(toast)"
        >
          {{ toast.type === 'support' ? '查看客服消息' : '查看聊天' }}
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
