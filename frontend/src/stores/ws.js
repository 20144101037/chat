import { defineStore } from 'pinia';
import { createChatSocket } from '../ws/useWebSocket';

/**
 * 全局 WebSocket 单例：登录后在布局层建立连接，各页面按需订阅 topic。
 */
export const useWsStore = defineStore('ws', {
  state: () => ({
    connected: false,
    socket: null,
  }),
  actions: {
    ensureConnected(token) {
      const jwt = token || localStorage.getItem('token');
      if (!jwt || this.socket) return;

      this.socket = createChatSocket();
      this.socket.connect(jwt, {
        onConnect: () => {
          this.connected = true;
        },
        onError: () => {
          this.connected = false;
        },
        onWsClose: () => {
          this.connected = false;
        },
      });
    },
    disconnect() {
      this.socket?.disconnect();
      this.socket = null;
      this.connected = false;
    },
  },
});
