import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Layout from '@/views/Layout.vue'
import Ranking from '@/views/Ranking.vue'
import Trend from '@/views/Trend.vue'
import Dashboard from '@/views/Dashboard.vue'
import Map from '@/views/Map.vue'
import Manage from '@/views/Manage.vue'
import About from '@/views/About.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/ranking',
    children: [
      {
        path: 'ranking',
        name: 'Ranking',
        component: Ranking,
        meta: { title: '排名查询' }
      },
      {
        path: 'trend',
        name: 'Trend',
        component: Trend,
        meta: { title: '趋势分析' }
      },
      {
        path: 'map',
        name: 'ClimateMap',
        component: Map,
        meta: { title: '气候地图' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '数据大屏' }
      },
      {
        path: 'manage',
        name: 'Manage',
        component: Manage,
        meta: { title: '数据管理', adminOnly: true }
      },
      {
        path: 'about',
        name: 'About',
        component: About,
        meta: { title: '帮助与关于' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 无需登录即可访问的页面 */
const WHITE_LIST = new Set(['/login'])

/** 解析 JWT payload 的 exp（秒级时间戳）判断是否过期；解析失败一律视为无效 */
function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(
      atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    )
    return typeof payload.exp === 'number' && Date.now() / 1000 < payload.exp
  } catch {
    return false
  }
}

function clearLoginState() {
  localStorage.removeItem('weather_token')
  localStorage.removeItem('weather_user')
}

// 全局前置守卫：未登录或 token 已过期，一律拦回登录页
router.beforeEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title as string} - 云观气象`
    : '云观气象 · 城市气象大数据可视化平台'

  const token = localStorage.getItem('weather_token')

  // 登录页：已登录直接送回首页，未登录放行
  if (WHITE_LIST.has(to.path)) {
    return token && isTokenValid(token) ? { path: '/ranking' } : true
  }

  // 其余页面：无 token 或 token 已过期/伪造 → 清理登录态并拦截
  if (!token || !isTokenValid(token)) {
    clearLoginState()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 管理页仅 admin 可入（后端接口另有 403 兜底）
  if (to.meta.adminOnly) {
    let role: string | undefined
    try {
      role = JSON.parse(localStorage.getItem('weather_user') || 'null')?.role
    } catch {
      role = undefined
    }
    if (role !== 'admin') {
      return { path: '/ranking' }
    }
  }

  return true
})

export default router
