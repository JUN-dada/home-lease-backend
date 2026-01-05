<script setup>
import { computed } from 'vue'
import SupportChatPanel from './SupportChatPanel.vue'
import { useSupportStore } from '../stores/support'

const supportStore = useSupportStore()
const panelState = computed(() => supportStore.panel)

function handleClose() {
  supportStore.closePanel()
}
</script>

<template>
  <SupportChatPanel
    :open="panelState.open"
    :ticket-id="panelState.ticketId"
    :subject="panelState.subject"
    @close="handleClose"
  />
</template>
