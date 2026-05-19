<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const messageStore = useMessageStore()

interface NavItem {
  name: string
  label: string
  icon: string
  badgeKey?: 'unread'
}

const navItems: NavItem[] = [
  { name: 'market', label: '广场', icon: '🛍' },
  { name: 'publish', label: '发布', icon: '✨' },
  { name: 'my-products', label: '我的商品', icon: '📦' },
  { name: 'my-orders', label: '我的订单', icon: '📋' },
  { name: 'my-messages', label: '消息', icon: '💬', badgeKey: 'unread' },
]

const activeName = computed(() => route.name as string)
const activeRoute = (n: string) => activeName.value === n
const initials = computed(() => {
  const n = user.profile?.nickname || user.profile?.username || '?'
  return n.slice(0, 1).toUpperCase()
})

function go(name: string) {
  router.push({ name })
}

function onLogout() {
  messageStore.stopPolling()
  messageStore.reset()
  user.logout()
  router.replace('/login')
}

watch(
  () => user.token,
  (t) => {
    if (t) messageStore.startPolling()
    else {
      messageStore.stopPolling()
      messageStore.reset()
    }
  },
)

onMounted(() => {
  if (user.token) messageStore.startPolling()
})

onUnmounted(() => {
  /* 切登录态时已在 watch 里处理 */
})
</script>

<template>
  <header class="app-header">
    <div class="bar">
      <RouterLink to="/" class="logo">
        <span class="logo-mark">校</span>
        <span class="logo-text">
          <strong>校园集市</strong>
          <small>Campus Market</small>
        </span>
      </RouterLink>

      <nav class="nav">
        <button
          v-for="item in navItems"
          :key="item.name"
          class="nav-item"
          :class="{ active: activeRoute(item.name) }"
          @click="go(item.name)"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
          <span
            v-if="item.badgeKey === 'unread' && messageStore.unread > 0"
            class="nav-dot"
          >
            {{ messageStore.unread > 99 ? '99+' : messageStore.unread }}
          </span>
          <span class="nav-indicator" />
        </button>
      </nav>

      <el-dropdown trigger="click" placement="bottom-end">
        <div class="me">
          <div class="avatar">
            <img v-if="user.profile?.avatar" :src="user.profile.avatar" alt="me" />
            <span v-else>{{ initials }}</span>
          </div>
          <div class="me-text">
            <span class="hi">你好</span>
            <strong>{{ user.profile?.nickname || user.profile?.username || '同学' }}</strong>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="go('profile')">个人资料</el-dropdown-item>
            <el-dropdown-item @click="go('my-products')">我的商品</el-dropdown-item>
            <el-dropdown-item @click="go('my-orders')">我的订单</el-dropdown-item>
            <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  /* 顶部一道渐变光带 */
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78) 0%, rgba(255, 255, 255, 0.62) 100%);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 8px 24px -16px rgba(80, 70, 200, 0.18);

  &::before {
    content: '';
    position: absolute;
    inset: 0 0 auto 0;
    height: 3px;
    background: var(--brand-grad);
    opacity: 0.9;
  }
}

.bar {
  max-width: 1320px;
  margin: 0 auto;
  height: 68px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--brand-grad);
  color: #fff;
  font-weight: 700;
  font-size: 18px;
  display: grid;
  place-items: center;
  box-shadow: var(--shadow-brand);
  letter-spacing: 0;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}
.logo-text strong {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.logo-text small {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-top: 2px;
}

.nav {
  flex: 1;
  display: flex;
  gap: 4px;
  justify-content: center;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s var(--ease-out), background 0.2s var(--ease-out);
}
.nav-item:hover {
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.06);
}
.nav-item.active {
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.10);
}
.nav-icon {
  font-size: 16px;
  line-height: 1;
}
.nav-indicator {
  position: absolute;
  left: 50%;
  bottom: -2px;
  transform: translateX(-50%) scaleX(0);
  width: 22px;
  height: 3px;
  background: var(--brand-grad);
  border-radius: 999px;
  transition: transform 0.3s var(--ease-out);
}
.nav-item.active .nav-indicator {
  transform: translateX(-50%) scaleX(1);
}
.nav-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ef4444 0%, #ec4899 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  margin-left: 2px;
  box-shadow: 0 4px 10px -2px rgba(239, 68, 68, 0.5);
}

.me {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  &:hover {
    border-color: rgba(99, 102, 241, 0.3);
    box-shadow: var(--shadow-sm);
  }
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-brand);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.me-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  font-size: 12px;
}
.hi {
  color: var(--text-muted);
  font-size: 11px;
}
.me-text strong {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .bar {
    padding: 0 16px;
    gap: 12px;
  }
  .logo-text {
    display: none;
  }
  .nav-label {
    display: none;
  }
  .me-text {
    display: none;
  }
}
</style>
