<template>
  <div class="home">
    <!-- 第一屏：标题居中 -->
    <section class="hero">
      <h1 class="main-title">英超预测</h1>
      <p class="sub-title">2026/27 赛季</p>
    </section>

    <!-- 第二屏：三个模块 -->
    <section class="blocks">
      <!-- 大图：观众预测 -->
      <router-link to="/audience" class="block block-big">
        <img src="/images/home/guanzhong.jpg" alt="观众预测">
        <div class="overlay">
          <h2>观众预测</h2>
          <p>看看大家都支持谁</p>
        </div>
      </router-link>

      <!-- 两张小图：并排 -->
      <div class="row-small">
        <router-link to="/prediction" class="block block-small">
          <img src="/images/home/ai.png" alt="AI 预测">
          <div class="overlay">
            <h2>AI 预测</h2>
            <p>算出夺冠概率</p>
          </div>
        </router-link>

        <router-link to="/analysis" class="block block-small">
          <img src="/images/home/quanwei.png" alt="专家预测">
          <div class="overlay">
            <h2>专家预测</h2>
            <p>权威为你解析</p>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 页面整体：底图（用 ::before 铺满视口） */
.home {
  position: relative;
  z-index: 0;              /* ← 关键 */
  min-height: 100vh;
  font-family: "SimSun", serif;
}

.home::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image: url('/back/bule.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* ============ 第一屏：标题居中 ============ */
.hero {
  height: 40vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.main-title {
  font-size: 100px;
  margin: 0 0 16px;
  color: #fcf083;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
  text-align: center;
  font-family: "Microsoft YaHei", sans-serif; 
}
.sub-title {
  font-size: 18px;
  color: rgba(66, 61, 1, 0.9);
  margin: 0;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
  text-align: center;
}

/* ============ 第二屏：三个模块 ============ */
.blocks {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.block {
  position: relative;
  display: block;
  border-radius: 12px;
  overflow: hidden;
  text-decoration: none;
  color: #fff;
  transition: transform 0.3s ease;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  padding: 10px;
}
.block:hover {
  transform: scale(1.02);
  background: rgba(255, 255, 255, 0.25);
}

.block img {
  width: 100%;
  display: block;
  object-fit: cover;
  border-radius: 8px;
}

.block-big img {
  aspect-ratio: 21 / 9;
}

.row-small {
  display: flex;
  gap: 20px;
}
.block-small {
  flex: 1 1 0;
  min-width: 0;
}
.block-small img {
  aspect-ratio: 16 / 9;
}
.block-small .overlay h2 {
  font-size: 18px;
}
.block-small .overlay p {
  font-size: 12px;
}

.overlay {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  padding: 15px 20px;
  border-radius: 0 0 8px 8px;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.75) 0%,
    rgba(0, 0, 0, 0.3) 60%,
    rgba(0, 0, 0, 0) 100%
  );
}
.overlay h2 {
  margin: 0 0 6px;
  font-size: 24px;
}
.overlay p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

/* ============ 手机端 ============ */
@media (max-width: 768px) {
  .main-title {
    font-size: 60px;
  }
  .sub-title {
    font-size: 20px;
  }
  .blocks {
    padding: 20px 15px 60px;
  }
}
</style>