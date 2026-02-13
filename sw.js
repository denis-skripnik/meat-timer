self.addEventListener('install', e => {
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  self.clients.claim();
});

self.addEventListener('message', event => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'MINUTE_NOTIFICATION') {
    event.waitUntil(showNotification(data.title, data.text));
  } else if (data.type === 'FINISH_NOTIFICATION') {
    event.waitUntil(showNotification(data.title, data.text, { requireInteraction: true }));
  }
});

function showNotification(title, body, options = {}) {
  const iconUrl = new URL('icon-192.png', self.registration.scope).toString();
  return self.registration.showNotification(title, {
    body,
    tag: 'meat-timer-notification',
    icon: iconUrl,
    badge: iconUrl,
    vibrate: [200, 100, 200],
    requireInteraction: options.requireInteraction || false
  });
}
