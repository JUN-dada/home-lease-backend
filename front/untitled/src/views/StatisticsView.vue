<script setup>
import { ref, onMounted } from 'vue'
import { statisticsApi } from '../services/apiClient'

const stats = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function loadStats() {
  loading.value = true
  errorMessage.value = ''
  try {
    stats.value = await statisticsApi.summary()
  } catch (err) {
    errorMessage.value = err.message || '加载统计数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-900">平台统计</h2>
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100"
        @click="loadStats"
      >
        刷新
      </button>
    </div>

    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载统计数据...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <div v-else-if="stats" class="grid gap-6 lg:grid-cols-3">
      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 class="text-sm font-medium text-slate-500">近30天租赁趋势</h3>
        <p class="mt-4 text-3xl font-semibold text-slate-900">{{ stats.rentalTrendLastMonth?.totalOrders ?? 0 }}</p>
        <p class="mt-1 text-xs text-slate-400">总订单数</p>
        <div class="mt-4 space-y-2">
          <div
            v-for="item in stats.rentalTrendLastMonth?.dailyStats || []"
            :key="item.date"
            class="flex items-center justify-between text-xs text-slate-500"
          >
            <span>{{ item.date }}</span>
            <span>{{ item.count }} 单</span>
          </div>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 class="text-sm font-medium text-slate-500">地铁线路分布</h3>
        <ul class="mt-4 space-y-2 text-sm text-slate-600">
          <li
            v-for="item in stats.subwayDistribution || []"
            :key="item.name"
            class="flex items-center justify-between"
          >
            <span>{{ item.name }}</span>
            <span>{{ item.count }} 套</span>
          </li>
        </ul>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 class="text-sm font-medium text-slate-500">区域房源分布</h3>
        <ul class="mt-4 space-y-2 text-sm text-slate-600">
          <li
            v-for="item in stats.regionDistribution || []"
            :key="item.name"
            class="flex items-center justify-between"
          >
            <span>{{ item.name }}</span>
            <span>{{ item.count }} 套</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>
