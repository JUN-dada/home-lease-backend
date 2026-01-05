<script setup>
import { reactive, ref, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import MediaUploader from '../components/MediaUploader.vue'
import { mediaApi } from '../services/apiClient'

const authStore = useAuthStore()
const router = useRouter()

const form = reactive({
  fullName: '',
  avatarUrl: '',
  email: '',
  phone: '',
  gender: '',
  bio: '',
  idNumber: '',
})

const gallery = ref([])
const successMessage = ref('')
const errorMessage = ref('')
const saving = ref(false)
const avatarInput = ref(null)
const avatarUploading = ref(false)
const avatarError = ref('')

function hydrateForm(user) {
  if (!user) return
  form.fullName = user.fullName || ''
  form.avatarUrl = user.avatarUrl || ''
  form.email = user.email || ''
  form.phone = user.phone || ''
  form.gender = user.gender || ''
  form.bio = user.bio || ''
  form.idNumber = user.idNumber || ''
  gallery.value = (user.gallery || []).map((item) => ({
    id: item.id ?? null,
    type: item.type ?? item.mediaType ?? 'IMAGE',
    url: item.url,
    coverUrl: item.coverUrl ?? '',
    description: item.description ?? '',
    sortOrder: item.sortOrder ?? 0,
  }))
}

watch(
  () => authStore.user,
  (user) => hydrateForm(user),
  { immediate: true, deep: true },
)

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.replace('/login')
    return
  }
  if (!authStore.user) {
    try {
      await authStore.fetchProfile()
    } catch (err) {
      errorMessage.value = err.message || '加载资料失败'
    }
  }
})

const avatarPreview = computed(() => form.avatarUrl?.trim() || '')

async function handleSubmit() {
  saving.value = true
  successMessage.value = ''
  errorMessage.value = ''
  const payload = {
    ...form,
    gallery: gallery.value.map((item, index) => ({
      id: item.id,
      type: item.type,
      url: item.url,
      coverUrl: item.coverUrl,
      description: item.description,
      sortOrder: index,
    })),
  }
  try {
    await authStore.updateProfile(payload)
    successMessage.value = '资料已更新'
  } catch (err) {
    errorMessage.value = err.message || '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  avatarError.value = ''
  avatarUploading.value = true
  try {
    const uploaded = await mediaApi.upload(file, 'IMAGE')
    form.avatarUrl = uploaded.url
    successMessage.value = '头像上传成功'
  } catch (err) {
    avatarError.value = err.message || '头像上传失败'
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}
</script>

<template>
  <form class="space-y-8" @submit.prevent="handleSubmit">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">基础信息</h2>
      <div class="mt-6 grid gap-6 md:grid-cols-2">
        <div class="md:col-span-2 space-y-3">
          <label class="text-sm font-medium text-slate-700">头像</label>
          <div class="flex flex-wrap items-center gap-4">
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              alt="头像预览"
              class="h-16 w-16 rounded-full object-cover ring-2 ring-indigo-100"
            />
            <div
              v-else
              class="flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-500"
            >
              无头像
            </div>
            <div class="space-y-2 text-xs text-slate-500">
              <button
                type="button"
                class="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-xs font-medium text-slate-700 transition hover:border-indigo-200 hover:text-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-200 disabled:cursor-not-allowed"
                :disabled="avatarUploading"
                @click="triggerAvatarUpload"
              >
                <svg
                  class="h-3.5 w-3.5"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M12 12v9m0-9l3 3m-3-3l-3 3m9-5V5a2 2 0 00-2-2H8a2 2 0 00-2 2v4" />
                </svg>
                {{ avatarUploading ? '上传中...' : '上传头像' }}
              </button>
              <p>支持 PNG/JPG，建议尺寸 512×512，5MB 以内。</p>
            </div>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />
          <p v-if="avatarError" class="text-xs text-rose-500">
            {{ avatarError }}
          </p>
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">姓名</label>
          <input
            v-model="form.fullName"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入真实姓名"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">头像地址</label>
          <input
            v-model="form.avatarUrl"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="用于系统显示的头像链接"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">联系电话</label>
          <input
            v-model="form.phone"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入手机号"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">邮箱</label>
          <input
            v-model="form.email"
            type="email"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入邮箱地址"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">性别</label>
          <div class="relative">
            <select
              v-model="form.gender"
              class="w-full appearance-none rounded-lg border border-slate-200 px-4 py-2.5 pr-10 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="">未填写</option>
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="其他">其他</option>
            </select>
            <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-slate-400">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M5.23 7.21a.75.75 0 011.06.02L10 10.168l3.71-2.938a.75.75 0 11.94 1.18l-4.18 3.31a.75.75 0 01-.94 0l-4.18-3.31a.75.75 0 01.02-1.06z"
                  clip-rule="evenodd"
                />
              </svg>
            </span>
          </div>
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">身份证号</label>
          <input
            v-model="form.idNumber"
            type="text"
            maxlength="18"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="用于实名认证（选填）"
          />
        </div>
        <div class="md:col-span-2 space-y-2">
          <label class="text-sm font-medium text-slate-700">个人简介</label>
          <textarea
            v-model="form.bio"
            rows="4"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="简单介绍自己，方便租客了解您"
          />
        </div>
      </div>
    </section>

    <MediaUploader
      v-model="gallery"
      title="个人展示"
      description="上传房东介绍、证书或个人形象的图片与视频，提升可信度。"
    />

    <div class="flex items-center gap-3">
      <button
        type="submit"
        class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
        :disabled="saving"
      >
        <svg
          v-if="saving"
          class="h-4 w-4 animate-spin"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
          />
        </svg>
        保存修改
      </button>
      <p v-if="successMessage" class="text-sm text-emerald-600">
        {{ successMessage }}
      </p>
      <p v-if="errorMessage" class="text-sm text-rose-600">
        {{ errorMessage }}
      </p>
    </div>
  </form>
</template>
