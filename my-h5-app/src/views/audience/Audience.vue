<template>
  <div class="audience">
    <div class="content">
      

      <!-- ============ 排行榜 ============ -->
      <section class="ranking">
        <div class="podium">
          <div
            v-for="team in podiumOrder"
            :key="team.teamName"
            class="podium-item"
            :class="'rank-' + team.rank"
          >
            <div class="medal">{{ medalOf(team.rank) }}</div>
            <img class="team-logo" :src="team.teamLogo" :alt="team.teamName">
            <p class="team-name">{{ team.teamName }}</p>
            <p class="votes">{{ team.votes.toLocaleString() }} 票</p>
            <p class="percent">{{ team.percentage }}%</p>
          </div>
        </div>

        <div class="rest-list">
          <div
            v-for="team in restTeams"
            :key="team.teamName"
            class="rest-item"
          >
            <span class="rank-num">{{ team.rank }}</span>
            <img class="team-logo-small" :src="team.teamLogo" :alt="team.teamName">
            <span class="team-name-small">{{ team.teamName }}</span>
            <span class="dots">· · · · · · · · · · · · · · · ·</span>
            <span class="votes-small">{{ team.votes.toLocaleString() }} / 987,463（{{ team.percentage }}%）</span>
          </div>
        </div>
      </section>

      <!-- ============ 评论区 ============ -->
      <section class="comments">
        <h2 class="section-title">观众评论</h2>

        <!-- 队伍筛选 -->
        <div class="filter">
          <select v-model="selectedTeam" @change="onTeamChange">
            <option value="">全部队伍</option>
            <option v-for="team in allTeams" :key="team.teamName" :value="team.teamName">
              {{ team.teamName }}
            </option>
          </select>
        </div>

        <!-- 发评论 -->
        <div class="post-comment">
          <template v-if="isLoggedIn">
            <textarea
              v-model="newComment"
              placeholder="说点什么..."
              rows="3"
            ></textarea>
            <button
              class="submit-btn"
              :disabled="!newComment.trim() || submitting"
              @click="submitComment"
            >
              {{ submitting ? '发送中...' : '发表评论' }}
            </button>
          </template>
          <p v-else class="login-hint">
            <router-link to="/login">登录</router-link> 后才能发表评论
          </p>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <img class="avatar" :src="c.avatar" :alt="c.nickname">
            <div class="comment-body">
              <p class="nickname">{{ c.nickname }}</p>
              <p class="content">{{ c.content }}</p>
              <p class="time">{{ c.createdAt }}</p>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <button
          v-if="comments.length < fakeTotal"
          class="load-more"
          @click="loadMore"
        >
          点击展开剩余 {{ fakeTotal - comments.length }} 条评论
        </button>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface VoteItem {
  rank: number
  teamName: string
  teamLogo: string
  votes: number
  percentage: number
}

interface CommentItem {
  id: number
  nickname: string
  avatar: string
  content: string
  createdAt: string
}

const allTeams = ref<VoteItem[]>([])
const comments = ref<CommentItem[]>([])
const selectedTeam = ref('')
const page = ref(1)
const size = 3

// 发评论相关
const newComment = ref('')
const submitting = ref(false)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const top3 = computed(() => allTeams.value.slice(0, 3))
const restTeams = computed(() => allTeams.value.slice(3))

const podiumOrder = computed(() => {
  const t = top3.value
  if (t.length < 3) return t
  return [t[1], t[0], t[2]]
})

const fakeTotal = computed(() => {
  if (!selectedTeam.value) return 0
  const team = allTeams.value.find(t => t.teamName === selectedTeam.value)
  return team ? Math.floor(team.votes / 50) : 0
})

function medalOf(rank: number): string {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return ''
}

async function fetchRanking() {
  const res = await fetch('/api/audience/ranking')
  const json = await res.json()
  if (json.code === 0) {
    allTeams.value = json.data
    if (json.data.length > 0 && !selectedTeam.value) {
      selectedTeam.value = json.data[0].teamName
      await fetchComments()
    }
  }
}

async function fetchComments() {
  const res = await fetch(
    `/api/audience/comments?teamName=${encodeURIComponent(selectedTeam.value)}&page=${page.value}&size=${size}`
  )
  const json = await res.json()
  if (json.code === 0) {
    comments.value = json.data.list
  }
}

function onTeamChange() {
  page.value = 1
  comments.value = []
  fetchComments()
}

function loadMore() {
  page.value++
  fetch(
    `/api/audience/comments?teamName=${encodeURIComponent(selectedTeam.value)}&page=${page.value}&size=${size}`
  )
    .then(r => r.json())
    .then(json => {
      if (json.code === 0) {
        if (json.data.list.length === 0) {
          const realComments = comments.value.slice(0, 10)
          const addCount = Math.min(size, realComments.length)
          for (let i = 0; i < addCount; i++) {
            const base = realComments[(comments.value.length + i) % realComments.length]
            comments.value.push({ ...base, id: base.id + comments.value.length * 1000 })
          }
        } else {
          comments.value.push(...json.data.list)
        }
      }
    })
}

