package io.customerly.entity.chat

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
import android.text.Spanned
import android.widget.ImageView
import androidx.annotation.Px
import io.customerly.R
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.STimestamp
import io.customerly.utils.ggkext.formatByTimeAgo

/**
 * Created by Gianni on 11/04/18.
 * Project: Customerly-KAndroid-SDK
 */

internal class ClyConvLastMessage(
        internal val message : Spanned,
        @STimestamp internal val date : Long,
        internal val writer : ClyWriter,
        internal var discarded : Boolean
) {

    internal fun loadUrl(into: ImageView, @Px size: Int): ClyImageRequest?
            = this.writer.loadUrl(into = into, sizePx = size)

    internal fun getWriterName(context : Context) : String = this.writer.getName(context = context)

    internal fun getTimeFormatted(context: Context) : String {
        return this.date.formatByTimeAgo(
                seconds = { context.resources.getString(R.string.io_customerly__XX_sec_ago, it) },
                minutes = { context.resources.getString(R.string.io_customerly__XX_min_ago, it) },
                hours = { context.resources.getQuantityString(R.plurals.io_customerly__XX_hours_ago, it.toInt(), it) },
                days = { context.resources.getQuantityString(R.plurals.io_customerly__XX_days_ago, it.toInt(), it) },
                weeks = { context.resources.getQuantityString(R.plurals.io_customerly__XX_weeks_ago, it.toInt(), it) },
                months = { context.resources.getQuantityString(R.plurals.io_customerly__XX_months_ago, it.toInt(), it) } )
    }
}