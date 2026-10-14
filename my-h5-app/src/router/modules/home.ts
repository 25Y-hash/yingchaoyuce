import type { RouteRecordRaw } from 'vue-router'

const homeRoutes: RouteRecordRaw[] = [
  {
    path: '/home',
    name: 'home',
    component: () => import('../../views/home/Home.vue'),
    meta: { title: '英超预测' },
  },
]

export default homeRoutes