<template>
  <div class="trend-page">
    <!-- 页头 -->
    <header class="page-head">
      <div>
        <h2 class="t-headline page-title">{{ filter.city }}{{ factorConfig.unitLabel }}历史长卷</h2>
        <p class="page-sub">廿载寒暑变迁记录 (2000 - 2020)</p>
      </div>
      <div class="head-actions">
        <div class="view-pills">
          <button class="btn-ghost" :class="{ 'is-active': view === 'yearly' }" @click="view = 'yearly'">年度趋势</button>
          <button class="btn-ghost" :class="{ 'is-active': view === 'monthly' }" @click="view = 'monthly'">月度分布</button>
        </div>
        <span class="page-update t-label">更新于 {{ lastRefreshTime || '—' }}</span>
        <button class="btn btn-outline" :disabled="!trendData.length" @click="handleExport">
          <span class="material-symbols-outlined">download</span>
          导出 Excel
        </button>
      </div>
    </header>

    <!-- 筛选栏 -->
    <section class="filter-card scroll-card ink-border">
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
        <label class="field-label">城市</label>
        <div class="field-control city-select">
          <el-select
            v-model="filter.city"
            filterable
            :filter-method="filterCity"
            placeholder="输入城市名搜索，或下拉选择"
          >
            <el-option v-for="c in cityOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-outline" @click="handleReset">重置</button>
        <button class="btn btn-primary" :disabled="loading" @click="handleQueryClick">查询</button>
      </div>
    </section>

    <!-- 年度趋势视图 -->
    <template v-if="view === 'yearly'">
    <!-- 统计印卡 -->
    <section class="stat-grid">
      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <span class="t-label">廿一年均{{ factorConfig.label }}</span>
          <span class="material-symbols-outlined stat-glyph" :style="{ color: factorConfig.color }">{{ factorConfig.icon }}</span>
        </div>
        <div class="stat-value t-num">{{ avgValue }}<span class="stat-unit">{{ factorConfig.unit }}</span></div>
        <div class="stat-foot" :style="{ color: trendColor }">
          <span class="material-symbols-outlined">{{ trendDirection === 'up' ? 'trending_up' : trendDirection === 'down' ? 'trending_down' : 'trending_flat' }}</span>
          <span class="t-label">较首个观测年</span>
        </div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <span class="t-label">历史极高</span>
          <span class="material-symbols-outlined stat-glyph" style="color: var(--ink-error)">wb_sunny</span>
        </div>
        <div class="stat-value t-num">{{ maxValue }}<span class="stat-unit">{{ factorConfig.unit }}</span></div>
        <div class="stat-foot text-variant t-label">{{ maxYear }}年记录</div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <span class="t-label">历史极低</span>
          <span class="material-symbols-outlined stat-glyph" style="color: var(--ink-tertiary-container)">ac_unit</span>
        </div>
        <div class="stat-value t-num">{{ minValue }}<span class="stat-unit">{{ factorConfig.unit }}</span></div>
        <div class="stat-foot text-variant t-label">{{ minYear }}年记录</div>
      </div>

      <div class="stat-card scroll-card ink-border">
        <div class="stat-head">
          <span class="t-label">长期趋势</span>
          <span class="material-symbols-outlined stat-glyph" style="color: var(--ink-primary)">moving</span>
        </div>
        <div class="stat-value stat-city">{{ trendLabel }}</div>
        <div class="stat-foot">
          <span class="trend-tag" :class="trendDirection">{{ trendDirection === 'up' ? '波动上升' : trendDirection === 'down' ? '波动下降' : '基本稳定' }}</span>
        </div>
      </div>
    </section>

    <!-- 主体：图 + 岁次详录 -->
    <section class="main-grid">
      <div class="chart-card scroll-card ink-border">
        <div class="panel-head">
          <div>
            <h3 class="t-title">{{ factorConfig.unitLabel }}流变图</h3>
            <div class="brush-stroke" style="width: 64px; margin-top: 6px"></div>
          </div>
          <div class="chart-tools">
            <button
              v-for="line in factorConfig.lineOptions"
              :key="line.value"
              class="btn-ghost line-pill"
              :class="{ 'is-active': checkedLines.includes(line.value) }"
              @click="toggleLine(line.value)"
            >
              <span class="pill-dot" :style="{ background: line.color }"></span>
              {{ line.label }}
            </button>
            <button class="btn-ghost" :class="{ 'is-active': smooth }" @click="smooth = !smooth">
              {{ smooth ? '平滑' : '折线' }}
            </button>
          </div>
        </div>
        <div ref="chartRef" class="chart-box"></div>
      </div>

      <div class="table-card scroll-card ink-border">
        <div class="panel-head">
          <h3 class="t-title">岁次详录</h3>
          <span class="t-label">共 {{ trendData.length }} 条</span>
        </div>
        <div class="table-scroll">
          <table class="ink-table">
            <thead>
              <tr>
                <th>岁次</th>
                <th class="col-num">{{ mainMetricName }}</th>
                <th class="col-num">变动</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in displayRows" :key="row.year">
                <td>
                  <span class="ganzhi">{{ ganzhi(row.year) }}</span>
                  <span class="year text-variant">({{ row.year }})</span>
                </td>
                <td class="col-num t-num">{{ formatVal(row[factorConfig.mainMetric as keyof TrendRow]) }}{{ factorConfig.unit }}</td>
                <td class="col-num">
                  <span class="trend-tag" :class="trendClass(row.trendLabel)">
                    <span class="material-symbols-outlined">{{ trendIcon(row.trendLabel) }}</span>
                    {{ trendTagText(row.trendLabel) }}
                  </span>
                </td>
              </tr>
              <tr v-if="!trendData.length && !loading">
                <td colspan="3" class="empty-cell">该城市暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="trendData.length > collapsedCount" class="table-foot">
          <button class="expand-btn t-label" @click="expanded = !expanded">
            {{ expanded ? '收起早年记录' : '展开更早纪元' }}
            <span class="material-symbols-outlined">{{ expanded ? 'expand_less' : 'expand_more' }}</span>
          </button>
        </div>
      </div>
    </section>
    </template>

    <!-- 月度分布视图 -->
    <template v-else>
      <section class="monthly-grid">
        <div class="scroll-card ink-border monthly-panel">
          <div class="panel-head">
            <div>
              <h3 class="t-title">{{ filter.city }}年内月度分布</h3>
              <div class="brush-stroke" style="width: 64px; margin-top: 6px"></div>
            </div>
            <span class="t-label">21 年逐月均值</span>
          </div>
          <div ref="seasonalRef" class="monthly-chart"></div>
        </div>

        <div class="scroll-card ink-border monthly-panel">
          <div class="panel-head">
            <div>
              <h3 class="t-title">气候指纹热力图</h3>
              <div class="brush-stroke" style="width: 64px; margin-top: 6px"></div>
            </div>
            <span class="t-label">年份 × 月份</span>
          </div>
          <div ref="heatmapRef" class="monthly-chart"></div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getTrend, getCities, getMonthly, type TrendRow } from '@/api/weather'
