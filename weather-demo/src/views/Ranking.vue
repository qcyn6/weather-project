<template>
  <div class="ranking-page">
    <!-- 页头 -->
    <header class="page-head">
      <div>
        <h2 class="t-headline page-title">{{ factorConfig.label }} · 城市排名榜</h2>
        <p class="page-sub">全国 {{ rankingData.length }} 城 · 年度气象数据综合评估与对比分析</p>
      </div>
      <div class="head-actions">
        <span class="page-update t-label">更新于 {{ lastRefreshTime || '—' }}</span>
        <button class="btn btn-outline" :disabled="!rankingData.length" @click="handleExport">
          <span class="material-symbols-outlined">download</span>
          导出 Excel
        </button>
      </div>
    </header>

    <!-- 筛选栏 -->
    <section class="filter-card scroll-card ink-border">
      <svg class="filter-cloud" viewBox="0 0 24 24" fill="currentColor" width="110" height="110">
        <path d="M6 14C4.34 14 3 12.66 3 11C3 9.34 4.34 8 6 8C6.33 8 6.64 8.05 6.94 8.15C7.57 6.32 9.33 5 11.5 5C14.26 5 16.5 7.24 16.5 10C16.5 10.15 16.49 10.29 16.47 10.43C18.46 10.74 20 12.44 20 14.5C20 16.71 18.21 18.5 16 18.5H6C4.07 18.5 2.5 16.93 2.5 15C2.5 13.07 4.07 11.5 6 11.5V14Z" />
      </svg>

      <div class="field filter-field">
        <label class="field-label">观测要素</label>
        <div class="field-control">
          <select v-model="filter.factor">
            <option v-for="f in FACTOR_OPTIONS" :key="f.value" :value="f.value">
              {{ f.label }}（{{ FACTOR_CONFIGS[f.value].unitLabel }}）
            </option>
          </select>
          <span class="field-arrow material-symbols-outlined">arrow_drop_down</span>
        </div>
      </div>

      <div class="field filter-field">
        <label class="field-label">统计年份</label>
        <div class="field-control">
          <select v-model.number="filter.year">
            <option v-for="y in YEAR_OPTIONS" :key="y" :value="y">{{ y }} {{ ganzhi(y) }}年</option>
          </select>
          <span class="field-arrow material-symbols-outlined">arrow_drop_down</span>
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-outline" @click="handleReset">重置</button>
        <button class="btn btn-primary" :disabled="loading" @click="handleQuery">查询</button>
      </div>
    </section>

    <!-- 指标印卡 -->
    <section class="stat-grid">
      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <div class="stat-icon" :style="{ background: factorConfig.color + '1a', color: factorConfig.color }">
            <span class="material-symbols-outlined">{{ factorConfig.icon }}</span>
          </div>
          <span class="t-label">极值{{ factorConfig.label }}（{{ rankingData[0]?.city ?? '—' }}）</span>
        </div>
        <div class="stat-value t-num">
          {{ rankingData[0] ? rankingData[0][factorConfig.mainMetric] : '—' }}
          <span class="stat-unit">{{ factorConfig.unit }}</span>
        </div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <div class="stat-icon" style="background: rgba(31,68,97,0.1); color: var(--ink-tertiary-container)">
            <span class="material-symbols-outlined">location_city</span>
          </div>
          <span class="t-label">参评城市数</span>
        </div>
        <div class="stat-value t-num">{{ rankingData.length }}<span class="stat-unit">座</span></div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <div class="stat-icon" style="background: rgba(20,48,38,0.1); color: var(--ink-primary)">
            <span class="material-symbols-outlined">calendar_today</span>
          </div>
          <span class="t-label">数据基准年</span>
        </div>
        <div class="stat-value t-num">{{ filter.year }}<span class="stat-unit">{{ ganzhi(filter.year) }}年</span></div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <div class="stat-icon" style="background: rgba(148,74,39,0.1); color: var(--ink-secondary)">
            <span class="material-symbols-outlined">emoji_events</span>
          </div>
          <span class="t-label">榜首城市</span>
        </div>
        <div class="stat-value stat-city">{{ rankingData[0]?.city ?? '—' }}</div>
      </div>
    </section>

    <!-- 主体：图 + 表 -->
    <section class="main-grid">
      <div class="chart-card scroll-card ink-border">
        <div class="panel-head">
          <h3 class="t-title">{{ filter.year }}年各城市{{ factorConfig.label }}梯队分布</h3>
          <span class="t-label">仅列前 {{ chartLimit }} 城 · 共 {{ rankingData.length }} 城</span>
        </div>
        <div ref="chartRef" class="chart-box"></div>
      </div>

      <div class="table-card scroll-card ink-border">
        <div class="panel-head">
          <h3 class="t-title">详细排名</h3>
          <span class="t-label">共 {{ rankingData.length }} 条</span>
        </div>
        <div class="table-scroll">
          <table class="ink-table">
            <thead>
              <tr>
                <th class="col-rank">排名</th>
                <th>城市</th>
                <th>省份</th>
                <th v-for="col in factorConfig.columns" :key="col.prop" class="col-num">
                  {{ col.label }}({{ col.unit }})
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pagedData" :key="row.city" :class="{ 'is-first': row.rankNum === 1 && page === 1 }">
                <td class="col-rank">
                  <span v-if="row.rankNum === 1" class="material-symbols-outlined rank-gold material-symbols-filled">workspace_premium</span>
                  <span v-else class="rank-num t-num">{{ row.rankNum }}</span>
                </td>
                <td class="col-city">{{ row.city }}</td>
                <td class="text-variant">{{ row.province }}</td>
                <td v-for="col in factorConfig.columns" :key="col.prop" class="col-num t-num">
                  {{ row[col.prop] }}
                </td>
              </tr>
              <tr v-if="!rankingData.length && !loading">
                <td :colspan="3 + factorConfig.columns.length" class="empty-cell">该年份暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="table-pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="rankingData.length"
            layout="prev, pager, next, total"
            background
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRanking, type MetricRow } from '@/api/weather'
import { download } from '@/utils/request'
import { createLatestGuard } from '@/utils/latestOnly'
import { inkPalette, onThemeChange, seriesColor } from '@/theme/chart'
import { FACTOR_CONFIGS, FACTOR_OPTIONS, YEAR_OPTIONS, ganzhi, shortName, type FactorKey } from '@/config/factor'

