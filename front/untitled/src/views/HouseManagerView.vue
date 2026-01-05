<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { houseApi, locationApi } from '../services/apiClient'
import MediaUploader from '../components/MediaUploader.vue'

const authStore = useAuthStore()

const houses = ref([])
const regions = ref([])
const subways = ref([])
const loading = ref(false)
const submitting = ref(false)
const deletingHouseId = ref(null)
const successMessage = ref('')
const errorMessage = ref('')
const page = reactive({
  current: 0,
  size: 10,
  total: 0,
})

const form = reactive({
  title: '',
  description: '',
  rentPrice: '',
  deposit: '',
  area: '',
  layout: '',
  orientation: '',
  address: '',
  availableFrom: '',
  regionId: '',
  subwayLineId: '',
  amenitiesText: '',
})

const media = ref([])

const isLandlord = computed(() => authStore.userRole === 'LANDLORD')

function resetForm() {
  form.title = ''
  form.description = ''
  form.rentPrice = ''
  form.deposit = ''
  form.area = ''
  form.layout = ''
  form.orientation = ''
  form.address = ''
  form.availableFrom = ''
  form.regionId = ''
  form.subwayLineId = ''
  form.amenitiesText = ''
  media.value = []
}

async function fetchRegions() {
  try {
    regions.value = await locationApi.listRegions()
  } catch (err) {
    console.warn('加载地区失败', err)
  }
}

async function fetchSubways() {
  try {
    subways.value = await locationApi.listSubways()
  } catch (err) {
    console.warn('加载地铁线失败', err)
  }
}

async function fetchHouses(pageIndex = 0) {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await houseApi.listMine({ page: pageIndex, size: page.size })
    houses.value = data.content || []
    page.current = data.number || 0
    page.size = data.size || 10
    page.total = data.totalElements || houses.value.length
  } catch (err) {
    errorMessage.value = err.message || '加载房源失败'
  } finally {
    loading.value = false
  }
}

async function deleteHouse(houseId) {
  if (!houseId) return
  const confirmed = window.confirm('确定要删除这套房源吗？该操作不可恢复。')
  if (!confirmed) {
    return
  }
  deletingHouseId.value = houseId
  errorMessage.value = ''
  try {
    await houseApi.remove(houseId)
    successMessage.value = '房源已删除'
    await fetchHouses(page.current)
  } catch (err) {
    errorMessage.value = err.message || '删除失败'
  } finally {
    deletingHouseId.value = null
  }
}

function formatAmenities() {
  return Array.from(
    new Set(
      (form.amenitiesText || '')
        .split(/[，,]/)
        .map((item) => item.trim())
        .filter(Boolean),
    ),
  )
}

async function handleSubmit() {
  successMessage.value = ''
  errorMessage.value = ''
  if (!form.title || !form.rentPrice) {
    errorMessage.value = '请至少填写房源标题与租金'
    return
  }
  submitting.value = true
  const payload = {
    title: form.title,
    description: form.description,
    rentPrice: Number(form.rentPrice),
    deposit: form.deposit ? Number(form.deposit) : Number(form.rentPrice),
    area: form.area ? Number(form.area) : null,
    layout: form.layout,
    orientation: form.orientation,
    address: form.address,
    availableFrom: form.availableFrom || null,
    regionId: form.regionId || null,
    subwayLineId: form.subwayLineId || null,
    amenities: formatAmenities(),
    media: media.value.map((item, index) => ({
      id: item.id,
      type: item.type,
      url: item.url,
      coverUrl: item.coverUrl,
      description: item.description,
      sortOrder: index,
    })),
  }

  try {
    const created = await houseApi.create(payload)
    successMessage.value = '房源已发布'
    resetForm()
    await fetchHouses(page.current)
    return created
  } catch (err) {
    errorMessage.value = err.message || '发布失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

function formatCurrency(amount) {
  if (!amount && amount !== 0) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 }).format(amount)
}

function pageCount() {
  return Math.ceil(page.total / page.size)
}

function changePage(direction) {
  const next = page.current + direction
  if (next < 0 || next >= pageCount()) return
  fetchHouses(next)
}

onMounted(async () => {
  if (!authStore.user && authStore.isAuthenticated) {
    try {
      await authStore.fetchProfile()
    } catch (err) {
      errorMessage.value = err.message || '加载用户信息失败'
    }
  }
  await Promise.all([fetchRegions(), fetchSubways(), fetchHouses()])
})
</script>

