<template>
  <div class="dash-page">
    <!-- 页头 -->
    <header class="dash-head">
      <div>
        <h2 class="t-headline page-title">全国气象态势总览</h2>
        <p class="page-sub">三百七十一城 · 廿一载气象要素全景</p>
      </div>
      <div class="head-actions">
        <button class="btn btn-outline" :class="{ 'is-active': carousel }" @click="toggleCarousel">
          <span class="material-symbols-outlined">autorenew</span>
          {{ carousel ? '停止轮播' : '自动轮播' }}
        </button>
        <div class="head-summary">
          <div class="summary-item">
            <span class="t-label">监测周期</span>
            <span class="t-title t-num">{{ data.yearRange || '—' }}</span>
          </div>
          <div class="summary-item">
            <span class="t-label">覆盖城市</span>
            <span class="t-title t-num">{{ data.cityCount || '—' }}</span>
          </div>
          <div class="summary-item">
            <span class="t-label">统计年份</span>
            <span class="t-title t-num">{{ data.latestYear ?? '—' }}</span>
          </div>
          <div class="summary-item">
            <span class="t-label">当前时间</span>
            <span class="t-title t-num">{{ currentTime }}</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 自动结论条 -->
    <div v-if="conclusion" class="conclusion-bar scroll-card ink-border">
      <span class="material-symbols-outlined" style="color: var(--ink-secondary)">insights</span>
      <span class="conclusion-text">{{ conclusion }}</span>
    </div>

    <!-- 三栏 + 底部趋势 -->
    <div class="dash-grid">
      <!-- 左：要素极值栏 -->
      <aside class="col-extremes scroll-card ink-border">
        <div class="panel-head">
          <h3 class="t-title">{{ data.latestYear ?? '—' }}年极值速览</h3>
        </div>
        <div v-for="e in extremes" :key="e.key" class="extreme-row">
          <span class="material-symbols-outlined" :style="{ color: e.color }">{{ e.icon }}</span>
          <div class="extreme-info">
            <div class="t-label">{{ e.label }}</div>
            <div class="extreme-val">
              <b class="t-num">{{ e.value }}</b>
              <span class="t-label">{{ e.unit }}</span>
            </div>
          </div>
          <div class="extreme-city">{{ e.city }}</div>
        </div>
      </aside>

      <!-- 中：中国地图主图 -->
      <section class="map-card scroll-card ink-border">
        <div class="panel-head">
          <div>
            <h3 class="t-title">{{ data.latestYear ?? '—' }}年 {{ factorLabel }}地理格局</h3>
            <div class="brush-stroke" style="width: 64px; margin-top: 6px"></div>
          </div>
          <div class="factor-pills">
            <button
              v-for="f in factorList"
              :key="f.key"
              class="btn-ghost factor-pill"
              :class="{ 'is-active': mapFactor === f.key }"
              @click="switchFactor(f.key)"
            >
              <i class="pill-dot" :style="{ background: f.color }"></i>
              {{ f.label }}
            </button>
          </div>
        </div>
        <div v-loading="mapLoading" ref="mapRef" class="map-box"></div>
      </section>

      <!-- 右：气候分区画像 -->
      <aside class="col-cluster">
        <div class="cluster-head-row">
          <h3 class="t-title">气候分区画像</h3>
          <span class="t-label">KMeans · K=5</span>
        </div>
        <div v-for="(c, i) in clusters" :key="c.cluster_id" class="cluster-card scroll-card ink-border">
          <div class="cluster-top">
            <i class="cluster-dot" :style="{ background: CLUSTER_COLORS[i % CLUSTER_COLORS.length] }"></i>
            <span class="cluster-name">{{ c.cluster_name }}</span>
            <span class="t-label">{{ c.city_count }} 城</span>
          </div>
          <div class="cluster-stats t-label">
            <span>降水 {{ formatNum(c.avg_rain) }}mm</span>
            <span>均温 {{ formatNum(c.avg_temp) }}°C</span>
            <span>湿度 {{ formatNum(c.avg_hum) }}%</span>
          </div>
        </div>
      </aside>

      <!-- 底部：全国演变图（随要素联动） -->
      <section class="trend-card scroll-card ink-border">
        <div class="panel-head">
          <div>
            <h3 class="t-title">廿一年全国{{ factorLabel }}演变</h3>
            <div class="brush-stroke" style="width: 64px; margin-top: 6px"></div>
          </div>
          <div class="legend-pills t-label">
            <span><i class="lg-bar"></i>全国平均{{ factorLabel }}</span>
            <template v-if="mapFactor === 'rain'">
              <span><i class="lg-line"></i>全国平均气温</span>
            </template>
          </div>
        </div>
        <div ref="trendRef" class="trend-box"></div>
      </section>
    </div>

    <!-- 加载失败兜底 -->
    <div v-if="loadFailed" class="dash-error">
      <span class="material-symbols-outlined" style="font-size: 40px">cloud_off</span>
      <p>大屏数据加载失败，请检查后端服务是否启动</p>
      <button class="btn btn-primary" @click="loadAll">重新加载</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummary, getNationalTrend, type DashboardSummary, type NationalTrendRow } from '@/api/weather'
