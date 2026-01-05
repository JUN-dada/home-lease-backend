<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { houseApi } from '../services/apiClient'

const favorites = ref([])
const loading = ref(false)
const errorMessage = ref('')

async function loadFavorites() {
  loading.value = true
  errorMessage.value = ''
  try {
    favorites.value = await houseApi.favorites()
  } catch (err) {
    errorMessage.value = err.message || '加载收藏失败'
  } finally {
    loading.value = false
  }
}

async function toggleFavorite(houseId) {
  try {
    await houseApi.toggleFavorite(houseId)
    await loadFavorites()
  } catch (err) {
    errorMessage.value = err.message || '操作失败'
  }
}

onMounted(() => {
  loadFavorites()
})
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base text-slate-900">我的收藏</h2>
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100"
        @click="loadFavorites"
      >
        刷新
      </button>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载收藏...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="!favorites.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      还没有收藏房源，去房源广场看看吧。
    </div>

    <div v-else class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="favorite in favorites"
        :key="favorite.id"
        class="flex h-full flex-col justify-between rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md"
      >
        <div>
          <h3 class="text-lg font-semibold text-slate-900">{{ favorite.houseTitle }}</h3>
          <p class="mt-1 text-xs text-slate-500">房东：{{ favorite.ownerName || '未知' }}</p>
        </div>
        <div class="mt-4 flex items-center justify-between text-xs text-slate-500">
          <span>ID：{{ favorite.houseId }}</span>
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-100"
            @click="toggleFavorite(favorite.houseId)"
          >
            取消收藏
          </button>
        </div>
        <div class="mt-3 flex flex-wrap gap-2 text-xs">
          <RouterLink
            :to="`/houses/${favorite.houseId}`"
            class="rounded-lg border border-slate-200 px-3 py-1.5 text-slate-600 transition hover:border-indigo-200 hover:bg-indigo-50"
          >
            查看详情
          </RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>
