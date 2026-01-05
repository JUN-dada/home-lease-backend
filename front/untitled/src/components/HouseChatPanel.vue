<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { createChatClient, ensureContact, fetchHistory } from '../services/chatClient'
import { mediaApi } from '../services/apiClient'
import { useChatNotificationStore } from '../stores/chatNotifications'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  houseId: {
    type: Number,
    default: null,
  },
  houseTitle: {
    type: String,
    default: '',
  },
  contactId: {
    type: Number,
    default: null,
  },
  headerTitle: {
    type: String,
    default: '',
  },
  autoEnsure: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['close'])

const authStore = useAuthStore()
const chatNotificationStore = useChatNotificationStore()

const contactRecord = ref(null)
const messages = ref([])
const loading = ref(false)
const connectionState = ref('idle')
const chatError = ref('')
const messageInput = ref('')
const attachments = ref([])
const uploading = ref(false)
const sending = ref(false)
const historyPage = ref(0)
const historyFinished = ref(false)
const messagesContainer = ref(null)

const chatClientRef = ref(null)
const subscriptionRef = ref(null)
const pendingQueue = []

const currentUserId = computed(() => authStore.user?.id)
const panelTitle = computed(() => props.headerTitle || props.houseTitle || '房源聊天')

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      initChat()
    } else {
      teardown()
    }
  },
)

watch(
  () => props.houseId,
  () => {
    if (props.open && props.autoEnsure) {
      teardown()
      initChat()
    }
  },
)

