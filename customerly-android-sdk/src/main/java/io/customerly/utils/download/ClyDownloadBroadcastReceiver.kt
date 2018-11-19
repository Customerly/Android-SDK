package io.customerly.utils.download

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

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import android.view.Gravity
import android.widget.Toast
import io.customerly.R
import io.customerly.activity.ClyOpenDownloadedFileActivity
import java.io.File

/**
 * Created by Gianni on 20/02/17.
 * Project: CustomerlyAndroidSDK-demo
 */

internal const val CHANNEL_ID_DOWNLOAD = "io.customerly.customerly_sdk.notification_channel_download"

private val LOCK = arrayOfNulls<Any>(0)
private var pendingDownloadsId = longArrayOf(0)

private fun addPendingDownloadId(id: Long) {
    synchronized(LOCK) {
        pendingDownloadsId.indexOfFirst { it == 0L }
                .let {
                    if(it != -1) {
                        it
                    } else {
                        val newArray = LongArray(size = pendingDownloadsId.size + 1)
                        System.arraycopy(pendingDownloadsId, 0, newArray, 0,pendingDownloadsId.size)
                        pendingDownloadsId = newArray
                        newArray.size - 1
                    }
                }
                .let {
                    pendingDownloadsId[it] = id
                }
    }
}

private fun checkAndRemovePendingDownloadId(id: Long): Boolean {
    return synchronized(LOCK) {
        pendingDownloadsId
                .indexOfFirst { it == id }
                .let {
                    if(it != -1) {
                        pendingDownloadsId[it] = 0
                        true
                    } else {
                        false
                    }
                }
    }
}

internal fun startFileDownload(context: Context, filename: String, fullPath: String) {
    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
        (context.getSystemService(DOWNLOAD_SERVICE) as? DownloadManager)
            ?.let { dm ->
                addPendingDownloadId(
                    id = dm.enqueue(
                            DownloadManager.Request(Uri.parse(fullPath))
                                    .setTitle(filename)
                                    .setDestinationUri(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)))
                                    .setVisibleInDownloadsUi(true)
                                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)))
        }
    } else {
        Toast.makeText(context.applicationContext, R.string.io_customerly__cant_access_external_memory, Toast.LENGTH_SHORT).show()
    }
}

class ClyDownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if(dm != null) {
                var c: Cursor? = null
                try {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if(downloadId != -1L && checkAndRemovePendingDownloadId(id = downloadId)) {

                        c = dm.query(DownloadManager.Query().setFilterById(downloadId))
                        if (c.moveToFirst() && DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                            File(Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))).path).name.takeIf { it.isNotEmpty() }?.let { filename ->
                                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename).takeIf { it.exists() }?.let { file ->
                                    (context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                                            ?.notify(downloadId.toInt(), NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD)
                                            .setSmallIcon(R.drawable.io_customerly__ic_file_download)
                                            .setContentTitle(context.getString(R.string.io_customerly__download_complete))
                                            .setContentText(filename)
                                            .setAutoCancel(true)
                                            .setContentIntent(
                                                    PendingIntent.getActivity(
                                                            context,
                                                            0,
                                                            Intent(context, ClyOpenDownloadedFileActivity::class.java)
                                                                    .setData(FileProvider.getUriForFile(context, "io.customerly.provider.${context.packageName}", file)),
                                                            PendingIntent.FLAG_UPDATE_CURRENT
                                                    )).build())
                                    val toast = Toast.makeText(context, R.string.io_customerly__download_complete, Toast.LENGTH_SHORT)
                                    toast.setGravity(Gravity.TOP, 25, 400)
                                    toast.show()
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {
                } finally {
                    if (c != null) {
                        c.close()
                    }
                }
            }
        }
    }

}
