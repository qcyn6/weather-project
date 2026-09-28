import { get } from '@/utils/request'
import type { FactorKey } from '@/config/factor'

/**
 * 排名行通用结构：城市/年份等固定字段 + 各要素指标（totalRain、avgTemp 等）
 * 指标名由 config/factor.ts 的 mainMetric / columns 动态决定
 */
export interface MetricRow {
  [key: string]: number | string | undefined
  year: number
  rankNum: number
  city: string
  province: string
}

export interface RainRankingRow extends MetricRow {
  totalRain: number
  rainyDays: number
}

export interface TrendRow {
  city: string
  year: number
  trendLabel: string
  totalRain?: number
  rainyDays?: number
  avgTemp?: number
  maxTemp?: number
  minTemp?: number
  totalSun?: number
  avgSun?: number
  sunnyDays?: number
  avgHumidity?: number
  maxHumidity?: number
  minHumidity?: number
  highHumDays?: number
  avgWind?: number
  maxWind?: number
  galeDays?: number
}

export interface DashboardSummary {
  latestYear: number | null
  cityCount: number
  yearRange: string
  rain: MetricRow[]
  temp: MetricRow[]
  sun: MetricRow[]
  hum: MetricRow[]
  wind: MetricRow[]
  rainTrend: Array<Record<string, number | string>>
  tempTrend: Array<Record<string, number | string>>
}

export function getRanking(factor: FactorKey, year?: number): Promise<MetricRow[]> {
  return get(`/${factor}/ranking`, year !== undefined ? { year } : undefined)
}

export function getTrend(factor: FactorKey, city: string): Promise<TrendRow[]> {
  return get(`/${factor}/trend`, { city })
}

export function getCities(): Promise<string[]> {
  return get('/common/cities')
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return get('/dashboard/summary')
}

export interface NationalTrendRow {
  year: number
  value: number
}

/** 全国均值逐年演变（21 行聚合），大屏底部图 */
export function getNationalTrend(factor: FactorKey): Promise<NationalTrendRow[]> {
  return get('/dashboard/national-trend', { factor })
}

/** 单城市月度序列（yearMonth + 该要素各指标列） */
export function getMonthly(
  factor: FactorKey,
  city: string
): Promise<Array<Record<string, number | string>>> {
  return get('/monthly', { factor, city })
}
