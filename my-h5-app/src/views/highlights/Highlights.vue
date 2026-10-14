<template>
  <div class="highlights">
    <div class="content">
      <!-- 三组，每组 1 大 + 6 小 -->
      <div
        v-for="(group, gi) in groups"
        :key="gi"
        class="group"
      >
        <!-- 大视频 -->
        <a :href="group[0].url" target="_blank" class="video-big">
          <img class="cover big" :src="group[0].cover" :alt="group[0].title">
          <p class="title">{{ group[0].title }}</p>
        </a>

        <!-- 6 个小视频：3 列 2 行 -->
        <div class="small-grid">
          <a
            v-for="(video, vi) in group.slice(1)"
            :key="vi"
            :href="video.url"
            target="_blank"
            class="video-small"
          >
            <img class="cover small" :src="video.cover" :alt="video.title">
            <p class="title">{{ video.title }}</p>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { highlightVideos, type HighlightVideo } from '../../data/highlights'

const groups = computed<HighlightVideo[][]>(() => {
  const result: HighlightVideo[][] = []
  for (let i = 0; i < highlightVideos.length; i += 7) {
    result.push(highlightVideos.slice(i, i + 7))
  }
  return result
})
</script>

<style scoped>
/* 页面整体：底图铺满 */
.highlights {
  position: relative;      /* 给 ::before 定位参照 */
  z-index: 0;              /* ← 关键：建立堆叠上下文 */
  min-height: 100vh;
  padding: 20px;
  font-family: "SimSun", serif;
}

.highlights::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;             /* ← 压在内容下面，但要在 #app 上面 */
  pointer-events: none;
  background-image: url('/back/purple.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 内容容器：居中、限宽 */
.content {
  max-width: 1200px;
  margin: 0 auto;
}

/* 每组 */
.group {
  margin-bottom: 50px;
}

/* ============ 大视频 ============ */
.video-big {
  display: block;
  text-decoration: none;
  color: #fff;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  padding: 10px;
  border-radius: 12px;
  transition: background 0.3s ease;
}
.video-big:hover {
  background: rgba(255, 255, 255, 0.25);
}
.video-big .cover.big {
  width: 100%;
  aspect-ratio: 21 / 9;
}
.video-big .title {
  font-size: 18px;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

/* ============ 小视频：3 列 2 行 ============ */
.small-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.video-small {
  display: block;
  text-decoration: none;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  padding: 10px;
  border-radius: 12px;
  transition: background 0.3s ease;
}
.video-small:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* 封面通用 */
.cover {
  width: 100%;
  display: block;
  background: #ddd;
  border-radius: 8px;
  object-fit: cover;
  transition: transform 0.3s ease;
  overflow: hidden;
}
.cover.small {
  aspect-ratio: 16 / 9;
}

/* 悬停：封面放大 */
.video-big:hover .cover,
.video-small:hover .cover {
  transform: scale(1.05);
}

/* 标题 */
.title {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: #fff;
  transition: color 0.3s ease;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

/* 悬停：标题变色 */
.video-big:hover .title,
.video-small:hover .title {
  color: #b39cdf;
}

/* ============ 手机端 ============ */
@media (max-width: 768px) {
  .small-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .group {
    margin-bottom: 30px;
  }
  .video-big .title {
    font-size: 15px;
  }
  .title {
    font-size: 12px;
  }
}
</style>