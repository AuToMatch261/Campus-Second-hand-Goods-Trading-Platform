<script setup lang="ts">
import { computed } from 'vue'
import type { ProductVO } from '@/api/product'

const props = defineProps<{
  product: ProductVO
}>()

defineEmits<{
  (e: 'click', id: number): void
}>()

const cover = computed(
  () => props.product.images?.[0] || 'https://placehold.co/600x400/eef0fb/94a3b8?text=No+Image',
)

const statusBadge = computed(() => {
  switch (props.product.status) {
    case 1:
      return { cls: 'on', label: props.product.statusLabel || '在售' }
    case 2:
      return { cls: 'sold', label: props.product.statusLabel || '已售' }
    case 3:
      return { cls: 'off', label: props.product.statusLabel || '已下架' }
    default:
      return { cls: 'off', label: props.product.statusLabel || '-' }
  }
})

const date = computed(() => props.product.createdAt?.slice(5, 10) || '')
</script>

<template>
  <article class="card" @click="$emit('click', product.id)">
    <div class="cover">
      <img :src="cover" :alt="product.title" loading="lazy" />
      <span class="badge" :class="statusBadge.cls">{{ statusBadge.label }}</span>
      <span class="cat">{{ product.categoryLabel }}</span>
    </div>
    <div class="body">
      <h3 class="title" :title="product.title">{{ product.title }}</h3>
      <div class="row">
        <div class="price">
          <span class="sym">¥</span>
          <span class="num">{{ product.price }}</span>
        </div>
        <div class="meta">
          <span class="dot">👁 {{ product.viewCount }}</span>
          <span class="dot" v-if="date">{{ date }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped lang="scss">
.card {
  position: relative;
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--r-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s var(--ease-out), box-shadow 0.3s var(--ease-out);
  box-shadow: var(--shadow-sm);

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.7), rgba(99, 102, 241, 0.1));
    -webkit-mask:
      linear-gradient(#fff 0 0) content-box,
      linear-gradient(#fff 0 0);
    mask:
      linear-gradient(#fff 0 0) content-box,
      linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-6px);
    box-shadow: var(--shadow-lg);

    .cover img {
      transform: scale(1.06);
    }
  }
}

.cover {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, #eef0fb, #e0e7ff);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s var(--ease-out);
  }
}

.badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  letter-spacing: 0.5px;
  box-shadow: 0 6px 14px -4px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(6px);

  &.on {
    background: linear-gradient(135deg, #10b981, #06b6d4);
  }
  &.sold {
    background: linear-gradient(135deg, #6b7280, #4b5563);
  }
  &.off {
    background: linear-gradient(135deg, #f59e0b, #f97316);
  }
}

.cat {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 999px;
  box-shadow: 0 4px 10px -2px rgba(0, 0, 0, 0.1);
}

.body {
  padding: 14px 16px 16px;
}

.title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

.price {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
  line-height: 1;
  display: flex;
  align-items: baseline;
  gap: 1px;

  .sym {
    font-size: 14px;
  }
  .num {
    font-size: 22px;
  }
}

.meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: var(--text-muted);
}

.dot {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
</style>
