<template>
  <div class="map-page">
    <!-- 页头 -->
    <header class="page-head">
      <div>
        <h2 class="t-headline page-title">气候地图</h2>
        <p class="page-sub">三百七十一城气象要素地理分布 · 城市气候聚类分区</p>
      </div>
    </header>

    <!-- 筛选栏 -->
    <section class="filter-card scroll-card ink-border">
      <div class="mode-pills">
        <button class="btn-ghost" :class="{ 'is-active': mode === 'factor' }" @click="mode = 'factor'">
          <span class="material-symbols-outlined" style="font-size: 16px">public</span>
          要素分布
        </button>
        <button class="btn-ghost" :class="{ 'is-active': mode === 'cluster' }" @click="mode = 'cluster'">
          <span class="material-symbols-outlined" style="font-size: 16px">bubble_chart</span>
          气候分区
        </button>
      </div>

      <template v-if="mode === 'factor'">
        <div class="field filter-field">
          <label class="field-label">观测要素</label>
          <div class="field-control">
            <select v-model="filter.factor">
              <option v-for="f in FACTOR_OPTIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
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
        <div class="granularity-pills">
          <button class="btn-ghost" :class="{ 'is-active': granularity === 'city' }" @click="granularity = 'city'">城市分布</button>
          <button class="btn-ghost" :class="{ 'is-active': granularity === 'province' }" @click="granularity = 'province'">省级聚合</button>
        </div>
      </template>
      <div v-else class="cluster-note t-label">基于 Spark KMeans 聚类的五要素城市气候分区（371 城）</div>
    </section>

    <!-- 主体：地图 + 侧栏 -->
    <section class="map-grid">
      <div class="map-card scroll-card ink-border">
        <div class="panel-head">
          <h3 class="t-title">{{ mapTitle }}</h3>
          <span class="t-label">{{ sideCount }}</span>
        </div>
        <div v-loading="loading" class="chart-box" ref="mapRef"></div>
      </div>

      <div class="side-panel scroll-card ink-border">
        <!-- 要素分布：Top10 -->
        <template v-if="mode === 'factor'">
          <div class="panel-head">
            <h3 class="t-title">{{ factorConfig.label }}{{ granularity === 'province' ? '省级均值榜' : '极值榜' }}</h3>
            <span class="t-label">TOP 10</span>
          </div>
          <div class="rank-list">
            <div
              v-for="(row, idx) in sideList"
              :key="row.name"
              class="rank-item"
              @click="row.kind === 'city' && showCityTrend(row.name)"
            >
              <div class="rank-badge" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</div>
              <span class="rank-city">{{ row.name }}</span>
              <span v-if="row.sub" class="t-label">{{ row.sub }}</span>
              <span class="rank-value t-num">{{ formatNum(row.value) }}</span>
            </div>
          </div>
        </template>

        <!-- 气候分区：簇画像 -->
        <template v-else>
          <div class="panel-head">
            <h3 class="t-title">气候区画像</h3>
            <span class="t-label">五要素均值</span>
          </div>
          <div class="cluster-list">
            <div v-for="c in clusters" :key="c.cluster_id" class="cluster-card ink-border">
              <div class="cluster-head">
                <i class="cluster-dot" :style="{ background: CLUSTER_COLORS[c.cluster_id % CLUSTER_COLORS.length] }"></i>
                <span class="cluster-name">{{ c.cluster_name }}</span>
                <span class="t-label">{{ c.city_count }} 城</span>
              </div>
              <div class="cluster-stats t-label">
                <span>降水 {{ formatNum(c.avg_rain) }}mm</span>
                <span>气温 {{ formatNum(c.avg_temp) }}°C</span>
                <span>湿度 {{ formatNum(c.avg_hum) }}%</span>
                <span>风速 {{ formatNum(c.avg_wind) }}m/s</span>
              </div>
            </div>
          </div>
        </template>
        <p class="panel-tip t-label">点击地图上的城市可查看 21 年趋势</p>
      </div>
    </section>

    <!-- 城市趋势弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogCity + ' 21 年趋势'" width="640px">
      <div v-loading="trendLoading" class="dialog-chart" ref="dialogChartRef"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getGeoValues, getProvinceValues, type GeoValueRow, type ProvinceValueRow } from '@/api/analysis'
