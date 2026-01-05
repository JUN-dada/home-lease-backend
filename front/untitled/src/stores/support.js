import { defineStore } from 'pinia'

export const useSupportStore = defineStore('support', {
  state: () => ({
    panel: {
      open: false,
      ticketId: null,
      subject: '',
    },
    refreshToken: 0,
    lastUpdatedTicketId: null,
  }),
  actions: {
    openTicket(ticket) {
      if (!ticket?.id) return
      this.panel.open = true
      this.panel.ticketId = ticket.id
      this.panel.subject = ticket.subject || ''
    },
    openTicketById(ticketId, subject = '') {
      if (!ticketId) return
      this.panel.open = true
      this.panel.ticketId = ticketId
      this.panel.subject = subject || ''
    },
    closePanel() {
      this.panel.open = false
      this.panel.ticketId = null
      this.panel.subject = ''
    },
    markTicketNeedsRefresh(ticketId) {
      this.refreshToken = Date.now()
      this.lastUpdatedTicketId = ticketId || null
    },
  },
})
