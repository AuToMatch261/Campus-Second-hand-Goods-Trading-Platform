<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="nf">
    <div class="blob b-1" />
    <div class="blob b-2" />
    <div class="blob b-3" />

    <div class="card glass-strong">
      <div class="big">404</div>
      <h1>这里空空如也</h1>
      <p>你想找的页面已经搬家,或从未存在过。</p>
      <div class="ops">
        <button class="primary" @click="router.replace('/')">回到广场</button>
        <button class="ghost" @click="router.back()">← 返回上一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.nf {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 16px;
  overflow: hidden;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  pointer-events: none;
}
.b-1 { width: 480px; height: 480px; background: #8b5cf6; top: -160px; left: -120px; }
.b-2 { width: 420px; height: 420px; background: #06b6d4; bottom: -160px; right: -120px; }
.b-3 { width: 320px; height: 320px; background: #ec4899; top: 50%; left: 50%; opacity: 0.25; }

.card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  padding: 56px 40px 40px;
  text-align: center;
}
.big {
  font-size: 120px;
  font-weight: 900;
  letter-spacing: -8px;
  line-height: 1;
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 8px;
}
h1 {
  margin: 8px 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
p {
  margin: 0 0 28px;
  color: var(--text-muted);
  font-size: 14px;
}
.ops {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
.primary,
.ghost {
  border: none;
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}
.primary {
  background: var(--brand-grad);
  color: #fff;
  box-shadow: var(--shadow-brand);
  &:hover { transform: translateY(-1px); filter: brightness(1.06); }
}
.ghost {
  background: rgba(99, 102, 241, 0.06);
  color: var(--brand-1);
  border: 1px solid rgba(99, 102, 241, 0.18);
  &:hover { background: rgba(99, 102, 241, 0.12); }
}
</style>
