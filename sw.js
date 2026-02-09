self.addEventListener('install', e => {
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  self.clients.claim();
});

self.addEventListener('message', event => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'MINUTE_NOTIFICATION') showNotification('Минутка готовки', data.text);
  if (data.type === 'FINISH_NOTIFICATION') showNotification('Готовка завершена', data.text);
});

function showNotification(title, body) {
  self.registration.showNotification(title, {
    body,
    icon: 'icon-192.png',
    badge: 'icon-192.png',
    vibrate: [200, 100, 200],
    requireInteraction: true
  });
}
