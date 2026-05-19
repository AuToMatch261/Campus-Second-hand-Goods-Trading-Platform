<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type FormItemRule } from 'element-plus'
import type { RegisterRequest } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()

interface RegisterForm extends RegisterRequest {
  confirmPassword: string
}

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<RegisterForm>({
  username: '',
  password: '',
  nickname: '',
  confirmPassword: '',
})

const validateConfirm: FormItemRule['validator'] = (_rule, value, cb) => {
  if (value !== form.password) cb(new Error('两次输入的密码不一致'))
  else cb()
}

const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '4-20 位字母 / 数字 / 下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  nickname: [{ max: 50, message: '昵称最长 50 字符', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await user.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
    })
    ElMessage.success('注册成功,请登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="blob blob-1" />
    <div class="blob blob-2" />
    <div class="blob blob-3" />

    <div class="shell">
      <aside class="brand">
        <div class="brand-mark">校</div>
        <h1 class="brand-title">加入集市</h1>
        <p class="brand-sub">开始你在校园里的第一笔好交易</p>

        <ul class="brand-feats">
          <li><span>🚀</span><div><strong>10 秒注册</strong><small>不需要邮箱验证,马上开逛</small></div></li>
          <li><span>🛡</span><div><strong>密码加密</strong><small>BCrypt 强哈希,放心存放</small></div></li>
          <li><span>🎁</span><div><strong>新人福利</strong><small>首发免审 + 推荐位曝光</small></div></li>
        </ul>

        <div class="brand-foot">© Campus Market · 2026</div>
      </aside>

      <section class="panel">
        <header class="panel-head">
          <span class="tag">第一次来?</span>
          <h2>创建你的账号</h2>
          <p>注册后立即可以发布商品 / 联系卖家</p>
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
            <el-input v-model="form.username" placeholder="4-20 位字母 / 数字 / 下划线" size="large" />
          </el-form-item>
          <el-form-item prop="nickname" label="昵称(可选)">
            <el-input v-model="form.nickname" placeholder="同学叫你什么?" size="large" />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="form.password"
              placeholder="6-32 位"
              size="large"
              type="password"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword" label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              placeholder="再输一次"
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
            {{ loading ? '注册中…' : '创 建 账 号' }}
          </el-button>
        </el-form>

        <div class="extra">
          已有账号?
          <RouterLink to="/login" class="link">去登录 →</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 复用登录页的视觉语言,但渐变方向不同,让两个页面有"对称呼应感" */
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
  background: #06b6d4;
  top: -180px;
  right: -120px;
  animation: float 18s ease-in-out infinite;
}
.blob-2 {
  width: 460px;
  height: 460px;
  background: #ec4899;
  bottom: -160px;
  left: -120px;
  animation: float 22s ease-in-out infinite reverse;
}
.blob-3 {
  width: 360px;
  height: 360px;
  background: #8b5cf6;
  top: 35%;
  right: 50%;
  opacity: 0.25;
  animation: float 26s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, 30px) scale(1.08); }
}

.shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 960px;
  display: grid;
  grid-template-columns: 1fr 380px; /* 表单在左,品牌在右 — 与登录页镜像 */
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--r-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.brand {
  position: relative;
  padding: 40px 32px;
  background: linear-gradient(135deg, #06b6d4 0%, #6366f1 50%, #ec4899 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  order: 2;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(at 80% 0%, rgba(255, 255, 255, 0.25) 0, transparent 50%),
      radial-gradient(at 0% 100%, rgba(255, 255, 255, 0.18) 0, transparent 50%);
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

.panel {
  padding: 40px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  order: 1;
}
.panel-head {
  margin-bottom: 20px;

  .tag {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 1px;
    color: var(--brand-3);
    background: rgba(6, 182, 212, 0.12);
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
.form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  margin-top: 4px;
}

.extra {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
.link {
  color: var(--brand-1);
  font-weight: 600;
  margin-left: 4px;
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
    order: 1;
  }
  .brand-feats {
    display: none;
  }
  .panel {
    padding: 32px 28px;
    order: 2;
  }
}
</style>