<template>
  <div class="space-y-8">
    <div v-if="!isLandlord" class="rounded-2xl border border-amber-200 bg-amber-50 px-6 py-4 text-sm text-amber-700 shadow-sm">
      当前账号非房东角色，仅能查看房源列表。若需发布房源，请联系管理员升级权限。
    </div>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">发布新房源</h2>
        <span class="text-xs text-slate-500">必填：标题、租金；其余信息可后续补充</span>
      </div>
      <form class="mt-6 grid gap-6" @submit.prevent="handleSubmit">
        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">房源标题 *</label>
            <input
              v-model="form.title"
              type="text"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="例如：市中心精装两居室"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">租金 (元/月) *</label>
            <input
              v-model="form.rentPrice"
              type="number"
              min="0"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="4500"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">押金 (元)</label>
            <input
              v-model="form.deposit"
              type="number"
              min="0"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="默认与月租相同，可自定义"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">面积 (㎡)</label>
            <input
              v-model="form.area"
              type="number"
              min="0"
              step="0.1"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="80"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">户型</label>
            <input
              v-model="form.layout"
              type="text"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="2室1厅1卫"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">朝向</label>
            <input
              v-model="form.orientation"
              type="text"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
              placeholder="南北通透"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">可入住日期</label>
            <input
              v-model="form.availableFrom"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">所在地区</label>
            <select
              v-model="form.regionId"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="">请选择</option>
              <option v-for="region in regions" :key="region.id" :value="region.id">
                {{ region.name }}
              </option>
            </select>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-slate-700">地铁线路</label>
            <select
              v-model="form.subwayLineId"
              class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              <option value="">请选择</option>
              <option v-for="subway in subways" :key="subway.id" :value="subway.id">
                {{ subway.lineName }} · {{ subway.stationName }}
              </option>
            </select>
          </div>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">详细地址</label>
          <input
            v-model="form.address"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="请输入详细地址"
          />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">房源简介</label>
          <textarea
            v-model="form.description"
            rows="4"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="简单介绍房源亮点、周边配套等信息"
          />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">配套设施</label>
          <input
            v-model="form.amenitiesText"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="使用逗号分隔，例如：空调,暖气,独立卫生间"
          />
        </div>

        <MediaUploader
          v-model="media"
          title="房源媒体"
          description="支持上传多张图片或视频，展示房源全貌与细节。"
        />

        <div class="flex items-center gap-3">
          <button
            type="submit"
            class="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
            :disabled="submitting || !isLandlord"
          >
            <svg
              v-if="submitting"
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
            发布房源
          </button>
          <p v-if="successMessage" class="text-sm text-emerald-600">
            {{ successMessage }}
          </p>
          <p v-if="errorMessage" class="text-sm text-rose-600">
            {{ errorMessage }}
          </p>
        </div>
      </form>
    </section>

    <section class="space-y-6">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-900">我的房源</h2>
        <div class="text-xs text-slate-500">
          共 {{ page.total }} 套，当前第 {{ page.current + 1 }} / {{ pageCount() || 1 }} 页
        </div>
      </div>

      <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
        正在加载房源列表...
      </div>

      <div v-else-if="!houses.length" class="rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        暂无房源，请先发布新的房源信息。
      </div>

      <div v-else class="grid gap-6 lg:grid-cols-2">
        <article
          v-for="house in houses"
          :key="house.id"
          class="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md"
        >
          <div v-if="house.media?.length" class="relative aspect-video w-full bg-slate-100">
            <img
              v-if="house.media[0].type === 'IMAGE'"
              :src="house.media[0].url"
              alt="封面图"
              class="h-full w-full object-cover"
            />
            <video
              v-else
              :src="house.media[0].url"
              controls
              class="h-full w-full object-cover"
            />
            <span
              class="absolute left-3 top-3 inline-flex items-center rounded-full bg-white/90 px-2 py-1 text-xs font-medium text-slate-700 shadow"
            >
              {{ house.media.length }} 个媒体
            </span>
          </div>

          <div class="flex flex-1 flex-col gap-4 px-6 py-5">
            <div>
              <div class="flex items-start justify-between gap-3">
                <h3 class="text-lg font-semibold text-slate-900">{{ house.title }}</h3>
                <div class="flex items-center gap-2">
                  <span class="rounded-full bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-600">
                    {{ formatCurrency(house.rentPrice) }}
                  </span>
                  <button
                    type="button"
                    class="rounded-full border border-rose-200 px-3 py-1 text-[11px] font-medium text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="deletingHouseId === house.id"
                    @click="deleteHouse(house.id)"
                  >
                    {{ deletingHouseId === house.id ? '删除中...' : '删除' }}
                  </button>
                </div>
              </div>
              <p class="mt-2 text-sm text-slate-600 line-clamp-3">
                {{ house.description || '暂无描述' }}
              </p>
            </div>

            <dl class="grid grid-cols-2 gap-3 text-xs text-slate-500">
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-700">面积</span>
                <span>{{ house.area ? `${house.area} ㎡` : '-' }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-700">户型</span>
                <span>{{ house.layout || '-' }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-700">朝向</span>
                <span>{{ house.orientation || '-' }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-medium text-slate-700">状态</span>
                <span>{{ house.status || '-' }}</span>
              </div>
              <div class="col-span-2 flex items-center gap-2">
                <span class="font-medium text-slate-700">地址</span>
                <span>{{ house.address || '-' }}</span>
              </div>
            </dl>

            <div v-if="house.media?.length" class="grid gap-3">
              <h4 class="text-sm font-medium text-slate-700">媒体列表</h4>
              <ul class="grid gap-3">
                <li
                  v-for="item in house.media"
                  :key="item.url"
                  class="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-xs text-slate-600"
                >
                  <span class="flex items-center gap-2">
                    <span
                      class="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600"
                    >
                      {{ item.type === 'VIDEO' ? '视频' : '图片' }}
                    </span>
                    <a
                      :href="item.url"
                      target="_blank"
                      class="truncate text-indigo-600 hover:underline"
                    >
                      {{ item.url }}
                    </a>
                  </span>
                  <span v-if="item.description" class="truncate text-slate-400">
                    {{ item.description }}
                  </span>
                </li>
              </ul>
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
  </div>
</template>
