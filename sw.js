const CACHE = 'pantalla-ruleta-v94';
const FILES = [
    '/manifest.json',
    '/dreams-logo.png',
    '/casino-bg.jpg',
    '/roulette-table.jpg'
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
              || url.pathname === '/'
              || url.pathname.endsWith('.html');

    if (isHTML) {
        // Network first for HTML — always get the latest version
        e.respondWith(
            fetch(e.request).then(function(response) {
                if (response && response.status === 200) {
                    var clone = response.clone();
                    caches.open(CACHE).then(function(cache) { cache.put(e.request, clone); });
                }
                return response;
            }).catch(function() {
                return caches.match(e.request) || caches.match('/index.html');
            })
        );
    } else {
        // Cache first for assets (images, manifest, etc.)
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
                return caches.match('/index.html');
            })
        );
    }
});
