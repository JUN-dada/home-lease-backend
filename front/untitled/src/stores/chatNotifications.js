import { defineStore } from 'pinia'
import { contactApi } from '../services/apiClient'
import { createChatClient } from '../services/chatClient'
import { useAuthStore } from './auth'
import { useSupportStore } from './support'

let toastSeed = 0

const CONTACT_SEEN_KEY = 'chat_seen_contacts'
const SUPPORT_SEEN_KEY = 'chat_seen_support'

function formatPreview(message) {
  if (message?.content) {
    return message.content.length > 32 ? `${message.content.slice(0, 32)}…` : message.content
  }
  if (message?.imageUrls?.length) {
    return '发送了图片'
  }
  return '发送了新消息'
}

function loadSeenMap(key) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      return parsed
    }
    return {}
  } catch {
    return {}
  }
}

function saveSeenMap(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // ignore storage issues
  }
}

export const useChatNotificationStore = defineStore('chatNotifications', {
  state: () => ({
    status: 'idle',
    clientRef: null,
    userSubscription: null,
    conversations: [],
    toastQueue: [],
    activeContactId: null,
    panel: {
      open: false,
      contactId: null,
      houseId: null,
      houseTitle: '',
      headerTitle: '',
    },
    lastSeenContacts: loadSeenMap(CONTACT_SEEN_KEY),
    lastSeenSupport: loadSeenMap(SUPPORT_SEEN_KEY),
  }),
  getters: {
    sortedConversations(state) {
      return [...state.conversations].sort((a, b) => (b.lastMessageAt ?? 0) - (a.lastMessageAt ?? 0))
    },
    unreadTotal(state) {
      return state.conversations.reduce((sum, convo) => sum + (convo.unread || 0), 0)
    },
    toasts(state) {
      return state.toastQueue
    },
  },
  actions: {
    async bootstrap(force = false) {
      const authStore = useAuthStore()
      if (!authStore.token || !authStore.user?.id) {
        return
      }
      if (this.clientRef && !force) {
        return
      }
      await this.refreshConversations()
      this.startClient(authStore.token)
    },
    async refreshConversations() {
      const authStore = useAuthStore()
      const role = authStore.userRole
      const params = { page: 0, size: 50 }
      try {
        if (role === 'USER') {
          const response = await contactApi.mine(params)
          this.conversations = this.normalizeContacts(response?.content || [], role)
        } else if (role === 'LANDLORD') {
          const response = await contactApi.landlord(params)
          this.conversations = this.normalizeContacts(response?.content || [], role)
        } else {
          this.conversations = []
        }
      } catch (err) {
        console.error('Failed to load contacts for notifications', err)
        this.conversations = []
      }
      this.generateOfflineContactToasts()
    },
    normalizeContacts(records, role) {
      return records.map(record => ({
        contactId: record.id,
        houseId: record.houseId,
        houseTitle: record.houseTitle,
        partnerName: role === 'USER' ? record.landlordName : record.tenantName,
        lastMessage: '',
        lastMessageAt: null,
        unread: 0,
      }))
    },
    generateOfflineContactToasts() {
      if (!Array.isArray(this.conversations)) return
      for (const convo of this.conversations) {
        if (!convo.contactId || !convo.lastMessageAt) continue
        const lastTime = typeof convo.lastMessageAt === 'number'
          ? convo.lastMessageAt
          : new Date(convo.lastMessageAt).getTime()
        if (!lastTime || Number.isNaN(lastTime)) continue
        const seen = this.lastSeenContacts[convo.contactId]
        if (seen == null) {
          this.lastSeenContacts[convo.contactId] = lastTime
          saveSeenMap(CONTACT_SEEN_KEY, this.lastSeenContacts)
          continue
        }
        if (lastTime > seen) {
          const dummyMessage = {
            contactId: convo.contactId,
            senderName: convo.partnerName,
            content: convo.lastMessage,
            createdAt: convo.lastMessageAt,
          }
          this.enqueueToast(dummyMessage, convo)
        }
      }
    },
    startClient(token) {
      if (this.clientRef) {
        return
      }
      const authStore = useAuthStore()
      this.status = 'connecting'
      this.clientRef = createChatClient({
        token,
        onConnected: () => {
          this.status = 'connected'
          const userId = authStore.user?.id
          if (userId) {
            this.userSubscription = this.clientRef.subscribeUser(userId, (message) => this.handleIncoming(message))
          }
        },
        onError: () => {
          this.status = 'error'
        },
      })
    },
    handleIncoming(message) {
      if (!message) return
      const authStore = useAuthStore()
      if (message.senderId && authStore.user?.id && message.senderId === authStore.user.id) {
        if (message.contactId) {
          this.markConversationRead(message.contactId, message.createdAt)
        }
        return
      }
      if (!message.contactId && message.ticketId) {
        this.enqueueSupportToast(message)
        return
      }
      if (!message.contactId) {
        return
      }
      const entry = this.ensureConversationFromMessage(message)
      entry.lastMessage = formatPreview(message)
      entry.lastMessageAt = message.createdAt ? new Date(message.createdAt).getTime() : Date.now()
      if (this.activeContactId === message.contactId && this.panel.open) {
        entry.unread = 0
        this.markConversationRead(message.contactId, message.createdAt)
        return
      }
      entry.unread = (entry.unread || 0) + 1
      this.enqueueToast(message, entry)
    },
    ensureConversationFromMessage(message) {
      let entry = this.conversations.find(convo => convo.contactId === message.contactId)
      if (!entry) {
        entry = {
          contactId: message.contactId,
          houseId: message.contactHouseId || null,
          houseTitle: message.contactHouseTitle || '聊天',
          partnerName: this.resolvePartnerName(message),
          lastMessage: '',
          lastMessageAt: null,
          unread: 0,
        }
        this.conversations.unshift(entry)
      } else {
        if (!entry.houseId && message.contactHouseId) {
          entry.houseId = message.contactHouseId
        }
        if (!entry.houseTitle && message.contactHouseTitle) {
          entry.houseTitle = message.contactHouseTitle
        }
        if (!entry.partnerName) {
          entry.partnerName = this.resolvePartnerName(message)
        }
      }
      return entry
    },
    resolvePartnerName(message) {
      const authStore = useAuthStore()
      if (authStore.userRole === 'LANDLORD') {
        return message.contactTenantName || message.senderName || '租客'
      }
      if (authStore.userRole === 'USER') {
        return message.contactLandlordName || message.senderName || '房东'
      }
      return message.senderName || '用户'
    },
    enqueueToast(message, entry) {
      if (entry.contactId === this.activeContactId && this.panel.open) {
        entry.unread = 0
        return
      }
      const toast = {
        id: ++toastSeed,
        type: 'contact',
        contactId: message.contactId,
        senderName: message.senderName || entry.partnerName || '新消息',
        preview: formatPreview(message),
        createdAt: message.createdAt,
      }
      this.toastQueue.push(toast)
      setTimeout(() => this.dismissToast(toast.id), 5000)
    },
    enqueueSupportToast(message) {
      const supportStore = useSupportStore()
      if (supportStore.panel.open && supportStore.panel.ticketId === message.ticketId) {
        supportStore.markTicketNeedsRefresh(message.ticketId)
        this.markSupportTicketRead(message.ticketId, message.createdAt)
        return
      }
      const toast = {
        id: ++toastSeed,
        type: 'support',
        ticketId: message.ticketId,
        senderName: message.senderName || '客服',
        preview: formatPreview(message) || `工单：${message.ticketSubject || ''}`,
        subject: message.ticketSubject || '',
        createdAt: message.createdAt,
      }
      this.toastQueue.push(toast)
      supportStore.markTicketNeedsRefresh(message.ticketId)
      setTimeout(() => this.dismissToast(toast.id), 5000)
    },
    syncSupportTickets(tickets = []) {
      const supportStore = useSupportStore()
      tickets.forEach(ticket => {
        if (!ticket.id || !ticket.updatedAt) return
        const lastTime = new Date(ticket.updatedAt).getTime()
        if (!lastTime || Number.isNaN(lastTime)) return
        const seen = this.lastSeenSupport[ticket.id]
        if (seen == null) {
          this.lastSeenSupport[ticket.id] = lastTime
          saveSeenMap(SUPPORT_SEEN_KEY, this.lastSeenSupport)
          return
        }
        if (lastTime > seen) {
          if (supportStore.panel.open && supportStore.panel.ticketId === ticket.id) {
            this.markSupportTicketRead(ticket.id, ticket.updatedAt)
            return
          }
          const toast = {
            id: ++toastSeed,
            type: 'support',
            ticketId: ticket.id,
            senderName: ticket.requesterName || '用户',
            preview: ticket.latestMessage || ticket.subject || '新的客服消息',
            subject: ticket.subject || '',
            createdAt: ticket.updatedAt,
          }
          this.toastQueue.push(toast)
          setTimeout(() => this.dismissToast(toast.id), 5000)
        }
      })
    },
    markSupportTicketRead(ticketId, timestamp) {
      if (!ticketId) return
      const timeValue = timestamp ? new Date(timestamp).getTime() : Date.now()
      if (!timeValue || Number.isNaN(timeValue)) return
      this.lastSeenSupport[ticketId] = timeValue
      saveSeenMap(SUPPORT_SEEN_KEY, this.lastSeenSupport)
    },
    dismissToast(id) {
      this.toastQueue = this.toastQueue.filter(item => item.id !== id)
    },
    openConversation(contactId) {
      if (!contactId) return
      const entry = this.conversations.find(convo => convo.contactId === contactId)
      if (!entry) {
        return
      }
      this.panel = {
        open: true,
        contactId,
        houseId: entry.houseId,
        houseTitle: entry.houseTitle,
        headerTitle: `${entry.houseTitle || '聊天'} · ${entry.partnerName || ''}`,
      }
      this.activeContactId = contactId
      entry.unread = 0
    },
    closePanel() {
      this.panel = {
        open: false,
        contactId: null,
        houseId: null,
        houseTitle: '',
        headerTitle: '',
      }
      this.activeContactId = null
    },
    setActiveContact(contactId) {
      this.activeContactId = contactId
      this.markConversationRead(contactId)
    },
    clearActiveContact(contactId) {
      if (this.activeContactId === contactId) {
        this.activeContactId = null
      }
      this.markConversationRead(contactId)
    },
    markConversationRead(contactId, timestamp) {
      if (!contactId) return
      const entry = this.conversations.find(convo => convo.contactId === contactId)
      if (entry) {
        entry.unread = 0
      }
      const timeValue = timestamp ? new Date(timestamp).getTime() : Date.now()
      if (timeValue) {
        this.lastSeenContacts[contactId] = timeValue
        saveSeenMap(CONTACT_SEEN_KEY, this.lastSeenContacts)
      }
    },
    teardown() {
      if (this.userSubscription) {
        this.userSubscription.unsubscribe()
        this.userSubscription = null
      }
      if (this.clientRef) {
        this.clientRef.disconnect()
        this.clientRef = null
      }
      this.status = 'idle'
      this.conversations = []
      this.toastQueue = []
      this.panel = {
        open: false,
        contactId: null,
        houseId: null,
        houseTitle: '',
        headerTitle: '',
      }
      this.activeContactId = null
    },
  },
})
