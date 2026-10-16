<script setup lang="ts">
// ===== 本文件是「历史回顾」页面（路由 /history 对应的视图组件）=====
// <script setup> 是 Vue 3 的「组合式 API 语法糖」：
//   在它里面写的顶层变量 / 函数会自动暴露给下面的 <template> 使用，不用手动 return。
//   lang="ts" 表示这块脚本用 TypeScript 写（有类型检查，写错类型会报错）。

// 从 Vue 框架引入三个最常用的能力：
//   computed —— 计算属性：依赖别的响应式数据，自动算出新值，且会缓存（依赖不变就不重算）
//   onMounted —— 生命周期钩子：组件挂载（插入到网页 DOM）后自动执行一次
//   ref      —— 响应式变量：用 ref(初值) 包起来的数据，改了它会自动刷新界面
import { computed, onMounted, ref } from 'vue'

// ElMessage 是 Element Plus 组件库提供的「轻提示」：弹一行小字消息（成功 / 错误等）
import { ElMessage } from 'element-plus'

// 引入一个「队徽」小组件（全局共用）：负责显示球队的徽章 + 队名。
// 后面模板里用 <TeamCrest ... /> 这种自定义标签来使用它。
import TeamCrest from '@/components/TeamCrest.vue'

// 引入「历史回顾」的数据仓库（Pinia store）：所有数据请求和状态都集中在这里。
// @ 是项目配置的别名，指向 src 目录，所以 '@/stores/history' = 'src/stores/history'。
import { useHistoryStore } from '@/stores/history'

// 拿到这个页面的数据仓库实例。之后通过 historyStore.xxx 读写数据。
const historyStore = useHistoryStore()

// 下面两行用 computed 把 store 里的数据「桥接」到本页面局部变量：
// seasons = 历届战绩列表；titles = 历届夺冠次数统计。
// 为什么多包一层？模板里直接读 historyStore.seasons 也行，但写成局部 computed 更直观。
const seasons = computed(() => historyStore.seasons)
const titles = computed(() => historyStore.titles)

/** 最大届数：排序按钮文案（如「34 → 1」）从数据现算，不写死——以后再加赛季前端零改动 */
const maxEdition = computed(() =>
  seasons.value.length ? Math.max(...seasons.value.map((s) => s.edition)) : 1
)

/** 排序方向：'desc' = 从新到旧（届数大→小），'asc' = 从旧到新（届数小→大） */
// ref<'desc' | 'asc'> 是带「泛型 + 联合类型」的响应式变量：
//   <'desc' | 'asc'> 表示这个字符串只可能是这两个值之一（写别的值 TS 会报错）
//   初值给 'desc'，也就是默认按「新到旧」展示。
const sortOrder = ref<'desc' | 'asc'>('desc')

/** 按届数排序后的赛季列表（computed 依赖 sortOrder，切换时自动重算） */
const sortedSeasons = computed(() => {
  // ...seasons.value 是「展开运算符」：把数组内容复制出一份新数组，避免直接改动原数组
  const list = [...seasons.value]
  // Array.sort 原地排序；传入一个「比较函数」决定先后顺序：
  //   a、b 是两个赛季对象；返回负数表示 a 排前面，正数表示 b 排前面，0 表示相等。
  list.sort((a, b) =>
    // 箭头函数体如果只有一句 return，可省略 {} 和 return 关键字（这种写法叫「隐式返回」）
    sortOrder.value === 'desc'
      ? b.edition - a.edition   // 降序：届数大的（更新的）排前面
      : a.edition - b.edition   // 升序：届数小的排前面
  )
  return list
})

/** 柱状图归一化：找出最大的夺冠次数，用来把每根柱子缩放到 0~100% 宽度 */
// Math.max(1, ...数组) 里的 ... 又是展开：把数组元素摊开成多个参数传给 Math.max；
// 再加个 1 是为了防止全部为 0 时除以 0。.map((t) => t.count) 把对象数组映射成「只取 count」的数字数组。
const maxCount = computed(() => Math.max(1, ...titles.value.map((t) => t.count)))
// 根据某个队的夺冠次数，算出它柱子的宽度百分比字符串，如 "75%"
function barWidth(count: number): string {
  // 模板字符串 `...${}...`：用 ${} 把表达式结果塞进字符串里
  return `${(count / maxCount.value) * 100}%`
}

// onMounted：组件一挂载就执行——这里用来「进页面就拉数据」。
onMounted(() => {
  // Promise.all([...])：同时发起两个异步请求（赛季列表 + 夺冠次数），两个都完成才算完成。
  //   historyStore.loadSeasons() / loadTitleCounts() 内部会发网络请求去后端拿数据。
  // .catch(() => {})：万一两个请求都失败，这里「吞掉错误」——因为页面上已经有
  //   专门的「加载失败」提示块（见模板里的 el-result），不必再额外弹错误。
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {})
})

