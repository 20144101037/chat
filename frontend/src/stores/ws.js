import { defineStore } from 'pinia';
import { createChatSocket } from '../ws/useWebSocket';

/**
 * 全局 WebSocket 单例：登录后在 Home 建立连接，各页面只订阅/取消 topic，避免频繁断连导致收不到推送。
 */
export const useWsStore = defineStore('ws', {
  state: () => ({
    connected: false,
    connecting: false,
    socket: null,
    waiters: [],
    notificationsBound: false,
  }),
  actions: {
    connect() {
      const token = localStorage.getItem('token');
      if (!token) return Promise.resolve();
      if (this.connected) return Promise.resolve();
      if (this.connecting) {
        return new Promise((resolve) => this.waiters.push(resolve));
      }
      this.connecting = true;
      if (!this.socket) {
        this.socket = createChatSocket();
      }
      return new Promise((resolve) => {
        const onReady = () => {
          this.connected = true;
          this.connecting = false;
          resolve();
          this.waiters.splice(0).forEach((w) => w());
        };
        this.socket.connect(token, {
          onConnect: onReady,
          onWsClose: () => {
            this.connected = false;
          },
        });
      });
    },

    ensureConnected() {
      return this.connect();
    },

    disconnect() {
      this.socket?.disconnect();
      this.socket = null;
      this.connected = false;
      this.connecting = false;
      this.notificationsBound = false;
      this.waiters = [];
    },

    subscribeRoom(roomId, handler) {
      return this.socket?.subscribeRoom(roomId, handler);
    },

    unsubscribeRoom(roomId) {
      this.socket?.unsubscribeRoom(roomId);
    },

    subscribeAudit(handler) {
      return this.socket?.subscribeAudit(handler);
    },

    unsubscribeAudit() {
      this.socket?.unsubscribeAudit();
    },

    subscribeNotifications(handler) {
      if (this.notificationsBound) return;
      this.notificationsBound = true;
      return this.socket?.subscribeNotifications(handler);
    },

    unsubscribeNotifications() {
      this.socket?.unsubscribeNotifications();
      this.notificationsBound = false;
    },
  },
});
