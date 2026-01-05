<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { houseApi, contactApi, orderApi } from '../services/apiClient'
import { useAuthStore } from '../stores/auth'
import HouseChatPanel from '../components/HouseChatPanel.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const house = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const chatOpen = ref(false)

const contactModal = reactive({
  open: false,
  message: '',
  preferredVisitTime: '',
  submitting: false,
  error: '',
})

const orderModal = reactive({
  open: false,
  startDate: '',
  endDate: '',
  submitting: false,
  error: '',
})

const isUser = computed(() => authStore.userRole === 'USER')

async function loadHouse() {
  const houseId = route.params.houseId
  if (!houseId) {
    errorMessage.value = '未提供房源ID'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    house.value = await houseApi.detail(houseId)
  } catch (err) {
    errorMessage.value = err.message || '加载房源失败'
  } finally {
    loading.value = false
  }
}

function openContactModal() {
  contactModal.message = `您好，我想咨询房源「${house.value?.title || ''}」。`
  contactModal.preferredVisitTime = ''
  contactModal.error = ''
  contactModal.open = true
}

function openChatPanel() {
  if (!isUser.value) {
    successMessage.value = '仅租客可发起聊天'
    return
  }
  chatOpen.value = true
}

function openOrderModal() {
  orderModal.startDate = ''
  orderModal.endDate = ''
  orderModal.error = ''
  orderModal.open = true
}

async function submitContact() {
  if (!house.value) return
  contactModal.submitting = true
  contactModal.error = ''
  try {
    await contactApi.create({
      houseId: house.value.id,
      message: contactModal.message,
      preferredVisitTime: contactModal.preferredVisitTime || null,
    })
    successMessage.value = '已提交看房请求'
    contactModal.open = false
  } catch (err) {
    contactModal.error = err.message || '提交失败'
  } finally {
    contactModal.submitting = false
  }
}