import { getGeoValues, getClusterResult, type GeoValueRow, type ClusterProfile } from '@/api/analysis'
import { FACTOR_CONFIGS, shortName, type FactorKey } from '@/config/factor'
import { inkPalette, onThemeChange } from '@/theme/chart'

const data = reactive<DashboardSummary>({
  latestYear: null,
  cityCount: 0,
  yearRange: '-',
  rain: [],
  temp: [],
  sun: [],
  hum: [],
  wind: [],
  rainTrend: [],
  tempTrend: []
})

const clusters = ref<ClusterProfile[]>([])
const loadFailed = ref(false)
const currentTime = ref('')
let clockTimer: number | undefined

// ---------- 地图（主图） ----------
const mapFactor = ref<FactorKey>('rain')
const geoRows = ref<GeoValueRow[]>([])
const mapLoading = ref(false)
const mapRef = ref<HTMLElement>()
let mapChart: echarts.ECharts | null = null
let mapSeq = 0

const CLUSTER_COLORS = ['#2b463c', '#944a27', '#1f4461', '#8f6a1d', '#486459']

const factorList = (Object.keys(FACTOR_CONFIGS) as FactorKey[]).map(key => {
  const cfg = FACTOR_CONFIGS[key]
  return {
    key,
    label: { rain: '降水', temp: '气温', sun: '日照', hum: '湿度', wind: '风速' }[key],
    unit: cfg.unit,
    unitLabel: cfg.unitLabel,
    mainMetric: cfg.mainMetric,
    icon: cfg.icon,
    color: cfg.color
  }
})

const factorLabel = computed(() => factorList.find(f => f.key === mapFactor.value)?.label ?? '')

const extremes = computed(() =>
  factorList.map(f => {
    const list = data[f.key]
    if (!list.length) return { ...f, city: '—', value: '—' as string | number }
    const maxCity = list.reduce((a, b) => ((a[f.mainMetric] as number) > (b[f.mainMetric] as number) ? a : b))
    return { ...f, city: shortName(String(maxCity.city)), value: maxCity[f.mainMetric] as number }
  })
)

// 自动结论：降水榜首 + 廿一年均温变化
function nationalAvg(row: Record<string, number | string>): number {
  const vals = Object.keys(row)
    .filter(k => k !== 'year')
    .map(k => Number(row[k]))
    .filter(v => !Number.isNaN(v))
  return vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 0
}

const conclusion = computed(() => {
  const top = data.rain[0]
  if (!top) return ''
  let tempPart = ''
  if (data.tempTrend.length >= 2) {
    const first = nationalAvg(data.tempTrend[0])
    const last = nationalAvg(data.tempTrend[data.tempTrend.length - 1])
    const delta = last - first
    const firstYear = data.tempTrend[0].year
    tempPart = `；全国均温较 ${firstYear} 年${delta >= 0 ? '上升' : '下降'} ${Math.abs(delta).toFixed(2)}℃`
  }
  return `${data.latestYear}年，${shortName(String(top.city))}以 ${top.totalRain}mm 降水量居全国之首${tempPart}`
})

