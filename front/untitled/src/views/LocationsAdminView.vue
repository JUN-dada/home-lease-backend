<script setup>
import { ref, reactive, onMounted } from 'vue'
import { locationApi } from '../services/apiClient'

const regions = ref([])
const subways = ref([])
const regionForm = reactive({
  name: '',
  description: '',
})
const subwayForm = reactive({
  lineName: '',
  stationName: '',
  regionId: '',
})
const errorMessage = ref('')
const successMessage = ref('')

async function loadRegions() {
  try {
    regions.value = await locationApi.listRegions()
  } catch (err) {
    errorMessage.value = err.message || '加载地区失败'
  }
}

async function loadSubways() {
  try {
    subways.value = await locationApi.listSubways()
  } catch (err) {
    errorMessage.value = err.message || '加载地铁线路失败'
  }
}

async function createRegion() {
  if (!regionForm.name) {
    errorMessage.value = '请填写地区名称'
    return
  }
  try {
    await locationApi.createRegion({ ...regionForm })
    successMessage.value = '地区已创建'
    regionForm.name = ''
    regionForm.description = ''
    await loadRegions()
  } catch (err) {
    errorMessage.value = err.message || '创建地区失败'
  }
}

async function deleteRegion(regionId) {
  if (!confirm('确定删除该地区吗？')) return
  try {
    await locationApi.deleteRegion(regionId)
    successMessage.value = '地区已删除'
    await Promise.all([loadRegions(), loadSubways()])
  } catch (err) {
    errorMessage.value = err.message || '删除地区失败'
  }
}

async function createSubway() {
  if (!subwayForm.lineName || !subwayForm.stationName) {
    errorMessage.value = '请填写线路名称和站点名称'
    return
  }
  try {
    await locationApi.createSubway({
      lineName: subwayForm.lineName,
      stationName: subwayForm.stationName,
      regionId: subwayForm.regionId || null,
    })
    successMessage.value = '地铁信息已创建'
    subwayForm.lineName = ''
    subwayForm.stationName = ''
    subwayForm.regionId = ''
    await loadSubways()
  } catch (err) {
    errorMessage.value = err.message || '创建失败'
  }
}

async function deleteSubway(subwayId) {
  if (!confirm('确定删除该地铁信息吗？')) return
  try {
    await locationApi.deleteSubway(subwayId)
    successMessage.value = '地铁信息已删除'
    await loadSubways()
  } catch (err) {
    errorMessage.value = err.message || '删除失败'
  }
}

onMounted(async () => {
  await Promise.all([loadRegions(), loadSubways()])
})
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">地区管理</h2>
      <form class="mt-4 grid gap-4 md:grid-cols-2" @submit.prevent="createRegion">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">地区名称</label>
          <input
            v-model="regionForm.name"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2 md:col-span-2">
          <label class="text-sm font-medium text-slate-700">描述</label>
          <textarea
            v-model="regionForm.description"
            rows="3"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="md:col-span-2">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300"
          >
            新增地区
          </button>
        </div>
      </form>

      <ul class="mt-6 space-y-3">
        <li
          v-for="region in regions"
          :key="region.id"
          class="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600"
        >
          <div>
            <p class="font-medium text-slate-900">{{ region.name }}</p>
            <p class="text-xs text-slate-400">{{ region.description || '暂无描述' }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg border border-rose-200 px-3 py-1 text-xs text-rose-600 transition hover:bg-rose-50"
            @click="deleteRegion(region.id)"
          >
            删除
          </button>
        </li>
      </ul>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">地铁线路管理</h2>
      <form class="mt-4 grid gap-4 md:grid-cols-3" @submit.prevent="createSubway">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">线路名称</label>
          <input
            v-model="subwayForm.lineName"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">站点名称</label>
          <input
            v-model="subwayForm.stationName"
            type="text"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">所属地区</label>
          <select
            v-model="subwayForm.regionId"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
          >
            <option value="">未关联</option>
            <option v-for="region in regions" :key="region.id" :value="region.id">
              {{ region.name }}
            </option>
          </select>
        </div>
        <div class="md:col-span-3">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300"
          >
            新增地铁
          </button>
        </div>
      </form>

      <ul class="mt-6 space-y-3">
        <li
          v-for="subway in subways"
          :key="subway.id"
          class="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600"
        >
          <div>
            <p class="font-medium text-slate-900">{{ subway.lineName }} · {{ subway.stationName }}</p>
            <p class="text-xs text-slate-400">地区：{{ regions.find((r) => r.id === subway.regionId)?.name || '未关联' }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg border border-rose-200 px-3 py-1 text-xs text-rose-600 transition hover:bg-rose-50"
            @click="deleteSubway(subway.id)"
          >
            删除
          </button>
        </li>
      </ul>
    </section>

    <div v-if="successMessage" class="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-600">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-600">
      {{ errorMessage }}
    </div>
  </div>
</template>
