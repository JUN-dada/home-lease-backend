<script setup>
import { ref, reactive, onMounted } from 'vue'
import { contactApi } from '../services/apiClient'
import { getContactStatusLabel } from '../utils/statusLabels'
import HouseChatPanel from '../components/HouseChatPanel.vue'

const contacts = ref([])
const loading = ref(false)
const errorMessage = ref('')
const chatOpen = ref(false)
const activeContact = ref(null)

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

async function loadContacts(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await contactApi.mine({ page: pageIndex, size: page.size })
    contacts.value = response.content || []
    page.current = response.number || 0
    page.size = response.size || page.size
    page.total = response.totalElements || contacts.value.length
  } catch (err) {
    errorMessage.value = err.message || '加载联系记录失败'
  } finally {
    loading.value = false
  }
}

function pageCount() {
  return Math.ceil(page.total / page.size)
}

function changePage(direction) {
  const next = page.current + direction
  if (next < 0 || next >= pageCount()) return
  loadContacts(next)
}

onMounted(() => {
  loadContacts()
})

function openChat(contact) {
  activeContact.value = contact
  chatOpen.value = true
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">我的看房联系</h2>
      <span class="text-xs text-slate-500">共 {{ page.total }} 条</span>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!contacts.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      暂无联系记录。
    </div>

    <ul v-else class="space-y-4">
      <li
        v-for="contact in contacts"
        :key="contact.id"
        class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <div class="flex items-start justify-between">
          <div>
            <p class="text-sm font-semibold text-slate-900">{{ contact.houseTitle }}</p>
            <p class="text-xs text-slate-400">房东：{{ contact.landlordName || '未知' }}</p>
          </div>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
            状态：{{ getContactStatusLabel(contact.status) }}
          </span>
        </div>
        <p class="mt-3 text-sm text-slate-600">{{ contact.message }}</p>
        <div class="mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-2">
          <span>期望时间：{{ contact.preferredVisitTime || '未指定' }}</span>
          <span>最后处理：{{ contact.handledAt || '无' }}</span>
          <span class="md:col-span-2 text-slate-400">备注：{{ contact.remarks || '暂无' }}</span>
        </div>
        <div class="mt-4 flex flex-wrap items-center gap-2 text-xs">
          <button
            type="button"
            class="rounded-lg border border-indigo-200 px-3 py-1.5 text-indigo-600 transition hover:bg-indigo-50"
            @click="openChat(contact)"
          >
            实时聊天
          </button>
        </div>
      </li>
    </ul>

    <div v-if="contacts.length && pageCount() > 1" class="flex items-center justify-center gap-4">
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

    <HouseChatPanel
      :open="chatOpen"
      :contact-id="activeContact?.id || null"
      :house-title="activeContact?.houseTitle || ''"
      :header-title="activeContact ? `${activeContact.houseTitle || ''} · ${activeContact.landlordName || ''}` : ''"
      :auto-ensure="false"
      @close="chatOpen = false"
    />
  </section>
</template>
