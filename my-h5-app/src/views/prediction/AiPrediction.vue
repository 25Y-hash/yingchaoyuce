<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TeamCrest from '@/components/TeamCrest.vue'
import RadarChart from './components/RadarChart.vue'
import { useAiPredictionStore } from '@/stores/aiPrediction'

const predictionStore = useAiPredictionStore()

/** 雷达图上是否显示数值 */
const showValue = ref(true)

const teams = computed(() => predictionStore.teams)

/** 当前显示第几队（左右滑切换用） */
const currentIndex = ref(0)
const currentTeam = computed(() => teams.value[currentIndex.value])

/** 下拉框选队 */
function onSelectTeam(index: number) {
  currentIndex.value = index
}

/** 左右滑：触摸起始 X */
let touchStartX = 0
function onTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX
}
function onTouchEnd(e: TouchEvent) {
  const dx = e.changedTouches[0].clientX - touchStartX
  if (dx > 50 && currentIndex.value > 0) {
    currentIndex.value--
  } else if (dx < -50 && currentIndex.value < teams.value.length - 1) {
    currentIndex.value++
  }
}

/** 六维属性浮层 */
const hoverRank = ref<number | null>(null)
const tip = reactive({ x: 0, y: 0, flipX: false, flipY: false })
const TIP_W = 176
const TIP_H = 150

function onRadarMove(e: MouseEvent, rank: number) {
  hoverRank.value = rank
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  tip.x = x
  tip.y = y
  tip.flipX = x + TIP_W > rect.width
  tip.flipY = y + TIP_H > rect.height
}
function onRadarLeave() {
  hoverRank.value = null
}

function probColor(p: number): string {
  if (p >= 80) return '#e34d4d'
  if (p >= 60) return '#e6a23c'
  return '#409eff'
}

onMounted(() => {
  Promise.all([predictionStore.loadPredictions(), predictionStore.loadAlgorithm()]).catch(
    () => {}
  )
})

function retry() {
  Promise.all([predictionStore.loadPredictions(), predictionStore.loadAlgorithm()]).catch(() => {
    ElMessage.error('仍然加载失败')
  })
}
</script>

<template>
  <div class="page">
    <!-- ============ 数据说明（移到开头） ============ -->
    <h3 class="sec-title">数据说明</h3>
    <el-card shadow="never" class="algo">
      <p class="algo-desc">
        本页数据来自已训练的 AI 模型：对每支球队从下列 5 个维度评估打分（0~100），并直接输出夺冠概率。
        维度评分越高表示该队该项越强；其中「伤病风险」例外，评分越高表示伤病风险越大。
        夺冠概率由模型综合给出，并非各维度简单加权。
      </p>
      <div class="algo-weights">
        <div v-for="w in predictionStore.weights" :key="w.key" class="weight-chip">
          <span class="w-label">{{ w.label }}</span>
        </div>
      </div>
    </el-card>

    <!-- ============ 顶部：下拉框 + 数值开关 ============ -->
    <div class="page-head">
      <el-select
        :model-value="currentIndex"
        @change="onSelectTeam"
        placeholder="选择球队"
        style="width: 180px"
      >
        <el-option
          v-for="(t, i) in teams"
          :key="t.rank"
          :label="t.teamName"
          :value="i"
        />
      </el-select>
      <el-switch v-model="showValue" active-text="显示数值" inline-prompt />
    </div>

    <!-- ============ 加载 / 错误 ============ -->
    <div v-if="predictionStore.loading" class="team-list">
      <div v-for="i in 4" :key="i" class="team-item skeleton">
        <el-skeleton animated>
          <template #template>
            <div class="ti-prob">
              <el-skeleton-item variant="circle" style="width: 44px; height: 44px" />
              <el-skeleton-item variant="p" style="width: 70%; margin-top: 12px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <el-result v-else-if="predictionStore.error" icon="error" title="预测加载失败" :sub-title="predictionStore.error">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <!-- ============ 当前球队卡片（左右滑切换） ============ -->
    <template v-else-if="currentTeam">
      <div
        class="team-slider"
        @touchstart="onTouchStart"
        @touchend="onTouchEnd"
      >
        <div
          class="team-item"
          :style="{ '--team-color': currentTeam.colorPrimary }"
        >
          <!-- 左：概率 -->
          <div class="ti-prob">
            <TeamCrest
              :short-name="currentTeam.shortName"
              :color-primary="currentTeam.colorPrimary"
              :color-secondary="currentTeam.colorSecondary"
              :logo-url="currentTeam.logoUrl"
              :team-name="currentTeam.teamName"
              :size="40"
            />
            <div class="ti-prob-num" :style="{ color: probColor(currentTeam.winProbability) }">
              {{ currentTeam.winProbability }}<small>%</small>
            </div>
            <div class="ti-prob-name">{{ currentTeam.teamName }}</div>
            <el-progress
              :percentage="currentTeam.winProbability"
              :stroke-width="6"
              :color="probColor(currentTeam.winProbability)"
              :show-text="false"
            />
          </div>

          <!-- 中：雷达图 -->
          <div class="ti-radar">
            <div
              class="ti-radar-hit"
              @mousemove="onRadarMove($event, currentTeam.rank)"
              @mouseleave="onRadarLeave"
            >
              <RadarChart :dims="currentTeam.dims" :color-primary="currentTeam.colorPrimary" :show-value="showValue" />
              <transition name="tip">
                <div
                  v-if="hoverRank === currentTeam.rank"
                  class="ti-dims"
                  :style="{
                    left: tip.x + 'px',
                    top: tip.y + 'px',
                    transform: tip.flipX
                      ? `translate(calc(-100% - 14px), ${tip.flipY ? 'calc(-100% - 12px)' : '12px'})`
                      : `translate(14px, ${tip.flipY ? 'calc(-100% - 12px)' : '12px'})`
                  }"
                >
                  <div class="ti-dims-title">{{ currentTeam.teamName }}</div>
                  <div v-for="d in currentTeam.dims" :key="d.key" class="ti-dim">
                    <span class="ti-dim-label">{{ d.label }}</span>
                    <span class="ti-dim-score">{{ d.score }}</span>
                  </div>
                </div>
              </transition>
            </div>
          </div>

          <!-- 右：球星照片 -->
          <div class="ti-stars">
            <template v-if="currentTeam.starPlayers.length > 0">
              <div v-for="s in currentTeam.starPlayers.slice(0, 2)" :key="s.playerName" class="star-photo">
                <div class="star-frame">
                  <img v-if="s.photoUrl" :src="s.photoUrl" :alt="s.playerName" class="star-img" />
                  <div v-else class="star-card">
                    <span class="sc-num">{{ s.jerseyNumber }}</span>
                    <span class="sc-init">{{ s.playerName.charAt(0) }}</span>
                  </div>
                  <span v-if="s.jerseyNumber > 0" class="star-jersey">{{ s.jerseyNumber }}</span>
                </div>
                <div class="star-cap">
                  <span class="star-name">{{ s.playerName }}</span>
                  <span v-if="s.position" class="star-pos">{{ s.position }}</span>
                </div>
              </div>
            </template>
            <span v-else class="no-star">暂无球星资料</span>
          </div>

          <!-- 底部：AI 分析文字 -->
          <div v-if="currentTeam.analysis" class="ti-analysis">{{ currentTeam.analysis }}</div>
        </div>
      </div>

      <!-- 左右切换提示 + 按钮（电脑上也能点） -->
      <div class="slider-nav">
        <el-button
          :disabled="currentIndex === 0"
          @click="currentIndex--"
        >上一队</el-button>
        <span class="slider-indicator">{{ currentIndex + 1 }} / {{ teams.length }}</span>
        <el-button
          :disabled="currentIndex === teams.length - 1"
          @click="currentIndex++"
        >下一队</el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  font-family: "SimSun", "宋体", serif;
  position: relative;
  z-index: 0;
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.46);
}

