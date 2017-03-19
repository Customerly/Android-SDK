package io.customerly;

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

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

import java.io.File;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Gianni on 20/02/17.
 * Project: CustomerlyAndroidSDK-demo
 */
@RestrictTo(android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public class IBR_DownloadBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Cursor c = null;
        try {
            final DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if(downloadID != -1 && IBR_DownloadBroadcastReceiver.checkAndRemove(downloadID)) {
                    String filename = null;
                    c = dm.query(new DownloadManager.Query().setFilterById(downloadID));
                    if (c.moveToFirst() && DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        filename = new File(Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))).getPath()).getName();
                    }

                    if(filename != null) {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                        if(file.exists()) {
                            ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                                    .notify((int) downloadID, new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.ic_file_download)
                                            .setContentTitle(context.getString(R.string.io_customerly__download_complete))
                                            .setContentText(filename)
                                            .setAutoCancel(true)
                                            .setContentIntent(
                                                    PendingIntent.getActivity(
                                                            context,
                                                            0,
                                                            new Intent(context, IAct_OpenDownloadedFileActivity.class)
                                                                    .setData(
                                                                            IU_CustomerlyFileProvider.getUriForFile(context, "io.customerly.provider", file)
                                                                    ),
                                                            PendingIntent.FLAG_UPDATE_CURRENT
                                                    )).build());

                            Toast toast = Toast.makeText(context, R.string.io_customerly__download_complete, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.TOP, 25, 400);
                            toast.show();
                        }
                    }
                }
            }
        } catch (Exception ignored) { } finally {
            if(c != null) {
                c.close();
            }
        }
    }

    public static void startDownload(@NonNull Context context, @NonNull String filename, @NonNull String full_path) {
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            final DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            IBR_DownloadBroadcastReceiver.addID(
                    dm.enqueue(
                    new DownloadManager.Request(Uri.parse(full_path))
                            .setTitle(filename)
                            .setDestinationUri(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)))
                            .setVisibleInDownloadsUi(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)));
        } else {
            Toast.makeText(context.getApplicationContext(), R.string.io_customerly__cant_access_external_memory, Toast.LENGTH_SHORT).show();
        }
    }
    @NonNull private static final Object[] LOCK = new Object[0];
    @NonNull private static long[] _PendingDownloadsID = new long[] {0};
    private static void addID(long id) {
        synchronized (LOCK) {
            for(int i = 0; i < _PendingDownloadsID.length; i++) {
                if(_PendingDownloadsID[i] == 0) {
                    _PendingDownloadsID[i] = id;
                }
            }
            long[] newArray = new long[_PendingDownloadsID.length + 1];
            System.arraycopy(_PendingDownloadsID, 0, newArray, 0, _PendingDownloadsID.length);
            newArray[newArray.length - 1] = id;
            _PendingDownloadsID = newArray;
        }
    }
    private static boolean checkAndRemove(long id) {
        synchronized (LOCK) {
            for(int i = 0; i < _PendingDownloadsID.length; i++) {
                if(_PendingDownloadsID[i] == id) {
                    _PendingDownloadsID[i] = 0;
                    return true;
                }
            }
            return false;
        }
    }
}
