package io.customerly.entity

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
import android.support.annotation.Px
import android.text.Spanned
import io.customerly.utils.WriterType
import io.customerly.utils.ggkext.STimestamp

/**
 * Created by Gianni on 11/04/18.
 * Project: Customerly-KAndroid-SDK
 */

internal class ClyConvLastMessage(
        internal val message : Spanned,
        @STimestamp internal val date : Long,
        internal val writer : ClyWriter
) {
    internal constructor(message : Spanned, @STimestamp date : Long, @WriterType writerType : Int, writerId : Long, writerName : String?)
            : this(message = message, date = date, writer = ClyWriter(type = writerType, id = writerId, name = writerName))

    internal fun getImageUrl(@Px sizePx: Int) : String = this.writer.getImageUrl(sizePx = sizePx)

    internal fun getLastWriterName(context : Context) : String = this.writer.getName(context = context)
}