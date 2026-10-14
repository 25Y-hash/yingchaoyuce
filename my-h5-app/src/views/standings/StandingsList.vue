<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useStandingsStore } from '@/stores/standings'
import type { TeamStanding } from '@/types/api'

const standingsStore = useStandingsStore()

// ── 三态判定 ──
const isInitialLoading = computed(
  () => standingsStore.loading && standingsStore.standings.length === 0
)
const isInitialError = computed(
  () => standingsStore.error !== '' && standingsStore.standings.length === 0
)
const isEmpty = computed(
  () => !standingsStore.loading && !standingsStore.error && standingsStore.standings.length === 0
)

onMounted(() => {
  standingsStore.loadStandings().catch(() => {})
})

function onRetry() {
  standingsStore.loadStandings().catch(() => {})
}

async function onRefresh() {
  try {
    await standingsStore.loadStandings()
    ElMessage.success('已更新')
  } catch {
    ElMessage.error('更新失败，请稍后再试')
  }
}

/** 分区配置 */
const zones = computed(() => [
  { key: 'ucl', title: '欧冠区', teams: standingsStore.championsLeagueZone },
  { key: 'uel', title: '欧联区', teams: standingsStore.europaLeagueZone },
  { key: 'rel', title: '降级区', teams: standingsStore.relegationZone }
])

/** 队徽降级策略 */
function rankBadgeClass(team: TeamStanding) {
  if (team.rank <= 6) return 'badge badge--ucl'
  if (team.rank <= 17) return 'badge badge--uel'
  return 'badge badge--rel'
}

/** 点击某队，弹窗显示详情 */
const selectedTeam = ref<TeamStanding | null>(null)
const dialogVisible = ref(false)

function onRowClick(team: TeamStanding) {
  selectedTeam.value = team
  dialogVisible.value = true
}
</script>

<template>
  <div class="page">
    <header class="titlebar">
      <div>
        <h1 class="title">当前积分排名</h1>
        <p v-if="standingsStore.roundSummary" class="subtitle">{{ standingsStore.roundSummary }}</p>
      </div>
      <div class="titlebar__side">
        <el-button
          class="refresh-btn"
          :loading="standingsStore.refreshing"
          :disabled="standingsStore.loading"
          @click="onRefresh"
        >
          刷新
        </el-button>
        <div class="legend">
          <span class="legend__item"><i class="dot dot--ucl" />欧冠区</span>
          <span class="legend__item"><i class="dot dot--uel" />欧联区</span>
          <span class="legend__item"><i class="dot dot--rel" />降级区</span>
        </div>
      </div>
    </header>

    <div v-if="isInitialLoading" class="skeleton">
      <el-skeleton v-for="n in 8" :key="n" class="skeleton__row" :rows="1" animated />
    </div>

    <el-result
      v-else-if="isInitialError"
      icon="error"
      :title="standingsStore.error"
    >
      <template #extra>
        <el-button type="primary" @click="onRetry">重新加载</el-button>
      </template>
    </el-result>

    <el-empty v-else-if="isEmpty" description="赛季尚未开始，暂无积分数据" />

    <template v-else>
      <div class="row row--header">
        <span class="col col--rank">排名</span>
        <span class="col col--team">球队</span>
        <span class="col col--num">轮次</span>
        <span class="col col--num col--wide">胜/平/负</span>
        <span class="col col--num">进失</span>
        <span class="col col--num">积分</span>
      </div>

      <template v-for="zone in zones" :key="zone.key">
        <div class="zone" :class="`zone--${zone.key}`">{{ zone.title }}</div>
        <div
          v-for="team in zone.teams"
          :key="team.rank"
          class="row row--clickable"
          @click="onRowClick(team)"
        >
          <span class="col col--rank">
            <img v-if="team.logoUrl" class="badge-img" :src="team.logoUrl" :alt="team.teamName" />
            <i v-else :class="rankBadgeClass(team)">{{ team.rank }}</i>
          </span>
          <span class="col col--team">{{ team.teamName }}</span>
          <span class="col col--num">{{ team.played }}</span>
          <span class="col col--num col--wide">{{ team.win }}/{{ team.draw }}/{{ team.lose }}</span>
          <span class="col col--num">{{ team.goalsFor }}/{{ team.goalsAgainst }}</span>
          <span class="col col--num col--points">{{ team.points }}</span>
        </div>
      </template>

      <!-- 点击某队，弹出详情 -->
      <el-dialog v-model="dialogVisible" :title="selectedTeam?.teamName" width="300px">
        <div v-if="selectedTeam" class="detail">
          <div class="detail-row"><span>排名</span><span>{{ selectedTeam.rank }}</span></div>
          <div class="detail-row"><span>球队</span><span>{{ selectedTeam.teamName }}</span></div>
          <div class="detail-row"><span>轮次</span><span>{{ selectedTeam.played }}</span></div>
          <div class="detail-row"><span>胜</span><span>{{ selectedTeam.win }}</span></div>
          <div class="detail-row"><span>平</span><span>{{ selectedTeam.draw }}</span></div>
          <div class="detail-row"><span>负</span><span>{{ selectedTeam.lose }}</span></div>
          <div class="detail-row"><span>进球</span><span>{{ selectedTeam.goalsFor }}</span></div>
          <div class="detail-row"><span>失球</span><span>{{ selectedTeam.goalsAgainst }}</span></div>
          <div class="detail-row"><span>积分</span><span class="points">{{ selectedTeam.points }}</span></div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.page {
  position: relative;
  z-index: 0;
  min-height: 100vh;
  padding: 0 24px;
  font-family: "SimSun", "宋体", serif;
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.6);
}