/** 发表评论 */
async function submitComment() {
  if (!selectedTeam.value) {
    alert('请先选择队伍')
    return
  }
  if (!newComment.value.trim()) return

  submitting.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/audience/comment', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({
        teamName: selectedTeam.value,
        content: newComment.value.trim(),
      }),
    })
    const json = await res.json()

    if (json.code === 0) {
      newComment.value = ''
      page.value = 1
      comments.value = []
      await fetchComments()
    } else {
      alert(json.message || '评论失败')
    }
  } catch (e) {
    alert('网络错误，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchRanking)
</script>

<style scoped>
/* 页面整体：底图（用 ::before 铺满视口） */
.audience {
  position: relative;
  z-index: 0;              /* ← 关键 */
  min-height: 100vh;
  padding: 20px;
  font-family: "SimSun", serif;
}

.audience::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image: url('/back/red.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 内容容器：居中、限宽 */
.content {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 60px;
  text-align: center;
  margin: 20px 0 30px;
  color: #ef7ea4;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  font-family: "SimHei", sans-serif;
}

/* ============ 排行榜 ============ */
.ranking {
  margin-bottom: 50px;
}
.podium {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 30px;
}
.podium-item {
  text-align: center;
  padding: 20px;
  border-radius: 12px;
  background: #f9f9f9;
  flex: 1;
  max-width: 220px;
}
.podium-item.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  transform: scale(1.1);
}
.podium-item.rank-2 {
  transform: scale(0.90);
  margin-top: 15px;
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
}
.podium-item.rank-3 {
  transform: scale(0.85);
  margin-top: 15px;
  background: linear-gradient(135deg, #cd7f32, #e8a76a);
}
.medal {
  font-size: 36px;
  margin-bottom: 10px;
}
.team-logo {
  width: 60px;
  height: 60px;
  object-fit: contain;
  margin-bottom: 10px;
}
.team-name {
  font-size: 16px;
  font-weight: bold;
  margin: 5px 0;
}
.votes {
  font-size: 14px;
  margin: 3px 0;
}
.percent {
  font-size: 13px;
  color: #555;
  margin: 0;
}

.rest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rest-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
}
.rank-num {
  font-weight: bold;
  color: #888;
  width: 25px;
  text-align: center;
}
.team-logo-small {
  width: 30px;
  height: 30px;
  object-fit: contain;
}
.team-name-small {
  font-weight: bold;
  min-width: 100px;
}
.dots {
  color: #ccc;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
}
.votes-small {
  font-size: 14px;
  color: #666;
}

/* ============ 评论区 ============ */
.comments {
  margin-top: 40px;
}
.section-title {
  font-size: 20px;
  margin-bottom: 15px;
  color: #fb7299;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
}
.filter {
  margin-bottom: 20px;
}
.filter select {
  padding: 8px 15px;
  border-radius: 6px;
  border: 1px solid #fb7299;
  font-size: 14px;
}

.post-comment {
  margin-bottom: 20px;
}
.post-comment textarea {
  width: 100%;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.95);
}
.post-comment .submit-btn {
  margin-top: 10px;
  padding: 8px 24px;
  background: #fb7299;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.3s;
}
.post-comment .submit-btn:hover {
  opacity: 0.85;
}
.post-comment .submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.login-hint {
  font-size: 14px;
  color: #000000;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
}
.login-hint a {
  color: #ff3784;
  text-decoration: none;
}
.login-hint a:hover {
  text-decoration: underline;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 10px;
}
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
}
.nickname {
  font-weight: bold;
  font-size: 14px;
  margin: 0 0 5px;
}
.content {
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 5px;
}
.time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.load-more {
  display: block;
  margin: 20px auto 0;
  padding: 10px 30px;
  background: #fb7299;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.3s;
}
.load-more:hover {
  opacity: 0.85;
}

/* ============ 手机端 ============ */
@media (max-width: 768px) {
  .podium {
    flex-direction: row;
    gap: 8px;
  }
  .podium-item {
    padding: 10px;
  }
  .medal {
    font-size: 24px;
    margin-bottom: 5px;
  }
  .team-logo {
    width: 40px;
    height: 40px;
    margin-bottom: 5px;
  }
  .team-name {
    font-size: 13px;
    margin: 3px 0;
  }
  .votes {
    font-size: 11px;
  }
  .percent {
    font-size: 11px;
  }
  .podium-item.rank-1 {
    transform: none;
  }
  .dots {
    display: none;
  }
  .rest-item {
    flex-wrap: wrap;
  }
}
</style>