// 「重试」按钮触发：和上面一样重新拉数据，但这次失败要弹错误提示。
function retry() {
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {
    // ElMessage.error：弹出一个红色的错误小提示条
    ElMessage.error('仍然加载失败')
  })
}
</script>

<template>
  <!-- 页面最外层容器，class="page" 对应下面 <style> 里的样式 -->
  <div class="page">
    <!-- 页头：标题 + 副标题 -->
    <div class="page-head">
      <div>
        <h2 class="title">历史回顾</h2>
        <!-- {{ }} 是 Vue 的「插值语法」：把 JS 表达式的结果填到页面上。
             这里用三元 ?: 判断：store 里有届数范围就显示它，否则显示默认文字。 -->
        <p class="subtitle">
          {{ historyStore.editionRange ? `历届战绩（${historyStore.editionRange}）` : '历届战绩' }}
        </p>
      </div>
    </div>

    <!-- v-if / v-else-if / v-else 是 Vue 的条件渲染：三选一显示。
         优先级：正在加载 → 加载失败 → 正常（前面都不满足时走最后的 <template v-else>）。 -->
    <!-- 1) 正在加载：显示骨架屏（灰条占位动画，给用户「在加载」的反馈） -->
    <div v-if="historyStore.loading">
      <el-skeleton animated :rows="6" />
    </div>

    <!-- 2) 加载失败：显示错误结果页 + 一个「重试」按钮 -->
    <el-result
      v-else-if="historyStore.error"
      icon="error"
      title="战绩加载失败"
      :sub-title="historyStore.error"
    >
      <!-- <template #extra> 是 Element Plus 的具名插槽：把按钮放到结果页预留的「额外区域」里 -->
      <template #extra>
        <!-- @click="retry"：点击按钮时调用上面 script 里定义的 retry 函数 -->
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <!-- 3) 正常：数据加载成功，展示内容 -->
    <template v-else>
      <!-- 区块 1：英超历届夺冠次数（柱状图） -->
      <h3 class="sec-title">英超历届夺冠次数（自 1992-93 起）</h3>
      <!-- el-card 是 Element Plus 的卡片容器；shadow="never" 表示不要悬浮阴影 -->
      <el-card shadow="never" class="title-card">
        <!-- v-for 是 Vue 的列表渲染：把 titles 数组每一项循环出来。
             (item, i) 中 item 是元素、i 是下标（从 0 开始）。
             :key 是每个循环项的唯一标识，Vue 靠它高效更新列表（这里用队名当 key）。 -->
        <div v-for="(item, i) in titles" :key="item.team.teamName" class="bar-row">
          <!-- 排名：i+1 是因为下标从 0 开始，显示时要从 1 开始 -->
          <span class="bar-rank">{{ i + 1 }}</span>
          <!-- 队徽组件：把每个队的属性用 :属性名="值" 传进去（: 是 v-bind 的简写，表示右边是 JS 表达式） -->
          <TeamCrest
            :short-name="item.team.shortName"
            :color-primary="item.team.colorPrimary"
            :color-secondary="item.team.colorSecondary"
            :logo-url="item.team.logoUrl"
            :team-name="item.team.teamName"
            :size="24"
          />
          <!-- 队名 -->
          <span class="bar-name">{{ item.team.teamName }}</span>
          <!-- 柱子轨道（灰色底） -->
          <span class="bar-track">
            <!-- 真正的柱子：用 :style 动态绑定一个样式对象。
                 width 由 barWidth(count) 算出百分比；background 用队的主色。 -->
            <span
              class="bar-fill"
              :style="{
                width: barWidth(item.count),
                background: item.team.colorPrimary
              }"
            />
          </span>
          <!-- 夺冠次数数字 -->
          <span class="bar-count">{{ item.count }}</span>
        </div>
      </el-card>

      <!-- 区块 2：历届战绩表格（带排序切换） -->
      <div class="sec-title-row">
        <h3 class="sec-title">历届战绩</h3>
        <!-- 单选按钮组：v-model="sortOrder" 把选中值「双向绑定」到上面的 sortOrder 变量。
             选「新到旧」sortOrder 变 'desc'，选「旧到新」变 'asc'，表格会自动重排。 -->
        <el-radio-group v-model="sortOrder" size="small">
          <el-radio-button value="desc">{{ maxEdition }} → 1（新到旧）</el-radio-button>
          <el-radio-button value="asc">1 → {{ maxEdition }}（旧到新）</el-radio-button>
        </el-radio-group>
      </div>
      <!-- el-table：Element Plus 的表格。:data 是数据数组；border 显示边框；
           stripe 斑马纹；size="small" 紧凑；height 固定表头高度后可滚动。 -->
      <el-table :data="sortedSeasons" border stripe size="small" height="420" class="hist-table">
        <!-- 每一列：prop 直接绑定数据中同名字段；label 是列头文字；
             width 固定列宽；min-width 最小列宽（窗口变窄时此列至少这么宽）；align 对齐。 -->
        <el-table-column prop="edition" label="届数" width="80" align="center" />
        <el-table-column prop="seasonLabel" label="赛季" width="100" align="center" />
        <!-- 自定义列：用 <template #default="{ row }"> 拿到当前行数据 row。
             { row } 是「解构赋值」：从插槽作用域对象里直接取出 row 字段来用。 -->
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
        <!-- 亚军 / 季军 / 殿军 三列结构同上，只是改取 row.runnerUp / row.third / row.fourth -->
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
/* scoped = 这里的样式只作用在本组件，不会影响别的页面（Vue 会给元素加特殊属性） */

