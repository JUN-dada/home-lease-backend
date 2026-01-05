import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: () => import('../layouts/DashboardLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '仪表盘', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/ProfileView.vue'),
          meta: { title: '个人资料', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
        {
          path: 'houses/explore',
          name: 'houses-explore',
          component: () => import('../views/HouseExploreView.vue'),
          meta: { title: '房源广场', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
        {
          path: 'houses/:houseId',
          name: 'houses-detail',
          component: () => import('../views/HouseDetailView.vue'),
          meta: { title: '房源详情', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
        {
          path: 'houses/favorites',
          name: 'houses-favorites',
          component: () => import('../views/HouseFavoritesView.vue'),
          meta: { title: '我的收藏', roles: ['USER'] },
        },
        {
          path: 'houses/mine',
          name: 'houses-mine',
          component: () => import('../views/HouseManagerView.vue'),
          meta: { title: '我的房源', roles: ['LANDLORD'] },
        },
        {
          path: 'houses/admin',
          name: 'houses-admin',
          component: () => import('../views/HouseAdminView.vue'),
          meta: { title: '房源管理', roles: ['ADMIN'] },
        },
        {
          path: 'contacts/mine',
          name: 'contacts-mine',
          component: () => import('../views/ContactTenantView.vue'),
          meta: { title: '我的联系', roles: ['USER'] },
        },
        {
          path: 'contacts/landlord',
          name: 'contacts-landlord',
          component: () => import('../views/ContactLandlordView.vue'),
          meta: { title: '租客咨询', roles: ['LANDLORD'] },
        },
        {
          path: 'contacts/admin',
          name: 'contacts-admin',
          component: () => import('../views/ContactAdminView.vue'),
          meta: { title: '联系管理', roles: ['ADMIN'] },
        },
        {
          path: 'support/center',
          name: 'support-center',
          component: () => import('../views/SupportCenterView.vue'),
          meta: { title: '客服中心', roles: ['USER', 'LANDLORD'] },
        },
        {
          path: 'support/admin',
          name: 'support-admin',
          component: () => import('../views/SupportAdminView.vue'),
          meta: { title: '客服工单', roles: ['ADMIN'] },
        },
        {
          path: 'orders/mine',
          name: 'orders-mine',
          component: () => import('../views/OrderTenantView.vue'),
          meta: { title: '我的订单', roles: ['USER'] },
        },
        {
          path: 'orders/landlord',
          name: 'orders-landlord',
          component: () => import('../views/OrderLandlordView.vue'),
          meta: { title: '房东订单', roles: ['LANDLORD'] },
        },
        {
          path: 'orders/admin',
          name: 'orders-admin',
          component: () => import('../views/OrderAdminView.vue'),
          meta: { title: '订单总览', roles: ['ADMIN'] },
        },
        {
          path: 'announcements',
          name: 'announcements',
          component: () => import('../views/AnnouncementListView.vue'),
          meta: { title: '公告中心', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
        {
          path: 'announcements/admin',
          name: 'announcements-admin',
          component: () => import('../views/AnnouncementAdminView.vue'),
          meta: { title: '公告管理', roles: ['ADMIN'] },
        },
        {
          path: 'certification/apply',
          name: 'certification-apply',
          component: () => import('../views/CertificationApplyView.vue'),
          meta: { title: '房东认证', roles: ['USER', 'LANDLORD'] },
        },
        {
          path: 'certification/admin',
          name: 'certification-admin',
          component: () => import('../views/CertificationAdminView.vue'),
          meta: { title: '认证审核', roles: ['ADMIN'] },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: () => import('../views/StatisticsView.vue'),
          meta: { title: '统计分析', roles: ['ADMIN'] },
        },
        {
          path: 'users/admin',
          name: 'users-admin',
          component: () => import('../views/UsersAdminView.vue'),
          meta: { title: '用户管理', roles: ['ADMIN'] },
        },
        {
          path: 'locations/admin',
          name: 'locations-admin',
          component: () => import('../views/LocationsAdminView.vue'),
          meta: { title: '地区/地铁管理', roles: ['ADMIN'] },
        },
        {
          path: 'landlords/:landlordId',
          name: 'landlord-detail',
          component: () => import('../views/LandlordDetailView.vue'),
          meta: { title: '房东详情', roles: ['ADMIN', 'LANDLORD', 'USER'] },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (authStore.token && !authStore.user) {
    try {
      await authStore.bootstrap()
    } catch (error) {
      console.error('Failed to initialize auth store', error)
    }
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }
  if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    next('/dashboard')
    return
  }
  if (to.meta.roles && to.meta.roles.length > 0 && !authStore.hasRole(to.meta.roles)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
