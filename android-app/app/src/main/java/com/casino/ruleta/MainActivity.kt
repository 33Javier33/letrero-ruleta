package com.casino.ruleta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var webView: WebView

    companion object {
        const val PORT = 8765
        const val BASE = "http://127.0.0.1:$PORT"
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/version.json"
        private const val BASE_RAW_URL =
            "https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/"

        private var server: LocalServer? = null

        // Un solo servidor para todo el proceso (operador + pantalla de la mesa)
        fun ensureServer(ctx: Context) {
            if (server == null) {
                server = LocalServer(PORT, ctx.applicationContext).also { it.start() }
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        fun setupWebView(wv: WebView) {
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                mediaPlaybackRequiresUserGesture = false
            }
            wv.webChromeClient = WebChromeClient()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ensureServer(this)

        webView = WebView(this)
        setContentView(webView)
        setupWebView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val local = uri.host == "127.0.0.1" || uri.host == "localhost"
                if (local && !(uri.path ?: "/").startsWith("/operador")) {
                    // "Abrir Pantalla de la Mesa" → pantalla completa aparte
                    startActivity(Intent(this@MainActivity, DisplayActivity::class.java)
                        .putExtra("url", uri.toString()))
                    return true
                }
                if (!local) {
                    // Links externos (descargas, etc.) → navegador del sistema
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return false
            }
        }

        webView.loadUrl("$BASE/operador/")

        Thread { checkForUpdates() }.start()
    }

    private fun checkForUpdates() {
        try {
            val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Cache-Control", "no-cache")
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            val remoteVersion = json.getString("version").toIntOrNull() ?: return

            val webDir = File(filesDir, "web")
            val versionFile = File(webDir, "version.txt")
            val localVersion = if (versionFile.exists())
                versionFile.readText().trim().toIntOrNull() ?: 0 else 0

            if (remoteVersion > localVersion) {
                val files = json.getJSONArray("files")
                webDir.mkdirs()
                var allOk = true

                for (i in 0 until files.length()) {
                    val file = files.getString(i)
                    val dest = File(webDir, file)
                    dest.parentFile?.mkdirs()
                    val tmp = File(dest.parent, "${dest.name}.tmp")

                    try {
                        val fc = URL(BASE_RAW_URL + file).openConnection() as HttpURLConnection
                        fc.connectTimeout = 20000
                        fc.readTimeout = 20000
                        fc.inputStream.use { input ->
                            tmp.outputStream().use { out -> input.copyTo(out) }
                        }
                        fc.disconnect()
                        tmp.renameTo(dest)
                    } catch (e: Exception) {
                        tmp.delete()
                        allOk = false
                    }
                }

                if (allOk) versionFile.writeText(remoteVersion.toString())
            }
        } catch (e: Exception) {
            // Sin internet — continuar con la versión actual
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
