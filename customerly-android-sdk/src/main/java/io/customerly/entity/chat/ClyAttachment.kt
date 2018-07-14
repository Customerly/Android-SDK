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
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Base64
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.ClyIInputActivity
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.dp2px
import io.customerly.utils.ggkext.getFileName
import io.customerly.utils.ggkext.getTyped
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by Gianni on 04/04/18.
 * Project: Customerly-KAndroid-SDK
 */

@Throws(JSONException::class)
internal fun JSONObject.parseAttachment() : ClyAttachment {
    return ClyAttachment(uri = null, name = this.getTyped("name"), path = this.getTyped("path"))
}

internal fun Array<ClyAttachment>?.toJSON(context: Context): JSONArray {
    val array = JSONArray()
    if (this != null) {
        for (attachment in this) {
            val base64 = attachment.loadBase64FromMemory(context)
            if (base64 != null) {
                try {
                    val jo = JSONObject()
                    jo.put("filename", attachment.name)
                    jo.put("base64", base64)
                    array.put(jo)
                } catch (ignored: JSONException) {
                }

            }
        }
    }
    return array
}

@Parcelize
internal class ClyAttachment internal constructor(
        internal val uri : Uri?,
        internal val name : String,
        internal var path : String? = null,
        @field:Transient private var base64 : String? = null
): Parcelable {

    internal constructor(context: Context, uri: Uri): this(uri = uri, name = uri.getFileName(context = context))

    internal fun isImage() =
               this.name.endsWith(suffix = ".jpg", ignoreCase = true)
            || this.name.endsWith(suffix = ".png", ignoreCase = true)
            || this.name.endsWith(suffix = ".jpeg", ignoreCase = true)
            || this.name.endsWith(suffix = ".gif", ignoreCase = true)
            || this.name.endsWith(suffix = ".bmp", ignoreCase = true)

    @Throws(IllegalStateException::class)
    internal fun loadBase64FromMemory(context : Context): String? {
        if(this.base64 == null && this.uri != null) {
            var ins : InputStream? = null
            try {
                ins = context.contentResolver.openInputStream(this.uri)
                if (ins != null) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(1024 * 4)
                    var n = ins.read(buffer)
                    while (n != -1) {
                        output.write(buffer, 0, n)
                        n = ins.read(buffer)
                    }
                    this.base64 = Base64.encodeToString(output.toByteArray(), 0)
                }
            } catch (e : IOException) {
                e.printStackTrace()
            } finally {
                if (ins != null) {
                    try {
                        ins.close()
                    } catch (ignored : IOException) { }
                }
            }
        }
        return this.base64
    }

    internal fun addAttachmentToInput(inputActivity: ClyIInputActivity) {
        val tintColor = Customerly.lastPing.widgetColor.getContrastBW()
        inputActivity.attachments.add(this)
        val tv = TextView(inputActivity)
        tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        ContextCompat.getDrawable(tv.context, R.drawable.io_customerly__ld_chat_attachment)?.apply {
            DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_ATOP)
            DrawableCompat.setTint(this, tintColor)
            tv.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
            tv.compoundDrawablePadding = 5.dp2px
        }
        tv.setPadding(5.dp2px, 0, 0, 0)
        tv.setTextColor(tintColor)
        tv.setLines(1)
        tv.setSingleLine()
        tv.ellipsize = TextUtils.TruncateAt.MIDDLE
        tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
        tv.text = this.name
        tv.setOnClickListener { _ ->
            AlertDialog.Builder(inputActivity)
                    .setTitle(R.string.io_customerly__choose_a_file_to_attach)
                    .setMessage(inputActivity.getString(R.string.io_customerly__cancel_attachment, tv.text))
                    .setNegativeButton(R.string.io_customerly__cancel, null)
                    .setPositiveButton(R.string.io_customerly__remove) { _, _ ->
                        (tv.parent as? ViewGroup)?.removeView(tv)
                        inputActivity.attachments.remove(this)
                    }
                    .setCancelable(true)
                    .show()
        }
        inputActivity.inputAttachments?.addView(tv)
    }
}