<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { LoginRequest } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginRequest>({ username: '', password: '' })

const rules: FormRules<LoginRequest> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await user.login(form)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <!-- 装饰光斑 -->
    <div class="blob blob-1" />
    <div class="blob blob-2" />
    <div class="blob blob-3" />

    <div class="shell">
      <!-- 品牌侧 -->
      <aside class="brand">
        <div class="brand-mark">校</div>
        <h1 class="brand-title">校园集市</h1>
        <p class="brand-sub">让二手物品在校园里温柔流转</p>

        <ul class="brand-feats">
          <li><span>🎓</span><div><strong>校内闭环</strong><small>同校交易，更安全更靠谱</small></div></li>
          <li><span>💸</span><div><strong>便宜实在</strong><small>毕业季尾货 / 教材闲置一站搞定</small></div></li>
          <li><span>💬</span><div><strong>一键私信</strong><small>不留电话，先聊后买</small></div></li>
        </ul>

        <div class="brand-foot">© Campus Market · 2026</div>
      </aside>

      <!-- 表单侧 -->
      <section class="panel">
        <header class="panel-head">
          <span class="tag">欢迎回来</span>
          <h2>登录账号</h2>
          <p>使用你的校园账号登录,继续逛集市</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="form"
          @submit.prevent
        >
          <el-form-item prop="username" label="账号">
            <el-input
              v-model="form.username"
              placeholder="学号 / 用户名"
              size="large"
              :prefix-icon="undefined"
            />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="form.password"
              placeholder="密码"
              size="large"
              type="password"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit"
            @click="onSubmit"
          >
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form>

        <div class="extra">
          还没有账号?
          <RouterLink to="/register" class="link">立即注册 →</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-page {
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
  opacity: 0.55;
  z-index: 0;
  pointer-events: none;
}
.blob-1 {
  width: 520px;
  height: 520px;
  background: #8b5cf6;
  top: -180px;
  left: -120px;
  animation: float 18s ease-in-out infinite;
}
.blob-2 {
  width: 460px;
  height: 460px;
  background: #06b6d4;
  bottom: -160px;
  right: -120px;
  animation: float 22s ease-in-out infinite reverse;
}
.blob-3 {
  width: 360px;
  height: 360px;
  background: #ec4899;
  top: 40%;
  left: 50%;
  opacity: 0.25;
  animation: float 26s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, -30px) scale(1.08); }
}

.shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 920px;
  display: grid;
  grid-template-columns: 380px 1fr;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--r-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

/* 品牌侧 */
.brand {
  position: relative;
  padding: 40px 32px;
  background: var(--brand-grad);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(at 20% 0%, rgba(255, 255, 255, 0.25) 0, transparent 50%),
      radial-gradient(at 100% 100%, rgba(255, 255, 255, 0.18) 0, transparent 50%);
    pointer-events: none;
  }
}
.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 28px;
  backdrop-filter: blur(8px);
}
.brand-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
}
.brand-sub {
  margin: 8px 0 32px;
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.6;
}
.brand-feats {
  list-style: none;
  padding: 0;
  margin: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;

  li {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    background: rgba(255, 255, 255, 0.12);
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 14px;
    backdrop-filter: blur(6px);

    span {
      font-size: 22px;
      flex-shrink: 0;
    }
    strong {
      display: block;
      font-size: 14px;
      font-weight: 600;
    }
    small {
      display: block;
      font-size: 12px;
      opacity: 0.78;
      margin-top: 2px;
    }
  }
}
.brand-foot {
  margin-top: 32px;
  font-size: 11px;
  letter-spacing: 1px;
  opacity: 0.6;
  position: relative;
  z-index: 1;
}

/* 表单侧 */
.panel {
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.panel-head {
  margin-bottom: 28px;

  .tag {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 1px;
    color: var(--brand-1);
    background: rgba(99, 102, 241, 0.12);
  }
  h2 {
    margin: 12px 0 6px;
    font-size: 26px;
    font-weight: 700;
    color: var(--text-primary);
  }
  p {
    margin: 0;
    font-size: 13px;
    color: var(--text-muted);
  }
}

.form :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  padding-bottom: 4px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 4px;
}

.extra {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
.link {
  color: var(--brand-1);
  font-weight: 600;
  margin-left: 4px;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.75;
  }
}

@media (max-width: 720px) {
  .shell {
    grid-template-columns: 1fr;
    max-width: 420px;
  }
  .brand {
    padding: 32px 28px;
  }
  .brand-feats {
    display: none;
  }
  .panel {
    padding: 32px 28px;
  }
}
</style>
