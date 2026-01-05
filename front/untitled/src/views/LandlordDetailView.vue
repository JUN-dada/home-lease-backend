<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { userApi } from '../services/apiClient'

const route = useRoute()
const landlord = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function loadLandlord() {
  const landlordId = route.params.landlordId
  if (!landlordId) {
    errorMessage.value = '未提供房东ID'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    landlord.value = await userApi.getById(landlordId)
  } catch (err) {
    errorMessage.value = err.message || '加载房东资料失败'
  } finally {
    loading.value = false
  }
}

const gallery = computed(() => landlord.value?.gallery || [])

onMounted(loadLandlord)
watch(
  () => route.params.landlordId,
  () => {
    loadLandlord()
  },
)
</script>

<template>
  <div class="space-y-6">
    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载房东信息...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-4 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <section v-else-if="landlord" class="space-y-6">
      <div class="flex flex-col items-start gap-6 rounded-2xl border border-slate-200 bg-white px-6 py-6 shadow-sm md:flex-row md:items-center md:justify-between">
        <div class="flex flex-col items-start gap-4 md:flex-row md:items-center">
          <img
            v-if="landlord.avatarUrl"
            :src="landlord.avatarUrl"
            alt="房东头像"
            class="h-24 w-24 rounded-full object-cover ring-2 ring-indigo-100"
          />
          <div
            v-else
            class="flex h-24 w-24 items-center justify-center rounded-full bg-indigo-100 text-2xl font-semibold text-indigo-600"
          >
            {{ landlord.fullName?.[0] || landlord.username?.[0] || '房' }}
          </div>
          <div>
            <p class="text-2xl font-semibold text-slate-900">{{ landlord.fullName || landlord.username }}</p>
            <p class="text-sm text-slate-500">角色：{{ landlord.role }}</p>
            <p class="text-sm text-slate-500">电话：{{ landlord.phone || '未公开' }}</p>
            <p class="text-sm text-slate-500">邮箱：{{ landlord.email || '未填写' }}</p>
          </div>
        </div>
        <div class="rounded-2xl bg-indigo-50 px-4 py-3 text-sm text-indigo-700">
          <p>实名认证编号（仅房东可见）：{{ landlord.idNumber || '未提交' }}</p>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">房东简介</h2>
        <p class="mt-3 text-sm text-slate-600">
          {{ landlord.bio || '该房东暂未填写个人简介。' }}
        </p>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold text-slate-900">展示图库</h2>
          <p class="text-xs text-slate-500">共 {{ gallery.length }} 项</p>
        </div>
        <div v-if="!gallery.length" class="mt-4 rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
          房东暂未上传展示资料。
        </div>
        <div v-else class="mt-4 grid gap-4 md:grid-cols-3">
          <article
            v-for="item in gallery"
            :key="item.id || item.url"
            class="overflow-hidden rounded-xl border border-slate-100 bg-slate-50"
          >
            <img
              v-if="item.type === 'IMAGE'"
              :src="item.url"
              alt="房东资料"
              class="h-48 w-full object-cover"
            />
            <video v-else :src="item.url" controls class="h-48 w-full object-cover" />
            <div class="p-3 text-xs text-slate-500">
              <p v-if="item.description">{{ item.description }}</p>
              <p v-else class="text-slate-400">无描述</p>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>
