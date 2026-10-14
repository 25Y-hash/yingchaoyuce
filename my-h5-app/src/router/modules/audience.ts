import type { RouteRecordRaw } from 'vue-router'

const audienceRoutes: RouteRecordRaw[] = [
  {
    path: '/audience',
    name: 'audience',
    component: () => import('../../views/audience/Audience.vue'),
    meta: { title: '观众预测' },
  },
]

export default audienceRoutes