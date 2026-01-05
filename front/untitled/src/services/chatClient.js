import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import httpClient, { API_BASE_URL } from './httpClient'
import { contactApi } from './apiClient'

const WS_ENDPOINT = '/ws'
const SOCKJS_ENDPOINT = '/ws/sockjs'

function resolveWsBase() {
  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL.replace(/\/$/, '')
  }
  if (API_BASE_URL?.startsWith('http')) {
    const url = new URL(API_BASE_URL)
    return `${url.protocol}//${url.host}`
  }
  return window.location.origin
}

const WS_BASE = resolveWsBase()

export function createChatClient({ token, onConnected, onMessage, onError }) {
  const client = new Client({
    connectHeaders: {
      'X-Auth-Token': token,
    },
    debug: () => {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })

  const sockJsUrl = `${WS_BASE}${SOCKJS_ENDPOINT}?token=${token}`
  client.webSocketFactory = () => new SockJS(sockJsUrl)
  client.logRawCommunication = false

  client.onConnect = () => onConnected?.()
  client.onStompError = frame => onError?.(frame.headers['message'] || frame.body || '聊天连接异常')
  client.onWebSocketClose = event => {
    if (!event.wasClean) {
      onError?.('聊天连接已断开，正在重试...')
    }
  }
  client.onWebSocketError = () => onError?.('无法建立聊天连接')

  client.activate()

  return {
    client,
    subscribe(contactId) {
      return client.subscribe(`/topic/contacts/${contactId}`, message => {
        try {
          const body = JSON.parse(message.body)
          onMessage?.(body)
        } catch (err) {
          console.error('Failed to parse chat message', err)
        }
      })
    },
    subscribeSupport(ticketId, callback) {
      return client.subscribe(`/topic/support/${ticketId}`, message => {
        try {
          const body = JSON.parse(message.body)
          callback?.(body)
          onMessage?.(body)
        } catch (err) {
          console.error('Failed to parse support message', err)
        }
      })
    },
    subscribeUser(userId, callback) {
      return client.subscribe(`/topic/users/${userId}`, message => {
        try {
          const body = JSON.parse(message.body)
          callback?.(body)
          onMessage?.(body)
        } catch (err) {
          console.error('Failed to parse user notification message', err)
        }
      })
    },
    subscribeGeneric(destination, callback) {
      return client.subscribe(destination, message => {
        try {
          const body = JSON.parse(message.body)
          callback?.(body)
        } catch (err) {
          console.error('Failed to parse generic subscription message', err)
        }
      })
    },
    send(contactId, payload) {
      this.publish(`/app/contacts/${contactId}/messages`, payload)
    },
    sendSupport(ticketId, payload) {
      this.publish(`/app/support/${ticketId}/messages`, payload)
    },
    publish(destination, payload) {
      client.publish({
        destination,
        body: JSON.stringify(payload),
      })
    },
    disconnect() {
      client.deactivate()
    },
  }
}

export function fetchHistory(contactId, params = { page: 0, size: 50 }) {
  return httpClient.get(`/api/chat/contacts/${contactId}/messages`, { params })
}

export function ensureContact(payload) {
  return contactApi.ensure(payload)
}