import { download } from '@/utils/request'
import { createLatestGuard } from '@/utils/latestOnly'
import { inkPalette, onThemeChange, seriesColor, isDarkMode } from '@/theme/chart'
import { FACTOR_CONFIGS, FACTOR_OPTIONS, ganzhi, type FactorKey } from '@/config/factor'

const cities = ref<string[]>([])
const cityKeyword = ref('')

/** 城市搜索：关键字即时过滤下拉选项 */
const cityOptions = computed(() => {
  const kw = cityKeyword.value.trim()
  if (!kw) return cities.value
  return cities.value.filter(c => c.includes(kw))
})

function filterCity(query: string) {
  cityKeyword.value = query
}

/** 查询前把输入的关键字解析成有效城市名（精确匹配优先，其次包含匹配） */
function resolveCity() {
  const kw = cityKeyword.value.trim()
  if (!kw) return
  const exact = cities.value.find(c => c === kw)
  const hit = exact ?? cities.value.find(c => c.includes(kw))
  if (hit) filter.city = hit
}

const filter = reactive({
  factor: 'rain' as FactorKey,
  city: '福州市'
})

const checkedLines = ref<string[]>([])
const smooth = ref(true)
const expanded = ref(false)
const collapsedCount = 7
const view = ref<'yearly' | 'monthly'>('yearly')