.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/standings.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.titlebar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 14px 0 10px;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 8px;
}

.titlebar__side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: bold;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}

.legend {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #606266;
}

.legend__item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot--ucl { background: #e6a23c; }
.dot--uel { background: #409eff; }
.dot--rel { background: #f56c6c; }

.skeleton__row {
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fc;
}

.row {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f2f6fc;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.55);
}

.row--header {
  font-weight: bold;
  background: rgba(255, 255, 255, 0.78);
  border-radius: 8px 8px 0 0;
}

/* 可点击的行 */
.row--clickable {
  cursor: pointer;
}
.row--clickable:hover {
  background: rgba(255, 255, 255, 0.88);
}

.col--rank {
  width: 40px;
  flex-shrink: 0;
}

.col--team {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.col--num {
  width: 42px;
  flex-shrink: 0;
  text-align: center;
}

.col--wide {
  width: 62px;
}

.col--points {
  font-weight: bold;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-style: normal;
  color: #ffffff;
}

.badge--ucl { background: #e6a23c; }
.badge--uel { background: #409eff; }
.badge--rel { background: #909399; }

.badge-img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: contain;
}

.zone {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: bold;
  color: #ffffff;
}

.zone--ucl { background: #e6a23c; }
.zone--uel { background: #409eff; }
.zone--rel { background: #f56c6c; }

/* 弹窗详情 */
.detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f2f6fc;
  font-size: 14px;
}
.detail-row .points {
  font-weight: bold;
  color: var(--el-color-primary);
}

@media (min-width: 768px) {
  .page {
    padding: 0 48px;
  }

  .titlebar {
    padding: 20px 0 16px;
  }

  .title {
    font-size: 50px;
  }

  .subtitle {
    font-size: 14px;
  }

  .legend {
    gap: 16px;
    font-size: 13px;
  }

  .dot {
    width: 10px;
    height: 10px;
  }

  .row:hover {
    background: rgba(255, 255, 255, 0.88);
  }

  .col--rank {
    width: 56px;
  }

  .col--num {
    width: 72px;
  }

  .col--wide {
    width: 110px;
  }

  .badge {
    width: 32px;
    height: 32px;
    font-size: 15px;
  }

  .badge-img {
    width: 32px;
    height: 32px;
  }

  .zone {
    padding: 8px 12px;
    font-size: 13px;
  }

  .row {
    font-size: 15px;
  }
}
</style>