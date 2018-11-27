@file:Suppress("unused")

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

package io.customerly.utils.ggkext

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.view.LayoutInflater
import io.customerly.sxdependencies.annotations.SXDrawableRes
import io.customerly.sxdependencies.annotations.SXRequiresPermission

/**
 * Created by Gianni on 11/08/17.
 */
internal fun Context.inflater() : LayoutInflater = LayoutInflater.from(this)

@SXDrawableRes
internal fun Context.getDrawableId(drawableName :String) : Int =
        this.resources.getIdentifier(drawableName, "drawable", this.packageName)

@SXRequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
internal fun Context.checkConnection(): Boolean =
        (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnectedOrConnecting ?: false

internal val Context.tryBaseContextActivity :Activity?
    get() {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }