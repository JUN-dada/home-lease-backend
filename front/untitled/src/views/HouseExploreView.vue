<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { houseApi, locationApi, contactApi, orderApi } from '../services/apiClient'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const regions = ref([])
const subways = ref([])
const houses = ref([])
const recommended = ref([])
const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const favorites = ref(new Set())

const searchForm = reactive({
  regionId: '',
  subwayId: '',
  status: '',
  page: 0,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已出租', value: 'RENTED' },
  { label: '已下架', value: 'OFFLINE' },
]

const houseStatusLabels = {
  PUBLISHED: '已发布',
  DRAFT: '草稿',
  OFFLINE: '已下架',
  RENTED: '已出租',
}

const contactModal = reactive({
  open: false,
  house: null,
  message: '',
  preferredVisitTime: '',
  submitting: false,
  error: '',
})

const orderModal = reactive({
  open: false,
  house: null,
  startDate: '',
  endDate: '',
  submitting: false,
  error: '',
})

const isUser = computed(() => authStore.userRole === 'USER')

async function loadRegions() {
  try {
    regions.value = await locationApi.listRegions()
  } catch (err) {
    console.warn('加载区域失败', err)
  }
}

async function loadSubways() {
  try {
    subways.value = await locationApi.listSubways()
  } catch (err) {
    console.warn('加载地铁失败', err)
  }
}

async function loadFavorites() {
  if (!isUser.value) {
    favorites.value = new Set()
    return
  }
  try {
    const favList = await houseApi.favorites()
    favorites.value = new Set((favList || []).map((item) => item.houseId))
  } catch (err) {
    console.warn('加载收藏失败', err)
  }
}

async function fetchHouses(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await houseApi.search({
      regionId: searchForm.regionId || undefined,
      subwayId: searchForm.subwayId || undefined,
      status: searchForm.status || undefined,
      page: pageIndex,
      size: searchForm.size,
    })
    houses.value = response.content || []
    page.current = response.number || 0
    page.size = response.size || searchForm.size
    page.total = response.totalElements || houses.value.length
    searchForm.page = page.current
  } catch (err) {
    errorMessage.value = err.message || '加载房源失败'
  } finally {
    loading.value = false
  }
}

async function loadRecommended() {
  try {
    recommended.value = await houseApi.recommended()
  } catch (err) {
    console.warn('加载推荐房源失败', err)
  }
}

function pageCount() {
  return Math.ceil(page.total / page.size)
}

function changePage(direction) {
  const next = page.current + direction
  if (next < 0 || next >= pageCount()) return
  fetchHouses(next)
}

async function toggleFavorite(houseId) {
  if (!isUser.value) {
    successMessage.value = '收藏功能仅对租客开放'
    return
  }
  try {
    const message = await houseApi.toggleFavorite(houseId)
    successMessage.value = message
    await loadFavorites()
  } catch (err) {
    errorMessage.value = err.message || '切换收藏失败'
  }
}

function openContactModal(house) {
  contactModal.house = house
  contactModal.message = `您好，我对「${house.title}」感兴趣，想进一步了解。`
  contactModal.preferredVisitTime = ''
  contactModal.error = ''
  contactModal.open = true
}

function openOrderModal(house) {
  orderModal.house = house
  orderModal.startDate = ''
  orderModal.endDate = ''
  orderModal.error = ''
  orderModal.open = true
}

async function submitContact() {
  if (!contactModal.house) return
  contactModal.submitting = true
  contactModal.error = ''
  try {
    await contactApi.create({
      houseId: contactModal.house.id,
      message: contactModal.message,
      preferredVisitTime: contactModal.preferredVisitTime || null,
    })
    successMessage.value = '已提交看房联系'
    contactModal.open = false
  } catch (err) {
    contactModal.error = err.message || '提交失败'
  } finally {
    contactModal.submitting = false
  }
}

async function submitOrder() {
  if (!orderModal.house) return
  if (!orderModal.startDate || !orderModal.endDate) {
    orderModal.error = '请填写租期'
    return
  }
  orderModal.submitting = true
  orderModal.error = ''
  try {
    await orderApi.create({
      houseId: orderModal.house.id,
      startDate: orderModal.startDate,
      endDate: orderModal.endDate,
    })
    successMessage.value = '订单已提交，等待房东确认'
    orderModal.open = false
  } catch (err) {
    orderModal.error = err.message || '提交失败'
  } finally {
    orderModal.submitting = false
  }
}

onMounted(async () => {
  await Promise.all([loadRegions(), loadSubways(), loadFavorites(), fetchHouses(), loadRecommended()])
})

function readableStatus(status) {
  return houseStatusLabels[status] || status || '未知状态'
}
</script>

