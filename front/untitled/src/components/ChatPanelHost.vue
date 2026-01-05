<script setup>
import { computed } from 'vue'
import HouseChatPanel from './HouseChatPanel.vue'
import { useChatNotificationStore } from '../stores/chatNotifications'

const chatStore = useChatNotificationStore()
const panelState = computed(() => chatStore.panel)

function handleClose() {
  chatStore.closePanel()
}
</script>

<template>
  <HouseChatPanel
    :open="panelState.open"
    :contact-id="panelState.contactId"
    :house-id="panelState.houseId"
    :house-title="panelState.houseTitle"
    :header-title="panelState.headerTitle"
    :auto-ensure="false"
    @close="handleClose"
  />
</template>
