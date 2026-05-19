import request from './request'

export const PRODUCT_CATEGORIES = [
  { value: 1, label: '书籍' },
  { value: 2, label: '电子' },
  { value: 3, label: '日用' },
  { value: 4, label: '服装' },
  { value: 5, label: '运动' },
  { value: 6, label: '其它' },
] as const

export const PRODUCT_STATUSES = [
  { value: 1, label: '在售' },
  { value: 2, label: '已售' },
  { value: 3, label: '已下架' },
] as const

export type ProductCategoryCode = (typeof PRODUCT_CATEGORIES)[number]['value']
export type ProductStatusCode = (typeof PRODUCT_STATUSES)[number]['value']

export interface PublishProductRequest {
  title: string
  description?: string
  price: number | string
  category: ProductCategoryCode
  images?: string[]
}

export interface UpdateProductRequest {
  title?: string
  description?: string
  price?: number | string
  category?: ProductCategoryCode
  images?: string[]
}

export interface ProductVO {
  id: number
  sellerId: number
  title: string
  description?: string
  price: string
  category: ProductCategoryCode
  categoryLabel: string
  images: string[]
  status: ProductStatusCode
  statusLabel: string
  viewCount: number
  createdAt: string
}

export type ProductSortBy = 'createdAt' | 'price' | 'viewCount'
export type ProductSortOrder = 'asc' | 'desc'

export interface ProductListQuery {
  page?: number
  size?: number
  category?: ProductCategoryCode
  status?: ProductStatusCode
  keyword?: string
  sellerId?: number
  sortBy?: ProductSortBy
  sortOrder?: ProductSortOrder
}

export interface PageResult<T> {
  page: number
  size: number
  total: number
  records: T[]
}

export const productApi = {
  /** 上传单张商品图片,返回 OSS 公开 URL */
  uploadImage(file: File) {
    const fd = new FormData()
    fd.append('file', file)
    return request.post<unknown, { url: string }>(
      '/product/me/products/upload',
      fd,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60_000,
      },
    )
  },
  publish(body: PublishProductRequest) {
    return request.post<unknown, ProductVO>('/product/me/products', body)
  },
  update(id: number, body: UpdateProductRequest) {
    return request.put<unknown, ProductVO>(`/product/me/products/${id}`, body)
  },
  offShelf(id: number) {
    return request.post<unknown, void>(`/product/me/products/${id}/off-shelf`)
  },
  remove(id: number) {
    return request.delete<unknown, void>(`/product/me/products/${id}`)
  },
  detail(id: number) {
    return request.get<unknown, ProductVO>(`/product/product/${id}`)
  },
  list(params: ProductListQuery = {}) {
    return request.get<unknown, PageResult<ProductVO>>('/product/product', { params })
  },
  mine(page = 1, size = 20) {
    return request.get<unknown, PageResult<ProductVO>>('/product/me/products', {
      params: { page, size },
    })
  },
}