<template>
  <div class="space-y-10">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">房源筛选</h2>
      <form class="mt-4 grid gap-4 md:grid-cols-4" @submit.prevent="fetchHouses(0)">
        <div class="space-y-1">
          <label class="text-xs font-medium text-slate-500">地区</label>
          <div class="relative">
            <select
              v-model="searchForm.regionId"
              class="w-full appearance-none rounded-lg border border-slate-200 px-3 py-2 pr-10 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="">全部地区</option>
              <option v-for="region in regions" :key="region.id" :value="region.id">
                {{ region.name }}
              </option>
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
        <div class="space-y-1">
          <label class="text-xs font-medium text-slate-500">地铁线路</label>
          <div class="relative">
            <select
              v-model="searchForm.subwayId"
              class="w-full appearance-none rounded-lg border border-slate-200 px-3 py-2 pr-10 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="">全部线路</option>
              <option v-for="subway in subways" :key="subway.id" :value="subway.id">
                {{ subway.lineName }} · {{ subway.stationName }}
              </option>
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
        <div class="space-y-1">
          <label class="text-xs font-medium text-slate-500">状态</label>
          <select
            v-model="searchForm.status"
            class="w-full appearance-none rounded-lg border border-slate-200 px-3 py-2 pr-10 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          >
            <option v-for="status in statusOptions" :key="status.value" :value="status.value">
              {{ status.label }}
            </option>
          </select>
        </div>
        <div class="flex items-end gap-3">
          <button
            type="submit"
            class="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          >
            搜索
          </button>
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
            @click="
              Object.assign(searchForm, {
                regionId: '',
                subwayId: '',
                status: '',
              });
              fetchHouses(0);
            "
          >
            重置
          </button>
        </div>
      </form>
    </section>

    <section v-if="recommended.length" class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">推荐房源</h2>
      </div>
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="house in recommended"
          :key="house.id"
          class="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
        >
          <div v-if="house.media?.length" class="aspect-video bg-slate-100">
            <img v-if="house.media[0].type === 'IMAGE'" :src="house.media[0].url" alt class="h-full w-full object-cover" />
            <video v-else :src="house.media[0].url" controls class="h-full w-full object-cover" />
          </div>
          <div class="flex flex-1 flex-col gap-3 px-5 py-4">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="text-lg font-semibold text-slate-900">{{ house.title }}</h3>
                <p class="text-xs text-slate-400">{{ house.address || '暂无地址' }}</p>
              </div>
              <div class="text-right">
                <p class="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-600">
                  {{ house.rentPrice }} 元/月
                </p>
                <p class="mt-1 text-[11px] text-slate-400">状态：{{ readableStatus(house.status) }}</p>
              </div>
            </div>
            <p class="text-sm text-slate-600 line-clamp-3">
              {{ house.description || '暂无描述' }}
            </p>
            <div class="flex items-center gap-3 rounded-xl bg-slate-50 px-4 py-3">
              <img
                v-if="house.ownerAvatarUrl"
                :src="house.ownerAvatarUrl"
                alt="房东头像"
                class="h-10 w-10 rounded-full object-cover ring-2 ring-white"
              />
              <div
                v-else
                class="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-600"
              >
                {{ house.ownerName?.[0] || '房东' }}
              </div>
              <div class="text-xs text-slate-500">
                <p class="text-sm font-semibold text-slate-900">房东：{{ house.ownerName || '未认证房东' }}</p>
                <p>联系电话：{{ house.ownerPhone || '暂无' }}</p>
                <RouterLink
                  v-if="house.ownerId"
                  :to="`/landlords/${house.ownerId}`"
                  class="inline-flex items-center gap-1 text-indigo-600 hover:underline"
                >
                  查看房东资料
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                    <path
                      fill-rule="evenodd"
                      d="M12.293 5.293a1 1 0 011.414 0L18 9.586a1 1 0 010 1.414l-4.293 4.293a1 1 0 01-1.414-1.414L14.586 11H4a1 1 0 110-2h10.586l-2.293-2.293a1 1 0 010-1.414z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </RouterLink>
              </div>
            </div>
            <div class="flex flex-wrap gap-2 text-xs text-slate-500">
              <span>{{ house.area ? `${house.area} ㎡` : '面积未知' }}</span>
              <span>{{ house.layout || '户型未填' }}</span>
              <span>{{ house.orientation || '朝向未填' }}</span>
              <span v-if="house.regionName">{{ house.regionName }}</span>
              <span v-if="house.subwayLineName">{{ house.subwayLineName }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">房源列表</h2>
        <p class="text-xs text-slate-500">共 {{ page.total }} 套，当前第 {{ page.current + 1 }} / {{ pageCount() || 1 }} 页</p>
      </div>

      <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
        正在加载...
      </div>

      <div v-else-if="!houses.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        暂无房源，请调整筛选条件。
      </div>

      <div v-else class="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="house in houses"
          :key="house.id"
          class="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md"
        >
          <div v-if="house.media?.length" class="aspect-video bg-slate-100">
            <img v-if="house.media[0].type === 'IMAGE'" :src="house.media[0].url" alt class="h-full w-full object-cover" />
            <video v-else :src="house.media[0].url" controls class="h-full w-full object-cover" />
          </div>
          <div class="flex flex-1 flex-col gap-4 px-5 py-4">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="text-lg font-semibold text-slate-900">{{ house.title }}</h3>
                <p class="text-xs text-slate-400">{{ house.address || '暂无地址' }}</p>
              </div>
              <div class="text-right">
                <p class="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-600">
                  {{ house.rentPrice }} 元/月
                </p>
                <p class="mt-1 text-[11px] text-slate-400">状态：{{ readableStatus(house.status) }}</p>
              </div>
            </div>
            <p class="text-sm text-slate-600 line-clamp-3">
              {{ house.description || '暂无描述' }}
            </p>
            <div class="flex flex-wrap gap-2 text-xs text-slate-500">
              <span>{{ house.area ? `${house.area} ㎡` : '面积未知' }}</span>
              <span>{{ house.layout || '户型未填' }}</span>
              <span>{{ house.orientation || '朝向未填' }}</span>
              <span v-if="house.regionName">{{ house.regionName }}</span>
              <span v-if="house.subwayLineName">{{ house.subwayLineName }}</span>
            </div>
            <div class="mt-auto space-y-3">
              <div class="flex flex-wrap gap-2 text-xs font-medium md:flex-nowrap">
                <RouterLink
                  :to="`/houses/${house.id}`"
                  class="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-center text-slate-600 transition hover:border-indigo-200 hover:bg-indigo-50"
                >
                  查看详情
                </RouterLink>
                <button
                  v-if="isUser"
                  type="button"
                  class="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-center text-slate-600 transition hover:border-indigo-200 hover:bg-indigo-50"
                  @click="toggleFavorite(house.id)"
                >
                  {{ favorites.has(house.id) ? '取消收藏' : '收藏' }}
                </button>
                <button
                  v-if="isUser"
                  type="button"
                  class="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-center text-slate-600 transition hover:border-indigo-200 hover:bg-indigo-50"
                  @click="openOrderModal(house)"
                >
                  立即预订
                </button>
                <button
                  v-if="isUser"
                  type="button"
                  class="flex-1 rounded-lg bg-indigo-600 px-3 py-1.5 text-center text-white transition hover:bg-indigo-500"
                  @click="openContactModal(house)"
                >
                  看房联系
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-if="houses.length && pageCount() > 1" class="flex items-center justify-center gap-4">
        <button
          type="button"
          class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="page.current === 0"
          @click="changePage(-1)"
        >
          上一页
        </button>
        <span class="text-xs text-slate-500">第 {{ page.current + 1 }} / {{ pageCount() }} 页</span>
        <button
          type="button"
          class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="page.current >= pageCount() - 1"
          @click="changePage(1)"
        >
          下一页
        </button>
      </div>
    </section>

    <div
      v-if="successMessage"
      class="rounded-2xl border border-emerald-200 bg-emerald-50 px-6 py-3 text-sm text-emerald-600 shadow-sm"
    >
      {{ successMessage }}
    </div>

    <div
      v-if="errorMessage"
      class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-600 shadow-sm"
    >
      {{ errorMessage }}
    </div>

    <div
      v-if="contactModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="contactModal.open = false"
    >
      <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">预约看房：{{ contactModal.house?.title }}</h3>
        <form class="mt-4 grid gap-4" @submit.prevent="submitContact">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">留言</label>
            <textarea
              v-model="contactModal.message"
              rows="4"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">意向看房时间</label>
            <input
              v-model="contactModal.preferredVisitTime"
              type="datetime-local"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <p v-if="contactModal.error" class="rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
            {{ contactModal.error }}
          </p>
          <div class="flex items-center justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
              @click="contactModal.open = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="contactModal.submitting"
            >
              <svg
                v-if="contactModal.submitting"
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
              提交申请
            </button>
          </div>
        </form>
      </div>
    </div>

    <div
      v-if="orderModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="orderModal.open = false"
    >
      <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">预订房源：{{ orderModal.house?.title }}</h3>
        <form class="mt-4 grid gap-4 md:grid-cols-2" @submit.prevent="submitOrder">
          <div class="space-y-2 md:col-span-2">
            <label class="text-sm font-medium text-slate-700">入住日期</label>
            <input
              v-model="orderModal.startDate"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <div class="space-y-2 md:col-span-2">
            <label class="text-sm font-medium text-slate-700">退租日期</label>
            <input
              v-model="orderModal.endDate"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <div class="md:col-span-2 rounded-lg border border-slate-100 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <p>
              月租：
              <span class="font-semibold text-slate-900">{{ orderModal.house?.rentPrice ?? '-' }} 元</span>
            </p>
            <p class="mt-1">
              押金：
              <span class="font-semibold text-slate-900">{{ orderModal.house?.deposit ?? orderModal.house?.rentPrice ?? '-' }} 元</span>
            </p>
            <p class="mt-1 text-xs text-slate-400">费用由房东设置，提交即视为同意。</p>
          </div>
          <p v-if="orderModal.error" class="md:col-span-2 rounded-lg bg-rose-50 px-4 py-2 text-sm text-rose-600">
            {{ orderModal.error }}
          </p>
          <div class="md:col-span-2 flex items-center justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
              @click="orderModal.open = false"
            >
              取消
            </button>
            <button
              type="submit"
              class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
              :disabled="orderModal.submitting"
            >
              {{ orderModal.submitting ? '提交中...' : '提交订单' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
