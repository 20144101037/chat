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
    if (stompSubs.has(destination)) return;
    const sub = client.subscribe(destination, rawHandler);
    stompSubs.set(destination, sub);
  }

  function resubscribeAll() {
    // 重连后 stomp 订阅已失效，清空并按已登记的 handler 重新订阅
    stompSubs.clear();
    handlers.forEach((rawHandler, destination) => doSubscribe(destination, rawHandler));
  }

  function connect(token, { onConnect, onError, onWsClose } = {}) {
    client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${token}`),
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
        onWsClose?.();
      },
    });
    client.activate();
  }

  function subscribeRoom(roomId, handler) {
    const destination = `/topic/room.${roomId}`;
    const rawHandler = (msg) => {
      const payload = JSON.parse(msg.body);
      if (payload.messageId && seenMessageIds.has(payload.messageId)) return;
      if (payload.messageId) seenMessageIds.add(payload.messageId);
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

  function subscribeAudit(handler) {
    const destination = '/topic/audit';
    const rawHandler = (msg) => handler(JSON.parse(msg.body));
    handlers.set(destination, rawHandler);
    doSubscribe(destination, rawHandler);
  }

  function disconnect() {
    stompSubs.forEach((s) => s.unsubscribe());
    stompSubs.clear();
    handlers.clear();
    connected = false;
    client?.deactivate();
  }

  return { connect, subscribeRoom, unsubscribeRoom, subscribeNotifications, subscribeAudit, disconnect };
}
