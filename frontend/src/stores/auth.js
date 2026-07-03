import { defineStore } from 'pinia';
import { authApi, userApi } from '../api';
import { collectMenuPaths } from '../utils/menuPermission';

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
    logout() {
      this.token = '';
      this.user = null;
      this.menus = [];
      this.menusLoaded = false;
      localStorage.clear();
    },
  },
});
