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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View

/**
 * Created by Gianni on 21/03/18.
 * Project: Customerly
 *
 * This emulates the behavior of an URLSpan (text colored with linkcolor, underlined and opens url at click.
 * This is compatible with android:autoLink that apparently removes the URLSpan generated from an <a href></a> tag
 */
internal class HrefUrlSpan internal constructor(private val url: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        val context = widget.context
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(this.url)).putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("URLSpan", "Actvity was not found for intent, $intent")
        }
    }
}