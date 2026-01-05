<script setup>
import { computed, ref } from 'vue'
import { ChatBubbleLeftRightIcon } from '@heroicons/vue/24/outline'
import { useChatNotificationStore } from '../stores/chatNotifications'

const chatStore = useChatNotificationStore()
const open = ref(false)

const unread = computed(() => chatStore.unreadTotal)
const conversations = computed(() => chatStore.sortedConversations.slice(0, 10))

function toggle() {
  open.value = !open.value
}

function close() {
  open.value = false
}

function openConversation(contactId) {
  chatStore.openConversation(contactId)
  close()
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
  <div class="relative">
    <button
      type="button"
      class="relative rounded-full border border-slate-200 p-2 text-slate-500 transition hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-600"
      @click.stop="toggle"
    >
      <ChatBubbleLeftRightIcon class="h-5 w-5" />
      <span
        v-if="unread"
        class="absolute -right-1 -top-1 inline-flex h-4 min-w-[16px] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-semibold text-white"
      >
        {{ unread > 99 ? '99+' : unread }}
      </span>
    </button>

    <div
      v-if="open"
      class="fixed inset-0 z-30"
      @click="close"
    />

    <div
      v-if="open"
      class="absolute right-0 z-40 mt-3 w-80 rounded-2xl border border-slate-200 bg-white p-4 text-sm shadow-2xl shadow-black/5"
    >
      <div class="flex items-center justify-between">
        <p class="text-sm font-semibold text-slate-900">消息中心</p>
        <span class="text-xs text-slate-400">{{ unread ? `未读 ${unread}` : '全部已读' }}</span>
      </div>
      <div class="mt-3 max-h-80 overflow-y-auto">
        <p v-if="!conversations.length" class="py-6 text-center text-xs text-slate-400">暂无聊天记录</p>
        <ul v-else class="divide-y divide-slate-100">
          <li
            v-for="item in conversations"
            :key="item.contactId"
            class="flex cursor-pointer items-start gap-3 py-3 transition hover:bg-indigo-50/50"
            @click="openConversation(item.contactId)"
          >
            <div
              class="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-600"
            >
              {{ (item.partnerName || '聊').slice(0, 2) }}
            </div>
            <div class="flex-1">
              <div class="flex items-center justify-between">
                <p class="text-sm font-semibold text-slate-900">
                  {{ item.partnerName || '聊天' }}
                </p>
                <span class="text-[10px] text-slate-400">{{ formatTime(item.lastMessageAt) }}</span>
              </div>
              <p class="text-xs text-slate-500">{{ item.houseTitle || '房源聊天下' }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ item.lastMessage || '暂无消息' }}</p>
            </div>
            <span
              v-if="item.unread"
              class="mt-1 inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-semibold text-white"
            >
              {{ item.unread > 99 ? '99+' : item.unread }}
            </span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