.page {
  position: relative;   /* 定位基准，让下面的 ::before 背景相对它定位 */
  z-index: 0;           /* 把内容垫在背景之上 */
  /* 下面这一组 --el-* 是「覆盖 Element Plus 的内部变量」，用来把卡片 / 表格改成半透明、透出背景图 */
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.46);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: rgba(255, 255, 255, 0.62);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.82);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.95);
  font-family: "SimSun", "宋体", serif;   /* 本页正文字体设为宋体 */
}

/* ::before 是伪元素：在 .page 内部最前面插入一个「背景层」 */
.page::before {
  content: '';          /* 伪元素必须有 content 才显示，空字符串即可 */
  position: fixed;      /* 固定定位：始终铺满视口，不随页面滚动 */
  inset: 0;             /* 上右下左全为 0，等于 top/right/bottom/left:0，即铺满 */
  z-index: -1;          /* 压到内容下面当背景 */
  pointer-events: none; /* 穿透点击：背景不挡住上面的按钮 */
  /* 叠两层：一层半透明白蒙版 + 底层历史背景图，让文字更清晰 */
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/history.jpg');
  background-size: cover;      /* 背景图等比缩放铺满 */
  background-position: center; /* 居中 */
  background-repeat: no-repeat;
}

.page-head {
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 28px;    /* 页面大标题字号 */
  font-weight: bold;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary); /* 用 Element Plus 的次要文字色（灰） */
  font-size: 14px;
}
.sec-title {
  font-size: 16px;
  margin: 22px 0 10px;
}

/* 排序切换那一行：标题 + 单选按钮组，用 flex 横排 */
.sec-title-row {
  display: flex;            /* 弹性盒：子元素横向排列 */
  align-items: center;      /* 垂直居中 */
  justify-content: space-between; /* 两端对齐：标题靠左、按钮靠右 */
  gap: 12px;               /* 子元素之间间距 */
  flex-wrap: wrap;          /* 空间不够时允许换行（手机窄屏时按钮换到下一行） */
  margin: 22px 0 10px;
}
.sec-title-row .sec-title {
  margin: 0;
}

.hist-table {
  width: 100%;              /* 表格占满父容器宽度：父容器变窄它也跟着变窄，天然适配手机 */
}
.cell-team {
  display: flex;
  align-items: center;
  gap: 8px;
}
.champion {
  font-weight: 600;         /* 冠军名字加粗 */
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
  white-space: nowrap;      /* 队名不换行 */
}
.bar-track {
  flex: 1;                  /* 弹性伸展：占满剩余空间，所以柱子区会随屏幕变宽变窄 */
  height: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;       /* 圆角 */
  overflow: hidden;         /* 里面的柱子超出圆角部分裁掉 */
  min-width: 80px;          /* 但最窄也有 80px，保证小屏不至于挤没 */
}
.bar-fill {
  display: block;
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease; /* 宽度变化时有 0.4 秒平滑过渡动画 */
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

/* ── 关于手机适配 ──
   本页面没有写 @media（媒体查询），它的「手机友好」来自两点：
   ① 全局地基：index.html 里的 viewport 标签 + src/assets/styles/responsive.css（手机端把内容内边距收窄到 12px）；
   ② 本页布局本身用「弹性 + 百分比」而非固定像素：表格 width:100%、柱子 flex:1、
      排序行 flex-wrap 可换行——窗口（或手机屏）变窄时这些元素自动收缩，不会撑爆屏幕。
   其它页面的 @media 断点（如 App.vue 导航栏在 ≤640px 横向滚动）见各自文件。 */
</style>

<!-- 不带 scoped 的 <style>：全局生效。让 .page 内部所有元素（含 Element Plus 组件）都用宋体 -->
<style>
.page,
.page * {
  font-family: "SimSun", "宋体", serif;
}
</style>
