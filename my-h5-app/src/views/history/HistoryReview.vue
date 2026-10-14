<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TeamCrest from '@/components/TeamCrest.vue'
import { useHistoryStore } from '@/stores/history'

const historyStore = useHistoryStore()

const seasons = computed(() => historyStore.seasons)
const titles = computed(() => historyStore.titles)

/** 排序方向：'desc' = 从新到旧（33-1），'asc' = 从旧到新（1-33） */
const sortOrder = ref<'desc' | 'asc'>('desc')

/** 按届数排序后的赛季列表 */
const sortedSeasons = computed(() => {
  const list = [...seasons.value]
  list.sort((a, b) =>
    sortOrder.value === 'desc' ? b.edition - a.edition : a.edition - b.edition
  )
  return list
})

/** 柱状图归一化 */
const maxCount = computed(() => Math.max(1, ...titles.value.map((t) => t.count)))
function barWidth(count: number): string {
  return `${(count / maxCount.value) * 100}%`
}

onMounted(() => {
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {})
})

function retry() {
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {
    ElMessage.error('仍然加载失败')
  })
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="title">历史回顾</h2>
        <p class="subtitle">
          {{ historyStore.editionRange ? `历届战绩（${historyStore.editionRange}）` : '历届战绩' }}
        </p>
      </div>
    </div>

    <div v-if="historyStore.loading">
      <el-skeleton animated :rows="6" />
    </div>

    <el-result v-else-if="historyStore.error" icon="error" title="战绩加载失败" :sub-title="historyStore.error">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- ── 1. 英超历届夺冠次数（移到前面） ───────────────── -->
      <h3 class="sec-title">英超历届夺冠次数（自 1992-93 起）</h3>
      <el-card shadow="never" class="title-card">
        <div v-for="(item, i) in titles" :key="item.team.teamName" class="bar-row">
          <span class="bar-rank">{{ i + 1 }}</span>
          <TeamCrest
            :short-name="item.team.shortName"
            :color-primary="item.team.colorPrimary"
            :color-secondary="item.team.colorSecondary"
            :logo-url="item.team.logoUrl"
            :team-name="item.team.teamName"
            :size="24"
          />
          <span class="bar-name">{{ item.team.teamName }}</span>
          <span class="bar-track">
            <span
              class="bar-fill"
              :style="{
                width: barWidth(item.count),
                background: item.team.colorPrimary
              }"
            />
          </span>
          <span class="bar-count">{{ item.count }}</span>
        </div>
        
      </el-card>

      <!-- ── 2. 历届战绩（移到后面，带排序切换） ─────────── -->
      <div class="sec-title-row">
        <h3 class="sec-title">历届战绩</h3>
        <el-radio-group v-model="sortOrder" size="small">
          <el-radio-button value="desc">33 → 1（新到旧）</el-radio-button>
          <el-radio-button value="asc">1 → 33（旧到新）</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="sortedSeasons" border stripe size="small" height="420" class="hist-table">
        <el-table-column prop="edition" label="届数" width="80" align="center" />
        <el-table-column prop="seasonLabel" label="赛季" width="100" align="center" />
        <el-table-column label="冠军" min-width="150">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.champion.shortName"
                :color-primary="row.champion.colorPrimary"
                :color-secondary="row.champion.colorSecondary"
                :logo-url="row.champion.logoUrl"
                :team-name="row.champion.teamName"
                :size="22"
              />
              <span class="champion">{{ row.champion.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="亚军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.runnerUp.shortName"
                :color-primary="row.runnerUp.colorPrimary"
                :color-secondary="row.runnerUp.colorSecondary"
                :logo-url="row.runnerUp.logoUrl"
                :team-name="row.runnerUp.teamName"
                :size="22"
              />
              <span>{{ row.runnerUp.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="季军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.third.shortName"
                :color-primary="row.third.colorPrimary"
                :color-secondary="row.third.colorSecondary"
                :logo-url="row.third.logoUrl"
                :team-name="row.third.teamName"
                :size="22"
              />
              <span>{{ row.third.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="殿军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.fourth.shortName"
                :color-primary="row.fourth.colorPrimary"
                :color-secondary="row.fourth.colorSecondary"
                :logo-url="row.fourth.logoUrl"
                :team-name="row.fourth.teamName"
                :size="22"
              />
              <span>{{ row.fourth.teamName }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<style scoped>
.page {
  position: relative;
  z-index: 0;
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.46);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: rgba(255, 255, 255, 0.62);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.82);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.95);
  font-family: "SimSun", "宋体", serif;   /* ← 加这行 */
}

.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/history.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.page-head {
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 28px;    /* ← 放大，原来 20px */
  font-weight: bold;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.sec-title {
  font-size: 16px;
  margin: 22px 0 10px;
}

/* 排序切换那行：标题 + 单选按钮组 */
.sec-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 22px 0 10px;
}
.sec-title-row .sec-title {
  margin: 0;
}

.hist-table {
  width: 100%;
}
.cell-team {
  display: flex;
  align-items: center;
  gap: 8px;
}
.champion {
  font-weight: 600;
}
.title-card {
  padding: 4px 0;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.bar-rank {
  width: 20px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.bar-name {
  width: 84px;
  font-size: 14px;
  white-space: nowrap;
}
.bar-track {
  flex: 1;
  height: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  overflow: hidden;
  min-width: 80px;
}
.bar-fill {
  display: block;
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease;
}
.bar-count {
  width: 32px;
  text-align: right;
  font-weight: 700;
  font-size: 15px;
}
.bar-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
</style>

<!-- 全局：让 Element Plus 组件也变宋体 -->
<style>
.page,
.page * {
  font-family: "SimSun", "宋体", serif;
}
</style>