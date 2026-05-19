import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'market',
    component: () => import('@/views/MarketView.vue'),
    meta: { title: '商品广场' },
  },
  {
    path: '/products/:id(\\d+)',
    name: 'product-detail',
    component: () => import('@/views/ProductDetailView.vue'),
    meta: { title: '商品详情' },
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('@/views/PublishProductView.vue'),
    meta: { title: '发布商品' },
  },
  {
    path: '/products/mine',
    name: 'my-products',
    component: () => import('@/views/MyProductsView.vue'),
    meta: { title: '我的商品' },
  },
  {
    path: '/orders/mine',
    name: 'my-orders',
    component: () => import('@/views/MyOrdersView.vue'),
    meta: { title: '我的订单' },
  },
  {
    path: '/orders/:id(\\d+)',
    name: 'order-detail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情' },
  },
  {
    path: '/messages',
    name: 'my-messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '我的消息' },
  },
  {
    path: '/messages/:peerId(\\d+)',
    name: 'chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: '聊天' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人资料' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', noAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', noAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 校园二手交易`
  }

  if (to.meta?.noAuth) return true

  const user = useUserStore()
  if (!user.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
