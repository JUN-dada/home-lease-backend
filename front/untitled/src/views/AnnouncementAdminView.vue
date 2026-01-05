<script setup>
import { reactive, ref, onMounted } from 'vue'
import { announcementApi } from '../services/apiClient'

const announcements = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

const form = reactive({
  title: '',
  content: '',
  pinned: false,
})

const editingId = ref(null)

async function loadAnnouncements(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await announcementApi.list({ page: pageIndex, size: page.size })
    announcements.value = response.content || []
    page.current = response.number || 0
    page.size = response.size || page.size
    page.total = response.totalElements || announcements.value.length
  } catch (err) {
    errorMessage.value = err.message || '加载公告失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.pinned = false
  editingId.value = null
}

function editAnnouncement(announcement) {
  editingId.value = announcement.id
  form.title = announcement.title
  form.content = announcement.content
  form.pinned = announcement.pinned
}

async function submitForm() {
  if (!form.title || !form.content) {
    errorMessage.value = '请填写完整标题与内容'
    return
  }
  try {
    if (editingId.value) {
      await announcementApi.update(editingId.value, { ...form })
      successMessage.value = '公告已更新'
    } else {
      await announcementApi.create({ ...form })
      successMessage.value = '公告已发布'
    }
    resetForm()
    await loadAnnouncements(page.current)
  } catch (err) {
    errorMessage.value = err.message || '保存失败'
  }
}

async function removeAnnouncement(id) {
  if (!confirm('确定删除该公告吗？')) return
  try {
    await announcementApi.remove(id)
    successMessage.value = '公告已删除'
    await loadAnnouncements(page.current)
  } catch (err) {
    errorMessage.value = err.message || '删除失败'
  }
}

function pageCount() {
  return Math.ceil(page.total / page.size)
}

function changePage(direction) {
  const next = page.current + direction
  if (next < 0 || next >= pageCount()) return
  loadAnnouncements(next)
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">{{ editingId ? '编辑公告' : '发布公告' }}</h2>
      <form class="mt-4 space-y-4" @submit.prevent="submitForm">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">标题</label>
          <input
            v-model="form.title"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入公告标题"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">内容</label>
          <textarea
            v-model="form.content"
            rows="4"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="公告详细内容"
          />
        </div>
        <label class="inline-flex items-center gap-2 text-sm text-slate-600">
          <input v-model="form.pinned" type="checkbox" class="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500" />
          置顶该公告
        </label>
        <div class="flex items-center gap-3">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300"
          >
            {{ editingId ? '保存修改' : '发布公告' }}
          </button>
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
            @click="resetForm"
          >
            重置
          </button>
        </div>
      </form>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">公告列表</h2>
        <span class="text-xs text-slate-500">共 {{ page.total }} 条</span>
      </div>

      <div v-if="loading" class="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        正在加载公告...
      </div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="announcement in announcements"
          :key="announcement.id"
          class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-lg font-semibold text-slate-900">{{ announcement.title }}</h3>
              <p class="text-xs text-slate-400">创建于 {{ announcement.createdAt }}</p>
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
          <div class="mt-4 flex items-center gap-2 text-xs text-slate-600">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
              @click="editAnnouncement(announcement)"
            >
              编辑
            </button>
            <button
              type="button"
              class="rounded-lg border border-rose-200 px-3 py-1.5 text-rose-600 transition hover:bg-rose-50"
              @click="removeAnnouncement(announcement.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>

      <div v-if="announcements.length && pageCount() > 1" class="mt-4 flex items-center justify-center gap-4">
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

    <div v-if="successMessage" class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-600">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-600">
      {{ errorMessage }}
    </div>
  </div>
</template>
