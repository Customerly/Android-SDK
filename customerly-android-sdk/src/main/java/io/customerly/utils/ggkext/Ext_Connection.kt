package io.customerly.utils.ggkext

import android.graphics.BitmapFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Gianni on 15/05/18.
 * Project: CustomerlyApp
 */
@Throws(IOException::class)
internal fun String.resolveUrl(maxRedirectFollow: Int = 10): HttpURLConnection {
    return (URL(this).openConnection() as HttpURLConnection).let {
        it.readTimeout = 5000
        it.doInput = true
        it.instanceFollowRedirects = maxRedirectFollow > 0
        it.connect()
        when (it.responseCode) {
            HttpURLConnection.HTTP_OK -> it
            HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER -> {
                if (maxRedirectFollow > 0) {
                    it.getHeaderField("Location").resolveUrl(maxRedirectFollow - 1)
                } else {
                    it
                }
            }
            else -> it
        }
    }
}

@Throws(IOException::class)
internal fun String.resolveBitmapUrl() = BitmapFactory.decodeStream(this.resolveUrl(maxRedirectFollow = 10).inputStream)