.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/prediction.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* ============ 顶部：下拉框 + 开关 ============ */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 20px 0 18px;
}

/* ============ 球队滑块 ============ */
.team-slider {
  touch-action: pan-y;   /* 允许纵向滚动，横向滑动自己处理 */
  user-select: none;
}

.team-item {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 18px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-top: 3px solid var(--team-color, var(--el-color-primary));
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
}

/* 左右切换按钮 */
.slider-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}
.slider-indicator {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

/* ============ 左：概率 ============ */
.ti-prob {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
}
.ti-prob-num {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.05;
}
.ti-prob-num small {
  font-size: 18px;
  margin-left: 2px;
}
.ti-prob-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.ti-prob :deep(.el-progress) {
  width: 100%;
  margin-top: 4px;
}

/* ============ 中：雷达图 ============ */
.ti-radar {
  flex: 1 1 auto;
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ti-radar-hit {
  position: relative;
  width: 100%;
  max-width: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ti-radar :deep(.radar) {
  max-width: 240px;
  transition: transform 0.22s cubic-bezier(0.34, 1.16, 0.64, 1);
  transform-origin: center center;
}
.ti-radar-hit:hover :deep(.radar) {
  transform: scale(1.08);
}
.ti-dims {
  position: absolute;
  z-index: 6;
  top: 0;
  left: 0;
  min-width: 150px;
  padding: 9px 12px;
  border-radius: 8px;
  background: rgba(23, 38, 66, 0.94);
  border: 1px solid rgba(96, 148, 224, 0.5);
  box-shadow: 0 10px 26px rgba(8, 18, 38, 0.38);
  color: #eaf1ff;
  pointer-events: none;
  will-change: transform, left, top;
}
.ti-dims-title {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 5px;
  white-space: nowrap;
}
.ti-dim {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 12.5px;
  line-height: 1.75;
  white-space: nowrap;
}
.ti-dim-label {
  color: #b7c8e8;
}
.ti-dim-score {
  font-weight: 700;
  color: #7fc4ff;
}
.tip-enter-active,
.tip-leave-active {
  transition: opacity 0.15s ease;
}
.tip-enter-from,
.tip-leave-to {
  opacity: 0;
}

/* ============ 右：球星 ============ */
.ti-stars {
  flex: 0 0 180px;
  display: flex;
  gap: 10px;
}
.star-photo {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.star-frame {
  position: relative;
  width: 100%;
}
.star-img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  display: block;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}
.star-jersey {
  position: absolute;
  top: 6px;
  left: 6px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  color: #fff;
  background: rgba(20, 28, 40, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 6px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
}
.star-card {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.45);
  background: linear-gradient(160deg, var(--team-color, #409eff), rgba(20, 28, 40, 0.92));
}
.sc-num {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}
.sc-init {
  font-size: 22px;
  font-weight: 700;
}
.star-cap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.star-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.star-pos {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.no-star {
  align-self: center;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.ti-analysis {
  flex: 1 0 100%;
  margin-top: 2px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
}

/* ============ 数据说明 ============ */
.sec-title {
  font-size: 15px;
  margin: 22px 0 10px;
}
.algo-desc {
  margin: 0 0 10px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.algo-weights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.weight-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  font-size: 13px;
}
.w-label {
  color: var(--el-text-color-secondary);
}

/* ============ 窄屏 ============ */
@media (max-width: 768px) {
  .team-item {
    flex-direction: column;
    align-items: stretch;
  }
  .ti-prob,
  .ti-stars {
    flex-basis: auto;
  }
}
</style>