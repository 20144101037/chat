import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * WebSocket(STOMP) 客户端封装：
 * - 单连接多路复用（按 topic 订阅房间）
 * - 自动重连（内置指数退避）+ 断线重连后自动恢复所有订阅
 * - 按 messageId 去重，配合服务端 at-least-once 语义
 */
export function createChatSocket() {
  let client = null;
  const stompSubs = new Map(); // destination -> stompSubscription
  const handlers = new Map(); // destination -> handler，用于重连后恢复订阅
  const seenMessageIds = new Set();
  let connected = false;

  function doSubscribe(destination, rawHandler) {
    if (!client || !connected) return;
    stompSubs.get(destination)?.unsubscribe();
    const sub = client.subscribe(destination, rawHandler);
    stompSubs.set(destination, sub);
  }

  function resubscribeAll() {
    stompSubs.clear();
    handlers.forEach((rawHandler, destination) => doSubscribe(destination, rawHandler));
  }

  function connect(token, { onConnect, onError, onWsClose } = {}) {
    if (client?.active) {
      onConnect?.();
      return;
    }
    client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${encodeURIComponent(token)}`),
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected = true;
        resubscribeAll();
        onConnect?.();
      },
      onStompError: (frame) => onError?.(frame),
      onWebSocketClose: () => {
        connected = false;
        stompSubs.clear();
        onWsClose?.();
      },
    });
    client.activate();
  }

  function subscribeRoom(roomId, handler) {
    const destination = `/topic/room.${roomId}`;
    const rawHandler = (msg) => {
      const payload = JSON.parse(msg.body);
      const id = payload.messageId ?? payload.id;
      if (id != null && seenMessageIds.has(String(id))) return;
      if (id != null) seenMessageIds.add(String(id));
      handler(payload);
    };
    handlers.set(destination, rawHandler);
    doSubscribe(destination, rawHandler);
  }

  function unsubscribeRoom(roomId) {
    const destination = `/topic/room.${roomId}`;
    stompSubs.get(destination)?.unsubscribe();
    stompSubs.delete(destination);
    handlers.delete(destination);
  }

  function subscribeNotifications(handler) {
    const destination = '/user/queue/notifications';
    const rawHandler = (msg) => handler(JSON.parse(msg.body));
    handlers.set(destination, rawHandler);
    doSubscribe(destination, rawHandler);
  }

  function unsubscribeNotifications() {
    const destination = '/user/queue/notifications';
    stompSubs.get(destination)?.unsubscribe();
    stompSubs.delete(destination);
    handlers.delete(destination);
  }

  function subscribeAudit(handler) {
    const destination = '/topic/audit';
    const rawHandler = (msg) => handler(JSON.parse(msg.body));
    handlers.set(destination, rawHandler);
    doSubscribe(destination, rawHandler);
  }

  function unsubscribeAudit() {
    const destination = '/topic/audit';
    stompSubs.get(destination)?.unsubscribe();
    stompSubs.delete(destination);
    handlers.delete(destination);
  }

  function disconnect() {
    stompSubs.forEach((s) => s.unsubscribe());
    stompSubs.clear();
    handlers.clear();
    seenMessageIds.clear();
    connected = false;
    client?.deactivate();
    client = null;
  }

  return {
    connect,
    subscribeRoom,
    unsubscribeRoom,
    subscribeNotifications,
    unsubscribeNotifications,
    subscribeAudit,
    unsubscribeAudit,
    disconnect,
  };
}