const factorConfig = computed(() => FACTOR_CONFIGS[filter.factor])
// 表头主指标名（如"平均气温"）
const mainMetricName = computed(() => {
  const main = factorConfig.value.mainMetric
  return factorConfig.value.columns.find(c => c.prop === main)?.label ?? factorConfig.value.unitLabel
})
const trendData = ref<TrendRow[]>([])
const monthlyRows = ref<Array<Record<string, number | string>>>([])
const loading = ref(false)
const lastRefreshTime = ref('')
const chartRef = ref<HTMLElement>()
const seasonalRef = ref<HTMLElement>()
const heatmapRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null
let seasonalChart: echarts.ECharts | null = null
let heatmapChart: echarts.ECharts | null = null

/** 月度表主指标列与名称 */
const MONTHLY_METRIC: Record<FactorKey, { col: string; label: string }> = {
  rain: { col: 'total_rain', label: '月降水总量' },
  temp: { col: 'avg_temp', label: '月平均气温' },
  sun: { col: 'total_sun', label: '月日照时数' },
  hum: { col: 'avg_humidity', label: '月平均湿度' },
  wind: { col: 'avg_wind', label: '月平均风速' }
}

// 主指标序列（供统计卡使用）
const mainValues = computed(() =>
  trendData.value.map(d => Number(d[factorConfig.value.mainMetric as keyof TrendRow] ?? 0))
)

