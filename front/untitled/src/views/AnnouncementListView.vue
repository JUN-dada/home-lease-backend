<script setup>
import { ref, onMounted } from 'vue'
import { announcementApi } from '../services/apiClient'

const announcements = ref([])
const loading = ref(false)
const errorMessage = ref('')

async function loadAnnouncements() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await announcementApi.list({ page: 0, size: 20 })
    announcements.value = response.content || []
  } catch (err) {
    errorMessage.value = err.message || '加载公告失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">公告中心</h2>
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100"
        @click="loadAnnouncements"
      >
        刷新
      </button>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载公告...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!announcements.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      暂无公告。
    </div>

    <ul v-else class="space-y-4">
      <li
        v-for="announcement in announcements"
        :key="announcement.id"
        class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-slate-900">{{ announcement.title }}</h3>
            <p class="text-xs text-slate-400">{{ announcement.createdAt }}</p>
          </div>
          <span
            v-if="announcement.pinned"
            class="rounded-full bg-amber-100 px-3 py-1 text-xs text-amber-600"
          >
            置顶
          </span>
        </div>
        <p class="mt-3 whitespace-pre-wrap text-sm text-slate-600">
          {{ announcement.content }}
        </p>
      </li>
    </ul>
  </section>
</template>
