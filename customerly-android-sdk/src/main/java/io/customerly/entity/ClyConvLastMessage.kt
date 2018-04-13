package io.customerly.entity

import android.content.Context
import android.support.annotation.Px
import android.text.Spanned
import io.customerly.WriterType
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