<script setup>
import { computed } from 'vue'
import { useRouter, useRoute, RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Bars3Icon, HomeIcon, UserCircleIcon, ArrowLeftStartOnRectangleIcon } from '@heroicons/vue/24/outline'
import { ref } from 'vue'
import ChatNotificationBell from '../components/ChatNotificationBell.vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const menuOpen = ref(false)

const roleDisplay = {
  ADMIN: '管理员',
  LANDLORD: '房东',
  USER: '租客',
}

const navigationGroups = computed(() => {
  const role = authStore.userRole
  const base = [
    {
      title: '通用',
      items: [
        { name: '仪表盘', to: '/dashboard', icon: HomeIcon, roles: ['ADMIN', 'LANDLORD', 'USER'] },
        { name: '个人资料', to: '/profile', icon: UserCircleIcon, roles: ['ADMIN', 'LANDLORD', 'USER'] },
      ],
    },
    {
      title: '房源',
      items: [
        { name: '房源广场', to: '/houses/explore', roles: ['LANDLORD', 'USER', 'ADMIN'] },
        { name: '我的收藏', to: '/houses/favorites', roles: ['USER'] },
        { name: '我的房源', to: '/houses/mine', roles: ['LANDLORD'] },
        { name: '房源管理', to: '/houses/admin', roles: ['ADMIN'] },
      ],
    },
    {
      title: '沟通与订单',
      items: [
        { name: '看房联系', to: '/contacts/mine', roles: ['USER'] },
        { name: '租客咨询', to: '/contacts/landlord', roles: ['LANDLORD'] },
        { name: '全部咨询', to: '/contacts/admin', roles: ['ADMIN'] },
        { name: '客服中心', to: '/support/center', roles: ['USER', 'LANDLORD'] },
        { name: '客服工单', to: '/support/admin', roles: ['ADMIN'] },
        { name: '我的订单', to: '/orders/mine', roles: ['USER'] },
        { name: '房东订单', to: '/orders/landlord', roles: ['LANDLORD'] },
        { name: '全部订单', to: '/orders/admin', roles: ['ADMIN'] },
      ],
    },
    {
      title: '认证与公告',
      items: [
        { name: '房东认证', to: '/certification/apply', roles: ['LANDLORD'] },
        { name: '认证审核', to: '/certification/admin', roles: ['ADMIN'] },
        { name: '公告中心', to: '/announcements', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        { name: '公告管理', to: '/announcements/admin', roles: ['ADMIN'] },
      ],
    },
    {
      title: '后台管理',
      items: [
        { name: '统计分析', to: '/statistics', roles: ['ADMIN'] },
        { name: '用户管理', to: '/users/admin', roles: ['ADMIN'] },
        { name: '地区地铁', to: '/locations/admin', roles: ['ADMIN'] },
      ],
    },
  ]

  return base
    .map((group) => ({
      title: group.title,
      items: group.items.filter((item) => !item.roles || item.roles.includes(role)),
    }))
    .filter((group) => group.items.length > 0)
})

function isActive(path) {
  return route.path === path
}

async function logout() {
  await authStore.logout()
  router.replace('/login')
}

const userAvatar = computed(() => authStore.user?.avatarUrl || '')
const userInitial = computed(() => (authStore.userName?.[0] || '用').toUpperCase())
const userRoleLabel = computed(() => roleDisplay[authStore.userRole] || '访客')
</script>

<template>
  <div class="flex min-h-screen bg-slate-100">
    <aside
      class="z-20 flex w-64 flex-col border-r border-slate-200 bg-white/95 shadow-lg backdrop-blur transition md:sticky md:top-0 md:h-screen"
      :class="menuOpen ? 'fixed inset-y-0 left-0 md:static' : 'hidden md:flex'"
    >
      <div class="flex items-center justify-between px-6 py-5">
        <div>
          <p class="text-lg font-semibold text-slate-900">租房后台</p>
          <p class="text-xs text-slate-500">{{ authStore.userRole || '未登录' }}</p>
        </div>
        <button class="md:hidden" @click="menuOpen = false">
          <ArrowLeftStartOnRectangleIcon class="h-5 w-5 text-slate-500" />
        </button>
      </div>
      <nav class="flex-1 space-y-6 overflow-y-auto px-4 pb-6">
        <div v-for="group in navigationGroups" :key="group.title" class="space-y-2">
          <p class="px-2 text-xs font-medium uppercase tracking-wide text-slate-400">{{ group.title }}</p>
          <ul class="space-y-1">
            <li v-for="item in group.items" :key="item.to">
              <RouterLink
                :to="item.to"
                class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition"
                :class="isActive(item.to)
                  ? 'bg-indigo-50 text-indigo-600'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'"
                @click="menuOpen = false"
              >
                <component v-if="item.icon" :is="item.icon" class="h-4 w-4" />
                <span>{{ item.name }}</span>
              </RouterLink>
            </li>
          </ul>
        </div>
      </nav>
      <div class="border-t border-slate-200 px-4 py-4">
        <button
          class="flex w-full items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
          @click="logout"
        >
          <ArrowLeftStartOnRectangleIcon class="h-4 w-4" />
          退出登录
        </button>
      </div>
    </aside>

    <div class="flex flex-1 flex-col">
      <header class="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur">
        <div class="flex items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <div class="flex items-center gap-3">
            <button class="rounded-lg border border-slate-200 p-2 text-slate-600 md:hidden" @click="menuOpen = true">
              <Bars3Icon class="h-5 w-5" />
            </button>
            <div>
              <h1 class="text-lg font-semibold text-slate-900">
                {{ route.meta?.title || '控制面板' }}
              </h1>
              <p class="text-xs text-slate-500">
                欢迎回来，{{ authStore.userName || '用户' }}
              </p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <ChatNotificationBell />
            <RouterLink
              to="/profile"
              class="group flex items-center gap-3 rounded-full border border-slate-200 px-3 py-1.5 text-left transition hover:border-indigo-200 hover:bg-indigo-50"
            >
              <img
                v-if="userAvatar"
                :src="userAvatar"
                alt="用户头像"
                class="h-10 w-10 rounded-full object-cover ring-2 ring-white group-hover:ring-indigo-100"
              />
              <div
                v-else
                class="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-600"
              >
                {{ userInitial }}
              </div>
              <div class="leading-tight">
                <p class="text-sm font-semibold text-slate-900">
                  {{ authStore.userName || '用户' }}
                </p>
                <p class="text-xs text-slate-400">
                  {{ userRoleLabel }}
                </p>
              </div>
            </RouterLink>
          </div>
        </div>
      </header>

      <main class="flex-1 bg-slate-100/80">
        <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