import { getClusterResult, type ClusterProfile, type ClusterCityRow } from '@/api/analysis'
import { inkPalette, onThemeChange } from '@/theme/chart'
import { getTrend, type TrendRow } from '@/api/weather'
import { FACTOR_CONFIGS, FACTOR_OPTIONS, YEAR_OPTIONS, ganzhi, shortName, type FactorKey } from '@/config/factor'
import { ElMessage } from 'element-plus'

const mode = ref<'factor' | 'cluster'>('factor')
const granularity = ref<'city' | 'province'>('city')
const filter = reactive({ factor: 'rain' as FactorKey, year: 2020 })
const factorConfig = computed(() => FACTOR_CONFIGS[filter.factor])
const loading = ref(false)

const geoRows = ref<GeoValueRow[]>([])
const provinceRows = ref<ProvinceValueRow[]>([])
const clusters = ref<ClusterProfile[]>([])
const clusterCities = ref<ClusterCityRow[]>([])

const mapRef = ref<HTMLElement>()
let mapChart: echarts.ECharts | null = null

// 水墨系五色
const CLUSTER_COLORS = ['#2b463c', '#944a27', '#1f4461', '#8f6a1d', '#486459']

const mapTitle = computed(() =>
  mode.value === 'factor'
    ? `${filter.year}年 ${factorConfig.value.label}${granularity.value === 'province' ? '省级聚合' : '地理格局'}`
    : '城市气候分区（KMeans, K=5）'
)

const sideCount = computed(() =>
  mode.value === 'factor'
    ? granularity.value === 'province'
      ? `${provinceRows.value.length} 省已聚合`
      : `${geoRows.value.length} 城已定位`
    : `${clusterCities.value.length} 城`
)

const topTen = computed(() => [...geoRows.value].slice(0, 10))

/** 侧栏榜单：城市模式取城市 Top10，省级模式取省份均值 Top10 */
const sideList = computed(() => {
  if (granularity.value === 'province') {
    return provinceRows.value.slice(0, 10).map(p => ({
      kind: 'province' as const,
      name: p.province,
      value: Number(p.value),
      sub: `${p.cities} 城`
    }))
  }
  return geoRows.value.slice(0, 10).map(r => ({
    kind: 'city' as const,
    name: r.city,
    value: Number(r.value),
    sub: ''
  }))
})

function formatNum(v: number | null | undefined): string {
  return v === null || v === undefined ? '—' : String(v)
}

// 快速切换筛选时丢弃过期响应
let seq = 0
async function loadFactorMode() {
  const mySeq = ++seq
  loading.value = true
  try {
    if (granularity.value === 'province') {
      const rows = await getProvinceValues(filter.factor, filter.year)
      if (mySeq !== seq) return
      provinceRows.value = rows
    } else {
      const data = await getGeoValues(filter.factor, filter.year)
      if (mySeq !== seq) return
      geoRows.value = data
    }
    nextTick(renderFactorMap)
  } finally {
    if (mySeq === seq) loading.value = false
  }
}

/** 省级图层：整省着色，避免 371 个城市点互相遮挡 */
function renderProvinceLayer(chart: echarts.ECharts) {
  const pal = inkPalette()
  const config = factorConfig.value
  const vals = provinceRows.value.map(r => Number(r.value)).filter(v => !Number.isNaN(v))
  if (!vals.length) return

  chart.setOption({
    tooltip: {
      formatter: (p: { name: string }) => {
        const row = provinceRows.value.find(r => r.province === p.name)
        if (!row) return p.name
        return `${p.name}<br/>${config.unitLabel}（省内均值）：<b>${row.value}</b> ${config.unit}<br/>样本城市：${row.cities} 城`
      }
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
      inRange: { color: [pal.mapArea, '#d8ded9', config.color] }
    },
    series: [
      {
        name: config.unitLabel,
        type: 'map',
        map: 'china',
        roam: true,
        data: provinceRows.value.map(r => ({ name: r.province, value: Number(r.value) })),
        label: { show: false },
        itemStyle: { areaColor: pal.mapArea, borderColor: pal.mapBorder, borderWidth: 0.6 },
        emphasis: { label: { show: true, color: pal.mapLabel }, itemStyle: { areaColor: pal.mapEmphasis } }
      }
    ]
  }, true)
}

