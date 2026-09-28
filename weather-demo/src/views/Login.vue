<template>
  <div class="login-page">
    <!-- 左侧品牌视觉区（60%） -->
    <div class="brand-pane">
      <div class="particles">
        <span
          v-for="(p, i) in particles"
          :key="i"
          class="particle"
          :style="p"
        ></span>
      </div>

      <div class="brand-head">
        <h1 class="t-display">云观气象</h1>
        <p class="brand-sub">城市气象大数据可视化平台</p>
      </div>

      <div class="brand-cards">
        <div class="seal-card float-a">
          <span class="seal-watermark material-symbols-outlined">cloud</span>
          <div class="seal-label">
            <span class="material-symbols-outlined">device_thermostat</span>
            <span>实时温度</span>
          </div>
          <div class="seal-value t-num">24.5°C</div>
          <div class="seal-divider"></div>
          <div class="seal-foot">较昨日 <span class="text-variant up-note">+1.2°</span></div>
        </div>

        <div class="seal-card float-b">
          <span class="seal-watermark material-symbols-outlined">water_drop</span>
          <div class="seal-label">
            <span class="material-symbols-outlined">water_drop</span>
            <span>降水概率</span>
          </div>
          <div class="seal-value t-num">12%</div>
          <div class="seal-divider"></div>
          <div class="seal-foot">微风 2级</div>
        </div>
      </div>

      <div class="brand-foot t-label">
        <span class="material-symbols-outlined" style="font-size: 18px">auto_awesome</span>
        <span>融合传统智慧与现代数据科学</span>
      </div>
    </div>

    <!-- 右侧登录表单（40%） -->
    <div class="form-pane">
      <div class="form-card">
        <div class="form-head">
          <div class="brand-mini t-display">云观气象</div>
          <h2 class="t-headline">{{ isRegister ? '注册账号' : '系统登录' }}</h2>
          <p class="form-tip">
            {{ isRegister ? '创建您的分析员账号' : '请输入您的凭证以访问气象数据' }}
          </p>
        </div>

        <form class="form-body" @submit.prevent="handleSubmit">
          <div class="field">
            <label class="field-label" for="username">用户名</label>
            <div class="field-control">
              <span class="field-icon material-symbols-outlined">person</span>
              <input
                id="username"
                v-model.trim="form.username"
                type="text"
                placeholder="分析员账号"
                autocomplete="username"
              />
            </div>
          </div>

          <div v-if="isRegister" class="field">
            <label class="field-label" for="nickname">昵称</label>
            <div class="field-control">
              <span class="field-icon material-symbols-outlined">badge</span>
              <input
                id="nickname"
                v-model.trim="form.nickname"
                type="text"
                placeholder="展示昵称（可选）"
              />
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="password">密码</label>
            <div class="field-control">
              <span class="field-icon material-symbols-outlined">lock</span>
              <input
                id="password"
                v-model="form.password"
                type="password"
                placeholder="访问令牌"
                autocomplete="current-password"
              />
            </div>
          </div>

          <div v-if="!isRegister" class="form-row">
            <label class="remember">
              <input v-model="remember" type="checkbox" />
              <span class="t-label">保持登录</span>
            </label>
            <span class="t-label">演示账号 admin / 123456</span>
          </div>

          <button class="btn btn-primary login-btn" type="submit" :disabled="loading">
            <span class="login-btn-text">
              {{ loading ? '请稍候…' : isRegister ? '注 册' : '登 录' }}
            </span>
            <span class="material-symbols-outlined">arrow_forward</span>
          </button>
        </form>

        <div class="form-footer">
          <span class="t-label">
            {{ isRegister ? '已有账号？' : '无权访问？' }}
            <a class="switch-link" href="#" @click.prevent="toggleMode">
              {{ isRegister ? '返回登录' : '申请授权' }}
            </a>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { registerApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  username: 'admin',
  password: '123456',
  nickname: ''
})

