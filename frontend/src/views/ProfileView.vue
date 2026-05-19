<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import type { UpdateProfileRequest } from '@/api/user'
import { useUserStore } from '@/stores/user'

const user = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const form = reactive<UpdateProfileRequest>({
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
})

const rules: FormRules<UpdateProfileRequest> = {
  nickname: [{ max: 50, message: '昵称最长 50 字符', trigger: 'blur' }],
  phone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function syncFromStore() {
  const p = user.profile
  form.nickname = p?.nickname ?? ''
  form.avatar = p?.avatar ?? ''
  form.phone = p?.phone ?? ''
  form.email = p?.email ?? ''
}

onMounted(async () => {
  loading.value = true
  try {
    await user.refreshProfile()
    syncFromStore()
  } finally {
    loading.value = false
  }
})

watch(() => user.profile?.id, syncFromStore)

const initials = computed(() => {
  const n = user.profile?.nickname || user.profile?.username || '?'
  return n.slice(0, 1).toUpperCase()
})

const avatarPreview = computed(() => form.avatar || user.profile?.avatar || '')

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const patch: UpdateProfileRequest = {
      nickname: form.nickname || undefined,
      avatar: form.avatar || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
    }
    await user.updateProfile(patch)
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page profile">
    <AppHeader />
    <main class="page-main" v-loading="loading">
      <!-- 个人信息卡 (顶部 cover) -->
      <section class="hero glass-strong">
        <div class="cover">
          <div class="cover-blob b-1" />
          <div class="cover-blob b-2" />
        </div>
        <div class="hero-body">
          <div class="avatar">
            <img v-if="avatarPreview" :src="avatarPreview" :alt="initials" />
            <span v-else>{{ initials }}</span>
          </div>
          <div class="meta">
            <h1>{{ user.profile?.nickname || user.profile?.username }}</h1>
            <p>@{{ user.profile?.username }}</p>
            <div class="chips">
              <span v-if="user.profile?.phone" class="chip">📞 {{ user.profile.phone }}</span>
              <span v-if="user.profile?.email" class="chip">✉ {{ user.profile.email }}</span>
              <span v-if="!user.profile?.phone && !user.profile?.email" class="chip muted">
                填写联系方式,买卖更方便
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- 编辑表单 -->
      <section class="edit glass-strong">
        <header class="edit-head">
          <h2>编辑资料</h2>
          <p>更新昵称、头像和联系方式</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="form"
        >
          <div class="grid">
            <el-form-item label="账号">
              <el-input :value="user.profile?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="同学叫你什么?" />
            </el-form-item>
            <el-form-item label="头像 URL" prop="avatar" class="span-2">
              <el-input v-model="form.avatar" placeholder="https://..." />
            </el-form-item>
            <el-form-item label="手机" prop="phone">
              <el-input v-model="form.phone" placeholder="11 位手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@example.com" />
            </el-form-item>
          </div>

          <div class="actions">
            <el-button type="primary" size="large" :loading="saving" @click="onSubmit">
              {{ saving ? '保存中…' : '保 存 修 改' }}
            </el-button>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
/* Hero */
.hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 20px;
  padding: 0;
}
.cover {
  height: 140px;
  background: var(--brand-grad);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(at 20% 20%, rgba(255, 255, 255, 0.3) 0, transparent 50%),
      radial-gradient(at 80% 80%, rgba(255, 255, 255, 0.18) 0, transparent 50%);
  }
}
.cover-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.5;
}
.b-1 {
  width: 200px;
  height: 200px;
  background: #ec4899;
  top: -60px;
  right: 20%;
}
.b-2 {
  width: 160px;
  height: 160px;
  background: #22d3ee;
  bottom: -60px;
  left: 30%;
}

.hero-body {
  display: flex;
  align-items: flex-end;
  gap: 22px;
  padding: 0 32px 26px;
  margin-top: -42px;
  position: relative;
  z-index: 1;
}
.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 38px;
  font-weight: 700;
  border: 4px solid rgba(255, 255, 255, 0.95);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.meta {
  flex: 1;
  padding-bottom: 6px;

  h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
  }
  p {
    margin: 4px 0 10px;
    font-size: 13px;
    color: var(--text-muted);
  }
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 4px 12px;
  font-size: 12px;
  background: rgba(99, 102, 241, 0.10);
  color: var(--brand-1);
  border-radius: 999px;
  font-weight: 500;
  &.muted {
    background: rgba(245, 158, 11, 0.10);
    color: #b45309;
  }
}

/* 编辑卡 */
.edit {
  padding: 28px 32px 32px;
}
.edit-head {
  margin-bottom: 18px;
  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
  }
  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--text-muted);
  }
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 22px;
}
.span-2 {
  grid-column: 1 / -1;
}
.actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .hero-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
    margin-top: -52px;
  }
  .grid { grid-template-columns: 1fr; }
}
</style>
