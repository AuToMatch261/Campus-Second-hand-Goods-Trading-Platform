import request from './request'
import type { PageResult } from './product'

export interface SendMessageRequest {
  toUserId: number
  content: string
  relatedProductId?: number
}

export interface MessageVO {
  id: number
  conversationId: number
  fromId: number
  toId: number
  content: string
  relatedProductId?: number
  createdAt: string
}

export interface ConversationVO {
  id: number
  peerId: number
  lastMessageId?: number
  lastMessageText?: string
  lastMessageAt?: string
  unread: number
}

export interface NotificationVO {
  id: number
  type: string
  title: string
  content?: string
  refType?: string
  refId?: number
  readAt?: string
  createdAt: string
}

export const messageApi = {
  send(body: SendMessageRequest) {
    return request.post<unknown, MessageVO>('/message/message/send', body)
  },
  conversations() {
    return request.get<unknown, ConversationVO[]>('/message/message/conversations')
  },
  messages(peerId: number, page = 1, size = 30) {
    return request.get<unknown, PageResult<MessageVO>>(
      `/message/message/conversations/${peerId}/messages`,
      { params: { page, size } },
    )
  },
  unreadCount() {
    return request.get<unknown, number>('/message/message/unread-count')
  },
}

export const notificationApi = {
  list(page = 1, size = 20) {
    return request.get<unknown, PageResult<NotificationVO>>('/message/notifications', {
      params: { page, size },
    })
  },
  unreadCount() {
    return request.get<unknown, number>('/message/notifications/unread-count')
  },
  read(id: number) {
    return request.post<unknown, void>(`/message/notifications/${id}/read`)
  },
  readAll() {
    return request.post<unknown, void>('/message/notifications/read-all')
  },
}
