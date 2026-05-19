<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import { productApi, type ProductVO } from '@/api/product'
import { userApi, type Profile } from '@/api/user'
import { orderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const product = ref<ProductVO | null>(null)
const seller = ref<Profile | null>(null)
const loading = ref(false)
const activeImg = ref(0)

const productId = computed(() => Number(route.params.id))

async function load() {
  loading.value = true
  activeImg.value = 0
  try {
    const p = await productApi.detail(productId.value)
    product.value = p
    seller.value = await userApi.getById(p.sellerId).catch(() => null)
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

watch(productId, load)
onMounted(load)

const isMine = computed(
  () => !!user.profile && !!product.value && product.value.sellerId === user.profile.id,
)
const canBuy = computed(
  () => !isMine.value && !!product.value && product.value.status === 1,
)

const sellerName = computed(
  () => seller.value?.nickname || seller.value?.username || `用户#${product.value?.sellerId ?? ''}`,
)
const sellerInitial = computed(() => sellerName.value.slice(0, 1).toUpperCase())

const statusBadge = computed(() => {
  if (!product.value) return { cls: 'on', label: '' }
  switch (product.value.status) {
    case 1: return { cls: 'on', label: product.value.statusLabel || '在售' }
    case 2: return { cls: 'sold', label: product.value.statusLabel || '已售' }
    case 3: return { cls: 'off', label: product.value.statusLabel || '已下架' }
    default: return { cls: 'off', label: product.value.statusLabel || '-' }
  }
})

async function offShelf() {
  if (!product.value) return
  await productApi.offShelf(product.value.id)
  ElMessage.success('已下架')
  load()
}

const buyDialog = reactive({ visible: false, remark: '', submitting: false })

function openBuyDialog() {
  buyDialog.remark = ''
  buyDialog.visible = true
}

async function submitBuy() {
  if (!product.value) return
  buyDialog.submitting = true
  try {
    const order = await orderApi.create({
      productId: product.value.id,
      price: product.value.price,
      remark: buyDialog.remark || undefined,
    })
    buyDialog.visible = false
    ElMessage.success('下单成功')
    router.push({ name: 'order-detail', params: { id: order.id } })
  } finally {
    buyDialog.submitting = false
  }
}
</script>

<template>
  <div class="page detail">
    <AppHeader />

    <main class="page-main" v-loading="loading">
      <el-empty v-if="!loading && !product" description="商品不存在或已删除" />

      <div v-else-if="product" class="layout">
        <!-- 左:大图 + 缩略图 -->
        <section class="gallery glass-strong">
          <div class="big">
            <img
              v-if="product.images?.length"
              :src="product.images[activeImg]"
              :alt="product.title"
            />
            <div v-else class="big empty">📷 暂无图片</div>
          </div>
          <div v-if="(product.images?.length || 0) > 1" class="thumbs">
            <button
              v-for="(img, i) in product.images"
              :key="i"
              class="thumb"
              :class="{ active: activeImg === i }"
              @click="activeImg = i"
            >
              <img :src="img" :alt="`图${i + 1}`" />
            </button>
          </div>
        </section>

        <!-- 右:信息 -->
        <section class="info">
          <div class="info-card glass-strong">
            <div class="tags">
              <span class="badge" :class="statusBadge.cls">● {{ statusBadge.label }}</span>
              <span class="cat-tag">{{ product.categoryLabel }}</span>
              <span class="view-tag">👁 {{ product.viewCount }}</span>
            </div>

            <h1 class="title">{{ product.title }}</h1>

            <div class="price-line">
              <div class="price">
                <span class="sym">¥</span>
                <span class="num">{{ product.price }}</span>
              </div>
              <span class="time">📅 {{ product.createdAt?.slice(0, 10) }}</span>
            </div>

            <div class="actions">
              <button v-if="canBuy" class="btn-primary" @click="openBuyDialog">
                立即购买
              </button>
              <button
                v-else-if="!isMine"
                class="btn-primary"
                disabled
              >
                {{ product.statusLabel }} · 不可下单
              </button>

              <button
                v-if="isMine && product.status === 1"
                class="btn-warn"
                @click="offShelf"
              >
                下架商品
              </button>
              <button
                v-if="isMine"
                class="btn-ghost"
                @click="router.push({ name: 'my-products' })"
              >
                我的商品
              </button>
              <button
                v-if="!isMine"
                class="btn-ghost"
                @click="router.push({ name: 'chat', params: { peerId: product.sellerId } })"
              >
                💬 联系卖家
              </button>
            </div>
          </div>

          <!-- 卖家卡 -->
          <div class="seller-card glass">
            <div class="seller-avatar">
              <img v-if="seller?.avatar" :src="seller.avatar" :alt="sellerName" />
              <span v-else>{{ sellerInitial }}</span>
            </div>
            <div class="seller-info">
              <div class="seller-name">{{ sellerName }}</div>
              <div class="seller-meta">
                <span v-if="seller?.phone">📞 {{ seller.phone }}</span>
                <span v-else>校园同学</span>
              </div>
            </div>
            <button
              v-if="!isMine"
              class="seller-chat"
              @click="router.push({ name: 'chat', params: { peerId: product.sellerId } })"
            >
              发消息
            </button>
          </div>

          <!-- 描述 -->
          <div class="desc-card glass">
            <h3>商品描述</h3>
            <p class="desc">{{ product.description || '（卖家暂未填写描述）' }}</p>
          </div>
        </section>
      </div>

      <el-dialog v-model="buyDialog.visible" title="确认下单" width="440px">
        <div v-if="product" class="buy-summary">
          <div class="bs-thumb">
            <img v-if="product.images?.[0]" :src="product.images[0]" />
          </div>
          <div class="bs-text">
            <div class="bs-title">{{ product.title }}</div>
            <div class="bs-price">¥ {{ product.price }}</div>
          </div>
        </div>
        <el-input
          v-model="buyDialog.remark"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="给卖家的留言(自取地点 / 联系方式等)"
        />
        <template #footer>
          <el-button @click="buyDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="buyDialog.submitting" @click="submitBuy">
            确认下单
          </el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<style scoped lang="scss">
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

/* 左侧图库 */
.gallery {
  padding: 20px;
}
.big {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-md);
  overflow: hidden;
  background: linear-gradient(135deg, #eef0fb, #e0e7ff);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &.empty {
    display: grid;
    place-items: center;
    color: var(--text-muted);
    font-size: 16px;
  }
}
.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  overflow-x: auto;
  padding: 4px 2px;
}
.thumb {
  flex: 0 0 72px;
  height: 72px;
  padding: 0;
  background: transparent;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  &:hover {
    transform: translateY(-2px);
  }
  &.active {
    border-color: var(--brand-1);
    box-shadow: var(--shadow-brand);
  }
}

/* 右侧信息 */
.info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.info-card {
  padding: 28px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.badge {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  &.on { background: linear-gradient(135deg, #10b981, #06b6d4); }
  &.sold { background: linear-gradient(135deg, #6b7280, #4b5563); }
  &.off { background: linear-gradient(135deg, #f59e0b, #f97316); }
}
.cat-tag,
.view-tag {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(99, 102, 241, 0.10);
  color: var(--brand-1);
  border-radius: 999px;
}
.view-tag {
  background: rgba(99, 102, 241, 0.06);
  color: var(--text-muted);
}

.title {
  margin: 0 0 18px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
}

.price-line {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 22px;
  margin-bottom: 22px;
  border-bottom: 1px dashed rgba(99, 102, 241, 0.15);
}
.price {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
  display: inline-flex;
  align-items: baseline;
  line-height: 1;

  .sym { font-size: 18px; }
  .num { font-size: 36px; }
}
.time {
  font-size: 12px;
  color: var(--text-muted);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.btn-primary,
.btn-warn,
.btn-ghost {
  flex: 1;
  min-width: 120px;
  height: 44px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}
.btn-primary {
  background: var(--brand-grad);
  color: #fff;
  box-shadow: var(--shadow-brand);
  &:hover:not(:disabled) {
    transform: translateY(-1px);
    filter: brightness(1.06);
  }
  &:disabled {
    background: #cbd0e0;
    box-shadow: none;
    cursor: not-allowed;
    color: #fff;
  }
}
.btn-warn {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  box-shadow: 0 12px 24px -10px rgba(245, 158, 11, 0.55);
  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.06);
  }
}
.btn-ghost {
  background: rgba(99, 102, 241, 0.06);
  color: var(--brand-1);
  border: 1px solid rgba(99, 102, 241, 0.18);
  &:hover {
    background: rgba(99, 102, 241, 0.12);
  }
}

/* 卖家卡 */
.seller-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
}
.seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-brand);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.seller-info {
  flex: 1;
  min-width: 0;
}
.seller-name {
  font-weight: 600;
  color: var(--text-primary);
}
.seller-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-muted);
}
.seller-chat {
  border: 1px solid rgba(99, 102, 241, 0.25);
  background: rgba(99, 102, 241, 0.08);
  color: var(--brand-1);
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
  &:hover {
    background: var(--brand-grad);
    color: #fff;
    border-color: transparent;
  }
}

/* 描述 */
.desc-card {
  padding: 22px 24px;

  h3 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
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
.desc {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.75;
  color: var(--text-secondary);
  font-size: 14px;
}

/* 下单弹窗内的小预览 */
.buy-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 12px;
  margin-bottom: 14px;
}
.bs-thumb {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  overflow: hidden;
  background: #eef0fb;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.bs-title {
  font-weight: 600;
  font-size: 14px;
}
.bs-price {
  margin-top: 4px;
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: 16px;
  font-weight: 700;
}

@media (max-width: 880px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
