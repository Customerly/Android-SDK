package io.customerly.utils.htmlformatter

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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Created by Gianni on 21/03/18.
 * Project: Customerly
 */
internal class URLDrawable internal constructor(resources: Resources) : BitmapDrawable(resources, null as Bitmap?) {
    private var drawable: Drawable? = null

    internal fun setDrawable(drawable: Drawable) {
        this.drawable = drawable
    }

    override fun draw(canvas: Canvas) {
        if (this.drawable != null) {
            this.drawable!!.draw(canvas)
        }
    }
}