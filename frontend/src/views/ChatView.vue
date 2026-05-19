<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { messageApi, type MessageVO } from '@/api/message'
import { userApi, type Profile } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const messageStore = useMessageStore()

const peerId = computed(() => Number(route.params.peerId))
const peer = ref<Profile | null>(null)
const messages = ref<MessageVO[]>([])
const loading = ref(false)
const sending = ref(false)
const draft = ref('')
const listRef = ref<HTMLDivElement>()

let pollTimer: number | undefined

const peerName = computed(() => peer.value?.nickname || peer.value?.username || `用户#${peerId.value}`)
const peerInitial = computed(() => peerName.value.slice(0, 1).toUpperCase())

async function loadMessages() {
  if (!peerId.value) return
  const r = await messageApi.messages(peerId.value, 1, 50)
  messages.value = [...r.records].reverse()
  messageStore.refresh()
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

async function load() {
  loading.value = true
  try {
    peer.value = await userApi.getById(peerId.value).catch(() => null)
    await loadMessages()
  } finally {
    loading.value = false
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text) return
  sending.value = true
  try {
    await messageApi.send({ toUserId: peerId.value, content: text })
    draft.value = ''
    await loadMessages()
  } finally {
    sending.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(() => {
    loadMessages().catch(() => undefined)
  }, 5000)
}
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

watch(peerId, () => {
  load()
  startPolling()
})

onMounted(() => {
  if (peerId.value === user.profile?.id) {
    ElMessage.warning('不能和自己聊天')
    router.replace({ name: 'my-messages' })
    return
  }
  load()
  startPolling()
})
onUnmounted(stopPolling)

function isMine(m: MessageVO) {
  return m.fromId === user.profile?.id
}

const myInitial = computed(() => {
  const n = user.profile?.nickname || user.profile?.username || '我'
  return n.slice(0, 1).toUpperCase()
})

/* 按日期分组,每天的第一条前面插入日期分隔 */
const groups = computed(() => {
  const out: Array<{ kind: 'date'; text: string } | { kind: 'msg'; m: MessageVO }> = []
  let lastDate = ''
  for (const m of messages.value) {
    const d = m.createdAt?.slice(0, 10) || ''
    if (d && d !== lastDate) {
      out.push({ kind: 'date', text: d })
      lastDate = d
    }
    out.push({ kind: 'msg', m })
  }
  return out
})
</script>

<template>
  <div class="page chat-page">
    <AppHeader />
    <main class="page-main" v-loading="loading">
      <div class="chat glass-strong">
        <!-- 顶栏 -->
        <header class="chat-head">
          <button class="back" @click="router.push({ name: 'my-messages' })">←</button>
          <div class="who">
            <div class="avatar">
              <img v-if="peer?.avatar" :src="peer.avatar" :alt="peerName" />
              <span v-else>{{ peerInitial }}</span>
            </div>
            <div>
              <strong>{{ peerName }}</strong>
              <small>· 在线即收</small>
            </div>
          </div>
          <span />
        </header>

        <!-- 消息列表 -->
        <div ref="listRef" class="list">
          <div v-if="!loading && messages.length === 0" class="welcome">
            <div class="welcome-icon">💬</div>
            <p>开始你们的第一句话吧</p>
          </div>

          <template v-for="(g, i) in groups" :key="i">
            <div v-if="g.kind === 'date'" class="date-divider">
              <span>{{ g.text }}</span>
            </div>
            <div v-else class="row" :class="{ mine: isMine(g.m) }">
              <div v-if="!isMine(g.m)" class="bubble-avatar peer">
                <img v-if="peer?.avatar" :src="peer.avatar" />
                <span v-else>{{ peerInitial }}</span>
              </div>
              <div class="bubble">
                <div class="text">{{ g.m.content }}</div>
                <div class="time">{{ g.m.createdAt?.slice(11, 16) }}</div>
              </div>
              <div v-if="isMine(g.m)" class="bubble-avatar mine">
                <img v-if="user.profile?.avatar" :src="user.profile.avatar" />
                <span v-else>{{ myInitial }}</span>
              </div>
            </div>
          </template>
        </div>

        <!-- 输入区 -->
        <footer class="composer">
          <textarea
            v-model="draft"
            placeholder="按 Enter 发送,Shift+Enter 换行"
            maxlength="2000"
            rows="2"
            @keydown.enter.exact.prevent="send"
          />
          <button
            class="send"
            :disabled="!draft.trim() || sending"
            @click="send"
          >
            {{ sending ? '…' : '发送' }}
          </button>
        </footer>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 68px - 84px); /* 减去 header + 上下 padding */
  min-height: 520px;
  padding: 0;
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 22px;
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
  background: rgba(255, 255, 255, 0.5);

  .back {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    background: rgba(99, 102, 241, 0.08);
    color: var(--brand-1);
    font-size: 20px;
    cursor: pointer;
    transition: background 0.2s;
    &:hover { background: rgba(99, 102, 241, 0.18); }
  }
}
.who {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;

  strong {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }
  small {
    display: block;
    font-size: 11px;
    color: var(--text-muted);
    margin-top: 2px;
  }
}
.who .avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-brand);
  img { width: 100%; height: 100%; object-fit: cover; }
}

