<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { messageApi, notificationApi, type ConversationVO, type NotificationVO } from '@/api/message'
import { userApi, type Profile } from '@/api/user'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const messageStore = useMessageStore()

const tab = ref<'conversations' | 'notifications'>('conversations')

const conversations = ref<ConversationVO[]>([])
const peerProfiles = ref<Record<number, Profile | null>>({})
const loadingConv = ref(false)

const notifications = ref<NotificationVO[]>([])
const notifTotal = ref(0)
const notifPage = ref(1)
const notifSize = 20
const loadingNotif = ref(false)

const unreadConvCount = computed(
  () => conversations.value.reduce((sum, c) => sum + (c.unread || 0), 0),
)
const unreadNotifCount = computed(
  () => notifications.value.filter((n) => !n.readAt).length,
)

async function loadConversations() {
  loadingConv.value = true
  try {
    conversations.value = await messageApi.conversations()
    const ids = Array.from(new Set(conversations.value.map((c) => c.peerId)))
    const profiles = await Promise.all(
      ids.map((id) => userApi.getById(id).catch(() => null)),
    )
    const map: Record<number, Profile | null> = {}
    ids.forEach((id, i) => (map[id] = profiles[i]))
    peerProfiles.value = map
  } finally {
    loadingConv.value = false
  }
}

async function loadNotifications() {
  loadingNotif.value = true
  try {
    const r = await notificationApi.list(notifPage.value, notifSize)
    notifications.value = r.records
    notifTotal.value = r.total
  } finally {
    loadingNotif.value = false
  }
}

function peerName(id: number): string {
  const p = peerProfiles.value[id]
  return p?.nickname || p?.username || `用户#${id}`
}
function peerInitial(id: number): string {
  return peerName(id).slice(0, 1).toUpperCase()
}
function peerAvatar(id: number): string | undefined {
  return peerProfiles.value[id]?.avatar
}

function openChat(peerId: number) {
  router.push({ name: 'chat', params: { peerId } })
}

async function markOneRead(n: NotificationVO) {
  if (n.readAt) return
  await notificationApi.read(n.id)
  n.readAt = new Date().toISOString()
  messageStore.refresh()
}

async function markAllRead() {
  await notificationApi.readAll()
  notifications.value.forEach((n) => (n.readAt = n.readAt || new Date().toISOString()))
  messageStore.refresh()
}

function jumpFromNotif(n: NotificationVO) {
  markOneRead(n)
  if (n.refType === 'ORDER' && n.refId) {
    router.push({ name: 'order-detail', params: { id: n.refId } })
  }
}

function notifIcon(type: string) {
  switch (type) {
    case 'ORDER_CREATED': return '🛒'
    case 'ORDER_CONFIRMED': return '✅'
    case 'ORDER_CANCELLED': return '✖'
    default: return '🔔'
  }
}
function notifCls(type: string) {
  switch (type) {
    case 'ORDER_CREATED': return 'i-blue'
    case 'ORDER_CONFIRMED': return 'i-green'
    case 'ORDER_CANCELLED': return 'i-orange'
    default: return 'i-gray'
  }
}
function notifLabel(type: string) {
  switch (type) {
    case 'ORDER_CREATED': return '新订单'
    case 'ORDER_CONFIRMED': return '已完成'
    case 'ORDER_CANCELLED': return '已取消'
    default: return type
  }
}

onMounted(() => {
  loadConversations()
  loadNotifications()
})
</script>

