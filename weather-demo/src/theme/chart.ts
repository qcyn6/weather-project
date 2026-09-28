/**
 * 深色水墨主题工具：
 * - applyTheme / initTheme 切换并记忆 html.dark 类
 * - inkPalette 返回当前主题下 ECharts 用的轴色 / 地图色
 * - seriesColor 深色下把要素主色提亮，保证深底可读
 * - onThemeChange 供图表视图监听主题切换后重渲染
 */
import * as echarts from 'echarts'

export const THEME_EVENT = 'ink-theme-change'
const THEME_KEY = 'ink-theme'

export function isDarkMode(): boolean {
  return document.documentElement.classList.contains('dark')
}

export function applyTheme(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
  localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
  window.dispatchEvent(new Event(THEME_EVENT))
}

export function initTheme() {
  if (localStorage.getItem(THEME_KEY) === 'dark') {
    document.documentElement.classList.add('dark')
  }
}

export interface InkPalette {
  axisLabel: string
  axisLine: string
  splitLine: string
  mapArea: string
  mapBorder: string
  mapEmphasis: string
  mapLabel: string
  pointBorder: string
  bar: string
  lineWarm: string
}

export function inkPalette(): InkPalette {
  const dark = isDarkMode()
  return dark
    ? {
        axisLabel: '#8a948a',
        axisLine: 'rgba(138, 148, 138, 0.4)',
        splitLine: 'rgba(138, 148, 138, 0.18)',
        mapArea: '#1b3026',
        mapBorder: 'rgba(138, 148, 138, 0.45)',
        mapEmphasis: '#16281f',
        mapLabel: '#a8ccb6',
        pointBorder: '#12201a',
        bar: 'rgba(122, 158, 138, 0.8)',
        lineWarm: '#e8926a'
      }
    : {
        axisLabel: '#727975',
        axisLine: '#c1c8c3',
        splitLine: 'rgba(193, 200, 195, 0.35)',
        mapArea: '#f1eedb',
        mapBorder: '#a8b3aa',
        mapEmphasis: '#e6e3d0',
        mapLabel: '#143026',
        pointBorder: '#fdfae7',
        bar: 'rgba(43, 70, 60, 0.75)',
        lineWarm: '#944a27'
      }
}

/** 深色下把较深的要素主色提亮（如降水深靛 → 靛青），浅色主题原样返回 */
export function seriesColor(base: string): string {
  return isDarkMode() ? echarts.color.lift(base, 0.4) : base
}

/** 主题变化时重渲染图表；返回取消监听函数 */
export function onThemeChange(cb: () => void): () => void {
  const handler = () => cb()
  window.addEventListener(THEME_EVENT, handler)
  return () => window.removeEventListener(THEME_EVENT, handler)
}
