const CACHE = 'operador-ruleta-v80';
const FILES = [
    '/operador/manifest.json',
    '/operador/icon.svg',
    '/operador/icon-192.png',
    '/operador/icon-512.png',
    '/dreams-logo.png'
];

self.addEventListener('install', function(e) {
    e.waitUntil(
        caches.open(CACHE).then(function(cache) {
            return cache.addAll(FILES);
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', function(e) {
    e.waitUntil(
        caches.keys().then(function(keys) {
            return Promise.all(
                keys.filter(function(k) { return k !== CACHE; })
                    .map(function(k) { return caches.delete(k); })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', function(e) {
    var url = new URL(e.request.url);
    var isHTML = e.request.destination === 'document'
              || url.pathname === '/operador/'
              || url.pathname === '/operador'
              || url.pathname.endsWith('.html');

    if (isHTML) {
        e.respondWith(
            fetch(e.request).then(function(response) {
                if (response && response.status === 200) {
                    var clone = response.clone();
                    caches.open(CACHE).then(function(cache) { cache.put(e.request, clone); });
                }
                return response;
            }).catch(function() {
                return caches.match(e.request) || caches.match('/operador/index.html');
            })
        );
    } else {
        e.respondWith(
            caches.match(e.request).then(function(cached) {
                return cached || fetch(e.request).then(function(response) {
                    if (response && response.status === 200 && response.type === 'basic') {
                        var clone = response.clone();
                        caches.open(CACHE).then(function(cache) { cache.put(e.request, clone); });
                    }
                    return response;
                });
            }).catch(function() {
                return caches.match('/operador/index.html');
            })
        );
    }
});