<template>
  <div class="page messages">
    <AppHeader />
    <main class="page-main">
      <header class="head">
        <div>
          <span class="head-tag">INBOX</span>
          <h1>我的消息</h1>
          <p>私信 <strong>{{ conversations.length }}</strong> · 未读 <strong>{{ unreadConvCount + unreadNotifCount }}</strong></p>
        </div>
        <button class="refresh" @click="tab === 'conversations' ? loadConversations() : loadNotifications()">
          🔄 刷新
        </button>
      </header>

      <div class="tabs">
        <button
          class="tab"
          :class="{ active: tab === 'conversations' }"
          @click="tab = 'conversations'"
        >
          💬 私信
          <span v-if="unreadConvCount > 0" class="tab-badge">{{ unreadConvCount > 99 ? '99+' : unreadConvCount }}</span>
        </button>
        <button
          class="tab"
          :class="{ active: tab === 'notifications' }"
          @click="tab = 'notifications'"
        >
          🔔 系统通知
          <span v-if="unreadNotifCount > 0" class="tab-badge">{{ unreadNotifCount > 99 ? '99+' : unreadNotifCount }}</span>
        </button>
      </div>

      <!-- Conversations -->
      <section v-if="tab === 'conversations'" v-loading="loadingConv" class="panel glass-strong">
        <el-empty
          v-if="!loadingConv && conversations.length === 0"
          description="还没有任何会话"
        />
        <div v-else class="list">
          <div
            v-for="c in conversations"
            :key="c.id"
            class="row"
            :class="{ unread: c.unread > 0 }"
            @click="openChat(c.peerId)"
          >
            <div class="avatar">
              <img v-if="peerAvatar(c.peerId)" :src="peerAvatar(c.peerId)" :alt="peerName(c.peerId)" />
              <span v-else>{{ peerInitial(c.peerId) }}</span>
              <span v-if="c.unread > 0" class="badge">{{ c.unread > 99 ? '99+' : c.unread }}</span>
            </div>
            <div class="body">
              <div class="row-top">
                <strong>{{ peerName(c.peerId) }}</strong>
                <span class="time">{{ c.lastMessageAt?.replace('T', ' ').slice(0, 16) }}</span>
              </div>
              <div class="preview">{{ c.lastMessageText || '(无消息)' }}</div>
            </div>
            <span class="arrow">→</span>
          </div>
        </div>
      </section>

      <!-- Notifications -->
      <section v-else v-loading="loadingNotif" class="panel glass-strong">
        <div class="notif-toolbar">
          <button
            class="mark-all"
            :disabled="unreadNotifCount === 0"
            @click="markAllRead"
          >
            全部标记为已读
          </button>
        </div>

        <el-empty
          v-if="!loadingNotif && notifications.length === 0"
          description="暂无系统通知"
        />

        <div v-else class="list">
          <div
            v-for="n in notifications"
            :key="n.id"
            class="row notif"
            :class="{ unread: !n.readAt }"
            @click="jumpFromNotif(n)"
          >
            <div class="icon" :class="notifCls(n.type)">{{ notifIcon(n.type) }}</div>
            <div class="body">
              <div class="row-top">
                <strong>
                  {{ n.title }}
                  <span class="type-tag" :class="notifCls(n.type)">{{ notifLabel(n.type) }}</span>
                </strong>
                <span class="time">{{ n.createdAt?.replace('T', ' ').slice(0, 16) }}</span>
              </div>
              <div class="preview">{{ n.content }}</div>
            </div>
            <span v-if="!n.readAt" class="dot-new" />
          </div>
        </div>

        <div v-if="notifTotal > notifSize" class="pager">
          <el-pagination
            v-model:current-page="notifPage"
            :page-size="notifSize"
            :total="notifTotal"
            background
            layout="prev, pager, next"
            @current-change="loadNotifications"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;

  .head-tag {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 2px;
    color: var(--brand-1);
    background: rgba(99, 102, 241, 0.12);
  }
  h1 {
    margin: 12px 0 6px;
    font-size: 26px;
    font-weight: 700;
  }
  p {
    margin: 0;
    color: var(--text-muted);
    font-size: 13px;
    strong { color: var(--brand-1); margin: 0 2px; }
  }
}
.refresh {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(99, 102, 241, 0.18);
  color: var(--brand-1);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  &:hover { background: var(--brand-grad); color: #fff; border-color: transparent; }
}

.tabs {
  display: inline-flex;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 999px;
  padding: 4px;
  gap: 2px;
  backdrop-filter: blur(10px);
}
.tab {
  border: none;
  background: transparent;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s var(--ease-out);
  &:hover { color: var(--brand-1); }
  &.active {
    background: var(--brand-grad);
    color: #fff;
    box-shadow: var(--shadow-brand);
  }
}
.tab-badge {
  background: linear-gradient(135deg, #ef4444, #ec4899);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
  box-shadow: 0 4px 10px -2px rgba(239, 68, 68, 0.5);
}

.panel {
  padding: 16px;
}

.notif-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
.mark-all {
  background: transparent;
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: var(--brand-1);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover:not(:disabled) { background: rgba(99, 102, 241, 0.08); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

.list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s var(--ease-out);
  position: relative;

  &:hover {
    background: rgba(99, 102, 241, 0.06);
    transform: translateX(2px);
  }

  &.unread {
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(6, 182, 212, 0.04));
  }
}

.avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-brand);
  img { width: 100%; height: 100%; object-fit: cover; }
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ef4444, #ec4899);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: grid;
  place-items: center;
  border: 2px solid #fff;
}

.icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 22px;
  flex-shrink: 0;
  color: #fff;

  &.i-blue { background: linear-gradient(135deg, #6366f1, #06b6d4); }
  &.i-green { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.i-orange { background: linear-gradient(135deg, #f59e0b, #f97316); }
  &.i-gray { background: linear-gradient(135deg, #6b7280, #4b5563); }
}

.body {
  flex: 1;
  min-width: 0;
}
.row-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;

  strong {
    color: var(--text-primary);
    font-size: 14px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}
.type-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  color: #fff;
  letter-spacing: 0.5px;
  &.i-blue { background: linear-gradient(135deg, #6366f1, #06b6d4); }
  &.i-green { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.i-orange { background: linear-gradient(135deg, #f59e0b, #f97316); }
  &.i-gray { background: linear-gradient(135deg, #6b7280, #4b5563); }
}
.time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.preview {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.arrow {
  color: var(--text-muted);
  opacity: 0.5;
  transition: transform 0.2s var(--ease-out), color 0.2s;
}
.row:hover .arrow {
  color: var(--brand-1);
  opacity: 1;
  transform: translateX(4px);
}
.dot-new {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444, #ec4899);
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.18);
  flex-shrink: 0;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
