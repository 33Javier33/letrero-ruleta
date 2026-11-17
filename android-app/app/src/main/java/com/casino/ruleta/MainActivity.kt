package com.casino.ruleta

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var server: LocalServer? = null

    companion object {
        private const val PORT = 8765
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/version.json"
        private const val BASE_RAW_URL =
            "https://raw.githubusercontent.com/33Javier33/letrero-ruleta/main/"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen + keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Start local server
        server = LocalServer(PORT, this).also { it.start() }

        // Setup WebView
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, errorCode: Int, desc: String, url: String) {
                // Ignore connection errors to localhost during startup
            }
        }

        webView.loadUrl("http://127.0.0.1:$PORT/")

        // Check for updates in background
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
            // Sin internet — continuar con versión actual sin modificar nada
        }
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
