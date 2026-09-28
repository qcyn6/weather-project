/**
 * 请求竞态防护：快速连续切换筛选条件时，先发出的请求可能后返回，
 * 导致旧数据覆盖新数据。每次查询用 next() 取号，响应回来后用
 * isStale(seq) 判断是否已被更新的请求取代，是则丢弃本次结果。
 */
export function createLatestGuard() {
  let latest = 0
  return {
    next: () => ++latest,
    isStale: (seq: number) => seq !== latest
  }
}
