import request from './request'
import type { PageResult } from './product'

export const ORDER_STATUSES = [
  { value: 1, label: '待完成' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已取消' },
] as const

export type OrderStatusCode = (typeof ORDER_STATUSES)[number]['value']

export interface CreateOrderRequest {
  productId: number
  price: number | string
  remark?: string
}

export interface OrderVO {
  id: number
  productId: number
  productTitle: string
  productImage?: string
  price: string
  buyerId: number
  sellerId: number
  status: OrderStatusCode
  statusLabel: string
  remark?: string
  createdAt: string
  completedAt?: string
  cancelledAt?: string
}

export const orderApi = {
  create(body: CreateOrderRequest) {
    return request.post<unknown, OrderVO>('/order/order', body)
  },
  confirm(id: number) {
    return request.post<unknown, OrderVO>(`/order/order/${id}/confirm`)
  },
  cancel(id: number) {
    return request.post<unknown, OrderVO>(`/order/order/${id}/cancel`)
  },
  detail(id: number) {
    return request.get<unknown, OrderVO>(`/order/order/${id}`)
  },
  mineAsBuyer(page = 1, size = 20) {
    return request.get<unknown, PageResult<OrderVO>>('/order/order/buyer/mine', {
      params: { page, size },
    })
  },
  mineAsSeller(page = 1, size = 20) {
    return request.get<unknown, PageResult<OrderVO>>('/order/order/seller/mine', {
      params: { page, size },
    })
  },
}