function formatNum(v: number | null | undefined): string {
  return v === null || v === undefined ? '—' : String(v)
}

async function switchFactor(key: FactorKey) {
  mapFactor.value = key
  await loadGeo()
  await loadNationalTrend()
}

// ---------- 自动轮播：每 8 秒切换要素，答辩演示用 ----------
const carousel = ref(false)
let carouselTimer: number | undefined
let carouselIndex = 0

function toggleCarousel() {
  carousel.value = !carousel.value
  if (carousel.value) {
    carouselTimer = window.setInterval(() => {
      carouselIndex = (carouselIndex + 1) % factorList.length
      switchFactor(factorList[carouselIndex].key)
    }, 8000)
  } else {
    window.clearInterval(carouselTimer)
  }
}

async function loadGeo() {
  const my = ++mapSeq
  mapLoading.value = true
  try {
    const rows = await getGeoValues(mapFactor.value, data.latestYear ?? 2020)
    if (my !== mapSeq) return
    geoRows.value = rows
    await nextTick()
    renderMap()
  } finally {
    if (my === mapSeq) mapLoading.value = false
  }
}

async function ensureMap() {
  if (mapChart) return mapChart
  if (!mapRef.value) return null
  const geoJson = await fetch('/china.json').then(r => r.json())
  echarts.registerMap('china', geoJson as Parameters<typeof echarts.registerMap>[1])
  mapChart = echarts.init(mapRef.value)
  return mapChart
}

async function renderMap() {
  const pal = inkPalette()
  const chart = await ensureMap()
  if (!chart) return
  const config = FACTOR_CONFIGS[mapFactor.value]
  const vals = geoRows.value.map(r => Number(r.value)).filter(v => !Number.isNaN(v))
  if (!vals.length) return
  const top10 = geoRows.value.slice(0, 10)

  chart.setOption({
    tooltip: {
      formatter: (p: { name: string; value: [number, number, number] }) =>
        `${p.name}<br/>${config.unitLabel}：<b>${p.value[2]}</b> ${config.unit}`
    },
    visualMap: {
      type: 'continuous',
      min: Math.min(...vals),
      max: Math.max(...vals),
      seriesIndex: 0,
      calculable: true,
      left: 12,
      bottom: 8,
      text: ['高', '低'],
      textStyle: { color: pal.axisLabel, fontSize: 11 },
      inRange: { color: ['#ece9d6', '#f7f4e1', config.color] }
    },
    geo: {
      map: 'china',
      roam: true,
      itemStyle: { areaColor: pal.mapArea, borderColor: pal.mapBorder, borderWidth: 0.6 },
      emphasis: { label: { show: false }, itemStyle: { areaColor: pal.mapEmphasis } }
    },
    series: [
      {
        name: config.unitLabel,
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: 7,
        data: geoRows.value.map(r => ({
          name: r.city,
          value: [Number(r.lon), Number(r.lat), Number(r.value)]
        })),
        itemStyle: { opacity: 0.85, borderColor: pal.pointBorder, borderWidth: 0.5 }
      },
      {
        name: 'Top10',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        symbolSize: 11,
        rippleEffect: { brushType: 'stroke', scale: 2.4 },
        data: top10.map(r => ({ name: r.city, value: [Number(r.lon), Number(r.lat), Number(r.value)] })),
        itemStyle: { color: pal.lineWarm },
        label: {
          show: true,
          formatter: (p: { name: string }) => shortName(p.name),
          position: 'right',
          color: pal.mapLabel,
          fontSize: 11,
          fontFamily: 'Noto Serif SC, serif'
        },
        zlevel: 2
      }
    ]
  }, true)
}

