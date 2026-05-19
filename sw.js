const CACHE_NAME = 'meat-breath-timer-v2';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './sw.js',
  './icon-192.png',
  './icon-512.png',
  './assets/audio/en/meat-start.mp3',
  './assets/audio/en/meat-minute.mp3',
  './assets/audio/en/meat-finish.mp3',
  './assets/audio/en/breath-start.mp3',
  './assets/audio/en/breath-inhale.mp3',
  './assets/audio/en/breath-exhale.mp3',
  './assets/audio/en/breath-minute.mp3',
  './assets/audio/en/breath-finish.mp3',
  './assets/audio/ru/meat-start.mp3',
  './assets/audio/ru/meat-minute.mp3',
  './assets/audio/ru/meat-finish.mp3',
  './assets/audio/ru/breath-start.mp3',
  './assets/audio/ru/breath-inhale.mp3',
  './assets/audio/ru/breath-exhale.mp3',
  './assets/audio/ru/breath-minute.mp3',
  './assets/audio/ru/breath-finish.mp3'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then(cached => cached || fetch(event.request))
  );
});

self.addEventListener('message', event => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'TIMER_NOTIFICATION') {
    event.waitUntil(showNotification(data.title, data.text, { requireInteraction: data.requireInteraction }));
  }
});

function showNotification(title, body, options = {}) {
  const iconUrl = new URL('icon-192.png', self.registration.scope).toString();
  return self.registration.showNotification(title, {
    body,
    tag: 'meat-breath-timer-notification',
    icon: iconUrl,
    badge: iconUrl,
    vibrate: [200, 100, 200],
    requireInteraction: options.requireInteraction || false
  });
}
