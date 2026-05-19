<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { orderApi, type OrderVO } from '@/api/order'
import { userApi, type Profile } from '@/api/user'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const order = ref<OrderVO | null>(null)
const buyer = ref<Profile | null>(null)
const seller = ref<Profile | null>(null)
const loading = ref(false)

const orderId = computed(() => Number(route.params.id))
const isBuyer = computed(() => !!user.profile && order.value?.buyerId === user.profile.id)

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.detail(orderId.value)
    const [b, s] = await Promise.all([
      userApi.getById(order.value.buyerId).catch(() => null),
      userApi.getById(order.value.sellerId).catch(() => null),
    ])
    buyer.value = b
    seller.value = s
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function confirm() {
  if (!order.value) return
  await ElMessageBox.confirm('确认已收到货并完成本次交易?', '完成订单', { type: 'warning' })
  await orderApi.confirm(order.value.id)
  ElMessage.success('订单已完成')
  load()
}

async function cancel() {
  if (!order.value) return
  await ElMessageBox.confirm('确认取消该订单?商品会重新上架', '取消订单', { type: 'warning' })
  await orderApi.cancel(order.value.id)
  ElMessage.success('订单已取消')
  load()
}

watch(orderId, load)
onMounted(load)

const statusInfo = computed(() => {
  if (!order.value) return { cls: 'pending', label: '' }
  const c = order.value.status
  if (c === 1) return { cls: 'pending', label: order.value.statusLabel || '进行中' }
  if (c === 2) return { cls: 'done', label: order.value.statusLabel || '已完成' }
  return { cls: 'cancel', label: order.value.statusLabel || '已取消' }
})

const buyerName = computed(
  () => buyer.value?.nickname || buyer.value?.username || `用户#${order.value?.buyerId ?? ''}`,
)
const sellerName = computed(
  () => seller.value?.nickname || seller.value?.username || `用户#${order.value?.sellerId ?? ''}`,
)

/* 订单时间线 */
const timeline = computed(() => {
  if (!order.value) return []
  const items = [
    { label: '下单', time: order.value.createdAt, dotCls: 'on' },
  ]
  if (order.value.completedAt) {
    items.push({ label: '完成', time: order.value.completedAt, dotCls: 'done' })
  }
  if (order.value.cancelledAt) {
    items.push({ label: '取消', time: order.value.cancelledAt, dotCls: 'cancel' })
  }
  return items
})

function fmt(s?: string) {
  return s?.replace('T', ' ').slice(0, 19) || '-'
}
</script>

