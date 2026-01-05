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
})

const errorMessage = ref('')
const isLoading = computed(() => authStore.loading)

async function handleSubmit() {
  errorMessage.value = ''
  if (!form.username || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }
  try {
    await authStore.login(form.username, form.password)
    await authStore.fetchProfile()
  const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (err) {
    errorMessage.value = err.message || '登录失败，请稍后再试'
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-slate-100 px-4 py-12">
    <div class="w-full max-w-md rounded-2xl bg-white p-10 shadow-xl ring-1 ring-slate-100">
      <div class="text-center">
        <h1 class="text-2xl font-semibold text-slate-900">后台登录</h1>
        <p class="mt-2 text-sm text-slate-500">使用管理员或房东账号登录以管理房源与资料</p>
      </div>

      <form class="mt-8 space-y-6" @submit.prevent="handleSubmit">
        <div class="space-y-2">
          <label class="block text-sm font-medium text-slate-700">用户名</label>
          <input
            v-model="form.username"
            type="text"
            autocomplete="username"
            class="w-full rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入用户名"
          />
        </div>

        <div class="space-y-2">
          <label class="block text-sm font-medium text-slate-700">密码</label>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            class="w-full rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入密码"
          />
        </div>

        <p v-if="errorMessage" class="rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
          {{ errorMessage }}
        </p>

        <button
          type="submit"
          class="flex w-full items-center justify-center rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
          :disabled="isLoading"
        >
          <span v-if="!isLoading">登录</span>
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
            登录中...
          </span>
        </button>

        <div class="flex justify-between text-xs text-slate-500">
          <router-link to="/register" class="text-indigo-600 hover:text-indigo-500">
            没有账号？去注册
          </router-link>
          <router-link to="/announcements" class="hover:text-slate-700">
            查看最新公告
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>
