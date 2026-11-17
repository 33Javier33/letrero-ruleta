package com.casino.ruleta

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class LocalServer(port: Int, private val context: Context) : NanoHTTPD(port) {

    private val updatedDir = File(context.filesDir, "web")

    override fun serve(session: IHTTPSession): Response {
        var path = session.uri.trimStart('/')
        if (path.isEmpty() || path.endsWith("/")) path += "index.html"
        if (path.split('/').contains("..")) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden")
        }

        // Priority 1: updated files downloaded from GitHub
        val updatedFile = File(updatedDir, path)
        if (updatedFile.exists() && updatedFile.isFile) {
            return try {
                newFixedLengthResponse(
                    Response.Status.OK,
                    mimeFor(path),
                    FileInputStream(updatedFile),
                    updatedFile.length()
                )
            } catch (e: Exception) {
                serveFromAssets(path)
            }
        }

        return serveFromAssets(path)
    }

    private fun serveFromAssets(path: String): Response {
        return try {
            val stream = context.assets.open("web/$path")
            val bytes = stream.readBytes()
            stream.close()
            newFixedLengthResponse(Response.Status.OK, mimeFor(path), bytes.inputStream(), bytes.size.toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found: $path")
        }
    }

    private fun mimeFor(path: String): String = when {
        path.endsWith(".html") -> "text/html; charset=utf-8"
        path.endsWith(".js")   -> "application/javascript; charset=utf-8"
        path.endsWith(".css")  -> "text/css; charset=utf-8"
        path.endsWith(".json") -> "application/json"
        path.endsWith(".png")  -> "image/png"
        path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
        path.endsWith(".svg")  -> "image/svg+xml"
        path.endsWith(".ico")  -> "image/x-icon"
        else -> "application/octet-stream"
    }
}