async function submitOrder() {
  if (!house.value) return
  if (!orderModal.startDate || !orderModal.endDate) {
    orderModal.error = '请填写租期'
    return
  }
  orderModal.submitting = true
  orderModal.error = ''
  try {
    await orderApi.create({
      houseId: house.value.id,
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

function viewLandlordProfile() {
  if (house.value?.ownerId) {
    router.push(`/landlords/${house.value.ownerId}`)
  }
}

onMounted(loadHouse)
watch(
  () => route.params.houseId,
  () => {
    loadHouse()
  },
)
</script>

<template>
  <div class="space-y-6">
    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      正在加载房源详情...
    </div>

    <div v-else-if="errorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-6 py-4 text-sm text-rose-600 shadow-sm">
      {{ errorMessage }}
    </div>

    <template v-else-if="house">
      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div v-if="house.media?.length" class="grid gap-4 bg-slate-50 p-4 md:grid-cols-2">
          <div class="rounded-2xl bg-white p-2">
            <img
              v-if="house.media[0].type === 'IMAGE'"
              :src="house.media[0].url"
              alt="封面图"
              class="h-full w-full rounded-xl object-cover"
            />
            <video v-else :src="house.media[0].url" controls class="h-full w-full rounded-xl object-cover" />
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div
              v-for="item in house.media.slice(1, 5)"
              :key="item.id || item.url"
              class="overflow-hidden rounded-2xl border border-slate-100 bg-white"
            >
              <img
                v-if="item.type === 'IMAGE'"
                :src="item.url"
                alt="房源图片"
                class="h-full w-full object-cover"
              />
              <video v-else :src="item.url" controls class="h-full w-full object-cover" />
            </div>
          </div>
        </div>
        <div class="space-y-6 p-6">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h1 class="text-2xl font-semibold text-slate-900">{{ house.title }}</h1>
              <p class="mt-1 text-sm text-slate-500">{{ house.address || '地址待完善' }}</p>
            </div>
            <div class="text-right">
              <p class="text-3xl font-semibold text-indigo-600">{{ house.rentPrice }} 元/月</p>
              <p class="text-xs text-slate-400">可入住时间：{{ house.availableFrom || '待确认' }}</p>
              <p class="text-xs text-slate-400">押金：{{ house.deposit ?? house.rentPrice }} 元</p>
            </div>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-xl border border-slate-100 bg-slate-50 p-4 text-sm text-slate-600">
              <p>户型：{{ house.layout || '未填写' }}</p>
              <p>面积：{{ house.area ? `${house.area} ㎡` : '未填写' }}</p>
              <p>朝向：{{ house.orientation || '未填写' }}</p>
              <p>地铁：{{ house.subwayLineName || '未填写' }}</p>
              <p>区域：{{ house.regionName || '未填写' }}</p>
              <p>状态：{{ house.status }}</p>
            </div>
            <div class="rounded-xl border border-slate-100 bg-slate-50 p-4 text-sm text-slate-600">
              <p v-for="amenity in house.amenities || []" :key="amenity">· {{ amenity }}</p>
              <p v-if="!(house.amenities?.length)" class="text-slate-400">房东暂未填写配套</p>
            </div>
          </div>

          <div class="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-sm font-semibold text-slate-900">房源描述</p>
            <p class="mt-2 text-sm text-slate-600">
              {{ house.description || '发布者尚未补充房源描述。' }}
            </p>
          </div>

          <div class="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
            <div class="flex items-center gap-4">
              <img
                v-if="house.ownerAvatarUrl"
                :src="house.ownerAvatarUrl"
                alt="房东头像"
                class="h-16 w-16 rounded-full object-cover ring-2 ring-white"
              />
              <div
                v-else
                class="flex h-16 w-16 items-center justify-center rounded-full bg-indigo-100 text-lg font-semibold text-indigo-600"
              >
                {{ house.ownerName?.[0] || '房东' }}
              </div>
              <div>
                <p class="text-base font-semibold text-slate-900">{{ house.ownerName || '未认证房东' }}</p>
                <p class="text-sm text-slate-500">电话：{{ house.ownerPhone || '未公开' }}</p>
                <button
                  v-if="house.ownerId"
                  type="button"
                  class="mt-1 text-xs text-indigo-600 hover:underline"
                  @click="viewLandlordProfile"
                >
                  查看房东详情
                </button>
              </div>
            </div>
            <div class="flex flex-wrap gap-3">
              <button
                type="button"
                class="inline-flex items-center justify-center rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50"
                @click="viewLandlordProfile"
              >
                房东资料
              </button>
              <button
                v-if="isUser"
                type="button"
                class="inline-flex items-center justify-center rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50"
                @click="openContactModal"
              >
                看房联系
              </button>
              <button
                v-if="isUser"
                type="button"
                class="inline-flex items-center justify-center rounded-lg border border-slate-200 px-4 py-2 text-sm text-indigo-600 transition hover:border-indigo-200 hover:bg-indigo-50"
                @click="openChatPanel"
              >
                实时聊天
              </button>
              <button
                v-if="isUser"
                type="button"
                class="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500"
                @click="openOrderModal"
              >
                立即预订
              </button>
            </div>
          </div>
        </div>
      </section>
    </template>

    <div
      v-if="successMessage"
      class="rounded-2xl border border-emerald-200 bg-emerald-50 px-6 py-3 text-sm text-emerald-600 shadow-sm"
    >
      {{ successMessage }}
    </div>

    <div
      v-if="contactModal.open"
      class="fixed inset-0 z-30 flex items-center justify-center bg-black/40 px-4"
      @click.self="contactModal.open = false"
    >
      <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <h3 class="text-lg font-semibold text-slate-900">预约看房：{{ house?.title }}</h3>
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
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
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
        <h3 class="text-lg font-semibold text-slate-900">预订房源：{{ house?.title }}</h3>
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
              <span class="font-semibold text-slate-900">{{ house?.rentPrice ?? '-' }} 元</span>
            </p>
            <p class="mt-1">
              押金：
              <span class="font-semibold text-slate-900">{{ house?.deposit ?? house?.rentPrice ?? '-' }} 元</span>
            </p>
            <p class="mt-1 text-xs text-slate-400">费用由房东设置，提交订单即视为同意上述金额。</p>
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
  <HouseChatPanel
    v-if="house?.id"
    :open="chatOpen"
    :house-id="house.id"
    :house-title="house.title"
    @close="chatOpen = false"
  />
</template>
