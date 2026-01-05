<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { createChatClient } from '../services/chatClient'
import { supportApi } from '../services/apiClient'
import { useChatNotificationStore } from '../stores/chatNotifications'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  ticketId: {
    type: Number,
    default: null,
  },
  subject: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close'])

const authStore = useAuthStore()
const chatNotificationStore = useChatNotificationStore()
const messages = ref([])
const loading = ref(false)
const chatError = ref('')
const messageInput = ref('')
const connectionState = ref('idle')
const messagesContainer = ref(null)
const historyPage = ref(0)
const historyFinished = ref(false)
const chatClientRef = ref(null)
const subscriptionRef = ref(null)

const currentUserId = computed(() => authStore.user?.id)

watch(
  () => props.open,
  (open) => {
    if (open) {
      initChat()
    } else {
      teardown()
    }
  },
)

watch(
  () => props.ticketId,
  () => {
    if (props.open) {
      teardown()
      initChat()
    }
  },
)

watch(
  () => authStore.token,
  (token) => {
    if (!token && props.open) {
      chatError.value = '登录状态失效，请重新登录'
      teardown()
    }
  },
)

watch(
  () => ({ open: props.open, ticketId: props.ticketId }),
  ({ open, ticketId }) => {
    if (open && ticketId) {
      chatNotificationStore.markSupportTicketRead(ticketId)
    }
  },
  { immediate: true },
)

onBeforeUnmount(teardown)

function teardown() {
  connectionState.value = 'idle'
  chatError.value = ''
  messages.value = []
  if (subscriptionRef.value) {
    subscriptionRef.value.unsubscribe()
    subscriptionRef.value = null
  }
  if (chatClientRef.value) {
    chatClientRef.value.disconnect()
    chatClientRef.value = null
  }
}

async function initChat() {
  if (!props.ticketId || !authStore.token) {
    chatError.value = '请选择要查看的工单'
    return
  }
  loading.value = true
  chatError.value = ''
  historyPage.value = -1
  historyFinished.value = false
  messages.value = []
  try {
    await loadHistory(true)
    setupSocket()
  } catch (err) {
    chatError.value = err.message || '初始化客服对话失败'
  } finally {
    loading.value = false
  }
}

async function loadHistory(reset = false) {
  if (!props.ticketId || historyFinished.value) return
  loading.value = true
  try {
    const page = reset ? 0 : historyPage.value + 1
    const data = await supportApi.messages(props.ticketId, { page, size: 50 })
    historyPage.value = page
    historyFinished.value = data?.last ?? true
    const content = data?.content || []
    if (reset) {
      messages.value = content
    } else {
      messages.value = [...content, ...messages.value]
    }
    await scrollToBottom()
  } catch (err) {
    chatError.value = err.message || '加载客服消息失败'
  } finally {
    loading.value = false
  }
}

function setupSocket() {
  if (!props.ticketId) {
    return
  }
  if (chatClientRef.value) {
    if (connectionState.value === 'connected' && !subscriptionRef.value) {
      subscriptionRef.value = chatClientRef.value.subscribeSupport(props.ticketId, handleIncoming)
    }
    return
  }
  connectionState.value = 'connecting'
  chatClientRef.value = createChatClient({
    token: authStore.token,
    onConnected: () => {
      connectionState.value = 'connected'
      subscriptionRef.value = chatClientRef.value.subscribeSupport(props.ticketId, handleIncoming)
    },
    onMessage: handleIncoming,
    onError: (msg) => {
      chatError.value = msg
      connectionState.value = 'error'
    },
  })
}

function handleIncoming(message) {
  if (!message || !message.id) return
  const exists = messages.value.find(item => item.id === message.id)
  if (!exists) {
    messages.value = [...messages.value, message]
    scrollToBottom()
    if (props.open && props.ticketId) {
      chatNotificationStore.markSupportTicketRead(props.ticketId, message.createdAt)
    }
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function isMine(message) {
  return message.senderId && message.senderId === currentUserId.value
}

function canSend() {
  return props.ticketId && authStore.token && connectionState.value === 'connected'
}

function formatTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatPreview(subject) {
  return subject || '客服对话'
}

async function handleSend() {
  const trimmed = messageInput.value.trim()
  if (!trimmed) {
    return
  }
  if (!chatClientRef.value || !canSend()) {
    setupSocket()
    if (!canSend()) {
      chatError.value = '连接未建立，请稍后重试'
      return
    }
  }
  chatClientRef.value.sendSupport(props.ticketId, {
    content: trimmed,
    attachmentUrls: [],
  })
  messageInput.value = ''
}
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-40 flex items-center justify-end bg-slate-900/30" @click.self="emit('close')">
    <div class="h-full w-full max-w-md bg-white shadow-xl">
      <header class="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <div>
          <p class="text-sm text-slate-500">客服工单</p>
          <h3 class="text-lg font-semibold text-slate-900">{{ formatPreview(subject) }}</h3>
          <p class="text-xs text-slate-400">
            {{ connectionState === 'connected' ? '已连接' : connectionState === 'connecting' ? '连接中...' : '未连接' }}
          </p>
        </div>
        <button
          type="button"
          class="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>
      <section class="flex h-[calc(100%-64px)] flex-col">
        <div ref="messagesContainer" class="flex-1 space-y-3 overflow-y-auto px-4 py-3">
          <p v-if="loading && !messages.length" class="text-center text-sm text-slate-400">加载客服消息中...</p>
          <p v-else-if="!messages.length" class="text-center text-sm text-slate-400">开始与管理员对话</p>
          <div
            v-for="message in messages"
            :key="message.id"
            class="flex flex-col"
            :class="isMine(message) ? 'items-end' : 'items-start'"
          >
            <div class="mb-1 text-xs text-slate-400">
              {{ message.senderName || (isMine(message) ? '我' : '管理员') }} · {{ formatTime(message.createdAt) }}
            </div>
            <div
              class="max-w-[80%] rounded-2xl px-4 py-2 text-sm"
              :class="isMine(message) ? 'bg-indigo-600 text-white rounded-br-md' : 'bg-slate-100 text-slate-800 rounded-bl-md'"
            >
              <p class="whitespace-pre-line">
                {{ message.content }}
              </p>
            </div>
          </div>
        </div>
        <div class="border-t border-slate-200 p-3 space-y-3">
          <p v-if="chatError" class="rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-600">{{ chatError }}</p>
          <textarea
            v-model="messageInput"
            rows="3"
            placeholder="输入消息..."
            class="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
          <div class="flex items-center justify-between">
            <button
              type="button"
              class="rounded-lg px-3 py-1.5 text-sm text-slate-500 hover:bg-slate-100"
              @click="emit('close')"
            >
              关闭
            </button>
            <button
              type="button"
              class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="!messageInput.trim()"
              @click="handleSend"
            >
              发送
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