<template>
  <div class="page detail">
    <AppHeader />
    <main class="page-main" v-loading="loading">
      <el-empty v-if="!loading && !order" description="订单不存在或无权访问" />

      <template v-else-if="order">
        <header class="head">
          <div>
            <span class="head-tag">ORDER · #{{ order.id }}</span>
            <h1>订单详情</h1>
            <p>下单于 {{ fmt(order.createdAt) }}</p>
          </div>
          <span class="status-big" :class="statusInfo.cls">
            ● {{ statusInfo.label }}
          </span>
        </header>

        <div class="grid">
          <!-- 主卡 -->
          <section class="main glass-strong">
            <!-- 商品概览 -->
            <div
              class="product-row"
              @click="router.push({ name: 'product-detail', params: { id: order.productId } })"
            >
              <div class="thumb">
                <span class="empty">📦</span>
              </div>
              <div class="p-info">
                <div class="p-title">{{ order.productTitle }}</div>
                <div class="p-meta">商品 #{{ order.productId }} · 点击查看详情 →</div>
              </div>
              <div class="p-price">
                <span class="sym">¥</span><span class="num">{{ order.price }}</span>
              </div>
            </div>

            <!-- 双方 -->
            <div class="two-cols">
              <div class="col">
                <div class="col-label">买家</div>
                <div class="col-value">{{ buyerName }}</div>
                <div v-if="buyer?.phone" class="col-extra">📞 {{ buyer.phone }}</div>
              </div>
              <div class="vs">⇄</div>
              <div class="col">
                <div class="col-label">卖家</div>
                <div class="col-value">{{ sellerName }}</div>
                <div v-if="seller?.phone" class="col-extra">📞 {{ seller.phone }}</div>
              </div>
            </div>

            <!-- 留言 -->
            <div v-if="order.remark" class="remark">
              <div class="r-label">买家留言</div>
              <div class="r-text">{{ order.remark }}</div>
            </div>

            <!-- 操作 -->
            <div class="actions">
              <button
                v-if="order.status === 1 && isBuyer"
                class="btn success"
                @click="confirm"
              >✅ 确认完成</button>
              <button
                v-if="order.status === 1"
                class="btn danger"
                @click="cancel"
              >✖ 取消订单</button>
              <button class="btn ghost" @click="router.back()">← 返回</button>
            </div>
          </section>

          <!-- 时间线 -->
          <aside class="side glass">
            <h3>状态进度</h3>
            <ul class="timeline">
              <li v-for="(t, i) in timeline" :key="i">
                <span class="dot" :class="t.dotCls" />
                <div>
                  <strong>{{ t.label }}</strong>
                  <small>{{ fmt(t.time) }}</small>
                </div>
              </li>
            </ul>

            <div class="hint">
              💡 同校交易,建议线下面交并验货后再确认完成。
            </div>
          </aside>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped lang="scss">
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  gap: 16px;
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
  }
}
.status-big {
  padding: 6px 18px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  &.pending { background: linear-gradient(135deg, #f59e0b, #f97316); }
  &.done { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.cancel { background: linear-gradient(135deg, #6b7280, #4b5563); }
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
  gap: 20px;
  align-items: start;
}

/* 主卡 */
.main {
  padding: 28px 32px;
}
.product-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(99, 102, 241, 0.05);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: background 0.2s;
  &:hover { background: rgba(99, 102, 241, 0.10); }
}
.thumb {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eef0fb, #e0e7ff);
  display: grid;
  place-items: center;
  flex-shrink: 0;
  .empty { font-size: 24px; }
}
.p-info { flex: 1; min-width: 0; }
.p-title {
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
.p-price {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
  display: inline-flex;
  align-items: baseline;
  .sym { font-size: 14px; }
  .num { font-size: 24px; }
}

.two-cols {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 16px;
  align-items: center;
  margin: 24px 0;
  padding: 18px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--r-md);
  border: 1px dashed rgba(99, 102, 241, 0.15);
}
.col-label {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 6px;
}
.col-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.col-extra {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
.vs {
  font-size: 22px;
  color: var(--brand-1);
  text-align: center;
}

.remark {
  margin: 0 0 24px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(236, 72, 153, 0.06));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: var(--r-md);

  .r-label {
    font-size: 11px;
    font-weight: 600;
    color: #b45309;
    letter-spacing: 1px;
    margin-bottom: 6px;
  }
  .r-text {
    color: var(--text-primary);
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.btn {
  border: none;
  padding: 10px 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  color: #fff;

  &.success { background: linear-gradient(135deg, #10b981, #06b6d4); box-shadow: 0 12px 24px -10px rgba(16, 185, 129, 0.55); }
  &.danger { background: linear-gradient(135deg, #ef4444, #ec4899); box-shadow: 0 12px 24px -10px rgba(239, 68, 68, 0.55); }
  &.ghost {
    background: rgba(99, 102, 241, 0.06);
    color: var(--brand-1);
    border: 1px solid rgba(99, 102, 241, 0.18);
  }
  &:hover { transform: translateY(-1px); filter: brightness(1.06); }
}

/* 侧栏:时间线 */
.side {
  padding: 22px 24px;
  position: sticky;
  top: 96px;

  h3 {
    margin: 0 0 16px;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 1px;
    &::before {
      content: '';
      display: inline-block;
      width: 4px;
      height: 14px;
      vertical-align: -2px;
      margin-right: 8px;
      background: var(--brand-grad);
      border-radius: 2px;
    }
  }
}
.timeline {
  list-style: none;
  padding: 0;
  margin: 0 0 16px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 8px;
    bottom: 8px;
    left: 7px;
    width: 2px;
    background: linear-gradient(180deg, var(--brand-1), transparent);
    opacity: 0.3;
  }

  li {
    display: flex;
    gap: 14px;
    padding: 6px 0 14px;
    position: relative;
  }
  .dot {
    flex-shrink: 0;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    margin-top: 2px;
    background: #fff;
    border: 2px solid var(--brand-1);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);

    &.on { border-color: #6366f1; }
    &.done { border-color: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18); }
    &.cancel { border-color: #6b7280; box-shadow: 0 0 0 3px rgba(107, 114, 128, 0.18); }
  }
  strong {
    display: block;
    font-size: 13px;
    color: var(--text-primary);
    font-weight: 600;
  }
  small {
    display: block;
    font-size: 11px;
    color: var(--text-muted);
    margin-top: 2px;
  }
}
.hint {
  padding: 10px 12px;
  background: rgba(99, 102, 241, 0.06);
  border-radius: 10px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

@media (max-width: 880px) {
  .grid { grid-template-columns: 1fr; }
  .side { position: static; }
  .two-cols { grid-template-columns: 1fr; }
  .vs { display: none; }
}
</style>
