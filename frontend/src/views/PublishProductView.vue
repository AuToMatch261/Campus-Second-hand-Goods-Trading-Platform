<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRawFile } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import {
  productApi,
  PRODUCT_CATEGORIES,
  type ProductCategoryCode,
  type PublishProductRequest,
} from '@/api/product'

const router = useRouter()

interface PublishForm {
  title: string
  description: string
  price: string
  category: ProductCategoryCode | null
}

const MAX_IMAGES = 9
const ACCEPT = 'image/jpeg,image/png,image/gif,image/webp,image/bmp'
const MAX_BYTES = 10 * 1024 * 1024

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<PublishForm>({
  title: '',
  description: '',
  price: '',
  category: null,
})

/** 已上传的 OSS URL 数组,提交时直接发给后端 */
const images = ref<string[]>([])
/** 同步用于 el-upload 的 file-list 渲染 */
const fileList = ref<Array<{ name: string; url: string; status: 'success' }>>([])
const uploading = ref(0)

const CATEGORY_ICONS: Record<number, string> = {
  1: '📚', 2: '💻', 3: '🧴', 4: '👕', 5: '⚽', 6: '✨',
}

const rules: FormRules<PublishForm> = {
  title: [
    { required: true, message: '请填写标题', trigger: 'blur' },
    { max: 100, message: '标题最长 100 字符', trigger: 'blur' },
  ],
  description: [{ max: 2000, message: '描述最长 2000 字符', trigger: 'blur' }],
  price: [
    { required: true, message: '请填写价格', trigger: 'blur' },
    {
      pattern: /^\d{1,8}(\.\d{1,2})?$/,
      message: '价格最多 8 位整数 2 位小数',
      trigger: 'blur',
    },
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

function beforeUpload(file: UploadRawFile): boolean {
  if (!ACCEPT.split(',').includes(file.type)) {
    ElMessage.warning('仅支持 JPG / PNG / GIF / WEBP / BMP')
    return false
  }
  if (file.size > MAX_BYTES) {
    ElMessage.warning('单张图片不能超过 10 MB')
    return false
  }
  if (images.value.length >= MAX_IMAGES) {
    ElMessage.warning(`最多上传 ${MAX_IMAGES} 张`)
    return false
  }
  return true
}

/** 接管 el-upload 的上传逻辑:走我们封装好的 productApi(自动带 token) */
async function customUpload(options: { file: File }) {
  uploading.value++
  try {
    const { url } = await productApi.uploadImage(options.file)
    images.value.push(url)
    fileList.value.push({ name: options.file.name, url, status: 'success' })
  } catch {
    /* request.ts 已经统一 ElMessage.error 提示 */
  } finally {
    uploading.value--
  }
}

function removeImage(idx: number) {
  images.value.splice(idx, 1)
  fileList.value.splice(idx, 1)
}

function moveImage(idx: number, dir: -1 | 1) {
  const to = idx + dir
  if (to < 0 || to >= images.value.length) return
  ;[images.value[idx], images.value[to]] = [images.value[to], images.value[idx]]
  ;[fileList.value[idx], fileList.value[to]] = [fileList.value[to], fileList.value[idx]]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (form.category === null) return
  if (uploading.value > 0) {
    ElMessage.warning('还有图片正在上传,请稍候')
    return
  }

  const body: PublishProductRequest = {
    title: form.title,
    description: form.description || undefined,
    price: form.price,
    category: form.category,
    images: images.value.length ? [...images.value] : undefined,
  }

  submitting.value = true
  try {
    const vo = await productApi.publish(body)
    ElMessage.success('发布成功')
    router.replace({ name: 'product-detail', params: { id: vo.id } })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page publish">
    <AppHeader />
    <main class="page-main">
      <header class="page-head">
        <span class="head-tag">PUBLISH</span>
        <h1>发布闲置 <span>✨</span></h1>
        <p>让你的物品找到新主人,赚回买入价的一部分</p>
      </header>

      <div class="layout">
        <!-- 左:表单 -->
        <section class="form-card glass-strong">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
          >
            <el-form-item label="商品标题" prop="title">
              <el-input
                v-model="form.title"
                maxlength="100"
                show-word-limit
                size="large"
                placeholder="一句话说清楚:什么 / 成色 / 卖点"
              />
            </el-form-item>

            <el-form-item label="选择分类" prop="category">
              <div class="cat-grid">
                <button
                  v-for="c in PRODUCT_CATEGORIES"
                  :key="c.value"
                  type="button"
                  class="cat-btn"
                  :class="{ active: form.category === c.value }"
                  @click="form.category = c.value"
                >
                  <span>{{ CATEGORY_ICONS[c.value] }}</span>
                  <strong>{{ c.label }}</strong>
                </button>
              </div>
            </el-form-item>

            <el-form-item label="售价" prop="price">
              <el-input v-model="form.price" placeholder="0.00" size="large" style="max-width:280px">
                <template #prepend>¥</template>
              </el-input>
            </el-form-item>

            <el-form-item label="详细描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="6"
                maxlength="2000"
                show-word-limit
                placeholder="使用时长、成色、有无瑕疵、配件、议价空间…"
              />
            </el-form-item>

            <el-form-item :label="`商品图片 (${images.length}/${MAX_IMAGES})`">
              <div class="uploads">
                <div
                  v-for="(u, i) in images"
                  :key="u"
                  class="up-item"
                  :class="{ cover: i === 0 }"
                >
                  <img :src="u" :alt="`图${i + 1}`" />
                  <span v-if="i === 0" class="cover-tag">封面</span>
                  <div class="up-ops">
                    <button
                      type="button"
                      class="up-op"
                      :disabled="i === 0"
                      title="左移"
                      @click="moveImage(i, -1)"
                    >‹</button>
                    <button
                      type="button"
                      class="up-op danger"
                      title="删除"
                      @click="removeImage(i)"
                    >×</button>
                    <button
                      type="button"
                      class="up-op"
                      :disabled="i === images.length - 1"
                      title="右移"
                      @click="moveImage(i, 1)"
                    >›</button>
                  </div>
                </div>

                <el-upload
                  v-if="images.length < MAX_IMAGES"
                  class="up-add"
                  :show-file-list="false"
                  :accept="ACCEPT"
                  :before-upload="beforeUpload"
                  :http-request="customUpload as any"
                >
                  <div class="up-add-inner">
                    <span class="up-plus">+</span>
                    <small>{{ uploading > 0 ? `上传中 ${uploading}` : '点击上传' }}</small>
                  </div>
                </el-upload>
              </div>
              <div class="up-tip">
                第一张作为封面 · 单张最大 10MB · 支持 JPG/PNG/GIF/WEBP/BMP
              </div>
            </el-form-item>

            <div class="actions">
              <el-button size="large" @click="router.back()">取消</el-button>
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="onSubmit"
              >
                {{ submitting ? '发布中…' : '发 布 商 品' }}
              </el-button>
            </div>
          </el-form>
        </section>

        <!-- 右:预览 -->
        <aside class="preview glass">
          <h3>实时预览</h3>
          <div class="preview-card">
            <div class="preview-cover">
              <img v-if="images[0]" :src="images[0]" />
              <div v-else class="cover-empty">📷 添加图片以预览</div>
            </div>
            <div class="preview-body">
              <div class="preview-title">
                {{ form.title || '商品标题会显示在这里' }}
              </div>
              <div class="preview-price">
                <span class="sym">¥</span>
                <span class="num">{{ form.price || '0.00' }}</span>
              </div>
            </div>
          </div>

          <div v-if="images.length > 1" class="thumbs">
            <div v-for="(u, i) in images.slice(1)" :key="i" class="t">
              <img :src="u" />
            </div>
          </div>

          <ul class="tips">
            <li>📸 第一张图会作为封面,可拖动顺序</li>
            <li>🏷 价格建议参考闲鱼同款的 6-7 折</li>
            <li>💡 描述里写"出厂年份 / 使用频率"会更好卖</li>
          </ul>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.page-head {
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
    font-size: 28px;
    font-weight: 700;

    span {
      background: var(--brand-grad);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
  }
  p {
    margin: 0;
    color: var(--text-muted);
    font-size: 14px;
  }
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

/* 表单卡 */
.form-card {
  padding: 32px;
}
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
  width: 100%;
}
.cat-btn {
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid rgba(99, 102, 241, 0.12);
  border-radius: 14px;
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  span {
    font-size: 24px;
  }
  strong {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
  }

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(99, 102, 241, 0.4);
  }
  &.active {
    border-color: transparent;
    background: var(--brand-grad);
    box-shadow: var(--shadow-brand);
    strong {
      color: #fff;
    }
  }
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

/* 预览 */
.preview {
  padding: 22px;
  position: sticky;
  top: 96px;

  h3 {
    margin: 0 0 16px;
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
.preview-card {
  border: 1px solid rgba(99, 102, 241, 0.1);
  border-radius: var(--r-md);
  overflow: hidden;
  background: #fff;
  box-shadow: var(--shadow-sm);
}
.preview-cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, #eef0fb, #e0e7ff);
  display: grid;
  place-items: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.cover-empty {
  color: var(--text-muted);
  font-size: 13px;
}
.preview-body {
  padding: 14px 16px;
}
.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}
.preview-price {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 700;
  display: inline-flex;
  align-items: baseline;
  gap: 1px;

  .sym { font-size: 12px; }
  .num { font-size: 20px; }
}
.thumbs {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;

  .t {
    aspect-ratio: 1;
    border-radius: 8px;
    overflow: hidden;
    background: #eef0fb;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.tips {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.9;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 12px;
  padding: 12px 14px;

  li + li {
    margin-top: 2px;
  }
}

/* 上传区域 */
.uploads {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
}
.up-item {
  position: relative;
  width: 104px;
  height: 104px;
  border-radius: 14px;
  overflow: hidden;
  border: 2px solid rgba(99, 102, 241, 0.12);
  background: #eef0fb;
  transition: all 0.2s var(--ease-out);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &.cover {
    border-color: var(--brand-1);
    box-shadow: var(--shadow-brand);
  }
}
.cover-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
  background: var(--brand-grad);
  letter-spacing: 1px;
}
.up-ops {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  background: rgba(0, 0, 0, 0.55);
  opacity: 0;
  transition: opacity 0.2s var(--ease-out);
}
.up-item:hover .up-ops { opacity: 1; }
.up-op {
  flex: 1;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 16px;
  padding: 4px 0;
  cursor: pointer;
  font-weight: 600;
  line-height: 1;
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.danger { color: #fca5a5; }
  &:hover:not(:disabled) { background: rgba(255, 255, 255, 0.12); }
}

.up-add {
  width: 104px;
  height: 104px;
  display: inline-block;

  :deep(.el-upload) {
    width: 100%;
    height: 100%;
  }
}
.up-add-inner {
  width: 104px;
  height: 104px;
  border-radius: 14px;
  border: 2px dashed rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--brand-1);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  &:hover {
    border-color: var(--brand-1);
    background: rgba(99, 102, 241, 0.08);
    transform: translateY(-2px);
  }
  .up-plus {
    font-size: 28px;
    line-height: 1;
    font-weight: 300;
  }
  small {
    font-size: 11px;
    color: var(--text-muted);
  }
}

.up-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 880px) {
  .layout { grid-template-columns: 1fr; }
  .preview { position: static; }
}
</style>
