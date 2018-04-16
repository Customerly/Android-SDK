@file:Suppress("unused")

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

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import io.customerly.utils.download.imagehandler.ClyImageHandler
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.dp2px
import java.util.*
import java.util.regex.Pattern

/**
 * Created by Gianni on 21/03/18.
 * Project: Customerly
 */

internal fun fromHtml(
        message: String?,
        tv: TextView? = null,
        pImageClickableSpan: ((Activity, String)->Unit)? = null,
        pImageDownloader: (Context, String, (Drawable)->Unit)->Unit = { context: Context, source: String, handleDrawable: (Drawable) -> Unit ->
            ClyImageHandler.request(
                    request = ClyImageRequest(context = context, url = source)
                            .into { bmp ->
                                if(Looper.getMainLooper().thread != Thread.currentThread()) {
                                    Handler(Looper.getMainLooper()).post {
                                        handleDrawable(BitmapDrawable(context.resources, bmp))
                                    }
                                } else {
                                    handleDrawable(BitmapDrawable(context.resources, bmp))
                                }
                            })
        }): Spanned {
    if (message == null || message.isEmpty()) {
        return SpannedString("")
    } else {
        val newLine = "<br>"
        val sb = StringBuilder(message.replace("\n".toRegex(), newLine).replace("&#10;".toRegex(), newLine))

        //Gestione tag <ol> <ul> <li> e relative chiusure
        var i = 0
        var inOl = false
        var inUl = false
        var olCount = 1
        val tagOlOpen = "<ol>"
        val tagOlClose = "</ol>"
        val tagUlOpen = "<ul>"
        val tagUlClose = "</ul>"
        val tagLiOpen = "<li>"
        val tagLiClose = "</li>"
        val replacementOlLiCountMoreThan9 = "$newLine%d. "
        val replacementOlLiCountLessThan9 = "$newLine  %d. "
        val replacementUlLi = "$newLine   •  "

        while (i < sb.length) {
            if (inOl) {
                val endOl = sb.indexOf(tagOlClose, i)
                val li = sb.indexOf(tagLiOpen, i)
                if (li != -1 && (li < endOl || endOl == -1)) {
                    val liEnd = sb.indexOf(tagLiClose, i)
                    sb.replace(liEnd, liEnd + tagLiClose.length, newLine)
                    val replaceString = String.format(Locale.UK, if (olCount > 9) replacementOlLiCountMoreThan9 else replacementOlLiCountLessThan9, olCount++)
                    sb.replace(li, li + tagLiOpen.length, replaceString)
                    i = liEnd + newLine.length - tagLiOpen.length + replaceString.length
                } else if (endOl != -1) {
                    sb.delete(endOl, endOl + tagOlClose.length)
                    i = endOl
                    inOl = false
                } else {
                    break
                }
            } else if (inUl) {
                val endUl = sb.indexOf(tagUlClose, i)
                val li = sb.indexOf(tagLiOpen, i)
                if (li != -1 && (li < endUl || endUl == -1)) {
                    val liEnd = sb.indexOf(tagLiClose, i)
                    sb.replace(liEnd, liEnd + tagLiClose.length, newLine)
                    sb.replace(li, li + tagLiOpen.length, replacementUlLi)
                    i = liEnd + newLine.length - tagLiOpen.length + replacementUlLi.length
                } else if (endUl != -1) {
                    sb.delete(endUl, endUl + tagUlClose.length)
                    i = endUl
                    inUl = false
                } else {
                    break
                }
            } else {
                val startOl = sb.indexOf(tagOlOpen, i)
                val startUl = sb.indexOf(tagUlOpen, i)
                if (startOl != -1 && (startOl < startUl || startUl == -1)) {
                    sb.delete(startOl, startOl + tagOlOpen.length)
                    inOl = true
                    olCount = 1
                    //i = i;//ho cancellato i 4 caratteri
                } else if (startUl != -1) {
                    sb.delete(startUl, startUl + tagUlOpen.length)
                    inUl = true
                    //i = i;//ho cancellato i 4 caratteri
                } else {
                    break
                }
            }
        }

        val spanImageImageGetter = if (tv == null) {
            null
        } else {
            Html.ImageGetter { source ->
                val drawable = URLDrawable(tv.resources)
                pImageDownloader.invoke(tv.context, source, { resource ->
                    var width = 250.dp2px
                    var height = (250f / resource.intrinsicWidth * resource.intrinsicHeight).toInt().dp2px
                    if (height > 250.dp2px) {//Maxheight 250dp
                        height = 250.dp2px
                        width = (250f / resource.intrinsicHeight * resource.intrinsicWidth).toInt().dp2px
                    }
                    drawable.setBounds(0, 0, width, height)
                    resource.setBounds(0, 0, width, height)
                    drawable.setDrawable(resource)
                    tv.text = tv.text
                })
                drawable
            }
        }
        val emojiHandler = Html.TagHandler { opening, tag, output, _ ->
            if (tag.equals("emoji", ignoreCase = true)) {
                if (opening) {
                    output.setSpan(EmojiSpan(), output.length, output.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                } else {
                    val emojiStart = output.getSpans(0, output.length, EmojiSpan::class.java)
                    if (emojiStart.isNotEmpty()) {
                        val startEmojiIndex = output.getSpanStart(emojiStart[emojiStart.size - 1])
                        output.removeSpan(emojiStart[emojiStart.size - 1])
                        val emojiUnicode: CharSequence?
                        try {
                            emojiUnicode = object : CharSequence {
                                internal var chars = Character.toChars(Integer.decode("0x" + output.subSequence(startEmojiIndex, output.length).toString().trim { it <= ' ' }))

                                override val length: Int get() = this.chars.size
                                override fun get(index: Int): Char = this.chars[index]
                                override fun subSequence(startIndex: Int, endIndex: Int) = String(this.chars, startIndex, endIndex)
                            }
                            output.replace(startEmojiIndex, output.length, emojiUnicode, 0, emojiUnicode.length)
                        } catch (ignored: IllegalArgumentException) {
                        }
                    }
                }
            }
        }

        val spannedMessage: Spanned = spannedFromHtml(source = sb.toString(), imageGetter = spanImageImageGetter, tagHandler = emojiHandler)

        val ssb = spannedMessage as? SpannableStringBuilder ?: SpannableStringBuilder(spannedMessage)

        val matcher = Pattern.compile("\n\n").matcher(ssb)
        var removedCount = 0
        while (matcher.find()) {
            val posMatch = matcher.start() - removedCount
            ssb.delete(posMatch, posMatch + 1)
            removedCount++
        }
        if (ssb.isNotEmpty() && ssb[ssb.length - 1] == '\n') {
            ssb.delete(ssb.length - 1, ssb.length)
        }

        if (pImageClickableSpan != null) {
            val imageSpans = ssb.getSpans(0, ssb.length, ImageSpan::class.java)
            for (imageSpan in imageSpans) {
                if (imageSpan.source != null) {
                    ssb.setSpan(object : ClickableSpan() {
                        internal var lastClickedAt = 0L
                        override fun onClick(widget: View) {
                            val now = System.currentTimeMillis()
                            if (now - this.lastClickedAt > 100) {//Trick because the onClick is fired twice
                                this.lastClickedAt = now
                                if (widget.context is Activity) {
                                    pImageClickableSpan.invoke(widget.context as Activity, imageSpan.source)
                                }
                            }
                        }
                    }, ssb.getSpanStart(imageSpan), ssb.getSpanEnd(imageSpan), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        if (ssb.isNotEmpty()) {
            val spans = ssb.getSpans(0, ssb.length, URLSpan::class.java)
            if (spans.isNotEmpty()) {
                for (urlSpan in spans) {
                    ssb.setSpan(HrefUrlSpan(urlSpan.url), ssb.getSpanStart(urlSpan), ssb.getSpanEnd(urlSpan), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.removeSpan(urlSpan)
                }
            }
        }

        return ssb
    }
}

const val HTML_FLAG_MODE_LEGACY = /*Html.FROM_HTML_MODE_LEGACY*/0

fun spannedFromHtml(source : String, flags : Int = HTML_FLAG_MODE_LEGACY, imageGetter : Html.ImageGetter? = null, tagHandler: Html.TagHandler? = null) : Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, flags, imageGetter, tagHandler)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(source, imageGetter, tagHandler)
    }
}