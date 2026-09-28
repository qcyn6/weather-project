<template>
  <div class="about-page">
    <header class="page-head">
      <div>
        <h2 class="t-headline page-title">帮助与关于</h2>
        <p class="page-sub">数据来源 · 指标字典 · 数仓管道 · 技术架构</p>
      </div>
    </header>

    <!-- 平台概览 -->
    <section class="scroll-card ink-border about-card">
      <h3 class="t-title card-title">平台概览</h3>
      <p class="about-text">
        云观气象 · 城市气象大数据可视化平台，基于
        <b>371 个城市、2000-2020 年共 21 年、降水 / 气温 / 日照 / 湿度 / 风速五要素</b>
        的逐日观测数据构建。原始日值约 1423 万行，经离线数仓分层清洗聚合后，
        以年度榜单、趋势曲线、月度分布、地理地图与气候聚类等形式呈现。
      </p>
    </section>

    <!-- 数据来源 -->
    <section class="scroll-card ink-border about-card">
      <h3 class="t-title card-title">数据来源</h3>
      <p class="about-text">
        原始数据取自 <b>2000-2020 年中国地面气候资料日值数据集</b>，按城市拆分为逐日 CSV
        （每行：日期、省份、城市、城市代码、要素值）。数据经上传 HDFS 后进入 Hive 数仓分层加工，
        全程不手工修改数值；异常值按要素物理范围过滤（如气温 -60~60℃、日照 0~24h、湿度 0~100%）。
      </p>
    </section>

    <!-- 指标字典 -->
    <section class="scroll-card ink-border about-card">
      <h3 class="t-title card-title">指标字典</h3>
      <table class="ink-table">
        <thead>
          <tr>
            <th>指标</th>
            <th>单位</th>
            <th>计算口径</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in metrics" :key="m.name">
            <td class="col-name">{{ m.name }}</td>
            <td class="text-variant">{{ m.unit }}</td>
            <td class="text-variant">{{ m.rule }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 数仓管道 -->
    <section class="scroll-card ink-border about-card">
      <h3 class="t-title card-title">数仓管道</h3>
      <div class="pipeline">
        <div v-for="(step, i) in pipeline" :key="step.name" class="pipe-step" :class="{ 'pipe-last': i === pipeline.length - 1 }">
          <div class="pipe-box">
            <div class="pipe-name">{{ step.name }}</div>
            <div class="t-label pipe-desc">{{ step.desc }}</div>
          </div>
        </div>
      </div>
      <p class="about-text pipe-note">
        原始 CSV 上传 HDFS 后被 Hive 外部表（ODS）挂载，Spark SQL 完成清洗（DWD）、
        年/月聚合（DWS）与业务指标（ADS），最终由 Spark JDBC 全量写入 MySQL 供接口层查询；
        页面查询另配进程内缓存，静态结果命中内存。
      </p>
    </section>

    <!-- 技术栈 -->
    <section class="scroll-card ink-border about-card">
      <h3 class="t-title card-title">技术栈</h3>
      <div class="tech-chips">
        <span v-for="t in techs" :key="t" class="tech-chip">{{ t }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
const metrics = [
  { name: '降水总量', unit: 'mm', rule: '逐日累积降水量求和；日降水 ≥ 0.1mm 记为 1 个降雨日' },
  { name: '平均气温', unit: '°C', rule: '逐日平均气温的均值；日温 ≥ 30℃ 记高温日，≤ 0℃ 记低温日' },
  { name: '日照时数', unit: 'h', rule: '逐日日照时数求和；日日照 ≥ 6h 记为 1 个晴天' },
  { name: '相对湿度', unit: '%', rule: '逐日平均湿度均值；日湿度 ≥ 80% 记高湿日，≤ 30% 记低湿日' },
  { name: '平均风速', unit: 'm/s', rule: '逐日平均风速均值；日最大风速 ≥ 10.8m/s（8 级风）记大风日' },
  { name: '同比趋势', unit: '—', rule: '年值较上年变化超过 ±5%（气温 ±0.3℃）记为上升/下降，否则为稳定' }
]

const pipeline = [
  { name: 'CSV / HDFS', desc: '逐日原始日值上传分布式存储' },
  { name: 'Hive ODS', desc: '外部表按行挂载原始层' },
  { name: 'DWD 清洗', desc: '日期解析、异常值过滤' },
  { name: 'DWS 聚合', desc: '按年 / 月 / 城市汇总' },
  { name: 'ADS 指标', desc: '排名、趋势、聚类结果' },
  { name: 'MySQL', desc: '落地服务库' },
  { name: 'API / 页面', desc: 'Spring Boot + Vue 展示' }
]

const techs = [
  'Hadoop 3.3 HDFS', 'Hive 3.1', 'Spark 3.1 (SQL / MLlib)',
  'MySQL 5.7', 'Spring Boot 3.3', 'JWT 鉴权',
  'Vue 3 + TypeScript', 'ECharts 6', 'Element Plus'
]
</script>

<style scoped lang="scss">
.about-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1000px;
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

.about-card {
  padding: 24px;

  .card-title {
    margin-bottom: 14px;
  }
}

.about-text {
  margin: 0;
  line-height: 1.9;
  color: var(--ink-text-variant);

  b {
    color: var(--ink-primary);
  }
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
    line-height: 1.6;
  }

  tbody tr:hover {
    background: var(--ink-bg-low);
  }

  .col-name {
    font-weight: 600;
    color: var(--ink-primary);
    white-space: nowrap;
  }
}

/* 管道流程 */
.pipeline {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 16px;

  .pipe-step {
    flex: 1;
    min-width: 110px;
    position: relative;

    &:not(.pipe-last)::after {
      content: '→';
      position: absolute;
      right: -9px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--ink-outline);
      z-index: 1;
    }

    .pipe-box {
      height: 100%;
      padding: 12px 10px;
      background: var(--ink-bg-low);
      border: 1px solid rgba(20, 48, 38, 0.15);
      border-radius: 6px;
      text-align: center;

      .pipe-name {
        font-family: var(--font-serif);
        font-size: 14px;
        font-weight: 600;
        color: var(--ink-primary);
      }

      .pipe-desc {
        margin-top: 4px;
        color: var(--ink-outline);
        line-height: 1.4;
      }
    }
  }
}

.pipe-note {
  font-size: 14px;
}

.tech-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .tech-chip {
    padding: 6px 14px;
    background: var(--ink-bg-low);
    border: 1px solid rgba(20, 48, 38, 0.15);
    border-radius: 999px;
    font-size: 13px;
    color: var(--ink-primary);
  }
}
</style>
