<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useUserStore } from '@/stores/user'
import {
  productApi,
  PRODUCT_CATEGORIES,
  type ProductCategoryCode,
  type ProductSortBy,
  type ProductSortOrder,
  type ProductVO,
} from '@/api/product'

const router = useRouter()
const user = useUserStore()

interface Filters {
  category: ProductCategoryCode | null
  keyword: string
  sortBy: ProductSortBy
  sortOrder: ProductSortOrder
}

const filters = reactive<Filters>({
  category: null,
  keyword: '',
  sortBy: 'createdAt',
  sortOrder: 'desc',
})

const SORT_OPTIONS = [
  { key: 'createdAt:desc', label: '最新', sortBy: 'createdAt' as ProductSortBy, sortOrder: 'desc' as ProductSortOrder },
  { key: 'price:asc', label: '低价优先', sortBy: 'price' as ProductSortBy, sortOrder: 'asc' as ProductSortOrder },
  { key: 'price:desc', label: '高价优先', sortBy: 'price' as ProductSortBy, sortOrder: 'desc' as ProductSortOrder },
  { key: 'viewCount:desc', label: '人气', sortBy: 'viewCount' as ProductSortBy, sortOrder: 'desc' as ProductSortOrder },
]
const sortKey = ref<string>('createdAt:desc')

const CATEGORY_ICONS: Record<number, string> = {
  1: '📚',
  2: '💻',
  3: '🧴',
  4: '👕',
  5: '⚽',
  6: '✨',
}

const page = ref(1)
const size = ref(12)
const total = ref(0)
const list = ref<ProductVO[]>([])
const loading = ref(false)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const niceName = computed(
  () => user.profile?.nickname || user.profile?.username || '同学',
)

function applySort() {
  const opt = SORT_OPTIONS.find((o) => o.key === sortKey.value)
  if (!opt) return
  filters.sortBy = opt.sortBy
  filters.sortOrder = opt.sortOrder
}

async function load() {
  applySort()
  loading.value = true
  try {
    const r = await productApi.list({
      page: page.value,
      size: size.value,
      category: filters.category ?? undefined,
      keyword: filters.keyword.trim() || undefined,
      sortBy: filters.sortBy,
      sortOrder: filters.sortOrder,
    })
    list.value = r.records
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onPageChange(p: number) {
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function pickCategory(c: ProductCategoryCode | null) {
  filters.category = c
  onSearch()
}

function pickSort(k: string) {
  sortKey.value = k
  onSearch()
}

function openDetail(id: number) {
  router.push({ name: 'product-detail', params: { id } })
}

onMounted(load)
</script>

<template>
  <div class="page market">
    <AppHeader />

    <main class="page-main">
      <!-- Hero —— -->
      <section class="hero">
        <div class="hero-text">
          <span class="hero-tag">CAMPUS · MARKET</span>
          <h1>
            {{ greeting }},<span class="hero-name">{{ niceName }}</span>
            <br />今天想淘点啥?
          </h1>
          <p>毕业季尾货 · 教材闲置 · 数码周边,同校交易,放心便宜。</p>

          <div class="hero-search">
            <span class="hero-search-icon">🔍</span>
            <input
              v-model="filters.keyword"
              placeholder="搜索商品标题、关键词…"
              @keyup.enter="onSearch"
            />
            <button class="hero-search-btn" @click="onSearch">搜索</button>
          </div>
        </div>

        <div class="hero-cards" aria-hidden="true">
          <div class="ph ph-1">
            <span>📚</span>
            <strong>{{ total > 0 ? total : '∞' }}</strong>
            <small>件商品在售</small>
          </div>
          <div class="ph ph-2">
            <span>🤝</span>
            <strong>同校</strong>
            <small>面对面更安心</small>
          </div>
          <div class="ph ph-3">
            <span>⚡</span>
            <strong>秒发</strong>
            <small>1 分钟上架</small>
          </div>
        </div>
      </section>

      <!-- 分类胶囊 —— -->
      <section class="cats">
        <button
          class="cat-pill"
          :class="{ active: filters.category === null }"
          @click="pickCategory(null)"
        >
          <span>🌐</span>全部
        </button>
        <button
          v-for="c in PRODUCT_CATEGORIES"
          :key="c.value"
          class="cat-pill"
          :class="{ active: filters.category === c.value }"
          @click="pickCategory(c.value)"
        >
          <span>{{ CATEGORY_ICONS[c.value] }}</span>{{ c.label }}
        </button>
      </section>

      <!-- 排序 / 计数 —— -->
      <section class="toolbar">
        <div class="left">
          <strong>{{ total }}</strong>
          <span>件商品</span>
        </div>
        <div class="sorts">
          <button
            v-for="o in SORT_OPTIONS"
            :key="o.key"
            class="sort"
            :class="{ active: sortKey === o.key }"
            @click="pickSort(o.key)"
          >
            {{ o.label }}
          </button>
        </div>
      </section>

      <!-- 商品网格 —— -->
      <section v-loading="loading" class="grid-wrap">
        <el-empty v-if="!loading && list.length === 0" description="还没有匹配的商品" />
        <div v-else class="grid">
          <ProductCard
            v-for="p in list"
            :key="p.id"
            :product="p"
            @click="openDetail"
          />
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
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
/* ============ Hero ============ */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--r-xl);
  padding: 40px 44px;
  margin-bottom: 28px;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 32px;
  align-items: center;
  box-shadow: var(--shadow-lg);

  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
    opacity: 0.55;
    pointer-events: none;
  }
  &::before {
    width: 360px;
    height: 360px;
    background: #ec4899;
    top: -120px;
    right: -80px;
  }
  &::after {
    width: 280px;
    height: 280px;
    background: #22d3ee;
    bottom: -120px;
    left: 30%;
  }
}

