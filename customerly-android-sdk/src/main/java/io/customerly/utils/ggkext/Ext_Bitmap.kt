@file:Suppress("unused")

package io.customerly.utils.ggkext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build

/**
 * Created by Gianni on 11/11/17.
 */
internal fun Drawable.asBitmap() : Bitmap? {
    return when {
        this is BitmapDrawable -> {
            this.bitmap
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this is VectorDrawable -> {
            val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
            bitmap
        }
        else -> {
            null
        }
    }
}