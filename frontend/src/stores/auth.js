import { defineStore } from 'pinia';
import { authApi, userApi } from '../api';
import { collectMenuPaths } from '../utils/menuPermission';
import { useWsStore } from './ws';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    menus: [],
    menusLoaded: false,
  }),
  getters: {
    menuPaths: (state) => collectMenuPaths(state.menus),
    hasMenuPath: (state) => (path) => collectMenuPaths(state.menus).has(path),
    isAdmin: (state) => ['ROOM_ADMIN', 'SYS_ADMIN'].includes(state.user?.role),
    isSysAdmin: (state) => state.user?.role === 'SYS_ADMIN',
  },
  actions: {
    async login(payload) {
      const data = await authApi.login(payload);
      this.token = data.token;
      this.user = data.user;
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      this.menusLoaded = false;
      this.menus = [];
      await this.loadMenus();
    },
    async loadMenus(force = false) {
      if (this.menusLoaded && !force) return this.menus;
      this.menus = await userApi.meMenus();
      this.menusLoaded = true;
      return this.menus;
    },
    async logout() {
      useWsStore().disconnect();
      try {
        if (this.token) {
          await authApi.logout();
        }
      } catch (e) {
        // 忽略注销接口失败，本地仍清除登录态
      }
      this.token = '';
      this.user = null;
      this.menus = [];
      this.menusLoaded = false;
      localStorage.clear();
    },
  },
});
