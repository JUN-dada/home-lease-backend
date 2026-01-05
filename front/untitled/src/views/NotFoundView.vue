<template>
  <div class="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slate-100 to-slate-200">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-slate-800">404</h1>
      <p class="mt-4 text-lg text-slate-600">页面走丢了</p>
      <router-link
        to="/"
        class="mt-8 inline-flex items-center rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow transition hover:bg-indigo-500"
      >
        返回首页
      </router-link>
    </div>
  </div>
</template>