async function loadClusterMode() {
  loading.value = true
  try {
    const result = await getClusterResult()
    clusters.value = result.clusters
    clusterCities.value = result.cities
    nextTick(renderClusterMap)
  } finally {
    loading.value = false
  }
}

async function ensureMap() {
  if (mapChart) return mapChart
  if (!mapRef.value) return null
  const geoJson = await fetch('/china.json').then(r => r.json())
  echarts.registerMap('china', geoJson as Parameters<typeof echarts.registerMap>[1])
  mapChart = echarts.init(mapRef.value)
  mapChart.on('click', params => {
    // 省级图层（type=map）点击不弹城市趋势
    if (params.componentType === 'series' && params.seriesType !== 'map' && params.name) {
      showCityTrend(String(params.name))
    }
  })
  return mapChart
}

// 主题切换后按新调色板重绘地图
const offTheme = onThemeChange(() => {
  nextTick(() => (mode.value === 'cluster' ? renderClusterMap() : renderFactorMap()))
})

async function renderFactorMap() {
  const pal = inkPalette()
  const chart = await ensureMap()
  if (!chart) return
  if (granularity.value === 'province') {
    renderProvinceLayer(chart)
    return
  }
  const vals = geoRows.value.map(r => Number(r.value)).filter(v => !Number.isNaN(v))
  if (!vals.length) return
  const color = factorConfig.value.color
  const top10 = new Set(topTen.value.map(r => r.city))

  chart.setOption({
    tooltip: {
      formatter: (p: { name: string; value: [number, number, number] }) =>
        `${p.name}<br/>${factorConfig.value.unitLabel}：<b>${p.value[2]}</b> ${factorConfig.value.unit}`
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
      inRange: { color: ['#ece9d6', '#f7f4e1', color] }
    },
    geo: {
      map: 'china',
      roam: true,
      itemStyle: { areaColor: pal.mapArea, borderColor: pal.mapBorder, borderWidth: 0.6 },
      emphasis: { label: { show: false }, itemStyle: { areaColor: pal.mapEmphasis } }
    },
    series: [
      {
        name: factorConfig.value.unitLabel,
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
        rippleEffect: { brushType: 'stroke', scale: 2.6 },
        data: geoRows.value
          .filter(r => top10.has(r.city))
          .map(r => ({ name: r.city, value: [Number(r.lon), Number(r.lat), Number(r.value)] })),
        itemStyle: { color: '#944a27' },
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

async function renderClusterMap() {
  const pal = inkPalette()
  const chart = await ensureMap()
  if (!chart) return

  const series = clusters.value.map((c, i) => ({
    name: c.cluster_name,
    type: 'scatter' as const,
    coordinateSystem: 'geo',
    symbolSize: 8,
    data: clusterCities.value
      .filter(city => city.cluster_id === c.cluster_id && city.lat !== null && city.lon !== null)
      .map(city => ({ name: city.city, value: [Number(city.lon), Number(city.lat), c.cluster_id] })),
    itemStyle: { color: CLUSTER_COLORS[i % CLUSTER_COLORS.length], opacity: 0.85, borderColor: pal.pointBorder, borderWidth: 0.5 }
  }))

  chart.setOption({
    tooltip: {
      formatter: (p: { seriesName: string; name: string }) => `${p.name}<br/>气候区：<b>${p.seriesName}</b>`
    },
    legend: {
      data: clusters.value.map(c => c.cluster_name),
      bottom: 4,
      textStyle: { color: pal.axisLabel, fontSize: 12, fontFamily: 'Work Sans, PingFang SC, sans-serif' },
      itemWidth: 12,
      itemHeight: 8
    },
    geo: {
      map: 'china',
      roam: true,
      itemStyle: { areaColor: pal.mapArea, borderColor: pal.mapBorder, borderWidth: 0.6 },
      emphasis: { label: { show: false }, itemStyle: { areaColor: pal.mapEmphasis } }
    },
    series
  }, true)
}

watch([mode], () => {
  if (mode.value === 'factor') loadFactorMode()
  else loadClusterMode()
})

watch([() => filter.factor, () => filter.year], () => {
  if (mode.value === 'factor') loadFactorMode()
})

watch(granularity, () => {
  if (mode.value === 'factor') loadFactorMode()
})

// ---------- 城市趋势弹窗 ----------
const dialogVisible = ref(false)
const dialogCity = ref('')
const trendLoading = ref(false)
const dialogChartRef = ref<HTMLElement>()
let dialogChart: echarts.ECharts | null = null
let trendSeq = 0

async function showCityTrend(city: string) {
  dialogCity.value = city
  dialogVisible.value = true
  const mySeq = ++trendSeq
  trendLoading.value = true
  try {
    const rows: TrendRow[] = await getTrend(filter.factor, city)
    if (mySeq !== trendSeq || !dialogVisible.value) return
    await nextTick()
    if (!dialogChartRef.value) return
    dialogChart?.dispose()
    dialogChart = echarts.init(dialogChartRef.value)
    const config = FACTOR_CONFIGS[filter.factor]
    const seriesData = config.columns.map((col, i) => ({
      name: col.label,
      type: 'line' as const,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: rows.map(r => r[col.prop as keyof TrendRow]),
      lineStyle: { width: 2, color: config.lineOptions[i % config.lineOptions.length]?.color ?? config.color },
      itemStyle: { color: config.lineOptions[i % config.lineOptions.length]?.color ?? config.color }
    }))
    dialogChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0, textStyle: { fontSize: 11, color: inkPalette().axisLabel } },
      grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: rows.map(r => r.year),
        axisLine: { lineStyle: { color: inkPalette().axisLine } },
        axisLabel: { color: inkPalette().axisLabel, fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        name: config.unit,
        axisLabel: { color: inkPalette().axisLabel, fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(193,200,195,0.35)', type: 'dashed' } }
      },
      series: seriesData
    })
  } catch {
    ElMessage.error('获取城市趋势失败')
  } finally {
    if (mySeq === trendSeq) trendLoading.value = false
  }
}

const handleResize = () => {
  mapChart?.resize()
  dialogChart?.resize()
}

onMounted(() => {
  loadFactorMode()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  offTheme()
  mapChart?.dispose()
  dialogChart?.dispose()
  mapChart = null
  dialogChart = null
})
</script>

<style scoped lang="scss">
.map-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-head {
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

.filter-card {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 20px;
  padding: 24px;

  .mode-pills {
    display: flex;
    gap: 8px;
  }

  .filter-field {
    min-width: 160px;
  }

  .cluster-note {
    align-self: center;
    color: var(--ink-outline);
  }

  .granularity-pills {
    display: flex;
    gap: 8px;
    align-self: center;
  }
}

.map-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;

  @media (min-width: 1100px) {
    grid-template-columns: 2fr 1fr;
  }
}

.map-card {
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

.chart-box {
  height: 480px;
}

.side-panel {
  padding: 24px;

  .panel-tip {
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid rgba(193, 200, 195, 0.2);
    color: var(--ink-outline);
    text-align: center;
  }
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .rank-item {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;

    &:hover .rank-city {
      color: var(--ink-primary);
    }

    .rank-badge {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 700;
      color: var(--ink-outline);

      &.rank-1 {
        background: rgba(255, 215, 0, 0.2);
        color: #b8860b;
        border: 1px solid rgba(255, 215, 0, 0.5);
      }
      &.rank-2 {
        background: rgba(192, 192, 192, 0.2);
        color: #708090;
        border: 1px solid rgba(192, 192, 192, 0.5);
      }
      &.rank-3 {
        background: rgba(205, 127, 50, 0.15);
        color: #8b4513;
        border: 1px solid rgba(205, 127, 50, 0.4);
      }
    }

    .rank-city {
      flex: 1;
      font-size: 14px;
      color: var(--ink-text);
    }

    .rank-value {
      font-weight: 600;
      color: var(--ink-primary);
    }
  }
}

.cluster-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .cluster-card {
    padding: 14px;
    border-radius: 6px;

    .cluster-head {
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
        font-weight: 600;
        color: var(--ink-primary);
      }
    }

    .cluster-stats {
      display: flex;
      flex-wrap: wrap;
      gap: 4px 12px;
      color: var(--ink-text-variant);
    }
  }
}

.dialog-chart {
  height: 320px;
}
</style>
