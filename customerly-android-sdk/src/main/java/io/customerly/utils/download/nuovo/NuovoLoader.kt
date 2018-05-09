package io.customerly.utils.download.nuovo

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import io.customerly.R
import io.customerly.utils.ggkext.decodeBitmap
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.write
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Created by Gianni on 07/05/18.
 * Project: CustomerlyApp
 */

val PLACEHOLDER = R.drawable.io_customerly__ic_default_admin//TODO Placeholder
const val REQUIRED_SIZE = 70//TODO Required size

object NuovoLoader {

    private lateinit var cacheDir: File
    private val memoryCache = MemoryCache()
    private var executorService: ExecutorService = Executors.newFixedThreadPool(5)
    private val ivBinding = Collections.synchronizedMap(WeakHashMap<ImageView, String>())

    private fun url2file(url: String) = File(this.cacheDir, URLEncoder.encode(url, "UTF-8"))

    private fun consolidate(context: Context) {
        if(!::cacheDir.isInitialized) {
            this.cacheDir = context.cacheDir.apply {
                if (!this.exists()) {
                    this.mkdirs()
                }
            }
        }
    }

    fun loadImage(url: String, imageView: ImageView) {
        this.consolidate(context = imageView.context)
        this.ivBinding[imageView] = url
        val bitmap: Bitmap? = this.memoryCache[url]
        if(bitmap?.isRecycled == false) {
            imageView.setImageBitmap(bitmap)
        } else {
            this.executorService.submit(ImageRequest(url = url, imageView = imageView))
            imageView.setImageResource(PLACEHOLDER)
        }
    }

    private data class ImageRequest(private val url: String, private val imageView: ImageView) : Runnable {
        override fun run() {
            if(!this.imageView.isReused(url = this.url)) {
                val bitmap = downloadBitmap(url = this.url)?.also {
                    NuovoLoader.memoryCache.put(this.url, it)
                }
                if(!this.imageView.isReused(url = this.url)) {
                    this.imageView.post(ImageDisplay(bitmap = bitmap, url = this.url, imageView = this.imageView))
                }
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val file = this.url2file(url = url)
        return /* from SD cache */ file.decodeBitmap(requiredSize = REQUIRED_SIZE)
            ?: /* from web */ nullOnException {
            (URL(url).openConnection() as HttpURLConnection).apply {
                this.connectTimeout = 30000
                this.readTimeout = 30000
                this.instanceFollowRedirects = true
                this.connect()
            }.write(on = file)
            file.decodeBitmap(requiredSize = REQUIRED_SIZE)
        }
    }

    private data class ImageDisplay(private val bitmap: Bitmap?, private val url: String, private val imageView: ImageView): Runnable {
        override fun run() {
            if(!this.imageView.isReused(url = this.url)) {
                this.bitmap?.let {
                    NuovoLoader.ivBinding.remove(this.imageView)
                    this.imageView.setImageBitmap(it)
                } ?: this.imageView.setImageResource(PLACEHOLDER)
            }
        }
    }

    private fun ImageView.isReused(url: String) = NuovoLoader.ivBinding[this] != url
}
