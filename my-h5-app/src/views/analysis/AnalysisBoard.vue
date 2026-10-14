<script setup lang="ts">
import { computed, onMounted } from 'vue'
import TeamCrest from '@/components/TeamCrest.vue'
import { useAnalysisStore } from '@/stores/analysis'

const analysisStore = useAnalysisStore()

const analyses = computed(() => analysisStore.analyses)

/** 专家姓名：优先中文名，没有英文名就回退中文 */
const expertName = (zh: string, en: string) => zh || en
const teamLabel = (zh: string, en: string) => zh || en

onMounted(async () => {
  try {
    await analysisStore.loadAnalyses()
  } catch {
    return
  }
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="title">专家预测</h2>
        <p class="subtitle">资深解说员与评论员眼中的夺冠格局</p>
      </div>
    </div>

    <!-- 加载中：骨架屏 -->
    <div v-if="analysisStore.loading" class="grid">
      <el-skeleton v-for="i in 4" :key="i" animated class="card-skeleton">
        <template #template>
          <el-skeleton-item variant="circle" style="width: 44px; height: 44px" />
          <el-skeleton-item variant="p" style="width: 60%; margin-top: 12px" />
          <el-skeleton-item variant="text" style="margin-top: 12px" />
          <el-skeleton-item variant="text" style="width: 80%" />
        </template>
      </el-skeleton>
    </div>

    <!-- 加载失败 -->
    <el-result
      v-else-if="analysisStore.error"
      icon="error"
      title="解析加载失败"
      :sub-title="analysisStore.error"
    >
      <template #extra>
        <el-button type="primary" @click="analysisStore.loadAnalyses()">重试</el-button>
      </template>
    </el-result>

    <div v-else class="grid">
      <el-card
        v-for="item in analyses"
        :key="item.id"
        class="expert-card"
        shadow="hover"
        :style="{ '--team-color': item.colorPrimary }"
      >
        <template #header>
          <div class="expert-head">
            <el-avatar :size="42" :src="item.avatarUrl || undefined">
              {{ expertName(item.nameCn, item.nameEn).charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="expert-meta">
              <div class="expert-name">{{ expertName(item.nameCn, item.nameEn) }}</div>
              <div v-if="item.title" class="expert-title">{{ item.title }}</div>
              <div class="expert-sub">
                支持
                <TeamCrest
                  :short-name="item.shortName"
                  :color-primary="item.colorPrimary"
                  :color-secondary="item.colorSecondary"
                  :logo-url="item.logoUrl"
                  :team-name="teamLabel(item.teamName, item.teamNameEn)"
                  :size="20"
                />
                <strong class="team">{{ teamLabel(item.teamName, item.teamNameEn) }}</strong>
              </div>
            </div>
          </div>
        </template>

        <p class="reason">{{ item.reason }}</p>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page {
  position: relative;
  z-index: 0;
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.6);
  font-family: "SimSun", "宋体", serif;
}

.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/analysis.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
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

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}
.card-skeleton {
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}
.expert-card {
  border-left: 4px solid var(--team-color, var(--el-color-primary));
}
.expert-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.expert-name {
  font-size: 15px;
  font-weight: 600;
}
.expert-title {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.expert-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.team {
  color: var(--team-color, var(--el-color-primary));
}
.reason {
  margin: 0;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
</style>