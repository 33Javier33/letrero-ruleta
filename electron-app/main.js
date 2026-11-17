const { app, BrowserWindow, Menu, Tray } = require('electron');
const express = require('express');
const path = require('path');
const fs = require('fs');
const https = require('https');

const PORT = 12399;
const VERSION_URL = 'https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/version.json';
const BASE_RAW_URL = 'https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/';

let mainWindow = null;

// ── Web file directories ──────────────────────────────────────────────────────
function getBundledWebDir() {
    if (app.isPackaged) {
        return path.join(process.resourcesPath, 'web');
    }
    // Dev: files are in the parent directory
    return path.join(__dirname, '..');
}

function getUserWebDir() {
    return path.join(app.getPath('userData'), 'web');
}

// ── Local HTTP server ─────────────────────────────────────────────────────────
function startServer(callback) {
    const srv = express();
    const userWebDir = getUserWebDir();
    const bundledWebDir = getBundledWebDir();

    // Priority 1: updated files in userData/web/
    srv.use(function(req, res, next) {
        var relPath = req.path === '/' ? 'index.html' : req.path.replace(/^\//, '');
        var userFile = path.join(userWebDir, relPath);
        try {
            if (fs.existsSync(userFile) && fs.statSync(userFile).isFile()) {
                return res.sendFile(userFile);
            }
        } catch (e) {}
        next();
    });

    // Priority 2: bundled files
    srv.use(express.static(bundledWebDir));

    srv.listen(PORT, '127.0.0.1', callback);
}

// ── Auto-update (web content only, no reinstall needed) ───────────────────────
function checkForUpdates() {
    https.get(VERSION_URL, { timeout: 8000 }, function(res) {
        var data = '';
        res.on('data', function(c) { data += c; });
        res.on('end', function() {
            try {
                var remote = JSON.parse(data);
                var remoteVer = parseInt(remote.version, 10);
                if (isNaN(remoteVer)) return;

                var userWebDir = getUserWebDir();
                var versionFile = path.join(userWebDir, 'version.txt');
                var localVer = 0;
                try { localVer = parseInt(fs.readFileSync(versionFile, 'utf8').trim(), 10) || 0; } catch (e) {}

                if (remoteVer > localVer) {
                    console.log('Actualizando web de v' + localVer + ' → v' + remoteVer);
                    downloadWebFiles(remote.files, userWebDir, remoteVer);
                }
            } catch (e) {}
        });
    }).on('error', function() {
        // Sin internet — continuar con versión actual
    });
}

function downloadWebFiles(files, destDir, version) {
    if (!fs.existsSync(destDir)) fs.mkdirSync(destDir, { recursive: true });

    var pending = files.length;
    var allOk = true;

    files.forEach(function(file) {
        var dest = path.join(destDir, file);
        var dir = path.dirname(dest);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

        var url = BASE_RAW_URL + file;
        var tmp = dest + '.tmp';
        var out = fs.createWriteStream(tmp);

        https.get(url, { timeout: 20000 }, function(res) {
            if (res.statusCode !== 200) {
                allOk = false;
                out.destroy();
                try { fs.unlinkSync(tmp); } catch (e) {}
                if (--pending === 0 && allOk) finalize(destDir, version);
                return;
            }
            res.pipe(out);
            out.on('finish', function() {
                out.close(function() {
                    fs.renameSync(tmp, dest);
                    if (--pending === 0 && allOk) finalize(destDir, version);
                });
            });
        }).on('error', function() {
            allOk = false;
            out.destroy();
            try { fs.unlinkSync(tmp); } catch (e) {}
            if (--pending === 0) {}
        });
    });
}

function finalize(destDir, version) {
    fs.writeFileSync(path.join(destDir, 'version.txt'), String(version));
    console.log('Actualización descargada. Se aplicará en el próximo inicio.');
}

// ── Create window ─────────────────────────────────────────────────────────────
function createWindow() {
    mainWindow = new BrowserWindow({
        fullscreen: true,
        frame: false,
        backgroundColor: '#000022',
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            webSecurity: false
        }
    });

    mainWindow.loadURL('http://127.0.0.1:' + PORT + '/');

    // F11 = toggle fullscreen, ESC = exit fullscreen
    mainWindow.webContents.on('before-input-event', function(event, input) {
        if (input.key === 'F11' && input.type === 'keyDown') {
            mainWindow.setFullScreen(!mainWindow.isFullScreen());
        }
        if (input.key === 'Escape' && input.type === 'keyDown' && mainWindow.isFullScreen()) {
            mainWindow.setFullScreen(false);
        }
    });

    mainWindow.webContents.once('did-finish-load', function() {
        // Check for updates 5 seconds after load
        setTimeout(checkForUpdates, 5000);
    });

    mainWindow.on('closed', function() { mainWindow = null; });
}

// ── App lifecycle ─────────────────────────────────────────────────────────────
app.whenReady().then(function() {
    Menu.setApplicationMenu(null);
    startServer(function() {
        createWindow();
    });
});

app.on('window-all-closed', function() {
    app.quit();
});

app.on('activate', function() {
    if (mainWindow === null) createWindow();
});