// ---------- 底部：全国演变图（随要素联动） ----------
const trendRef = ref<HTMLElement>()
const nationalTrend = ref<NationalTrendRow[]>([])
let trendChart: echarts.ECharts | null = null
let trendSeq = 0

async function loadNationalTrend() {
  const my = ++trendSeq
  try {
    const rows = await getNationalTrend(mapFactor.value)
    if (my !== trendSeq) return
    nationalTrend.value = rows
    renderTrend()
  } catch {
    // 保持上一份趋势数据
  }
}

function renderTrend() {
  const pal = inkPalette()
  if (!trendRef.value || !nationalTrend.value.length) return
  trendChart?.dispose()
  trendChart = echarts.init(trendRef.value)

  const config = FACTOR_CONFIGS[mapFactor.value]
  const years = nationalTrend.value.map(r => String(r.year))
  const values = nationalTrend.value.map(r => r.value)
  const isRain = mapFactor.value === 'rain'

  // 降水模式保留"均温折线"的对照叙事，其余要素单序列
  const series: echarts.SeriesOption[] = [
    {
      name: `全国平均${factorLabel.value}`,
      type: 'bar',
      barWidth: '45%',
      data: values,
      itemStyle: { color: pal.bar, borderRadius: [2, 2, 0, 0] }
    }
  ]
  const yAxis: echarts.YAXisComponentOption[] = [
    {
      type: 'value',
      name: `${config.label} (${config.unit})`,
      nameTextStyle: { color: pal.axisLabel, fontSize: 11 },
      axisLabel: { color: pal.axisLabel, fontSize: 11 },
      splitLine: { lineStyle: { color: pal.splitLine, type: 'dashed' } }
    }
  ]

  if (isRain && data.tempTrend.length) {
    yAxis.push({
      type: 'value',
      name: '均温 (°C)',
      nameTextStyle: { color: pal.axisLabel, fontSize: 11 },
      axisLabel: { color: pal.axisLabel, fontSize: 11 },
      splitLine: { show: false }
    })
    series.push({
      name: '全国平均气温',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      data: data.tempTrend.map(r => +nationalAvg(r).toFixed(2)),
      lineStyle: { width: 2.5, color: pal.lineWarm },
      itemStyle: { color: pal.lineWarm, borderColor: pal.pointBorder, borderWidth: 1 }
    })
  }

  // 各序列的单位映射（降水 mm / 均温 °C），避免提示框单位错乱
  const seriesUnits: Record<string, string> = {
    [`全国平均${factorLabel.value}`]: config.unit,
    全国平均气温: '°C'
  }

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(28, 28, 17, 0.9)',
      borderWidth: 0,
      textStyle: { color: '#fdfae7', fontFamily: 'Work Sans, PingFang SC, sans-serif' },
      formatter: (params: unknown) => {
        const arr = Array.isArray(params) ? params : [params]
        const list = arr as Array<{ name: string; seriesName: string; marker: string; value: number }>
        const title = list[0]?.name ?? ''
        const lines = list.map(
          p => `${p.marker} ${p.seriesName}　<b>${p.value}</b> ${seriesUnits[p.seriesName] ?? ''}`
        )
        return `${title}<br/>${lines.join('<br/>')}`
      }
    },
    legend: { show: false },
    grid: { left: 8, right: isRain ? 12 : 20, top: 32, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: years,
      axisLine: { lineStyle: { color: pal.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: pal.axisLabel, fontSize: 12 }
    },
    yAxis,
    series
  }, true)
}

// ---------- 加载 ----------
async function loadAll() {
  loadFailed.value = false
  try {
    Object.assign(data, await getDashboardSummary())
    await nextTick()
    loadNationalTrend()
    loadGeo()
    getClusterResult()
      .then(result => {
        clusters.value = result.clusters
      })
      .catch(() => {
        /* 气候区画像加载失败不影响主图 */
      })
  } catch {
    loadFailed.value = true
  }
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const handleResize = () => {
  mapChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  updateTime()
  clockTimer = window.setInterval(updateTime, 1000)
  loadAll()
  window.addEventListener('resize', handleResize)
})

