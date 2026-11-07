package com.abc.tvlauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Two-level icon cache: memory (LruCache) then PNG files in cacheDir/icons.
 * The cache key includes the app's last-update time, so updated apps get fresh icons.
 */
object IconCache {
    private const val SIZE = 192
    private val mem = object : LruCache<String, ImageBitmap>(48 * 1024) { // KB
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }
    private val gate = Semaphore(3)

    private fun key(pkg: String, stamp: Long) = "$pkg-$stamp"

    fun peek(pkg: String, stamp: Long): ImageBitmap? = mem.get(key(pkg, stamp))

    /** Test hook / prefetch: seed the memory cache. */
    internal fun put(pkg: String, stamp: Long, bmp: ImageBitmap) { mem.put(key(pkg, stamp), bmp) }

    suspend fun load(ctx: Context, pkg: String, stamp: Long): ImageBitmap? {
        val k = key(pkg, stamp)
        mem.get(k)?.let { return it }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                mem.get(k)?.let { return@withPermit it }
                val dir = File(ctx.cacheDir, "icons").apply { mkdirs() }
                val file = File(dir, "$k.png")
                var bmp: Bitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
                if (bmp == null) {
                    bmp = runCatching {
                        ctx.packageManager.getApplicationIcon(pkg).toBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
                    }.getOrNull()
                    if (bmp != null) runCatching {
                        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                bmp?.asImageBitmap()?.also { mem.put(k, it) }
            }
        }
    }

    /** Deletes cached icons for apps that were updated or removed. */
    fun prune(ctx: Context, keep: Collection<AppEntry>) {
        val keepNames = keep.mapTo(HashSet()) { "${key(it.pkg, it.updated)}.png" }
        File(ctx.cacheDir, "icons").listFiles()?.forEach { if (it.name !in keepNames) it.delete() }
    }
}

/** Downloads small remote images (team logos) once and keeps them on disk + in memory. */
object RemoteImages {
    private val mem = object : LruCache<String, ImageBitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    fun peek(url: String): ImageBitmap? = mem.get(url)

    suspend fun load(ctx: Context, url: String, targetPx: Int = 128): ImageBitmap? {
        mem.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "logos").apply { mkdirs() }
            val file = File(dir, sha(url) + ".png")
            if (!file.exists()) {
                runCatching {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 6000; c.readTimeout = 8000
                    c.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                }.onFailure { file.delete() }
            }
            if (!file.exists()) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetPx) sample *= 2
            val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            bmp?.asImageBitmap()?.also { mem.put(url, it) }
        }
    }

    private fun sha(s: String) =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