const avgValue = computed(() => {
  const vals = mainValues.value
  if (!vals.length) return '—'
  return (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
})

const maxValue = computed(() => {
  if (!mainValues.value.length) return '—'
  return Math.max(...mainValues.value).toFixed(1)
})

const maxYear = computed(() => {
  const vals = mainValues.value
  if (!vals.length) return '—'
  return trendData.value[vals.indexOf(Math.max(...vals))]?.year
})

const minValue = computed(() => {
  if (!mainValues.value.length) return '—'
  return Math.min(...mainValues.value).toFixed(1)
})

const minYear = computed(() => {
  const vals = mainValues.value
  if (!vals.length) return '—'
  return trendData.value[vals.indexOf(Math.min(...vals))]?.year
})

const trendDirection = computed(() => {
  const vals = mainValues.value
  if (vals.length < 2) return 'flat'
  const diff = ((vals[vals.length - 1] - vals[0]) / vals[0]) * 100
  if (Math.abs(diff) < 2) return 'flat'
  return diff > 0 ? 'up' : 'down'
})

const trendLabel = computed(() => {
  if (trendDirection.value === 'up') return '上升'
  if (trendDirection.value === 'down') return '下降'
  return '稳定'
})

const trendColor = computed(() => {
  if (trendDirection.value === 'up') return 'var(--ink-error)'
  if (trendDirection.value === 'down') return 'var(--ink-tertiary-container)'
  return 'var(--ink-text-variant)'
})

// 岁次详录：默认展示最近 7 年，可展开全部
const displayRows = computed(() => {
  const rows = [...trendData.value].sort((a, b) => b.year - a.year)
  return expanded.value ? rows : rows.slice(0, collapsedCount)
})

function formatVal(v: number | string | undefined): string {
  return v === undefined || v === null ? '—' : String(v)
}

function trendClass(label: string): string {
  if (label === '上升') return 'up'
  if (label === '下降') return 'down'
  return 'flat'
}

function trendIcon(label: string): string {
  if (label === '上升') return 'arrow_upward'
  if (label === '下降') return 'arrow_downward'
  return 'horizontal_rule'
}

function trendTagText(label: string): string {
  if (label === '上升') return '升'
  if (label === '下降') return '降'
  return '平'
}

function toggleLine(value: string) {
  checkedLines.value = checkedLines.value.includes(value)
    ? checkedLines.value.filter(v => v !== value)
    : [...checkedLines.value, value]
}

// 快速切换筛选时丢弃过期响应，防止旧数据覆盖新数据
const queryGuard = createLatestGuard()

async function handleQuery() {
  const seq = queryGuard.next()
  loading.value = true
  try {
    const data = await getTrend(filter.factor, filter.city)
    if (queryGuard.isStale(seq)) return
    trendData.value = data
    lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    // 赋新数组必触发 watch(checkedLines)，渲染统一由 watch 驱动，避免双重渲染
    checkedLines.value = factorConfig.value.lineOptions.map(l => l.value)
    // 月度序列一并提供（252 行，开销极小）
    const monthly = await getMonthly(filter.factor, filter.city)
    if (queryGuard.isStale(seq)) return
    monthlyRows.value = monthly
    if (view.value === 'monthly') {
      nextTick(renderMonthly)
    }
  } finally {
    if (!queryGuard.isStale(seq)) {
      loading.value = false
    }
  }
}

function handleExport() {
  download('/export/trend', { factor: filter.factor, city: filter.city },
    `${filter.city}${factorConfig.value.label}趋势_2000-2020.xlsx`)
}

function handleQueryClick() {
  resolveCity()
  handleQuery()
}

function handleReset() {
  // 已在默认状态时 watch 不会触发，需手动查询；否则由 watch 自动查询，避免重复请求
  const atDefault = filter.factor === 'rain' && filter.city === '福州市'
  filter.factor = 'rain'
  filter.city = '福州市'
  if (atDefault) handleQuery()
}

// 要素或城市变化时自动重新查询
watch(filter, handleQuery)

function renderChart() {
  const pal = inkPalette()
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const data = trendData.value
  const config = factorConfig.value

  const series = config.lineOptions
    .filter(line => checkedLines.value.includes(line.value))
    .map(line => ({
      name: line.label,
      type: 'line' as const,
      smooth: smooth.value,
      symbol: 'circle',
      symbolSize: 5,
      data: data.map(d => d[line.value as keyof TrendRow]),
      lineStyle: { width: 2, color: line.color },
      itemStyle: { color: line.color, borderColor: pal.pointBorder, borderWidth: 1 },
      areaStyle:
        config.lineOptions.length > 1
          ? undefined
          : {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: echarts.color.modifyAlpha(line.color, 0.25) },
                { offset: 1, color: echarts.color.modifyAlpha(line.color, 0.02) }
              ])
            }
    }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(28, 28, 17, 0.9)',
      borderWidth: 0,
      textStyle: { color: '#fdfae7', fontFamily: 'Work Sans, PingFang SC, sans-serif' }
    },
    legend: { show: false },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(d => String(d.year)),
      axisLine: { lineStyle: { color: pal.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: `${config.label}（${config.unit}）`,
      nameTextStyle: { color: pal.axisLabel, fontSize: 12 },
      axisLabel: { color: pal.axisLabel, fontSize: 12 },
      splitLine: { lineStyle: { color: pal.splitLine, type: 'dashed' } }
    },
    series
  }

  chartInstance.setOption(option, true)
}

watch(checkedLines, () => {
  nextTick(renderChart)
}, { deep: true })

watch(smooth, () => {
  nextTick(renderChart)
})

// ---------- 月度分布：季节曲线 + 气候指纹热力图 ----------
const MONTHS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

function renderMonthly() {
  renderSeasonal()
  renderHeatmap()
}