.hero-text {
  position: relative;
  z-index: 1;
}
.hero-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.28);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2px;
  backdrop-filter: blur(6px);
}
.hero-text h1 {
  margin: 14px 0 12px;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.5px;
}
.hero-name {
  background: linear-gradient(90deg, #fff 0%, #fde68a 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0 4px;
}
.hero-text p {
  margin: 0 0 24px;
  opacity: 0.85;
  font-size: 14px;
  line-height: 1.7;
}

.hero-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 6px 6px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  box-shadow: 0 16px 40px -16px rgba(0, 0, 0, 0.3);
  max-width: 460px;

  input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: var(--text-primary);
    padding: 10px 8px;
    &::placeholder {
      color: var(--text-muted);
    }
  }
}
.hero-search-icon {
  font-size: 16px;
  color: var(--text-muted);
}
.hero-search-btn {
  border: none;
  background: var(--brand-grad);
  color: #fff;
  font-weight: 600;
  letter-spacing: 2px;
  padding: 10px 22px;
  border-radius: 999px;
  cursor: pointer;
  transition: filter 0.2s, transform 0.2s var(--ease-out);
  &:hover {
    filter: brightness(1.08);
    transform: translateY(-1px);
  }
}

.hero-cards {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 12px;
  height: 220px;
}
.ph {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;

  span {
    font-size: 22px;
    line-height: 1;
  }
  strong {
    font-size: 22px;
    font-weight: 700;
    line-height: 1;
    margin-top: 6px;
  }
  small {
    font-size: 12px;
    opacity: 0.8;
  }
}
.ph-1 {
  grid-row: span 2;
  background: rgba(255, 255, 255, 0.28);
}

/* ============ Categories ============ */
.cats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}
.cat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  span {
    font-size: 14px;
  }
  &:hover {
    border-color: rgba(99, 102, 241, 0.4);
    color: var(--brand-1);
    transform: translateY(-1px);
  }
  &.active {
    background: var(--brand-grad);
    color: #fff;
    border-color: transparent;
    box-shadow: var(--shadow-brand);
  }
}

/* ============ Toolbar ============ */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 4px;

  .left {
    color: var(--text-secondary);
    font-size: 13px;
    strong {
      font-size: 18px;
      font-weight: 700;
      color: var(--brand-1);
      margin-right: 4px;
    }
  }
}
.sorts {
  display: inline-flex;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 999px;
  padding: 4px;
  gap: 2px;
  backdrop-filter: blur(10px);
}
.sort {
  border: none;
  background: transparent;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  &:hover {
    color: var(--brand-1);
  }
  &.active {
    background: var(--brand-grad);
    color: #fff;
    box-shadow: var(--shadow-brand);
  }
}

/* ============ Grid ============ */
.grid-wrap {
  min-height: 320px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 880px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 28px 24px;
  }
  .hero-cards {
    display: none;
  }
  .hero-text h1 {
    font-size: 24px;
  }
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