const filter = reactive({
  factor: 'rain' as FactorKey,
  year: 2020
})

const factorConfig = computed(() => FACTOR_CONFIGS[filter.factor])
const rankingData = ref<MetricRow[]>([])
const loading = ref(false)
const lastRefreshTime = ref('')
const chartRef = ref<HTMLElement>()
const chartLimit = 8
let chartInstance: echarts.ECharts | null = null

// 表格分页（前端分页，数据一次拉取后本地切片）
const page = ref(1)
const pageSize = 10
const pagedData = computed(() =>
  rankingData.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

// 快速切换筛选时丢弃过期响应，防止旧数据覆盖新数据
const queryGuard = createLatestGuard()

// 主题切换后按新调色板重绘图表
const offTheme = onThemeChange(() => nextTick(renderChart))

async function handleQuery() {
  const seq = queryGuard.next()
  loading.value = true
  try {
    const data = await getRanking(filter.factor, filter.year)
    if (queryGuard.isStale(seq)) return
    rankingData.value = data
    page.value = 1 // 新查询从第一页开始
    lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    nextTick(renderChart)
  } finally {
    if (!queryGuard.isStale(seq)) {
      loading.value = false
    }
  }
}

function handleExport() {
  download('/export/ranking', { factor: filter.factor, year: filter.year },
    `${factorConfig.value.label}城市排名_${filter.year}.xlsx`)
}

function handleReset() {
  // 已在默认状态时 watch 不会触发，需手动查询；否则由 watch 自动查询，避免重复请求
  const atDefault = filter.factor === 'rain' && filter.year === 2020
  filter.factor = 'rain'
  filter.year = 2020
  if (atDefault) handleQuery()
}

// 要素或年份变化时自动重新查询
watch(filter, handleQuery)

function renderChart() {
  const pal = inkPalette()
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const config = factorConfig.value
  const data = rankingData.value.slice(0, chartLimit)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(28, 28, 17, 0.9)',
      borderWidth: 0,
      textStyle: { color: '#fdfae7', fontFamily: 'Work Sans, PingFang SC, sans-serif' },
      formatter: (params: unknown) => {
        const p = (params as Array<{ name: string; value: number }>)[0]
        return `${p.name}<br/>${config.unitLabel}：<b>${p.value}</b> ${config.unit}`
      }
    },
    grid: { left: 8, right: 8, top: 30, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => shortName(String(d.city))),
      axisLine: { lineStyle: { color: pal.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: pal.axisLabel, fontSize: 12 },
      splitLine: { lineStyle: { color: pal.splitLine, type: 'dashed' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: '55%',
        data: data.map((d, i) => ({
          value: d[config.mainMetric],
          itemStyle: {
            // 墨色阶梯：名次越靠前墨色越重
            color: echarts.color.modifyAlpha(seriesColor(config.color), Math.max(0.35, 0.9 - i * 0.08)),
            borderRadius: [2, 2, 0, 0]
          }
        })),
        label: {
          show: true,
          position: 'top',
          color: pal.axisLabel,
          fontSize: 11,
          fontFamily: 'DIN Alternate, Work Sans, sans-serif'
        }
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const handleResize = () => chartInstance?.resize()

onMounted(() => {
  handleQuery()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  offTheme()
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped lang="scss">
.ranking-page {
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
  .head-actions {
    display: flex;
    align-items: center;
    gap: 16px;

    .page-update {
      color: var(--ink-outline);
    }
  }
}

/* ---------- 筛选栏 ---------- */
.filter-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  position: relative;
  overflow: hidden;

  @media (min-width: 768px) {
    flex-direction: row;
    align-items: flex-end;
  }

  .filter-cloud {
    position: absolute;
    top: 0;
    right: 0;
    color: var(--ink-primary);
    opacity: 0.05;
    pointer-events: none;
  }

  .filter-field {
    flex: 1;
    min-width: 180px;
  }

  .filter-actions {
    display: flex;
    gap: 12px;

    @media (min-width: 768px) {
      margin-left: auto;
    }
  }
}

/* ---------- 指标印卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.stat-card {
  padding: 24px;
  transition: border-color 0.3s;

  &:hover {
    border-color: rgba(20, 48, 38, 0.35);
  }

  .stat-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    .stat-icon {
      width: 34px;
      height: 34px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;

      .material-symbols-outlined {
        font-size: 20px;
      }
    }
  }

  .stat-value {
    font-size: 40px;
    font-weight: 700;
    line-height: 1.1;
    color: var(--ink-text);

    .stat-unit {
      font-size: 18px;
      font-weight: 400;
      color: var(--ink-outline);
      margin-left: 6px;
    }

    &.stat-city {
      font-family: var(--font-serif);
      font-size: 30px;
      padding-top: 6px;
    }
  }
}

/* ---------- 主体网格 ---------- */
.main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;

  @media (min-width: 1100px) {
    grid-template-columns: 2fr 1fr;
  }
}

.chart-card,
.table-card {
  padding: 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .t-label {
    color: var(--ink-outline);
  }
}

.chart-box {
  height: 320px;
}

/* ---------- 表格 ---------- */
.table-scroll {
  overflow: hidden;
}

.table-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
  border-top: 1px solid rgba(193, 200, 195, 0.2);

  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-text-color: var(--ink-text-variant);
    --el-pagination-button-disabled-color: var(--ink-outline-variant);
    font-size: 13px;

    .el-pager li {
      border-radius: 4px;
      font-family: var(--font-din);
    }

    .el-pager li.is-active {
      background: var(--ink-primary-container);
      color: #fff;
    }

    .btn-prev, .btn-next {
      border-radius: 4px;
      background: transparent;
      color: var(--ink-text-variant);
    }
  }
}

.ink-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  text-align: left;

  thead th {
    position: sticky;
    top: 0;
    background: #fff;
    padding: 10px 8px;
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.05em;
    color: var(--ink-text-variant);
    border-bottom: 1px solid rgba(193, 200, 195, 0.5);
    white-space: nowrap;
  }

  tbody td {
    padding: 10px 8px;
    border-bottom: 1px solid rgba(193, 200, 195, 0.15);
    white-space: nowrap;
  }

  tbody tr {
    transition: background 0.2s;

    &:hover {
      background: var(--ink-bg-low);
    }

    &.is-first {
      background: rgba(148, 74, 39, 0.06);
    }
  }

  .col-rank {
    width: 52px;
  }

  .rank-gold {
    color: var(--ink-secondary);
    font-size: 22px;
  }

  .rank-num {
    color: var(--ink-text-variant);
    font-weight: 700;
    padding-left: 6px;
  }

  .col-city {
    font-weight: 500;
    color: var(--ink-text);
    white-space: nowrap;
  }

  .col-num {
    text-align: right;
    white-space: nowrap;
  }

  .empty-cell {
    text-align: center;
    color: var(--ink-outline);
    padding: 32px 0;
  }
}
</style>
