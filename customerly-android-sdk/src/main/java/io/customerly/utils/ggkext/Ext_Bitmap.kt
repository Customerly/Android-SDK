@file:Suppress("unused")

package io.customerly.utils.ggkext

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import java.io.File
import java.io.FileInputStream

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

internal fun File.decodeBitmap(requiredSize: Int): Bitmap? {
    return nullOnException { it.decodeBitmap(inSampleSize = it.findScale(requiredSize = requiredSize)) }
}

internal fun File.decodeBitmap(inSampleSize: Int? = null): Bitmap? {
    return BitmapFactory.decodeStream(FileInputStream(this), null, inSampleSize?.let {
        BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
    })
}

internal fun File.findScale(requiredSize: Int): Int {
    return BitmapFactory.Options().let {
        it.inJustDecodeBounds = true
        BitmapFactory.decodeStream(FileInputStream(this), null, it)
        var widthTmp = it.outWidth
        var heightTmp = it.outHeight
        var scale = 1
        while (widthTmp / 2 >= requiredSize && heightTmp / 2 >= requiredSize) {
            widthTmp /= 2
            heightTmp /= 2
            scale *= 2
        }
        scale
    }
}