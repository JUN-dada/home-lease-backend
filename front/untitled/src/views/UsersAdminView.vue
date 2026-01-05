<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminUserApi } from '../services/apiClient'

const users = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const roles = [
  { label: '普通用户', value: 'USER' },
  { label: '房东', value: 'LANDLORD' },
  { label: '管理员', value: 'ADMIN' },
]

const filters = reactive({
  role: 'USER',
})

const createForm = reactive({
  username: '',
  password: '',
  fullName: '',
  phone: '',
  role: 'USER',
})

async function loadUsers() {
  if (!filters.role) return
  loading.value = true
  errorMessage.value = ''
  try {
    users.value = await adminUserApi.listByRole(filters.role)
  } catch (err) {
    errorMessage.value = err.message || '加载用户失败'
  } finally {
    loading.value = false
  }
}

async function createUser() {
  if (!createForm.username || !createForm.password || !createForm.fullName) {
    errorMessage.value = '请完整填写新用户信息'
    return
  }
  try {
    await adminUserApi.create({ ...createForm })
    successMessage.value = '用户已创建'
    Object.assign(createForm, {
      username: '',
      password: '',
      fullName: '',
      phone: '',
      role: 'USER',
    })
    await loadUsers()
  } catch (err) {
    errorMessage.value = err.message || '创建失败'
  }
}

async function deleteUser(userId) {
  if (!confirm('确认删除该用户吗？')) return
  try {
    await adminUserApi.remove(userId)
    successMessage.value = '用户已删除'
    await loadUsers()
  } catch (err) {
    errorMessage.value = err.message || '删除失败'
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-900">用户管理</h2>
          <p class="mt-1 text-sm text-slate-500">支持创建租客/房东/管理员账号</p>
        </div>
        <div class="flex items-center gap-3 text-xs">
          <label class="text-slate-500">筛选角色</label>
          <select
            v-model="filters.role"
            class="rounded-lg border border-slate-200 px-3 py-1.5 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            @change="loadUsers"
          >
            <option v-for="role in roles" :key="role.value" :value="role.value">
              {{ role.label }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        正在加载用户...
      </div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="user in users"
          :key="user.id"
          class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex items-center justify-between gap-4">
            <div>
              <h3 class="text-sm font-semibold text-slate-900">{{ user.fullName || user.username }}</h3>
              <p class="text-xs text-slate-400">用户名：{{ user.username }} | 角色：{{ user.role }}</p>
            </div>
            <button
              type="button"
              class="rounded-lg border border-rose-200 px-3 py-1.5 text-xs text-rose-600 transition hover:bg-rose-50"
              @click="deleteUser(user.id)"
            >
              删除
            </button>
          </div>
          <div class="mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-2">
            <span>电话：{{ user.phone || '未填写' }}</span>
            <span>邮箱：{{ user.email || '未填写' }}</span>
            <span>身份证：{{ user.idNumber || '未填写' }}</span>
            <span>状态：{{ user.status }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">创建新用户</h2>
      <form class="mt-4 grid gap-4 md:grid-cols-2" @submit.prevent="createUser">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">用户名 *</label>
          <input
            v-model="createForm.username"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">密码 *</label>
          <input
            v-model="createForm.password"
            type="password"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">全名 *</label>
          <input
            v-model="createForm.fullName"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">手机号</label>
          <input
            v-model="createForm.phone"
            type="tel"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">角色</label>
          <select
            v-model="createForm.role"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          >
            <option v-for="role in roles" :key="role.value" :value="role.value">
              {{ role.label }}
            </option>
          </select>
        </div>
        <div class="md:col-span-2 flex items-center gap-3">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300"
          >
            创建
          </button>
          <p v-if="successMessage" class="text-sm text-emerald-600">{{ successMessage }}</p>
          <p v-if="errorMessage" class="text-sm text-rose-600">{{ errorMessage }}</p>
        </div>
      </form>
    </section>
  </div>
</template>
