<template>
  <div class="app-shell">
    <!-- 顶导航 -->
    <nav class="top-nav">
      <div class="nav-brand">
        <span class="material-symbols-outlined material-symbols-filled brand-mark">water</span>
        <span class="t-title">云观气象 · 城市气象大数据可视化平台</span>
      </div>
      <div class="nav-actions">
        <button class="icon-btn" :title="dark ? '切换浅色宣纸' : '切换深色夜航'" @click="toggleDark">
          <span class="material-symbols-outlined">{{ dark ? 'light_mode' : 'dark_mode' }}</span>
        </button>
        <button class="icon-btn" title="当前用户">
          <span class="material-symbols-outlined">account_circle</span>
        </button>
        <button class="icon-btn" title="退出登录" @click="handleLogout">
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </nav>

    <div class="app-body">
      <!-- 侧边栏 -->
      <aside class="side-nav">
        <div class="side-user">
          <div class="side-avatar-seal">{{ avatarChar }}</div>
          <div>
            <div class="side-name">{{ nickname }}</div>
            <div class="t-label">{{ roleLabel }}</div>
          </div>
        </div>

        <nav class="side-menu">
          <div v-for="group in menuGroups" :key="group.title" class="menu-group">
            <div class="group-title t-label">{{ group.title }}</div>
            <RouterLink
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="side-link"
              :class="{ 'is-active': route.path === item.path }"
            >
              <span
                class="material-symbols-outlined"
                :class="{ 'material-symbols-filled': route.path === item.path }"
              >{{ item.icon }}</span>
              <span class="side-link-text">
                <span class="side-link-label">{{ item.label }}</span>
                <span class="side-link-desc">{{ item.desc }}</span>
              </span>
            </RouterLink>
          </div>
        </nav>

        <button class="ai-launcher" @click="aiOpen = true">
          <span class="material-symbols-outlined">smart_toy</span>
          <span class="side-link-text">
            <span class="side-link-label">AI 助手</span>
            <span class="side-link-desc">自然语言查询气候数据</span>
          </span>
        </button>

        <div class="side-status">
          <span class="status-dot" :class="healthState"></span>
          <span class="t-label">{{ healthText }}</span>
        </div>

        <!-- 云观印章 -->
        <div class="side-seal">
          <div class="seal-box">
            <span>云<br />观</span>
          </div>
        </div>

        <!-- 山峦剪影 -->
        <svg class="side-mountain" viewBox="0 0 100 50" preserveAspectRatio="none">
          <path d="M0,50 L20,30 L40,40 L70,10 L100,45 L100,50 Z" fill="currentColor" />
        </svg>
      </aside>

      <!-- 内容区 -->
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- AI 助手抽屉 -->
    <AssistantDrawer :open="aiOpen" @close="aiOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { applyTheme, isDarkMode, onThemeChange } from '@/theme/chart'
import AssistantDrawer from '@/views/AssistantDrawer.vue'
import { get } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuGroups = computed(() => {
  const analysis = [
    { path: '/ranking', label: '排名查询', desc: '全国年度要素榜单', icon: 'leaderboard' },
    { path: '/trend', label: '趋势分析', desc: '城市 21 年逐要素曲线', icon: 'monitoring' },
    { path: '/map', label: '气候地图', desc: '地理分布与气候分区', icon: 'map' }
  ]
  const overview = [{ path: '/dashboard', label: '数据大屏', desc: '全域态势一屏总览', icon: 'dashboard' }]
  const system = [{ path: '/about', label: '帮助与关于', desc: '数据来源与指标字典', icon: 'menu_book' }]
  if (userStore.userInfo?.role === 'admin') {
    system.push({ path: '/manage', label: '数据管理', desc: '资产统计与管道运维', icon: 'manage_accounts' })
  }
  return [
    { title: '数据分析', items: analysis },
    { title: '全景视图', items: overview },
    { title: '系统', items: system }
  ]
})

const nickname = computed(() => userStore.userInfo?.nickname || '游客')
const roleLabel = computed(() => (userStore.userInfo?.role === 'admin' ? '系统管理员' : '数据分析员'))
const avatarChar = computed(() => (userStore.userInfo?.nickname || '观').slice(0, 1))

// ---------- 服务状态灯 ----------
const healthState = ref<'ok' | 'db' | 'bad'>('ok')
const healthText = computed(
  () => ({ ok: '服务正常', db: '数据库异常', bad: '服务离线' })[healthState.value]
)
let healthTimer: number | undefined

async function checkHealth() {
  try {
    const res = await get<{ status: string; db: string }>('/health')
    healthState.value = res.db === 'up' ? 'ok' : 'db'
  } catch {
    healthState.value = 'bad'
  }
}

// ---------- 深色主题切换 ----------
const dark = ref(isDarkMode())
function toggleDark() {
  applyTheme(!isDarkMode())
  dark.value = isDarkMode()
}
onThemeChange(() => {
  dark.value = isDarkMode()
})

// ---------- AI 助手抽屉 ----------
const aiOpen = ref(false)

onMounted(() => {
  checkHealth()
  healthTimer = window.setInterval(checkHealth, 30_000)
})

