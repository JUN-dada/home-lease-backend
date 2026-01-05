<script setup>
import { RouterView } from 'vue-router'
import { watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import ChatToastStack from './components/ChatToastStack.vue'
import ChatPanelHost from './components/ChatPanelHost.vue'
import SupportPanelHost from './components/SupportPanelHost.vue'
import AdminCertificationToasts from './components/AdminCertificationToasts.vue'
import { useAuthStore } from './stores/auth'
import { useChatNotificationStore } from './stores/chatNotifications'
import { useAdminAlertStore } from './stores/adminAlerts'

const authStore = useAuthStore()
const chatStore = useChatNotificationStore()
const adminAlertStore = useAdminAlertStore()
const { token, user, userRole } = storeToRefs(authStore)

watchEffect(() => {
  const hasSession = Boolean(token.value && user.value?.id)
  if (hasSession) {
    chatStore.bootstrap()
    if (userRole.value === 'ADMIN') {
      adminAlertStore.bootstrap()
    } else {
      adminAlertStore.teardown()
    }
  } else {
    chatStore.teardown()
    adminAlertStore.teardown()
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <RouterView />
    <ChatToastStack />
    <AdminCertificationToasts />
    <ChatPanelHost />
    <SupportPanelHost />
  </div>
</template>
