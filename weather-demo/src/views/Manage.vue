<template>
  <div class="manage-page">
    <header class="page-head">
      <div>
        <h2 class="t-headline page-title">数据管理</h2>
        <p class="page-sub">数据资产总览 · 聚类管道运维 · 操作审计</p>
      </div>
      <button class="btn btn-primary" :disabled="reclusterRunning" @click="handleRecluster">
        <span class="material-symbols-outlined">{{ reclusterRunning ? 'progress_activity' : 'play_arrow' }}</span>
        {{ reclusterRunning ? '聚类作业运行中…' : '重跑聚类管道' }}
      </button>
    </header>

    <!-- 表行数统计 -->
    <section class="stats-card scroll-card ink-border">
      <div class="panel-head">
        <h3 class="t-title">数据资产统计</h3>
        <span class="t-label">共 {{ stats.length }} 张表</span>
      </div>
        <div class="stats-grid">
          <div v-for="s in stats" :key="s.table" class="stat-cell">
            <div class="stat-rows t-num">{{ s.rows.toLocaleString() }}</div>
            <div class="stat-cn">{{ tableCn(s.table) }}</div>
            <div class="t-label stat-table">{{ s.table }}</div>
          </div>
        </div>
    </section>

    <!-- 重跑日志 -->
    <section v-if="reclusterLog" class="scroll-card ink-border log-output">
      <div class="panel-head">
        <h3 class="t-title">聚类作业输出</h3>
      </div>
      <pre class="raw-log">{{ reclusterLog }}</pre>
    </section>

    <!-- 操作日志 -->
    <section class="scroll-card ink-border">
      <div class="panel-head">
        <h3 class="t-title">操作日志</h3>
        <span class="t-label">最近 {{ logs.length }} 条</span>
      </div>
      <table class="ink-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>动作</th>
            <th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td class="t-num text-variant">{{ formatTime(log.create_time) }}</td>
            <td>{{ log.username }}</td>
            <td>
              <span class="action-tag" :class="actionClass(log.action)">{{ actionLabel(log.action) }}</span>
            </td>
            <td class="text-variant detail-cell">{{ log.detail }}</td>
          </tr>
          <tr v-if="!logs.length">
            <td colspan="4" class="empty-cell">暂无日志</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getManageStats, getManageLogs, recluster, type TableStat, type OperationLog } from '@/api/analysis'

const stats = ref<TableStat[]>([])
const logs = ref<OperationLog[]>([])
const reclusterRunning = ref(false)
const reclusterLog = ref('')

async function load() {
  try {
    stats.value = await getManageStats()
  } catch {
    // 403 等错误拦截器已提示
  }
  try {
    logs.value = await getManageLogs()
  } catch {
    // 同上
  }
}

async function handleRecluster() {
  reclusterRunning.value = true
  try {
    reclusterLog.value = await recluster(String(Date.now()))
    ElMessage.success('聚类作业执行完成')
    await load()
  } catch {
    ElMessage.error('聚类作业执行失败，请查看后端日志')
  } finally {
    reclusterRunning.value = false
  }
}

/** 表名 → 中文名称 */
const TABLE_CN: Record<string, string> = {
  ads_rain_ranking: '降水排名',
  ads_rain_trend: '降水趋势',
  ads_temp_ranking: '气温排名',
  ads_temp_trend: '气温趋势',
  ads_sun_ranking: '日照排名',
  ads_sun_trend: '日照趋势',
  ads_hum_ranking: '湿度排名',
  ads_hum_trend: '湿度趋势',
  ads_wind_ranking: '风速排名',
  ads_wind_trend: '风速趋势',
  dim_city_geo: '城市经纬度维表',
  ads_city_cluster: '城市气候聚类',
  ads_cluster_profile: '气候区画像',
  sys_user: '系统用户',
  sys_operation_log: '操作日志'
}

function tableCn(table: string): string {
  return TABLE_CN[table] ?? table
}

function formatTime(s: string): string {
  return String(s).replace('T', ' ').slice(0, 19)
}

function actionLabel(action: string): string {
  return { LOGIN: '登录', LOGIN_FAIL: '登录失败', LOGIN_LOCKED: '账号锁定', REGISTER: '注册', EXPORT: '导出', RECLUSTER: '重跑聚类' }[action] ?? action
}

function actionClass(action: string): string {
  if (action === 'LOGIN') return 'ok'
  if (action === 'LOGIN_FAIL' || action === 'LOGIN_LOCKED') return 'warn'
  return 'info'
}

onMounted(load)
</script>

<style scoped lang="scss">
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(193, 200, 195, 0.3);

  .page-title {
    font-size: 32px;
  }
  .page-sub {
    margin: 8px 0 0;
    color: var(--ink-text-variant);
  }
}

.stats-card,
.log-output {
  padding: 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .t-label {
    color: var(--ink-outline);
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;

  .stat-cell {
    padding: 14px;
    background: var(--ink-bg-low);
    border-radius: 6px;

    .stat-rows {
      font-size: 22px;
      font-weight: 700;
      color: var(--ink-primary);
    }

    .stat-cn {
      margin-top: 4px;
      font-size: 13px;
      font-weight: 600;
      color: var(--ink-text);
    }

    .stat-table {
      margin-top: 2px;
      color: var(--ink-outline);
      word-break: break-all;
    }
  }
}

.raw-log {
  max-height: 240px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  background: #2a2f2b;
  color: #d8ded9;
  border-radius: 6px;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.ink-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;

  th {
    padding: 10px 8px;
    text-align: left;
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.05em;
    color: var(--ink-text-variant);
    border-bottom: 1px solid rgba(193, 200, 195, 0.5);
  }

  td {
    padding: 10px 8px;
    border-bottom: 1px solid rgba(193, 200, 195, 0.15);
  }

  tbody tr:hover {
    background: var(--ink-bg-low);
  }

  .action-tag {
    padding: 3px 8px;
    border-radius: 4px;
    font-size: 11px;
    border: 1px solid transparent;

    &.ok {
      background: rgba(43, 70, 60, 0.08);
      color: var(--ink-primary-container);
      border-color: rgba(43, 70, 60, 0.2);
    }
    &.warn {
      background: rgba(186, 26, 26, 0.08);
      color: var(--ink-error);
      border-color: rgba(186, 26, 26, 0.2);
    }
    &.info {
      background: rgba(31, 68, 97, 0.08);
      color: var(--ink-tertiary-container);
      border-color: rgba(31, 68, 97, 0.2);
    }
  }

  .detail-cell {
    max-width: 420px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .empty-cell {
    text-align: center;
    color: var(--ink-outline);
    padding: 32px 0;
  }
}
</style>
