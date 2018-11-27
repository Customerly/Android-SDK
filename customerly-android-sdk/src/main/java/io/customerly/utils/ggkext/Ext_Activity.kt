package io.customerly.utils.ggkext

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import io.customerly.sxdependencies.annotations.SXColorInt
import kotlin.reflect.KClass

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

/**
 * Created by Gianni on 11/08/17.
 */
internal fun Activity.start(activityClass: KClass<out Activity>, extras : Bundle? = null) {
    this.startActivity(Intent(this, activityClass.java)
            .apply {
                if (extras != null) {
                    this.putExtras(extras)
                }
            })
}

internal fun View.start(activityClass: KClass<out Activity>, extras : Bundle? = null) {
    this.activity?.start(activityClass, extras)
}

internal fun Activity.startUrl(url : String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse(if (!url.startsWith("https://") && !url.startsWith("http://")) "http://$url" else url))
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
    } catch (ignored : Exception) { }
}

fun Window.statusBarColorInt(@SXColorInt colorInt : Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.statusBarColor = colorInt
    }
}

fun Activity.statusBarColorInt(@SXColorInt colorInt : Int) {
    this.window.statusBarColorInt(colorInt)
}