function renderSeasonal() {
  const pal = inkPalette()
  if (!seasonalRef.value || !monthlyRows.value.length) return
  seasonalChart?.dispose()
  seasonalChart = echarts.init(seasonalRef.value)

  const metric = MONTHLY_METRIC[filter.factor]
  const color = factorConfig.value.color
  // 每个月份对 21 年求均值
  const avgByMonth = MONTHS.map((_, i) => {
    const mm = String(i + 1).padStart(2, '0')
    const vals = monthlyRows.value
      .filter(r => String(r.year_month).slice(5, 7) === mm)
      .map(r => Number(r[metric.col] ?? 0))
    return vals.length ? +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1) : 0
  })

  seasonalChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(28, 28, 17, 0.9)',
      borderWidth: 0,
      textStyle: { color: '#fdfae7', fontFamily: 'Work Sans, PingFang SC, sans-serif' },
      formatter: (params: unknown) => {
        const arr = Array.isArray(params) ? params : [params]
        const p = arr[0] as { name: string; dataIndex: number; value: number }
        // 补充该月峰值出现的年份，让"多年均值"有年份参照
        const mm = String(p.dataIndex + 1).padStart(2, '0')
        let peakYear = ''
        let peakVal = -Infinity
        for (const r of monthlyRows.value) {
          if (String(r.year_month).slice(5, 7) === mm) {
            const v = Number(r[metric.col] ?? 0)
            if (v > peakVal) {
              peakVal = v
              peakYear = String(r.year_month).slice(0, 4)
            }
          }
        }
        const lines = arr
          .slice(0, 1)
          .map(s => `${metric.label}：<b>${s.value}</b> ${factorConfig.value.unit}`)
        return `${p.name}（21 年均值）<br/>${lines.join('<br/>')}<br/>峰值年份：${peakYear} 年（${peakVal} ${factorConfig.value.unit}）`
      }
    },
    grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: MONTHS,
      axisLine: { lineStyle: { color: pal.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: `${metric.label}（${factorConfig.value.unit}）`,
      nameTextStyle: { color: pal.axisLabel, fontSize: 11 },
      axisLabel: { color: pal.axisLabel, fontSize: 11 },
      splitLine: { lineStyle: { color: pal.splitLine, type: 'dashed' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: '55%',
        data: avgByMonth,
        itemStyle: { color: echarts.color.modifyAlpha(seriesColor(color), 0.8), borderRadius: [2, 2, 0, 0] }
      },
      {
        type: 'line',
        data: avgByMonth,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, color: '#944a27' },
        itemStyle: { color: '#944a27', borderColor: pal.pointBorder, borderWidth: 1 }
      }
    ]
  })
}

function renderHeatmap() {
  const pal = inkPalette()
  if (!heatmapRef.value || !monthlyRows.value.length) return
  heatmapChart?.dispose()
  heatmapChart = echarts.init(heatmapRef.value)

  const metric = MONTHLY_METRIC[filter.factor]
  const color = factorConfig.value.color
  // 行：年份（倒序）；列：1-12 月
  const years = [...new Set(monthlyRows.value.map(r => String(r.year_month).slice(0, 4)))].sort((a, b) => Number(b) - Number(a))
  const lookup = new Map(monthlyRows.value.map(r => [String(r.year_month), Number(r[metric.col] ?? 0)]))
  const data: Array<[number, number, number]> = []
  years.forEach((y, yi) => {
    for (let m = 1; m <= 12; m++) {
      const key = `${y}-${String(m).padStart(2, '0')}`
      data.push([m - 1, yi, lookup.get(key) ?? 0])
    }
  })
  const vals = data.map(d => d[2])

  heatmapChart.setOption({
    tooltip: {
      backgroundColor: 'rgba(28, 28, 17, 0.9)',
      borderWidth: 0,
      textStyle: { color: '#fdfae7', fontFamily: 'Work Sans, PingFang SC, sans-serif' },
      formatter: (p: { value: [number, number, number] }) =>
        `${years[p.value[1]]}年${MONTHS[p.value[0]]}<br/>${metric.label}：<b>${p.value[2]}</b> ${factorConfig.value.unit}`
    },
    grid: { left: 8, right: 76, top: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: MONTHS,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: years,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 11 }
    },
    visualMap: {
      type: 'continuous',
      min: Math.min(...vals),
      max: Math.max(...vals),
      calculable: true,
      orient: 'vertical',
      right: 0,
      top: 'center',
      itemWidth: 12,
      itemHeight: 110,
      textStyle: { color: pal.axisLabel, fontSize: 11 },
      inRange: { color: isDarkMode() ? ['#1b3026', '#33503f', seriesColor(color)] : ['#f7f4e1', '#d8ded9', color] }
    },
    series: [
      {
        type: 'heatmap',
        data,
        itemStyle: { borderColor: pal.pointBorder, borderWidth: 1, borderRadius: 2 },
        emphasis: { itemStyle: { shadowBlur: 4, shadowColor: 'rgba(0,0,0,0.3)' } }
      }
    ]
  })
}

