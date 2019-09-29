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
import android.widget.ImageView
import io.customerly.R
import io.customerly.sxdependencies.annotations.SXDrawableRes
import io.customerly.sxdependencies.annotations.SXIntRange
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.CUSTOMERLY_SDK_NAME
import io.customerly.utils.ggkext.weak
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */

internal const val IMAGE_REQUEST_DONT_RESIZE = -1

internal class ClyImageRequest(context : Context, internal val url : String ) {

    internal val customerlyCacheDirPath: String = File(context.cacheDir, CUSTOMERLY_SDK_NAME).path

    private var scaleType: ImageView.ScaleType? = null
    private fun ImageView.ScaleType?.setTo(iv : ImageView) {
        if(this != null) {
            iv.scaleType = this
        }
    }

    @SXDrawableRes
    private var placeholder: Int = 0
    private var onPlaceholder: ((Int)->Unit)? = null

    @SXDrawableRes
    private var error: Int = 0
    private var onError: ((Int)->Unit)? = null

    @SXIntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE)
    private var resizeWidth: Int = IMAGE_REQUEST_DONT_RESIZE
    @SXIntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE)
    private var resizeHeight: Int = IMAGE_REQUEST_DONT_RESIZE

    private var applyCircleTransformation: Boolean = false

    private var intoImageView: WeakReference<ImageView>? = null
    private var intoGenericTarget: ((Bitmap)->Unit)? = null

    private var isCancelled: Boolean = false

    internal fun fitCenter() = this.apply {
        this.scaleType = ImageView.ScaleType.FIT_CENTER
    }
    internal fun centerCrop() = this.apply {
        this.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    internal fun placeholder(@SXDrawableRes placeholder : Int, onPlaceholder: ((Int)->Unit)? = null) = this.apply {
        this.placeholder = placeholder
        this.onPlaceholder = onPlaceholder
    }

    internal fun error(@SXDrawableRes error : Int, onError: ((Int)->Unit)? = null) = this.apply {
        this.error = error
        this.onError = onError
    }

    internal fun resize(@SXIntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE) width : Int, @SXIntRange(from = IMAGE_REQUEST_DONT_RESIZE.toLong(), to = Long.MAX_VALUE) height: Int = width ) = this.apply {
        this.resizeWidth = max(width, IMAGE_REQUEST_DONT_RESIZE)
        this.resizeHeight = max(height, IMAGE_REQUEST_DONT_RESIZE)
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

    internal fun handlerGetDiskKey() = CUSTOMERLY_SDK_NAME + '-' + "${this.url}|${this.applyCircleTransformation}|${this.resizeWidth}|${this.resizeHeight}".hashCode()

    internal val handlerGetHashCode : Int get() = this.intoImageView?.get()?.toString()?.hashCode() ?: this.intoGenericTarget?.hashCode() ?: -1

    internal fun handlerApplyTransformations(bmp: Bitmap) : Bitmap {
        return if(this.applyCircleTransformation) {
            val size = min(bmp.width, bmp.height)
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

    internal fun handlerSetScaleType() {
        this.intoImageView?.get()?.let { iv ->
            this.scaleType.setTo(iv)
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

    @SXUiThread
    internal fun handlerOnResponse(bmp : Bitmap) {
        if(!this.isCancelled) {
            this.intoGenericTarget?.invoke(bmp)
                    ?: this.intoImageView?.get()?.let { iv ->
                        if (!this.isCancelled){
                            iv.setTag(R.id.io_customerly__icon, bmp)
                            iv.setImageBitmap(bmp)
                        }
                    }
        }
    }

    @SXUiThread
    internal fun handlerLoadPlaceholder() {
        if(!this.isCancelled) {
            val placeholder = this.placeholder
            if (placeholder != 0) {
                this.onPlaceholder?.invoke(placeholder)
                        ?: this.intoImageView?.get()?.let { iv ->
                            iv.setTag(R.id.io_customerly__icon, placeholder)
                            iv.setImageBitmap(this.handlerApplyTransformations(BitmapFactory.decodeResource(iv.resources, placeholder)))
                        }
            }
        }
    }

    @SXUiThread
    internal fun handlerLoadError() {
        if(!this.isCancelled) {
            val error = this.error
            if (error != 0) {
                this.onError?.invoke(error)
                        ?: this.intoImageView?.get()?.let { iv ->
                            if (!this.isCancelled) {
                                iv.setTag(R.id.io_customerly__icon, error)
                                iv.setImageBitmap(this.handlerApplyTransformations(BitmapFactory.decodeResource(iv.resources, error)))
                            }
                        }
            }
        }
    }

    internal fun start(): ClyImageRequest {
        ClyImageHandler.request(request = this)
        return this
    }

    internal fun cancel() {
        this.isCancelled = true
    }
}