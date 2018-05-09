package io.customerly.utils.download.nuovo

import android.graphics.Bitmap
import java.lang.ref.SoftReference
import java.util.*

/**
 * Created by Gianni on 07/05/18.
 * Project: CustomerlyApp
 */
class MemoryCache {
    private val cache = Collections.synchronizedMap(HashMap<String, SoftReference<Bitmap>>())

    operator fun get(id: String) = this.cache.takeIf { it.containsKey(id) }?.get(id)?.get()

    fun put(id: String, bitmap: Bitmap) {
        this.cache[id] = SoftReference(bitmap)
    }
}