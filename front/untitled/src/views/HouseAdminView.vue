<script setup>
import { ref, reactive, onMounted } from 'vue'
import { houseApi } from '../services/apiClient'

const houses = ref([])
const favorites = ref([])
const loading = ref(false)
const errorMessage = ref('')

const statusOptions = ['PUBLISHED', 'DRAFT', 'ARCHIVED']

const query = reactive({
  page: 0,
  size: 10,
})

async function loadHouses() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await houseApi.search({ page: query.page, size: query.size })
    houses.value = response.content || []
  } catch (err) {
    errorMessage.value = err.message || '加载房源失败'
  } finally {
    loading.value = false
  }
}

async function loadFavoritesAll() {
  try {
    favorites.value = await houseApi.favoritesAll()
  } catch (err) {
    errorMessage.value = err.message || '加载收藏数据失败'
  }
}

async function updateStatus(houseId, status) {
  try {
    await houseApi.updateStatus(houseId, status)
    await loadHouses()
  } catch (err) {
    errorMessage.value = err.message || '更新状态失败'
  }
}

async function setRecommend(houseId, value) {
  try {
    await houseApi.recommend(houseId, value)
    await loadHouses()
  } catch (err) {
    errorMessage.value = err.message || '更新推荐状态失败'
  }
}

onMounted(async () => {
  await Promise.all([loadHouses(), loadFavoritesAll()])
})
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">房源总览</h2>
        <button
          type="button"
          class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100"
          @click="loadHouses"
        >
          刷新
        </button>
      </div>

      <div v-if="loading" class="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        正在加载房源列表...
      </div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="house in houses"
          :key="house.id"
          class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-lg font-semibold text-slate-900">{{ house.title }}</h3>
              <p class="text-xs text-slate-400">
                房东：{{ house.ownerName || '未知' }} | 地区：{{ house.regionName || '未知' }} | 地铁：{{
                  house.subwayLineName || '无'
                }}
              </p>
            </div>
            <div class="flex items-center gap-2 text-xs">
              <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">状态：{{ house.status }}</span>
              <span
                class="rounded-full px-3 py-1"
                :class="house.recommended ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-500'"
              >
                {{ house.recommended ? '推荐中' : '未推荐' }}
              </span>
            </div>
          </div>

          <p class="mt-3 text-sm text-slate-600 line-clamp-3">
            {{ house.description || '暂无描述' }}
          </p>

          <div class="mt-4 flex flex-wrap items-center gap-3 text-xs text-slate-500">
            <span>面积：{{ house.area ? `${house.area} ㎡` : '-' }}</span>
            <span>户型：{{ house.layout || '-' }}</span>
            <span>朝向：{{ house.orientation || '-' }}</span>
            <span>租金：{{ house.rentPrice }} 元/月</span>
            <span>收藏数：{{ favorites.filter((fav) => fav.houseId === house.id).length }}</span>
          </div>

          <div class="mt-4 flex flex-wrap gap-2 text-xs">
            <button
              v-for="status in statusOptions"
              :key="status"
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-100"
              :class="house.status === status ? 'bg-indigo-50 text-indigo-600' : 'text-slate-600'"
              @click="updateStatus(house.id, status)"
            >
              设置 {{ status }}
            </button>
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-3 py-1.5 text-slate-600 transition hover:bg-slate-100"
              @click="setRecommend(house.id, !house.recommended)"
            >
              {{ house.recommended ? '取消推荐' : '设为推荐' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">全站收藏记录</h2>
        <span class="text-xs text-slate-500">共 {{ favorites.length }} 条</span>
      </div>
      <div v-if="!favorites.length" class="mt-4 text-sm text-slate-500">暂无收藏记录。</div>
      <ul v-else class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
        <li
          v-for="favorite in favorites"
          :key="favorite.id"
          class="flex items-center justify-between py-3"
        >
          <div>
            <p class="font-medium text-slate-800">{{ favorite.houseTitle }}</p>
            <p class="text-xs text-slate-400">房东：{{ favorite.ownerName || '未知' }}</p>
          </div>
          <span class="text-xs text-slate-400">收藏ID：{{ favorite.id }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>
