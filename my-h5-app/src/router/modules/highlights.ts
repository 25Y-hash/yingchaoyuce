import type { RouteRecordRaw } from 'vue-router'

const highlightsRoutes: RouteRecordRaw[] = [
  {
    path: '/highlights',
    name: 'highlights',
    component: () => import('../../views/highlights/Highlights.vue'),
    meta: { title: '精彩时刻' },
  },
]

export default highlightsRoutes