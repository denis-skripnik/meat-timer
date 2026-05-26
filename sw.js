const CACHE_NAME = 'meat-breath-timer-v5';
const OFFLINE_URL = './index.html';

function buildMinuteAudioAssets() {
  const common = [
    'meat-start.mp3',
    'meat-minute.mp3',
    'meat-finish.mp3',
    'breath-start.mp3',
    'breath-inhale.mp3',
    'breath-exhale.mp3',
    'breath-minute.mp3',
    'breath-finish.mp3',
    'minute-one.mp3',
    'minute-many.mp3',
    'meat-minute-action.mp3',
    'breath-minute-action.mp3'
  ];
  const byLanguage = {
    en: ['minute-elapsed.mp3'],
    ru: ['minute-elapsed-one.mp3', 'minute-elapsed-many.mp3', 'minute-few.mp3']
  };
  const assets = [];

  for (const lang of ['en', 'ru']) {
    for (const fileName of [...common, ...byLanguage[lang]]) assets.push(`./assets/audio/${lang}/${fileName}`);
    for (let minute = 1; minute <= 120; minute += 1) assets.push(`./assets/audio/${lang}/number-${minute}.mp3`);
  }

  return assets;
}

const ASSETS = [
  './',
  OFFLINE_URL,
  './manifest.json',
  './sw.js',
  './icon-192.png',
  './icon-512.png',
  ...buildMinuteAudioAssets()
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
    caches.match(event.request, { ignoreSearch: true }).then(cached => {
      if (cached) return cached;

      return fetch(event.request).then(response => {
        if (!response || response.status !== 200 || response.type === 'error') return response;

        const copy = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
        return response;
      }).catch(() => {
        if (event.request.mode === 'navigate') return caches.match(OFFLINE_URL);
        return Response.error();
      });
    })
  );
});

self.addEventListener('message', event => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'SKIP_WAITING') {
    self.skipWaiting();
    return;
  }

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