/* 消息列表 */
.list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background:
    radial-gradient(at 0% 0%, rgba(139, 92, 246, 0.04) 0, transparent 50%),
    radial-gradient(at 100% 100%, rgba(6, 182, 212, 0.04) 0, transparent 50%);
}

.welcome {
  margin: auto;
  text-align: center;
  color: var(--text-muted);
  .welcome-icon {
    font-size: 56px;
    margin-bottom: 12px;
  }
  p { margin: 0; font-size: 14px; }
}

.date-divider {
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
  margin: 8px 0;

  span {
    display: inline-block;
    padding: 3px 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(6px);
  }
}

.row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  max-width: 80%;
}
.row.mine {
  align-self: flex-end;
  flex-direction: row;
  justify-content: flex-end;
}
.row:not(.mine) { align-self: flex-start; }

.bubble-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.8);
  &.peer { background: linear-gradient(135deg, #06b6d4, #6366f1); }
  &.mine { background: var(--brand-grad); }
  img { width: 100%; height: 100%; object-fit: cover; }
}

.bubble {
  position: relative;
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(99, 102, 241, 0.1);
  box-shadow: var(--shadow-xs);

  /* 对方气泡:左下圆角小一点形成"指针" */
  border-bottom-left-radius: 4px;
}
.row.mine .bubble {
  background: var(--brand-grad);
  color: #fff;
  border: none;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 4px;
  box-shadow: var(--shadow-brand);
}
.text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.55;
  font-size: 14px;
}
.time {
  margin-top: 4px;
  font-size: 10px;
  opacity: 0.6;
  text-align: right;
}

/* 输入区 */
.composer {
  display: flex;
  gap: 10px;
  padding: 16px 22px;
  background: rgba(255, 255, 255, 0.6);
  border-top: 1px solid rgba(99, 102, 241, 0.08);

  textarea {
    flex: 1;
    border: 1px solid rgba(99, 102, 241, 0.18);
    background: #fff;
    border-radius: 14px;
    padding: 10px 14px;
    font-family: inherit;
    font-size: 14px;
    line-height: 1.55;
    resize: none;
    outline: none;
    transition: border 0.2s, box-shadow 0.2s;
    &:focus {
      border-color: var(--brand-1);
      box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.12);
    }
  }
}
.send {
  border: none;
  background: var(--brand-grad);
  color: #fff;
  padding: 0 22px;
  border-radius: 14px;
  font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  box-shadow: var(--shadow-brand);
  align-self: stretch;
  min-width: 80px;
  &:hover:not(:disabled) { transform: translateY(-1px); filter: brightness(1.06); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
</style>