// 主题切换后按新调色板重绘地图与趋势图
const offTheme = onThemeChange(() => {
  nextTick(() => {
    renderMap()
    renderTrend()
  })
})

onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  if (carouselTimer) {
    window.clearInterval(carouselTimer)
  }
  window.removeEventListener('resize', handleResize)
  offTheme()
  mapChart?.dispose()
  trendChart?.dispose()
  mapChart = null
  trendChart = null
})
</script>

<style scoped lang="scss">
.dash-page {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
}

/* ---------- 页头 ---------- */
.dash-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
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
    gap: 24px;
    flex-wrap: wrap;
  }

  .head-summary {
    display: flex;
    gap: 32px;

    .summary-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .t-title {
        font-size: 18px;
      }
    }
  }
}

.btn-outline.is-active {
  background: var(--ink-primary-container);
  color: #fff;
  border-color: var(--ink-primary-container);

  &:hover:not(:disabled) {
    background: var(--ink-primary);
  }
}

/* ---------- 结论条 ---------- */
.conclusion-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;

  .conclusion-text {
    font-family: var(--font-serif);
    font-size: 16px;
    font-weight: 600;
    color: var(--ink-primary);
  }
}

/* ---------- 网格 ---------- */
.dash-grid {
  display: grid;
  grid-template-columns: 230px 1fr 250px;
  grid-template-areas:
    'extremes map cluster'
    'trend trend trend';
  gap: 20px;
  align-items: stretch;

  @media (max-width: 1100px) {
    grid-template-columns: 1fr;
    grid-template-areas:
      'map'
      'extremes'
      'cluster'
      'trend';
  }
}

.col-extremes {
  grid-area: extremes;
  padding: 20px;
}

.map-card {
  grid-area: map;
  padding: 20px;
}

.col-cluster {
  grid-area: cluster;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-card {
  grid-area: trend;
  padding: 20px;
}

/* ---------- 极值栏 ---------- */
.extreme-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(193, 200, 195, 0.2);

  &:last-child {
    border-bottom: none;
  }

  .material-symbols-outlined {
    font-size: 22px;
  }

  .extreme-info {
    flex: 1;

    .extreme-val {
      margin-top: 2px;

      b {
        font-size: 20px;
        font-weight: 700;
        color: var(--ink-primary);
      }

      span {
        margin-left: 4px;
      }
    }
  }

  .extreme-city {
    font-family: var(--font-serif);
    font-size: 15px;
    font-weight: 600;
    color: var(--ink-text-variant);
    text-align: right;
  }
}

/* ---------- 地图卡 ---------- */
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.factor-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  .factor-pill {
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

.map-box {
  height: 500px;
}

/* ---------- 气候区画像 ---------- */
.cluster-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 2px;

  .t-label {
    color: var(--ink-outline);
  }
}

.cluster-card {
  padding: 14px;

  .cluster-top {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .cluster-dot {
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }

    .cluster-name {
      flex: 1;
      font-family: var(--font-serif);
      font-size: 15px;
      font-weight: 600;
      color: var(--ink-primary);
    }
  }

  .cluster-stats {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 10px;
    color: var(--ink-text-variant);
  }
}

/* ---------- 底部趋势 ---------- */
.legend-pills {
  display: flex;
  gap: 16px;
  align-items: center;
  color: var(--ink-text-variant);

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .lg-bar {
    width: 12px;
    height: 10px;
    background: rgba(43, 70, 60, 0.75);
    border-radius: 2px;
  }

  .lg-line {
    width: 14px;
    height: 2.5px;
    background: #944a27;
    border-radius: 2px;
  }
}

.trend-box {
  height: 260px;
}

/* ---------- 加载失败兜底 ---------- */
.dash-error {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  background: rgba(253, 250, 231, 0.95);
  color: #94a3b8;

  p {
    margin: 0;
    font-size: 15px;
  }
}
</style>
