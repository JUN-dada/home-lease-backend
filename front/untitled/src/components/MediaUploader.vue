<script setup>
import { ref, watch, computed } from 'vue'
import { mediaApi } from '../services/apiClient'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: '媒体素材',
  },
  description: {
    type: String,
    default: '',
  },
  accept: {
    type: String,
    default: 'image/*,video/*',
  },
})

const emit = defineEmits(['update:modelValue'])
const fileInput = ref(null)
const items = ref([])
const uploading = ref(false)
const error = ref('')

watch(
  () => props.modelValue,
  (value) => {
    items.value = (value || []).map((item, index) => ({
      id: item.id ?? null,
      type: item.type ?? item.mediaType ?? 'IMAGE',
      url: item.url,
      coverUrl: item.coverUrl ?? '',
      description: item.description ?? '',
      sortOrder: item.sortOrder ?? index,
    }))
  },
  { immediate: true, deep: true },
)

const orderedItems = computed(() => [...items.value].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)))

function sync() {
  emit(
    'update:modelValue',
    orderedItems.value.map((item, index) => ({
      ...item,
      sortOrder: index,
    })),
  )
}

function chooseFile() {
  fileInput.value?.click()
}

async function handleFileChange(event) {
  const files = event.target.files
  if (!files?.length) return
  error.value = ''
  uploading.value = true
  for (const file of files) {
    const type = file.type.startsWith('video/') ? 'VIDEO' : 'IMAGE'
    try {
      const uploaded = await mediaApi.upload(file, type)
      items.value.push({
        id: null,
        type: uploaded.mediaType,
        url: uploaded.url,
        coverUrl: uploaded.mediaType === 'VIDEO' ? '' : uploaded.url,
        description: '',
        sortOrder: items.value.length,
      })
    } catch (err) {
      error.value = err.message || '上传失败，请稍后再试'
    }
  }
  uploading.value = false
  sync()
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function moveItem(index, direction) {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= orderedItems.value.length) return
  const reordered = [...orderedItems.value]
  const [removed] = reordered.splice(index, 1)
  reordered.splice(newIndex, 0, removed)
  items.value = reordered.map((item, idx) => ({ ...item, sortOrder: idx }))
  sync()
}

function removeItem(index) {
  const target = orderedItems.value[index]
  items.value = items.value.filter((item) => item !== target)
  items.value = items.value.map((item, idx) => ({ ...item, sortOrder: idx }))
  sync()
}

function updateField(index, field, value) {
  const target = orderedItems.value[index]
  const originalIndex = items.value.findIndex((item) => item === target)
  if (originalIndex !== -1) {
    items.value[originalIndex][field] = value
    sync()
  }
}
</script>

<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-base font-semibold text-slate-900">{{ title }}</h3>
        <p v-if="description" class="text-sm text-slate-500">
          {{ description }}
        </p>
      </div>
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-lg border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-medium text-indigo-600 transition hover:bg-indigo-100 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          @click="chooseFile"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4v16m8-8H4" />
          </svg>
          添加媒体
        </button>
        <input ref="fileInput" type="file" class="hidden" multiple :accept="accept" @change="handleFileChange" />
      </div>
    </div>

    <p v-if="error" class="mt-4 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
      {{ error }}
    </p>

    <div
      v-if="uploading"
      class="mt-4 flex items-center gap-2 rounded-lg border border-dashed border-indigo-200 bg-indigo-50 px-4 py-3 text-sm text-indigo-600"
    >
      <svg class="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
        />
      </svg>
      正在上传，请稍候...
    </div>

    <div v-if="!orderedItems.length && !uploading" class="mt-6 rounded-xl border border-dashed border-slate-200 p-6 text-center">
      <p class="text-sm text-slate-500">尚未添加媒体文件，点击上方“添加媒体”按钮上传图片或视频。</p>
    </div>

    <ul v-else class="mt-6 grid gap-4 md:grid-cols-2">
      <li
        v-for="(item, index) in orderedItems"
        :key="item.url"
        class="flex flex-col gap-4 rounded-xl border border-slate-200 p-4 shadow-sm"
      >
        <div class="relative aspect-video overflow-hidden rounded-lg bg-slate-100">
          <img
            v-if="item.type === 'IMAGE'"
            :src="item.url"
            alt="媒体预览"
            class="h-full w-full object-cover"
          />
          <video
            v-else
            :src="item.url"
            controls
            class="h-full w-full object-cover"
          />
          <span
            class="absolute left-3 top-3 inline-flex items-center rounded-full bg-white/90 px-2 py-1 text-xs font-medium text-slate-700 shadow"
          >
            {{ item.type === 'VIDEO' ? '视频' : '图片' }}
          </span>
        </div>

        <div class="space-y-3 text-sm">
          <div class="grid gap-2">
            <label class="text-xs font-medium text-slate-500">描述</label>
            <input
              type="text"
              :value="item.description"
              @input="(event) => updateField(index, 'description', event.target.value)"
              placeholder="为该媒体添加说明（可选）"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <div class="grid gap-2">
            <label class="text-xs font-medium text-slate-500">封面地址（可选）</label>
            <input
              type="text"
              :value="item.coverUrl"
              @input="(event) => updateField(index, 'coverUrl', event.target.value)"
              placeholder="用于视频封面或外部链接"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
        </div>

        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2 text-xs text-slate-500">
            <button
              type="button"
              class="rounded-md border border-slate-200 px-2 py-1 transition hover:border-slate-300 hover:text-slate-700"
              :disabled="index === 0"
              @click="moveItem(index, -1)"
            >
              上移
            </button>
            <button
              type="button"
              class="rounded-md border border-slate-200 px-2 py-1 transition hover:border-slate-300 hover:text-slate-700"
              :disabled="index === orderedItems.length - 1"
              @click="moveItem(index, 1)"
            >
              下移
            </button>
          </div>
          <button
            type="button"
            class="inline-flex items-center gap-1 rounded-lg bg-rose-50 px-3 py-1.5 text-xs font-medium text-rose-600 transition hover:bg-rose-100"
            @click="removeItem(index)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 18L18 6M6 6l12 12" />
            </svg>
            移除
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
