import { get, postWithTimeout } from '@/utils/request'

export interface GeoValueRow {
  city: string
  province: string
  lat: number
  lon: number
  rank_num: number
  value: number
}

export function getGeoValues(factor: string, year: number): Promise<GeoValueRow[]> {
  return get('/geo/values', { factor, year })
}

/** 省级聚合（按省取城市均值） */
export interface ProvinceValueRow {
  province: string
  value: number
  cities: number
}

export function getProvinceValues(factor: string, year: number): Promise<ProvinceValueRow[]> {
  return get('/geo/province-values', { factor, year })
}

export interface ClusterProfile {
  cluster_id: number
  cluster_name: string
  city_count: number
  avg_rain: number
  avg_temp: number
  avg_sun: number
  avg_hum: number
  avg_wind: number
}

export interface ClusterCityRow {
  city: string
  province: string
  cluster_id: number
  cluster_name: string
  lat: number | null
  lon: number | null
}

export interface ClusterResult {
  clusters: ClusterProfile[]
  cities: ClusterCityRow[]
}

export function getClusterResult(): Promise<ClusterResult> {
  return get('/cluster/result')
}

export function recluster(token: string): Promise<string> {
  // SSH 触发 Spark 作业耗时约 1-3 分钟
  return postWithTimeout<string>('/manage/recluster', null, { token }, 240_000)
}

export interface TableStat {
  table: string
  rows: number
}

export function getManageStats(): Promise<TableStat[]> {
  return get('/manage/stats')
}

export interface OperationLog {
  id: number
  username: string
  action: string
  detail: string
  create_time: string
}

export function getManageLogs(): Promise<OperationLog[]> {
  return get('/manage/logs')
}
