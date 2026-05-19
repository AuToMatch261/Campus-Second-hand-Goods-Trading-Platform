<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { orderApi, type OrderVO, type OrderStatusCode } from '@/api/order'

const router = useRouter()
const tab = ref<'buyer' | 'seller'>('buyer')

const list = ref<OrderVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

function statusInfo(code: OrderStatusCode) {
  if (code === 1) return { cls: 'pending', label: '进行中' }
  if (code === 2) return { cls: 'done', label: '已完成' }
  return { cls: 'cancel', label: '已取消' }
}

const stats = computed(() => {
  const pending = list.value.filter((o) => o.status === 1).length
  const done = list.value.filter((o) => o.status === 2).length
  const cancel = list.value.filter((o) => o.status === 3).length
  return { pending, done, cancel }
})

async function load() {
  loading.value = true
  try {
    const r =
      tab.value === 'buyer'
        ? await orderApi.mineAsBuyer(page.value, size.value)
        : await orderApi.mineAsSeller(page.value, size.value)
    list.value = r.records
    total.value = r.total
  } finally {
    loading.value = false
  }
}

async function confirm(id: number) {
  await ElMessageBox.confirm('确认已收到货并完成本次交易?', '完成订单', { type: 'warning' })
  await orderApi.confirm(id)
  ElMessage.success('订单已完成')
  load()
}

async function cancel(id: number) {
  await ElMessageBox.confirm('确认取消该订单?商品会重新上架', '取消订单', { type: 'warning' })
  await orderApi.cancel(id)
  ElMessage.success('订单已取消')
  load()
}

watch(tab, () => {
  page.value = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="page orders">
    <AppHeader />
    <main class="page-main" v-loading="loading">
      <header class="head">
        <div>
          <span class="head-tag">MY · ORDERS</span>
          <h1>我的订单</h1>
          <p>共 <strong>{{ total }}</strong> 笔 · 进行中 <strong>{{ stats.pending }}</strong> · 已完成 <strong>{{ stats.done }}</strong> · 已取消 <strong>{{ stats.cancel }}</strong></p>
        </div>
        <div class="tabs">
          <button class="tab" :class="{ active: tab === 'buyer' }" @click="tab = 'buyer'">
            🛒 我买的
          </button>
          <button class="tab" :class="{ active: tab === 'seller' }" @click="tab = 'seller'">
            💰 我卖的
          </button>
        </div>
      </header>

      <el-empty v-if="!loading && list.length === 0" description="暂无订单" />

      <div v-else class="list">
        <div v-for="o in list" :key="o.id" class="row glass">
          <div class="thumb"
            @click="router.push({ name: 'product-detail', params: { id: o.productId } })">
            <img v-if="o.productImage" :src="o.productImage" />
            <div v-else class="empty">📦</div>
          </div>

          <div class="info">
            <div class="row-top">
              <span
                class="title link"
                @click="router.push({ name: 'product-detail', params: { id: o.productId } })"
              >
                {{ o.productTitle }}
              </span>
              <span class="status" :class="statusInfo(o.status).cls">
                ● {{ statusInfo(o.status).label }}
              </span>
            </div>
            <div class="row-mid">
              <span>订单 #{{ o.id }}</span>
              <span class="dot">·</span>
              <span>{{ tab === 'buyer' ? '卖家' : '买家' }}: 用户#{{ tab === 'buyer' ? o.sellerId : o.buyerId }}</span>
              <span class="dot">·</span>
              <span>📅 {{ o.createdAt?.replace('T', ' ').slice(0, 16) }}</span>
            </div>
            <div v-if="o.remark" class="row-remark">
              <span>💬 留言: {{ o.remark }}</span>
            </div>
            <div class="row-bot">
              <span class="price">
                <span class="sym">¥</span><span class="num">{{ o.price }}</span>
              </span>
            </div>
          </div>

          <div class="ops">
            <button
              v-if="o.status === 1 && tab === 'buyer'"
              class="op success"
              @click="confirm(o.id)"
            >确认完成</button>
            <button
              v-if="o.status === 1"
              class="op danger"
              @click="cancel(o.id)"
            >取消</button>
            <button
              class="op ghost"
              @click="router.push({ name: 'order-detail', params: { id: o.id } })"
            >详情</button>
          </div>
        </div>
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="(p: number) => { page = p; load() }"
        />
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
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

.tabs {
  display: inline-flex;
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
  transition: all 0.2s var(--ease-out);
  &:hover { color: var(--brand-1); }
  &.active {
    background: var(--brand-grad);
    color: #fff;
    box-shadow: var(--shadow-brand);
  }
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px;
  transition: transform 0.25s var(--ease-out), box-shadow 0.25s var(--ease-out);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}
.thumb {
  width: 96px;
  height: 96px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #eef0fb, #e0e7ff);
  flex-shrink: 0;
  cursor: pointer;
  display: grid;
  place-items: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .empty {
    color: var(--text-muted);
    font-size: 28px;
  }
}

.info {
  flex: 1;
  min-width: 0;
}
.row-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.link { cursor: pointer; &:hover { color: var(--brand-1); } }
.status {
  flex-shrink: 0;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  &.pending { background: linear-gradient(135deg, #f59e0b, #f97316); }
  &.done { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.cancel { background: linear-gradient(135deg, #6b7280, #4b5563); }
}
.row-mid {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 4px;
}
.row-remark {
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(99, 102, 241, 0.05);
  padding: 6px 10px;
  border-radius: 8px;
  margin: 4px 0 6px;
}
.dot { color: rgba(0, 0, 0, 0.2); }
.row-bot .price {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
  display: inline-flex;
  align-items: baseline;
  .sym { font-size: 13px; }
  .num { font-size: 20px; }
}

.ops {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  min-width: 90px;
}
.op {
  border: none;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: filter 0.2s, transform 0.2s var(--ease-out);
  color: #fff;
  white-space: nowrap;

  &.success { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.danger { background: linear-gradient(135deg, #ef4444, #ec4899); }
  &.ghost {
    background: rgba(99, 102, 241, 0.08);
    color: var(--brand-1);
    border: 1px solid rgba(99, 102, 241, 0.18);
  }
  &:hover { transform: translateY(-1px); filter: brightness(1.06); }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .row { flex-wrap: wrap; }
  .ops { flex-direction: row; width: 100%; }
  .op { flex: 1; }
}
</style>
