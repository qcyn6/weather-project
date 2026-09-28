export type FactorKey = 'rain' | 'temp' | 'sun' | 'hum' | 'wind'

export interface FactorColumn {
  prop: string
  label: string
  unit?: string
}

export interface FactorLine {
  value: string
  label: string
  color: string
}

export interface FactorConfig {
  key: FactorKey
  label: string
  unit: string
  unitLabel: string
  /** 水墨主题主色（宣纸底上可读） */
  color: string
  /** Material Symbols 字形名 */
  icon: string
  mainMetric: string
  columns: FactorColumn[]
  lineOptions: FactorLine[]
}

export const FACTOR_CONFIGS: Record<FactorKey, FactorConfig> = {
  rain: {
    key: 'rain',
    label: '降水',
    unit: 'mm',
    unitLabel: '降水量',
    color: '#1f4461',
    icon: 'water_drop',
    mainMetric: 'totalRain',
    columns: [
      { prop: 'totalRain', label: '降水总量', unit: 'mm' },
      { prop: 'rainyDays', label: '降雨天数', unit: '天' }
    ],
    lineOptions: [
      { value: 'totalRain', label: '降水总量', color: '#1f4461' }
    ]
  },
  temp: {
    key: 'temp',
    label: '气温',
    unit: '°C',
    unitLabel: '平均气温',
    color: '#944a27',
    icon: 'device_thermostat',
    mainMetric: 'avgTemp',
    columns: [
      { prop: 'avgTemp', label: '平均气温', unit: '°C' },
      { prop: 'maxTemp', label: '最高气温', unit: '°C' },
      { prop: 'minTemp', label: '最低气温', unit: '°C' }
    ],
    lineOptions: [
      { value: 'avgTemp', label: '平均气温', color: '#2b463c' },
      { value: 'maxTemp', label: '最高气温', color: '#944a27' },
      { value: 'minTemp', label: '最低气温', color: '#1f4461' }
    ]
  },
  sun: {
    key: 'sun',
    label: '日照',
    unit: 'h',
    unitLabel: '日照时数',
    color: '#8f6a1d',
    icon: 'sunny',
    mainMetric: 'totalSun',
    columns: [
      { prop: 'totalSun', label: '日照总时数', unit: 'h' },
      { prop: 'avgSun', label: '日均日照', unit: 'h' },
      { prop: 'sunnyDays', label: '晴天数', unit: '天' }
    ],
    lineOptions: [
      { value: 'totalSun', label: '日照总时数', color: '#8f6a1d' }
    ]
  },
  hum: {
    key: 'hum',
    label: '湿度',
    unit: '%',
    unitLabel: '平均湿度',
    color: '#002e49',
    icon: 'opacity',
    mainMetric: 'avgHumidity',
    columns: [
      { prop: 'avgHumidity', label: '平均湿度', unit: '%' },
      { prop: 'maxHumidity', label: '最大湿度', unit: '%' },
      { prop: 'minHumidity', label: '最小湿度', unit: '%' }
    ],
    lineOptions: [
      { value: 'avgHumidity', label: '平均湿度', color: '#002e49' }
    ]
  },
  wind: {
    key: 'wind',
    label: '风速',
    unit: 'm/s',
    unitLabel: '平均风速',
    color: '#486459',
    icon: 'air',
    mainMetric: 'avgWind',
    columns: [
      { prop: 'avgWind', label: '平均风速', unit: 'm/s' },
      { prop: 'maxWind', label: '最大风速', unit: 'm/s' },
      { prop: 'galeDays', label: '大风天数', unit: '天' }
    ],
    lineOptions: [
      { value: 'avgWind', label: '平均风速', color: '#486459' },
      { value: 'maxWind', label: '最大风速', color: '#944a27' }
    ]
  }
}

export const FACTOR_OPTIONS = (Object.keys(FACTOR_CONFIGS) as FactorKey[]).map(key => ({
  label: FACTOR_CONFIGS[key].label,
  value: key
}))

export const YEAR_OPTIONS = Array.from({ length: 21 }, (_, i) => 2020 - i)

/** 公元纪年 → 干支纪年（如 2020 → 庚子），用于界面点缀 */
const STEMS = '甲乙丙丁戊己庚辛壬癸'
const BRANCHES = '子丑寅卯辰巳午未申酉戌亥'

export function ganzhi(year: number): string {
  const i = ((year - 4) % 10 + 10) % 10
  const j = ((year - 4) % 12 + 12) % 12
  return STEMS[i] + BRANCHES[j]
}

/** 地名精简：去"市"与民族自治后缀（昌江黎族自治县 → 昌江），供紧凑展示位使用 */
const ETHNIC_RE =
  /(黎族|回族|苗族|壮族|彝族|傣族|白族|蒙古族|藏族|哈萨克族|布依族|土家族|侗族|瑶族|朝鲜族|哈尼族|傈僳族|佤族|畲族|拉祜族|水族|纳西族|羌族|仡佬族|锡伯族|柯尔克孜族|达斡尔族|仫佬族|毛南族|景颇族|布朗族)+/g

export function shortName(name: string): string {
  return name
    .replace(/市$/, '')
    .replace(ETHNIC_RE, '')
    .replace(/自治[州县]/g, '')
    .replace(/地区$/, '')
    .replace(/盟$/, '')
}