watch(view, v => {
  if (v === 'monthly') {
    nextTick(renderMonthly)
  } else {
    nextTick(renderChart)
  }
})

// 主题切换后按新调色板重绘图表
const offTheme = onThemeChange(() => {
  nextTick(() => (view.value === 'monthly' ? renderMonthly() : renderChart()))
})

const handleResize = () => {
  chartInstance?.resize()
  seasonalChart?.resize()
  heatmapChart?.resize()
}

onMounted(async () => {
  cities.value = await getCities()
  handleQuery()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  offTheme()
  chartInstance?.dispose()
  seasonalChart?.dispose()
  heatmapChart?.dispose()
  chartInstance = null
  seasonalChart = null
  heatmapChart = null
})
</script>

<style scoped lang="scss">
.trend-page {
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

  @media (min-width: 768px) {
    flex-direction: row;
    align-items: flex-end;
  }

  .filter-field {
    flex: 1;
    min-width: 180px;
  }

  /* 可搜索城市下拉：对齐下划线水墨风格 */
  .city-select :deep(.el-select) {
    width: 100%;

    .el-select__wrapper {
      min-height: 38px;
      background: transparent;
      box-shadow: 0 1px 0 0 var(--ink-outline-variant);
      border-radius: 0;
      padding: 2px 8px;

      &:hover,
      &.is-focused {
        box-shadow: 0 1px 0 0 var(--ink-primary);
      }

      .el-select__placeholder,
      .el-select__selected-item {
        color: var(--ink-text);
        font-size: 16px;
      }

      .el-select__caret {
        color: var(--ink-outline);
      }
    }
  }

  .filter-actions {
    display: flex;
    gap: 12px;

    @media (min-width: 768px) {
      margin-left: auto;
    }
  }
}

/* ---------- 统计印卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.stat-card {
  padding: 24px;

  .stat-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;

    .stat-glyph {
      font-size: 20px;
    }
  }

  .stat-value {
    font-family: var(--font-serif);
    font-size: 32px;
    font-weight: 600;
    color: var(--ink-primary);

    .stat-unit {
      font-size: 16px;
      font-weight: 400;
      color: var(--ink-text-variant);
      margin-left: 4px;
    }

    &.stat-city {
      padding-top: 4px;
    }
  }

  .stat-foot {
    margin-top: 14px;
    display: flex;
    align-items: center;
    gap: 4px;

    .material-symbols-outlined {
      font-size: 16px;
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
  align-items: flex-start;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}

.chart-tools {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  .line-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .pill-dot {
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }
  }
}

.chart-box {
  height: 400px;
}

/* ---------- 视图切换 ---------- */
.view-pills {
  display: flex;
  gap: 8px;
}

/* ---------- 月度分布 ---------- */
.monthly-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;

  @media (min-width: 1100px) {
    grid-template-columns: 1fr 1fr;
  }
}

.monthly-panel {
  padding: 24px;
}

.monthly-chart {
  height: 380px;
}

/* ---------- 岁次详录 ---------- */
.table-scroll {
  max-height: 400px;
  overflow-y: auto;
}

.ink-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;

  thead th {
    position: sticky;
    top: 0;
    background: #fff;
    padding: 10px 8px;
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.05em;
    color: var(--ink-text-variant);
    text-align: left;
    border-bottom: 1px solid rgba(193, 200, 195, 0.5);
  }

  tbody td {
    padding: 12px 8px;
    border-bottom: 1px solid rgba(193, 200, 195, 0.15);
  }

  tbody tr:hover {
    background: var(--ink-bg-low);
  }

  .ganzhi {
    font-family: var(--font-serif);
    font-weight: 600;
    color: var(--ink-primary);
  }

  .year {
    margin-left: 4px;
    font-size: 12px;
  }

  .col-num {
    text-align: right;
  }

  .empty-cell {
    text-align: center;
    color: var(--ink-outline);
    padding: 32px 0;
  }
}

.table-foot {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(193, 200, 195, 0.2);
  text-align: center;

  .expand-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    background: none;
    border: none;
    cursor: pointer;
    color: var(--ink-primary);

    .material-symbols-outlined {
      font-size: 16px;
    }

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
