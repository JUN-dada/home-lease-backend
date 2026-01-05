<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  phone: '',
  role: 'USER',
})

const roles = [
  { label: '普通用户（租客）', value: 'USER' },
  { label: '房东', value: 'LANDLORD' },
]

const errorMessage = ref('')
const isLoading = computed(() => authStore.loading)

function validate() {
  if (!form.username || !form.password || !form.fullName || !form.phone) {
    errorMessage.value = '请填写完整信息'
    return false
  }
  if (form.password.length < 6) {
    errorMessage.value = '密码至少 6 位'
    return false
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return false
  }
  return true
}

async function handleSubmit() {
  errorMessage.value = ''
  if (!validate()) return
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
      fullName: form.fullName,
      phone: form.phone,
      role: form.role,
    })
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (err) {
    errorMessage.value = err.message || '注册失败，请稍后再试'
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-white to-indigo-50 px-4 py-12">
    <div class="w-full max-w-2xl rounded-2xl bg-white p-10 shadow-xl ring-1 ring-slate-100">
      <div class="text-center">
        <h1 class="text-2xl font-semibold text-slate-900">创建账号</h1>
        <p class="mt-2 text-sm text-slate-500">注册后可直接登录系统进行房源管理与预订</p>
      </div>

      <form class="mt-8 grid gap-6" @submit.prevent="handleSubmit">
        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">用户名 *</label>
            <input
              v-model="form.username"
              type="text"
              autocomplete="username"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="请输入用户名"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">姓名 *</label>
            <input
              v-model="form.fullName"
              type="text"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="真实姓名"
            />
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">密码 *</label>
            <input
              v-model="form.password"
              type="password"
              autocomplete="new-password"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="至少 6 位"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">确认密码 *</label>
            <input
              v-model="form.confirmPassword"
              type="password"
              autocomplete="new-password"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="再次输入密码"
            />
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">手机号 *</label>
            <input
              v-model="form.phone"
              type="tel"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="例如：13812345678"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">角色 *</label>
            <select
              v-model="form.role"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option v-for="role in roles" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </div>
        </div>

        <p v-if="errorMessage" class="rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
          {{ errorMessage }}
        </p>

        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="submit"
            class="inline-flex items-center justify-center gap-2 rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-medium text-white shadow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
            :disabled="isLoading"
          >
            <span v-if="!isLoading">注册并登录</span>
            <span v-else class="inline-flex items-center gap-2">
              <svg
                class="h-4 w-4 animate-spin text-white"
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
              提交中
            </span>
          </button>
          <router-link to="/login" class="text-sm text-indigo-600 hover:text-indigo-500">
            已有账号？点击登录
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>
