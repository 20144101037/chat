<template>
  <div class="page metrics" v-loading="loading">
    <PageHeader title="性能监控" subtitle="在线用户、消息延迟、审核队列与连接池实时指标" icon="DataLine">
      <template #actions>
        <span class="ts" v-if="data.timestamp">采样：{{ formatTime(data.timestamp) }}</span>
        <el-switch v-model="autoRefresh" active-text="自动刷新(5s)" />
        <el-button type="primary" :icon="Refresh" @click="load">立即刷新</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover"><el-statistic title="在线用户数" :value="data.onlineUsers" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><el-statistic title="待审核队列长度" :value="data.auditQueueLength" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="系统错误率" :value="errorRatePct" suffix="%" :value-style="errorStyle" />
          <div class="sub">错误 {{ data.errorRequests }} / 总请求 {{ data.totalRequests }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="审核延迟采样数" :value="data.latencySampleCount" />
          <div class="sub">平均 {{ data.latencyAvgMs }} ms / 峰值 {{ data.latencyMaxMs }} ms</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>消息处理延迟趋势（P95 / P99 / 平均，单位 ms）</template>
          <div ref="latencyEl" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>数据库连接池使用率</template>
          <div ref="poolEl" class="chart"></div>
          <div class="sub center">活跃 {{ data.dbPoolActive }} / 空闲 {{ data.dbPoolIdle }} / 上限 {{ data.dbPoolMax }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import { Refresh } from '@element-plus/icons-vue';
import { metricsApi } from '../api';
import PageHeader from '../components/PageHeader.vue';

const loading = ref(false);
const autoRefresh = ref(true);
const data = reactive({
  onlineUsers: 0, auditQueueLength: 0,
  latencyP95Ms: 0, latencyP99Ms: 0, latencyAvgMs: 0, latencyMaxMs: 0, latencySampleCount: 0,
  errorRate: 0, totalRequests: 0, errorRequests: 0,
  dbPoolActive: 0, dbPoolIdle: 0, dbPoolMax: 0, dbPoolUsage: 0,
  timestamp: null,
});

const latencyEl = ref(null);
const poolEl = ref(null);
let latencyChart = null;
let poolChart = null;
let timer = null;
const trend = { time: [], p95: [], p99: [], avg: [] };
const MAX_POINTS = 30;

const errorRatePct = computed(() => Math.round(data.errorRate * 10000) / 100);
const errorStyle = computed(() => ({ color: errorRatePct.value > 5 ? '#f56c6c' : '#67c23a' }));

function formatTime(t) {
  return t ? new Date(t).toLocaleTimeString() : '';
}

function initCharts() {
  latencyChart = echarts.init(latencyEl.value);
  latencyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['P95', 'P99', '平均'] },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: 'P95', type: 'line', smooth: true, data: [] },
      { name: 'P99', type: 'line', smooth: true, data: [] },
      { name: '平均', type: 'line', smooth: true, data: [] },
    ],
  });
  poolChart = echarts.init(poolEl.value);
  poolChart.setOption({
    series: [{
      type: 'gauge',
      min: 0, max: 100,
      progress: { show: true },
      axisLine: { lineStyle: { width: 14 } },
      detail: { formatter: '{value}%', fontSize: 20 },
      data: [{ value: 0, name: '使用率' }],
    }],
  });
}

function updateCharts() {
  const label = formatTime(data.timestamp) || new Date().toLocaleTimeString();
  trend.time.push(label);
  trend.p95.push(data.latencyP95Ms);
  trend.p99.push(data.latencyP99Ms);
  trend.avg.push(data.latencyAvgMs);
  if (trend.time.length > MAX_POINTS) {
    trend.time.shift(); trend.p95.shift(); trend.p99.shift(); trend.avg.shift();
  }
  latencyChart?.setOption({
    xAxis: { data: trend.time },
    series: [{ data: trend.p95 }, { data: trend.p99 }, { data: trend.avg }],
  });
  poolChart?.setOption({
    series: [{ data: [{ value: Math.round(data.dbPoolUsage * 10000) / 100, name: '使用率' }] }],
  });
}

async function load() {
  loading.value = true;
  try {
    const res = await metricsApi.dashboard();
    Object.assign(data, res);
    updateCharts();
  } finally {
    loading.value = false;
  }
}

function startTimer() {
  stopTimer();
  if (autoRefresh.value) {
    timer = setInterval(load, 5000);
  }
}
function stopTimer() {
  if (timer) { clearInterval(timer); timer = null; }
}

watch(autoRefresh, startTimer);

function onResize() {
  latencyChart?.resize();
  poolChart?.resize();
}

onMounted(async () => {
  await nextTick();
  initCharts();
  window.addEventListener('resize', onResize);
  await load();
  startTimer();
});

onUnmounted(() => {
  stopTimer();
  window.removeEventListener('resize', onResize);
  latencyChart?.dispose();
  poolChart?.dispose();
});
</script>

<style scoped>
.toolbar { display: flex; gap: 16px; align-items: center; margin-bottom: 16px; }
.ts { color: #909399; font-size: 12px; }
.chart { height: 300px; width: 100%; }
.sub { color: #909399; font-size: 12px; margin-top: 6px; }
.sub.center { text-align: center; }
</style>
