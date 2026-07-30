import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || (window.location.protocol === 'https:' ? 'https://' : 'http://') + window.location.host + '/ws';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = {};
    this.connected = false;
    this.onConnectCallbacks = [];
  }

  connect(token) {
    if (this.client && this.connected) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        console.log('WebSocket connected');
        this.onConnectCallbacks.forEach((cb) => cb());
        this.onConnectCallbacks = [];
      },
      onDisconnect: () => {
        this.connected = false;
        console.log('WebSocket disconnected');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      Object.values(this.subscriptions).forEach((sub) => sub.unsubscribe());
      this.subscriptions = {};
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  onConnect(callback) {
    if (this.connected) {
      callback();
    } else {
      this.onConnectCallbacks.push(callback);
    }
  }

  subscribe(destination, callback) {
    if (!this.client || !this.connected) {
      this.onConnect(() => this._doSubscribe(destination, callback));
      return;
    }
    this._doSubscribe(destination, callback);
  }

  _doSubscribe(destination, callback) {
    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe();
    }
    this.subscriptions[destination] = this.client.subscribe(destination, (message) => {
      const body = JSON.parse(message.body);
      callback(body);
    });
  }

  unsubscribe(destination) {
    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe();
      delete this.subscriptions[destination];
    }
  }

  send(destination, body) {
    if (this.client && this.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  }

  // Document-specific helpers
  subscribeToEdits(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/edits`, callback);
  }

  subscribeToCursors(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/cursors`, callback);
  }

  subscribeToPresence(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/presence`, callback);
  }

  subscribeToKeyChanges(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/key`, callback);
  }

  subscribeToKeyRequests(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/key-request`, callback);
  }

  subscribeToEvents(documentId, callback) {
    this.subscribe(`/topic/document/${documentId}/events`, callback);
  }

  sendEdit(documentId, content, title) {
    this.send(`/app/document/${documentId}/edit`, { documentId, content, title });
  }

  sendCursor(documentId, position, selectionStart, selectionEnd) {
    this.send(`/app/document/${documentId}/cursor`, {
      documentId,
      position,
      selectionStart,
      selectionEnd,
    });
  }

  sendPresence(documentId, action) {
    this.send(`/app/document/${documentId}/presence`, { documentId, action });
  }

  unsubscribeFromDocument(documentId) {
    this.unsubscribe(`/topic/document/${documentId}/edits`);
    this.unsubscribe(`/topic/document/${documentId}/cursors`);
    this.unsubscribe(`/topic/document/${documentId}/presence`);
    this.unsubscribe(`/topic/document/${documentId}/key`);
    this.unsubscribe(`/topic/document/${documentId}/key-request`);
    this.unsubscribe(`/topic/document/${documentId}/events`);
  }
}

const websocketService = new WebSocketService();
export default websocketService;
