package io.customerly.utils.download.imagehandler

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

import android.content.Context
import android.graphics.*
import android.support.annotation.DrawableRes
import android.support.annotation.IntRange
import android.widget.ImageView
import io.customerly.BuildConfig
import io.customerly.utils.ggkext.weak
import java.io.File
import java.lang.ref.WeakReference

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */

internal const val IMAGE_REQUEST_DONT_RESIZE = -1

internal class ClyImageRequest(context : Context, internal val url : String ) {

    internal val customerlyCacheDirPath: String = File(context.cacheDir, BuildConfig.CUSTOMERLY_SDK_NAME).path

    private var scaleType: ImageView.ScaleType? = null
    private fun ImageView.ScaleType?.setTo(iv : ImageView) {
        if(this != null) {
            iv.scaleType = this
        }
    }

    @DrawableRes private var placeholder: Int = 0
    private var onPlaceholder: ((Int)->Unit)? = null

    @DrawableRes private var error: Int = 0
    private var onError: ((Int)->Unit)? = null

    @IntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE)
    private var resizeWidth: Int = IMAGE_REQUEST_DONT_RESIZE
    @IntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE)
    private var resizeHeight: Int = IMAGE_REQUEST_DONT_RESIZE

    private var applyCircleTransformation: Boolean = false

    private var intoImageView: WeakReference<ImageView>? = null
    private var intoGenericTarget: ((Bitmap)->Unit)? = null

    internal fun fitCenter() = this.apply {
        this.scaleType = ImageView.ScaleType.FIT_CENTER
    }
    internal fun centerCrop() = this.apply {
        this.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    internal fun placeholder(@DrawableRes placeholder : Int, onPlaceholder: ((Int)->Unit)? = null) = this.apply {
        this.placeholder = placeholder
        this.onPlaceholder = onPlaceholder
    }

    internal fun error(@DrawableRes error : Int, onError: ((Int)->Unit)? = null) = this.apply {
        this.error = error
        this.onError = onError
    }

    internal fun resize(@IntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE) width : Int, @IntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE) height: Int = width ) = this.apply {
        this.resizeWidth = Math.max(width, IMAGE_REQUEST_DONT_RESIZE)
        this.resizeHeight = Math.max(height, IMAGE_REQUEST_DONT_RESIZE)
    }

    internal fun transformCircle() = this.apply {
        this.applyCircleTransformation = true
    }

    internal fun into(imageView: ImageView) = this.apply {
        this.intoImageView = imageView.weak()
        this.intoGenericTarget = null
    }

    internal fun into(target: (Bitmap)->Unit) = this.apply {
        this.intoImageView = null
        this.intoGenericTarget = target
    }

    internal fun handlerGetDiskKey() = BuildConfig.CUSTOMERLY_SDK_NAME + '-' + "${this.url}|${this.applyCircleTransformation}|${this.resizeWidth}|${this.resizeHeight}".hashCode()

    internal val handlerGetHashCode : Int get() = this.intoImageView?.get()?.hashCode() ?: this.intoGenericTarget?.hashCode() ?: -1

    internal fun handlerApplyTransformations(bmp: Bitmap) : Bitmap {
        return if(this.applyCircleTransformation) {
            val size = Math.min(bmp.width, bmp.height)
            val r = size / 2f
            val squared = Bitmap.createBitmap(bmp, (bmp.width - size) / 2, (bmp.height - size) / 2, size, size)
            val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val paint = Paint().apply {
                this.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                this.isAntiAlias = true
            }
            Canvas(result).drawCircle(r, r, r, paint)
            if (bmp != result) {
                bmp.recycle()
            }
            squared.recycle()
            result
        } else {
            bmp
        }
    }

    internal fun handlerApplyResize(bmp : Bitmap) : Bitmap {
        return if (this.resizeWidth != IMAGE_REQUEST_DONT_RESIZE && this.resizeHeight != IMAGE_REQUEST_DONT_RESIZE) {
            val matrix = Matrix().also {
                it.postScale(this.resizeWidth.toFloat() / bmp.width, this.resizeHeight.toFloat() / bmp.height)
            }
            val resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, false)
            if (resizedBitmap != bmp) {
                bmp.recycle()
            }
            resizedBitmap
        } else {
            bmp
        }
    }

    internal fun handlerValidateRequest() = this.intoGenericTarget ?: this.intoImageView != null

    internal fun handlerOnResponse(bmp : Bitmap) {
        this.intoGenericTarget?.invoke(bmp)
                ?: this.intoImageView?.get()?.let { iv ->
                    this.scaleType.setTo(iv)
                    iv.setImageBitmap(bmp)
                }
    }

    internal fun handlerLoadPlaceholder() {
        val placeholder = this.placeholder
        if(placeholder != 0) {
            this.onPlaceholder?.invoke(placeholder)
                    ?: this.intoImageView?.get()?.let { iv ->
                        this.scaleType.setTo(iv)
                        iv.setImageResource(placeholder)
                    }
        }
    }

    internal fun handlerLoadError() {
        val error = this.error
        if(error != 0) {
            this.onError?.invoke(error)
                    ?: this.intoImageView?.get()?.let { iv ->
                        this.scaleType.setTo(iv)
                        iv.setImageResource(error)
                    }
        }
    }

    internal fun start() {
        ClyImageHandler.request(request = this)
    }
}