onBeforeUnmount(() => {
  if (healthTimer) {
    window.clearInterval(healthTimer)
  }
})

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<style scoped lang="scss">
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ---------- 顶导航 ---------- */
.top-nav {
  height: 72px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  background: rgba(253, 250, 231, 0.95);
  backdrop-filter: blur(4px);
  border-bottom: 1px solid rgba(193, 200, 195, 0.3);
  z-index: 40;

  .nav-brand {
    display: flex;
    align-items: center;
    gap: 12px;

    .brand-mark {
      font-size: 28px;
      color: var(--ink-primary);
    }
  }

  .nav-actions {
    display: flex;
    gap: 8px;
  }
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--ink-text-variant);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;

  .material-symbols-outlined {
    font-size: 24px;
  }

  &:hover {
    background: var(--ink-bg-high);
    color: var(--ink-primary);
  }
}

/* ---------- 主体 ---------- */
.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.side-nav {
  display: none;
  width: 240px;
  flex-shrink: 0;
  flex-direction: column;
  padding: 24px 0;
  background: var(--ink-bg-low);
  border-right: 1px solid rgba(193, 200, 195, 0.2);
  z-index: 30;
  position: relative;

  @media (min-width: 768px) {
    display: flex;
  }
}

.side-user {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 24px 20px;
  border-bottom: 1px solid rgba(193, 200, 195, 0.2);
  margin-bottom: 16px;

  .side-avatar-seal {
    width: 44px;
    height: 44px;
    flex-shrink: 0;
    border-radius: 6px;
    background: var(--ink-error);
    color: #fff;
    font-family: var(--font-serif);
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-4deg);
    box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.55), 0 2px 6px rgba(148, 74, 39, 0.3);
  }

  .side-name {
    font-family: var(--font-serif);
    font-size: 17px;
    font-weight: 600;
    color: var(--ink-primary);
  }
}

.side-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 16px;

  .menu-group {
    margin-bottom: 12px;

    .group-title {
      padding: 6px 16px 4px;
      color: var(--ink-outline);
      letter-spacing: 0.15em;
    }
  }
}

.side-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 16px;
  border-radius: 999px;
  border-left: 4px solid transparent;
  color: var(--ink-text-variant);
  text-decoration: none;
  transition: all 0.25s;

  .material-symbols-outlined {
    font-size: 22px;
  }

  .side-link-text {
    display: flex;
    flex-direction: column;
    line-height: 1.3;

    .side-link-label {
      font-size: 14px;
      font-weight: 500;
    }

    .side-link-desc {
      font-size: 10px;
      color: var(--ink-outline);
    }
  }

  &:hover {
    background: rgba(230, 227, 208, 0.5);
  }

  &.is-active {
    background: rgba(253, 158, 116, 0.25);
    border-left-color: var(--ink-secondary);
    color: var(--ink-on-secondary-container);
    font-weight: 600;

    .side-link-desc {
      color: var(--ink-on-secondary-container);
      opacity: 0.75;
    }
  }
}

.ai-launcher {
  display: flex;
  align-items: center;
  gap: 12px;
  width: calc(100% - 32px);
  margin: 0 16px 12px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(232, 146, 106, 0.45);
  background: linear-gradient(120deg, rgba(232, 146, 106, 0.12), rgba(138, 182, 216, 0.08));
  color: var(--ink-on-secondary-container);
  cursor: pointer;
  text-align: left;
  transition: all 0.25s;

  .material-symbols-outlined {
    font-size: 22px;
    color: var(--ink-secondary);
  }

  .side-link-text {
    display: flex;
    flex-direction: column;
    line-height: 1.3;

    .side-link-label {
      font-size: 14px;
      font-weight: 600;
    }

    .side-link-desc {
      font-size: 10px;
      opacity: 0.75;
    }
  }

  &:hover {
    border-color: var(--ink-secondary);
    box-shadow: 0 2px 10px rgba(232, 146, 106, 0.25);
  }
}

.side-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: auto 24px 12px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 999px;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.ok {
      background: #2e7d32;
      box-shadow: 0 0 6px rgba(46, 125, 50, 0.6);
    }
    &.db {
      background: #f59e0b;
      box-shadow: 0 0 6px rgba(245, 158, 11, 0.6);
    }
    &.bad {
      background: var(--ink-error);
      box-shadow: 0 0 6px rgba(186, 26, 26, 0.6);
    }
  }
}

.side-seal {
  margin-top: auto;
  padding: 0 24px 16px;
  display: flex;
  justify-content: center;
  opacity: 0.55;

  .seal-box {
    width: 64px;
    height: 64px;
    border: 2px solid var(--ink-error);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;

    > span {
      width: 100%;
      height: 100%;
      border: 1px solid var(--ink-error);
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: var(--font-serif);
      font-size: 18px;
      line-height: 1.2;
      color: var(--ink-error);
      transform: rotate(12deg);
      text-align: center;
    }
  }
}

.side-mountain {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60px;
  color: var(--ink-primary);
  opacity: 0.08;
  pointer-events: none;
}

/* ---------- 内容区 ---------- */
.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  background-image:
    radial-gradient(circle at 100% 100%, rgba(20, 48, 38, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 0% 0%, rgba(43, 70, 60, 0.02) 0%, transparent 40%);

  @media (max-width: 767px) {
    padding: 20px;
  }
}

/* 路由过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
