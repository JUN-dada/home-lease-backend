import { defineStore } from 'pinia'
import { createChatClient } from '../services/chatClient'
import { useAuthStore } from './auth'
import { getCertificationStatusLabel } from '../utils/statusLabels'
import { useSupportStore } from './support'

let alertSeed = 0

export const useAdminAlertStore = defineStore('adminAlerts', {
  state: () => ({
    clientRef: null,
    subscriptions: [],
    status: 'idle',
    toasts: [],
  }),
  actions: {
    async bootstrap() {
      const authStore = useAuthStore()
      if (!authStore.token || authStore.userRole !== 'ADMIN') {
        return
      }
      if (this.clientRef) {
        return
      }
      this.status = 'connecting'
      this.clientRef = createChatClient({
        token: authStore.token,
        onConnected: () => {
          this.status = 'connected'
          this.subscriptions = [
            this.clientRef.subscribeGeneric('/topic/admin/certifications', (payload) => {
              this.handleCertification(payload)
            }),
            this.clientRef.subscribeGeneric('/topic/admin/support', (payload) => {
              this.handleSupport(payload)
            }),
          ]
        },
        onError: () => {
          this.status = 'error'
        },
      })
    },
    handleCertification(payload) {
      if (!payload) return
      const toast = {
        id: ++alertSeed,
        type: 'certification',
        applicant: payload?.applicantName || '房东',
        status: getCertificationStatusLabel(payload?.status) || '待审核',
        createdAt: payload?.createdAt,
        certificationId: payload?.id,
      }
      this.toasts.push(toast)
      setTimeout(() => this.dismissToast(toast.id), 6000)
    },
    handleSupport(payload) {
      if (!payload) return
      const supportStore = useSupportStore()
      const toast = {
        id: ++alertSeed,
        type: 'support',
        subject: payload?.subject || '新的客服工单',
        requester: payload?.requesterName || '用户',
        status: payload?.status || 'OPEN',
        createdAt: payload?.createdAt,
        ticketId: payload?.id,
      }
      this.toasts.push(toast)
      supportStore.markTicketNeedsRefresh(payload?.id)
      setTimeout(() => this.dismissToast(toast.id), 6000)
    },
    dismissToast(id) {
      this.toasts = this.toasts.filter(item => item.id !== id)
    },
    teardown() {
      this.subscriptions.forEach(sub => {
        try {
          sub?.unsubscribe?.()
        } catch {
          // ignore
        }
      })
      this.subscriptions = []
      if (this.clientRef) {
        this.clientRef.disconnect()
        this.clientRef = null
      }
      this.status = 'idle'
      this.toasts = []
    },
  },
})