function toggleMode() {
  isRegister.value = !isRegister.value
}

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await registerApi({ username: form.username, password: form.password, nickname: form.nickname || undefined })
      ElMessage.success('注册成功，请登录')
      toggleMode()
    } else {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')
      // 被拦截前想去的页面优先回跳，避免登录后总从排名页开始
      const redirect = route.query.redirect
      router.push(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/ranking')
    }
  } catch {
    // 错误提示已在 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

// 水墨漂移粒子：位置与动画参数一次性随机生成
interface ParticleStyle {
  width: string
  height: string
  left: string
  top: string
  background: string
  animationDuration: string
  animationDelay: string
}

const particles = ref<ParticleStyle[]>([])

onMounted(() => {
  const colors = ['rgba(20, 48, 38, 0.03)', 'rgba(31, 68, 97, 0.03)', 'rgba(114, 121, 117, 0.04)']
  particles.value = Array.from({ length: 15 }, () => {
    const size = Math.random() * 40 + 10
    return {
      width: `${size}px`,
      height: `${size}px`,
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      background: colors[Math.floor(Math.random() * colors.length)],
      animationDuration: `${Math.random() * 20 + 10}s`,
      animationDelay: `-${Math.random() * 10}s`
    }
  })
})
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

/* ---------- 左侧品牌区 ---------- */
.brand-pane {
  display: none;
  width: 60%;
  position: relative;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  overflow: hidden;
  border-right: 1px solid rgba(193, 200, 195, 0.3);
  background:
    radial-gradient(circle at 30% 70%, rgba(31, 68, 97, 0.08) 0%, transparent 60%),
    radial-gradient(circle at 80% 20%, rgba(43, 70, 60, 0.05) 0%, transparent 50%);

  @media (min-width: 960px) {
    display: flex;
  }

  .brand-sub {
    margin: 16px 0 0;
    font-family: var(--font-serif);
    font-size: 24px;
    color: var(--ink-text-variant);
    max-width: 26rem;
  }

  .brand-foot {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--ink-outline);
  }
}

.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
  animation: drift 15s linear infinite;
}

@keyframes drift {
  from { transform: translate(-20px, 10px) rotate(0deg); opacity: 0; }
  50% { opacity: 0.6; }
  to { transform: translate(40px, -20px) rotate(10deg); opacity: 0; }
}

.brand-head {
  position: relative;
  z-index: 2;
}

/* 浮动数据印卡 */
.brand-cards {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 24px;
  align-items: center;
  flex: 1;
}

.seal-card {
  width: 256px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: rgba(253, 250, 231, 0.8);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(193, 200, 195, 0.3);
  border-radius: 12px;
  box-shadow: var(--paper-shadow);

  .seal-watermark {
    position: absolute;
    top: 0;
    right: 0;
    padding: 14px;
    font-size: 60px;
    color: var(--ink-primary);
    opacity: 0.06;
  }

  .seal-label {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--ink-text-variant);

    .material-symbols-outlined {
      font-size: 22px;
      color: var(--ink-primary-container);
    }
  }

  .seal-value {
    font-family: var(--font-serif);
    font-size: 32px;
    font-weight: 600;
    color: var(--ink-primary);
    margin-top: 6px;
  }

  .seal-divider {
    width: 32px;
    height: 1px;
    background: rgba(20, 48, 38, 0.2);
    margin: 12px 0;
  }

  .seal-foot {
    font-size: 12px;
    color: var(--ink-text-variant);

    .up-note {
      color: var(--ink-secondary);
    }
  }
}

.float-a {
  animation: float 6s ease-in-out infinite;
  align-self: flex-start;
  margin-top: 48px;
}
.float-b {
  animation: float 7s ease-in-out infinite 1s;
  align-self: flex-end;
  margin-bottom: 48px;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-10px) rotate(1deg); }
}

/* ---------- 右侧表单区 ---------- */
.form-pane {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--ink-surface-white);
}

.form-card {
  width: 100%;
  max-width: 420px;
  background: var(--ink-surface);
  border: 1px solid rgba(193, 200, 195, 0.2);
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.02);
}

.form-head {
  text-align: center;
  margin-bottom: 36px;

  .brand-mini {
    display: none;
    font-size: 28px;
    margin-bottom: 8px;

    @media (max-width: 959px) {
      display: block;
    }
  }

  .form-tip {
    margin: 8px 0 0;
    font-size: 15px;
    color: var(--ink-text-variant);
  }
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 24px;

  .field-icon {
    position: absolute;
    left: 0;
    top: 8px;
    font-size: 22px;
    color: var(--ink-outline);
    transition: color 0.25s;
  }

  .field-control:focus-within .field-icon {
    color: var(--ink-primary);
  }

  .field-control input {
    padding-left: 30px;
  }
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .remember {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    input {
      width: 16px;
      height: 16px;
      accent-color: var(--ink-primary);
      cursor: pointer;
    }
  }
}

.login-btn {
  width: 100%;
  padding: 12px;
  letter-spacing: 0.3em;

  .login-btn-text {
    letter-spacing: 0.3em;
    margin-right: -0.3em;
  }

  .material-symbols-outlined {
    font-size: 16px;
    transition: transform 0.25s;
  }

  &:hover:not(:disabled) .material-symbols-outlined {
    transform: translateX(4px);
  }
}

.form-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(193, 200, 195, 0.2);
  text-align: center;

  .switch-link {
    color: var(--ink-primary);
    font-weight: 500;
    text-decoration: none;
    border-bottom: 1px solid rgba(20, 48, 38, 0.3);
    margin-left: 4px;

    &:hover {
      border-bottom-color: var(--ink-primary);
    }
  }
}
</style>
