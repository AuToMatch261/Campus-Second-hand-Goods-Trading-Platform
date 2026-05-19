<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { productApi, type ProductVO } from '@/api/product'

const router = useRouter()

const list = ref<ProductVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

const stats = computed(() => {
  const onSale = list.value.filter((p) => p.status === 1).length
  const sold = list.value.filter((p) => p.status === 2).length
  const off = list.value.filter((p) => p.status === 3).length
  return { onSale, sold, off }
})

async function load() {
  loading.value = true
  try {
    const r = await productApi.mine(page.value, size.value)
    list.value = r.records
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function statusInfo(code: number) {
  if (code === 1) return { cls: 'on', label: '在售' }
  if (code === 2) return { cls: 'sold', label: '已售' }
  return { cls: 'off', label: '已下架' }
}

async function offShelf(id: number) {
  await productApi.offShelf(id)
  ElMessage.success('已下架')
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该商品?此操作不可恢复', '删除确认', {
    type: 'warning',
  })
  await productApi.remove(id)
  ElMessage.success('已删除')
  load()
}

function onPageChange(p: number) {
  page.value = p
  load()
}

function gotoDetail(id: number) {
  router.push({ name: 'product-detail', params: { id } })
}

onMounted(load)
</script>

<template>
  <div class="page mine">
    <AppHeader />
    <main class="page-main" v-loading="loading">
      <header class="head">
        <div>
          <span class="head-tag">MY · LISTINGS</span>
          <h1>我发布的商品</h1>
          <p>共 <strong>{{ total }}</strong> 件 · 在售 <strong>{{ stats.onSale }}</strong> · 已售 <strong>{{ stats.sold }}</strong> · 下架 <strong>{{ stats.off }}</strong></p>
        </div>
        <button class="cta" @click="router.push({ name: 'publish' })">
          <span>✨</span>发布新商品
        </button>
      </header>

      <el-empty v-if="!loading && list.length === 0" description="还没有发布过商品">
        <el-button type="primary" @click="router.push({ name: 'publish' })">去发布</el-button>
      </el-empty>

      <div v-else class="list">
        <div v-for="p in list" :key="p.id" class="row glass">
          <div class="thumb" @click="gotoDetail(p.id)">
            <img v-if="p.images?.[0]" :src="p.images[0]" :alt="p.title" />
            <div v-else class="empty">📷</div>
          </div>

          <div class="info" @click="gotoDetail(p.id)">
            <div class="row-top">
              <h3 class="title">{{ p.title }}</h3>
              <span class="status" :class="statusInfo(p.status).cls">
                ● {{ statusInfo(p.status).label }}
              </span>
            </div>
            <div class="row-mid">
              <span class="cat">{{ p.categoryLabel }}</span>
              <span class="dot">·</span>
              <span>👁 {{ p.viewCount }}</span>
              <span class="dot">·</span>
              <span>📅 {{ p.createdAt?.slice(0, 10) }}</span>
            </div>
            <div class="row-bot">
              <span class="price">
                <span class="sym">¥</span><span class="num">{{ p.price }}</span>
              </span>
            </div>
          </div>

          <div class="ops" @click.stop>
            <button
              v-if="p.status === 1"
              class="op warn"
              @click="offShelf(p.id)"
            >下架</button>
            <button class="op danger" @click="remove(p.id)">删除</button>
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
          @current-change="onPageChange"
        />
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;

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
    color: var(--text-primary);
  }
  p {
    margin: 0;
    color: var(--text-muted);
    font-size: 13px;
    strong { color: var(--brand-1); margin: 0 2px; }
  }
}
.cta {
  border: none;
  background: var(--brand-grad);
  color: #fff;
  padding: 12px 22px;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: var(--shadow-brand);
  transition: transform 0.2s var(--ease-out), filter 0.2s var(--ease-out);
  flex-shrink: 0;
  &:hover { transform: translateY(-1px); filter: brightness(1.06); }
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
  cursor: default;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}
.thumb {
  width: 88px;
  height: 88px;
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
    font-size: 24px;
  }
}

.info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.row-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
  &:hover { color: var(--brand-1); }
}
.status {
  flex-shrink: 0;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  &.on { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.sold { background: linear-gradient(135deg, #6b7280, #4b5563); }
  &.off { background: linear-gradient(135deg, #f59e0b, #f97316); }
}
.row-mid {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 6px;
}
.cat {
  background: rgba(99, 102, 241, 0.10);
  color: var(--brand-1);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 500;
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

  &.warn { background: linear-gradient(135deg, #f59e0b, #f97316); }
  &.danger { background: linear-gradient(135deg, #ef4444, #ec4899); }
  &:hover { transform: translateY(-1px); filter: brightness(1.06); }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .head { flex-direction: column; align-items: flex-start; }
  .row { flex-wrap: wrap; }
  .ops { flex-direction: row; width: 100%; }
  .op { flex: 1; }
}
</style>
