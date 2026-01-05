<script setup>
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { statisticsApi, houseApi, orderApi, announcementApi } from '../services/apiClient'

const authStore = useAuthStore()

const loading = ref(false)
const stats = ref(null)
const latestHouses = ref([])
const latestOrders = ref([])
const latestAnnouncements = ref([])
const errorMessage = ref('')

const role = computed(() => authStore.userRole)

async function loadAdminData() {
  try {
    stats.value = await statisticsApi.summary()
    const announcements = await announcementApi.list({ page: 0, size: 5 })
    latestAnnouncements.value = announcements.content || []
  } catch (err) {
    errorMessage.value = err.message || '加载统计失败'
  }
}

async function loadLandlordData() {
  try {
    const housePage = await houseApi.listMine({ page: 0, size: 5 })
    latestHouses.value = housePage.content || []
    const orders = await orderApi.landlord({ page: 0, size: 5 })
    latestOrders.value = orders.content || []
  } catch (err) {
    errorMessage.value = err.message || '加载房东数据失败'
  }
}

async function loadUserData() {
  try {
    const orders = await orderApi.mine({ page: 0, size: 5 })
    latestOrders.value = orders.content || []
    const announcements = await announcementApi.latest()
    latestAnnouncements.value = announcements || []
  } catch (err) {
    errorMessage.value = err.message || '加载租客数据失败'
  }
}

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    if (role.value === 'ADMIN') {
      await loadAdminData()
    } else if (role.value === 'LANDLORD') {
      await loadLandlordData()
    } else if (role.value === 'USER') {
      await loadUserData()
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-8">
    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载数据...
    </div>

    <div v-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-4 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <template v-if="role === 'ADMIN' && stats">
      <section class="grid gap-6 lg:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p class="text-sm text-slate-500">近30天租赁趋势</p>
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
          <p class="text-sm text-slate-500">地铁线路分布</p>
          <ul class="mt-4 space-y-2 text-sm text-slate-600">
            <li v-for="item in stats.subwayDistribution || []" :key="item.name" class="flex items-center justify-between">
              <span>{{ item.name }}</span>
              <span>{{ item.count }} 套</span>
            </li>
          </ul>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p class="text-sm text-slate-500">区域房源分布</p>
          <ul class="mt-4 space-y-2 text-sm text-slate-600">
            <li v-for="item in stats.regionDistribution || []" :key="item.name" class="flex items-center justify-between">
              <span>{{ item.name }}</span>
              <span>{{ item.count }} 套</span>
            </li>
          </ul>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">最新公告</h2>
        <ul class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
          <li v-for="announcement in latestAnnouncements" :key="announcement.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-slate-800">{{ announcement.title }}</p>
              <p class="text-xs text-slate-400">{{ announcement.createdAt }}</p>
            </div>
            <span class="text-xs text-slate-400">{{ announcement.pinned ? '置顶' : '' }}</span>
          </li>
        </ul>
      </section>
    </template>

    <template v-else-if="role === 'LANDLORD'">
      <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">近期房源</h2>
        <div v-if="!latestHouses.length" class="mt-4 text-sm text-slate-500">暂无房源，去“我的房源”发布吧。</div>
        <ul v-else class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
          <li v-for="house in latestHouses" :key="house.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-slate-800">{{ house.title }}</p>
              <p class="text-xs text-slate-400">{{ house.address || '暂无地址' }}</p>
            </div>
            <span class="text-xs text-indigo-600">{{ house.status }}</span>
          </li>
        </ul>
      </section>

      <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">最新订单</h2>
        <div v-if="!latestOrders.length" class="mt-4 text-sm text-slate-500">暂无订单。</div>
        <ul v-else class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
          <li v-for="order in latestOrders" :key="order.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-slate-800">{{ order.houseTitle }}</p>
              <p class="text-xs text-slate-400">租客：{{ order.tenantName }}</p>
            </div>
            <span class="text-xs text-indigo-600">{{ order.status }}</span>
          </li>
        </ul>
      </section>
    </template>

    <template v-else-if="role === 'USER'">
      <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">我的订单</h2>
        <div v-if="!latestOrders.length" class="mt-4 text-sm text-slate-500">暂无订单记录。</div>
        <ul v-else class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
          <li v-for="order in latestOrders" :key="order.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-slate-800">{{ order.houseTitle }}</p>
              <p class="text-xs text-slate-400">
                {{ order.startDate }} 至 {{ order.endDate }}
              </p>
            </div>
            <span class="text-xs text-indigo-600">{{ order.status }}</span>
          </li>
        </ul>
      </section>

      <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 class="text-base font-semibold text-slate-900">最新公告</h2>
        <div v-if="!latestAnnouncements.length" class="mt-4 text-sm text-slate-500">暂无公告。</div>
        <ul v-else class="mt-4 divide-y divide-slate-100 text-sm text-slate-600">
          <li v-for="announcement in latestAnnouncements" :key="announcement.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-slate-800">{{ announcement.title }}</p>
              <p class="text-xs text-slate-400">{{ announcement.createdAt }}</p>
            </div>
            <span class="text-xs text-slate-400">{{ announcement.pinned ? '置顶' : '' }}</span>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>
