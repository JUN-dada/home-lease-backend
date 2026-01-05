<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { certificationApi, mediaApi } from '../services/apiClient'
import { getCertificationStatusLabel } from '../utils/statusLabels'

const latestCertification = ref(null)
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const fileInputRef = ref(null)
const MAX_ATTACHMENTS = 6

const form = reactive({
  documentUrls: [],
  reason: '',
})

const latestAttachments = computed(() => latestCertification.value?.documentUrls || [])

async function loadLatest() {
  loading.value = true
  errorMessage.value = ''
  try {
    latestCertification.value = await certificationApi.me()
  } catch (err) {
    if (err.message && err.message.includes('404')) {
      latestCertification.value = null
    } else {
      errorMessage.value = err.message || '加载认证信息失败'
    }
  } finally {
    loading.value = false
  }
}

function triggerFileDialog() {
  fileInputRef.value?.click()
}

async function handleFileChange(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  const remainingSlots = MAX_ATTACHMENTS - form.documentUrls.length
  if (remainingSlots <= 0) {
    errorMessage.value = `最多上传 ${MAX_ATTACHMENTS} 张资料图片`
    return
  }
  uploading.value = true
  errorMessage.value = ''
  try {
    for (const file of files.slice(0, remainingSlots)) {
      const response = await mediaApi.upload(file, 'IMAGE')
      form.documentUrls.push(response.url)
    }
  } catch (err) {
    errorMessage.value = err.message || '上传失败，请重试'
  } finally {
    uploading.value = false
  }
}

function removeAttachment(index) {
  form.documentUrls.splice(index, 1)
}

async function submitCertification() {
  if (!form.documentUrls.length) {
    errorMessage.value = '请至少上传 1 张资料图片'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await certificationApi.submit({
      documentUrls: [...form.documentUrls],
      reason: form.reason,
    })
    successMessage.value = '认证申请已提交，请等待审核'
    form.documentUrls.splice(0, form.documentUrls.length)
    form.reason = ''
    await loadLatest()
  } catch (err) {
    errorMessage.value = err.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadLatest()
})
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">房东认证申请</h2>
      <p class="mt-1 text-sm text-slate-500">
        请上传身份证明、房产证明等资料的链接，审核通过后即可发布房源。
      </p>

      <form class="mt-4 space-y-4" @submit.prevent="submitCertification">
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">认证资料</label>
          <div class="rounded-2xl border border-dashed border-slate-300 p-4">
            <p class="text-xs text-slate-500">支持 jpg/png，单次最多上传 {{ MAX_ATTACHMENTS }} 张图片。</p>
            <div class="mt-3 flex flex-wrap gap-3">
              <input
                ref="fileInputRef"
                type="file"
                class="hidden"
                accept="image/*"
                multiple
                @change="handleFileChange"
              />
              <button
                type="button"
                class="rounded-lg border border-indigo-200 px-4 py-2 text-sm text-indigo-600 transition hover:bg-indigo-50"
                :disabled="uploading"
                @click="triggerFileDialog"
              >
                {{ uploading ? '上传中...' : '上传图片' }}
              </button>
              <span class="text-xs text-slate-400">请确保图片清晰可辨。</span>
            </div>
            <div
              v-if="form.documentUrls.length"
              class="mt-4 grid gap-3 sm:grid-cols-3 lg:grid-cols-4"
            >
              <div
                v-for="(url, index) in form.documentUrls"
                :key="url"
                class="group relative overflow-hidden rounded-xl border border-slate-200"
              >
                <img :src="url" alt="认证资料" class="h-32 w-full object-cover transition group-hover:scale-105" />
                <button
                  type="button"
                  class="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-xs text-white opacity-0 transition group-hover:opacity-100"
                  @click="removeAttachment(index)"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-slate-700">补充说明</label>
          <textarea
            v-model="form.reason"
            rows="4"
            class="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            placeholder="可说明房源情况、资料说明等"
          />
        </div>
        <div class="flex items-center gap-3">
          <button
            type="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-300 disabled:cursor-not-allowed disabled:bg-indigo-300"
            :disabled="submitting"
          >
            {{ submitting ? '提交中...' : '提交认证' }}
          </button>
          <p v-if="successMessage" class="text-sm text-emerald-600">{{ successMessage }}</p>
          <p v-if="errorMessage" class="text-sm text-rose-600">{{ errorMessage }}</p>
        </div>
      </form>
    </section>

    <section v-if="latestCertification" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 class="text-base font-semibold text-slate-900">最近一次认证</h2>
      <div class="mt-4 grid gap-3 text-sm text-slate-600">
        <div class="flex items-center gap-3">
          <span class="text-slate-500">状态</span>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
            {{ getCertificationStatusLabel(latestCertification.status) }}
          </span>
        </div>
        <div>
          <span class="text-slate-500">资料附件：</span>
          <div v-if="latestAttachments.length" class="mt-2 grid gap-3 sm:grid-cols-3 lg:grid-cols-4">
            <a
              v-for="(url, index) in latestAttachments"
              :key="`${url}-${index}`"
              :href="url"
              target="_blank"
              class="group relative overflow-hidden rounded-xl border border-slate-200"
            >
              <img :src="url" alt="认证资料" class="h-28 w-full object-cover transition group-hover:scale-105" />
              <span class="absolute bottom-2 right-2 rounded-full bg-black/60 px-2 py-0.5 text-[10px] text-white opacity-0 transition group-hover:opacity-100">
                查看原图
              </span>
            </a>
          </div>
          <span v-else class="text-sm text-slate-500">申请人未上传附件</span>
        </div>
        <div>
          <span class="text-slate-500">说明：</span>
          <span>{{ latestCertification.reason || '无' }}</span>
        </div>
        <div>
          <span class="text-slate-500">提交时间：</span>
          <span>{{ latestCertification.createdAt }}</span>
        </div>
        <div>
          <span class="text-slate-500">审核时间：</span>
          <span>{{ latestCertification.reviewedAt || '审核中' }}</span>
        </div>
      </div>
    </section>
  </div>
</template>