watch(
  () => props.contactId,
  () => {
    if (props.open && !props.autoEnsure) {
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
  () => ({ open: props.open, contactId: props.contactId }),
  ({ open, contactId }) => {
    if (open && contactId) {
      chatNotificationStore.setActiveContact(contactId)
    } else if (!open && contactId) {
      chatNotificationStore.clearActiveContact(contactId)
    }
  },
  { immediate: true },
)

onBeforeUnmount(teardown)

function teardown() {
  connectionState.value = 'idle'
  chatError.value = ''
  pendingQueue.length = 0
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
  if (!authStore.token) {
    chatError.value = '请先登录后再发起聊天'
    return
  }
  loading.value = true
  chatError.value = ''
  connectionState.value = 'connecting'
  historyPage.value = -1
  historyFinished.value = false
  messages.value = []
  try {
    await resolveContactRecord()
    await loadHistory(true)
    setupSocket()
  } catch (err) {
    chatError.value = err.message || '初始化聊天失败'
    connectionState.value = 'error'
  } finally {
    loading.value = false
  }
}

async function loadHistory(reset = false) {
  if (!contactRecord.value || historyFinished.value) return
  loading.value = true
  chatError.value = ''
  try {
    const page = reset ? 0 : historyPage.value + 1
    const data = await fetchHistory(contactRecord.value.id, {
      page,
      size: 50,
    })
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
    chatError.value = err.message || '加载聊天记录失败'
  } finally {
    loading.value = false
  }
}

function setupSocket() {
  if (!contactRecord.value) {
    return
  }
  if (chatClientRef.value) {
    if (connectionState.value === 'connected' && !subscriptionRef.value) {
      subscriptionRef.value = chatClientRef.value.subscribe(contactRecord.value.id)
      flushPendingQueue()
    }
    return
  }
  connectionState.value = 'connecting'
  chatClientRef.value = createChatClient({
    token: authStore.token,
    onConnected: () => {
      connectionState.value = 'connected'
      subscriptionRef.value = chatClientRef.value.subscribe(contactRecord.value.id)
      flushPendingQueue()
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

async function handleSend() {
  const trimmed = messageInput.value.trim()
  if (!trimmed && attachments.value.length === 0) {
    return
  }
  sending.value = true
  chatError.value = ''
  const payload = {
    content: trimmed || null,
    imageUrls: attachments.value.map(item => item.url),
  }
  pendingQueue.push(payload)
  messageInput.value = ''
  attachments.value = []
  flushPendingQueue()
  if (!chatClientRef.value || connectionState.value !== 'connected') {
    setupSocket()
  }
  sending.value = false
}

async function handleFileChange(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  uploading.value = true
  chatError.value = ''
  try {
    for (const file of files) {
      if (!file.type.startsWith('image/')) continue
      const result = await mediaApi.upload(file, 'IMAGE')
      attachments.value.push({
        name: file.name,
        url: result.url,
      })
    }
  } catch (err) {
    chatError.value = err.message || '图片上传失败'
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function removeAttachment(index) {
  attachments.value.splice(index, 1)
}

const hasMessages = computed(() => messages.value.length > 0)

function formatTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function flushPendingQueue() {
  if (!contactRecord.value || !chatClientRef.value || connectionState.value !== 'connected') {
    return
  }
  while (pendingQueue.length) {
    const payload = pendingQueue.shift()
    try {
      chatClientRef.value.send(contactRecord.value.id, payload)
    } catch (err) {
      chatError.value = err.message || '发送失败'
      pendingQueue.unshift(payload)
      break
    }
  }
}

async function resolveContactRecord() {
  if (props.contactId) {
    contactRecord.value = { id: props.contactId }
    return
  }
  if (!props.autoEnsure) {
    throw new Error('缺少联系记录ID')
  }
  if (!props.houseId) {
    throw new Error('缺少房源信息')
  }
  contactRecord.value = await ensureContact({
    houseId: props.houseId,
    message: `咨询房源：${props.houseTitle || ''}`,
    preferredVisitTime: null,
  })
}

</script>

<template>
  <div v-if="open" class="fixed inset-0 z-40 flex items-center justify-end bg-slate-900/30" @click.self="emit('close')">
    <div class="h-full   w-full max-w-md bg-white shadow-xl">
      <header class="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <div>
          <p class="text-sm text-slate-500">实时沟通</p>
          <h3 class="text-lg font-semibold text-slate-900">{{ panelTitle }}</h3>
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

      <section class="flex h-[calc(100%-104px)] flex-col">
        <div ref="messagesContainer" class="flex-1 overflow-y-auto px-4 py-7 space-y-3">
          <p v-if="loading && !hasMessages" class="text-center text-sm text-slate-400">加载聊天中...</p>
          <p v-else-if="!hasMessages" class="text-center text-sm text-slate-400">开始与房东实时沟通吧</p>
          <div
            v-for="message in messages"
            :key="message.id"
            class="flex flex-col"
            :class="isMine(message) ? 'items-end' : 'items-start'"
          >
            <div class="mb-1 text-xs text-slate-400">
              {{ message.senderName || (isMine(message) ? '我' : '对方') }} · {{ formatTime(message.createdAt) }}
            </div>
            <div
              class="max-w-[80%] rounded-2xl px-4 py-2 text-sm"
              :class="isMine(message) ? 'bg-indigo-600 text-white rounded-br-md' : 'bg-slate-100 text-slate-800 rounded-bl-md'"
            >
              <p v-if="message.content" class="whitespace-pre-line">
                {{ message.content }}
              </p>
              <div v-if="message.imageUrls?.length" class="mt-2 grid gap-2">
                <img
                  v-for="url in message.imageUrls"
                  :key="url"
                  :src="url"
                  alt="聊天图片"
                  class="max-h-40 rounded-lg object-cover"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="border-t border-slate-200 p-3 space-y-3">
          <p v-if="chatError" class="rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-600">{{ chatError }}</p>
          <div v-if="attachments.length" class="flex flex-wrap gap-2">
            <div
              v-for="(file, index) in attachments"
              :key="file.url"
              class="relative h-16 w-16 overflow-hidden rounded-lg border border-slate-200"
            >
              <img :src="file.url" :alt="file.name" class="h-full w-full object-cover" />
              <button
                type="button"
                class="absolute right-1 top-1 rounded-full bg-black/60 px-1 text-xs text-white"
                @click="removeAttachment(index)"
              >
                ✕
              </button>
            </div>
          </div>
          <textarea
            v-model="messageInput"
            rows="3"
            placeholder="输入消息..."
            class="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
          <div class="flex items-center justify-between">
            <label class="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-500 hover:border-indigo-200 hover:text-indigo-600">
              <input type="file" accept="image/*" multiple class="hidden" @change="handleFileChange" />
              {{ uploading ? '上传中...' : '添加图片' }}
            </label>
            <div class="flex items-center gap-2">
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
                :disabled="sending || (!messageInput.trim() && !attachments.length)"
                @click="handleSend"
              >
                {{ sending ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
