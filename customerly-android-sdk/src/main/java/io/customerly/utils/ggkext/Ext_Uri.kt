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

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

/**
 * Created by Gianni on 04/04/18.
 */
internal fun Uri.getFileName(context: Context): String {
    var result: String? = null
    if (this.scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        this.path?.let {
            val cut = it.lastIndexOf('/')
            if (cut != -1) {
                result = it.substring(cut + 1)
            }
        }
    }
    return result ?: ""
}

internal fun Uri.getFileSize(context: Context): Long {
    return try {
        this.getPath(context)?.let{ File(it).length() } ?: 0
    } catch (impossible: Exception) {
        0
    }
}

private fun Uri.getPath(context: Context): String? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, this) -> {
            return when(this.authority) {
                "com.android.externalstorage.documents" -> {
                    val split = DocumentsContract.getDocumentId(this).split(":")
                    if ("primary".equals(/* type = */split[0], ignoreCase = true)) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    } else {
                        null
                    }
                }
                "com.android.providers.downloads.documents" -> {
                    ContentUris
                            .withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"),
                                    DocumentsContract.getDocumentId(this).toLong())
                            .getDataColumn(context, null, null)
                }
                "com.android.providers.media.documents" -> {
                    val split = DocumentsContract.getDocumentId(this).split(":")
                    when (/* type = */split[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }?.getDataColumn(context = context, selection = "_id=?", selectionArgs = arrayOf(split[1]))
                }
                else -> {
                    null
                }
            }
        }
        "content".equals(this.scheme, ignoreCase = true) -> {
            // Return the remote address
            if ("com.google.android.apps.photos.content" == this.authority){
                this.lastPathSegment
            } else {
                this.getDataColumn(context, null, null)
            }
        }
        "file".equals(this.scheme, ignoreCase = true) -> {
            this.path
        }
        else -> {
            null
        }
    }
}

private fun Uri?.getDataColumn(context: Context, selection: String?, selectionArgs: Array<String>?): String? {
    return this?.let {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(this, arrayOf("_data"), selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow("_data")
                return cursor.getString(index)
            } else {
                null
            }
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }
}