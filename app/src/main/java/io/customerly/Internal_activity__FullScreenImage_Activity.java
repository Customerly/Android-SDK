package io.customerly;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

/**
 * Created by Gianni on 23/09/16.
 * Project: CustomerlySDK
 */

public final class Internal_activity__FullScreenImage_Activity extends AppCompatActivity {
    static final String EXTRA_IMAGESOURCE = "EXTRA_IMAGESOURCE";

    private String _SourceUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getIntent() != null) {
            this._SourceUrl = this.getIntent().getStringExtra(EXTRA_IMAGESOURCE);
            if(this._SourceUrl != null) {
                ImageView _ImageView = new ImageView(this);
                _ImageView.setBackgroundColor(Color.WHITE);
                _ImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                try {
                    Customerly.get()._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
                            .fitCenter()
                            .load(this._SourceUrl)
                            .into(_ImageView)
                            .placeholder(R.drawable.io_customerly__pic_placeholder_fullscreen));
                    super.setContentView(_ImageView);

                    final ActionBar actionBar = this.getSupportActionBar();
                    if (actionBar != null) {
                        String title;
                        if(Customerly._Instance.__PING__LAST_widget_color != 0) {
                            actionBar.setBackgroundDrawable(new ColorDrawable(Customerly._Instance.__PING__LAST_widget_color));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                this.getWindow().setStatusBarColor(Internal_Utils__Utils.alterColor(Customerly._Instance.__PING__LAST_widget_color, 0.8f));
                            }

                            if (Internal_Utils__Utils.getContrastColor(Customerly._Instance.__PING__LAST_widget_color) == Color.BLACK) {
                                actionBar.setHomeAsUpIndicator(R.drawable.io_customerly__ic_arrow_back_black_24dp);
                                title = String.format("<font color='#000000'>%1$s</font>", actionBar.getTitle());

                            } else {
                                actionBar.setHomeAsUpIndicator(R.drawable.io_customerly__ic_arrow_back_white_24dp);
                                title = String.format("<font color='#ffffff'>%1$s</font>", actionBar.getTitle());
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                actionBar.setTitle(Html.fromHtml(title, 0));
                            } else {
                                //noinspection deprecation
                                actionBar.setTitle(Html.fromHtml(title));
                            }
                        }
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    }
                    return;
                } catch (Exception glideException) {
                    Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GLIDE_ERROR, "Error during Glide loading in FullScreenImage_Activity", glideException);
                }
            }
        }
        this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Customerly.get().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Customerly.get().setCurrentActivity(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.io_customerly__menu_downloadimage, menu);
        return true;
    }

    @Override
    @Contract("null -> false")
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item != null) {
            if (item.getItemId() == R.id.io_customerly__menu__download) {
                this.startAttachmentDownload();
                return true;
            }
            switch (item.getItemId()) {
                case android.R.id.home:
                    this.finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        } else {
            return false;
        }
    }

    private void startAttachmentDownload() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            final DownloadManager dm = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            final long downloadReference = dm.enqueue(
                    new DownloadManager.Request(Uri.parse(this._SourceUrl))
                            .setTitle("Immagine")
                            .setDescription("Immagine")
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Immagine")
                            .setVisibleInDownloadsUi(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE));
            this.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(downloadReference == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)) {
                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                                .notify(999, new NotificationCompat.Builder(Internal_activity__FullScreenImage_Activity.this)
                                        .setSmallIcon(Internal_activity__FullScreenImage_Activity.this.getApplication().getApplicationInfo().icon)
                                        .setContentTitle("Immagine")
                                        .setContentText("Immagine")
                                        .setAutoCancel(true)
                                        .setContentIntent(PendingIntent.getActivity(
                                                Internal_activity__FullScreenImage_Activity.this,
                                                0,
                                                new Intent().setAction(DownloadManager.ACTION_VIEW_DOWNLOADS),
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        )).build());

                        Toast toast = Toast.makeText(Internal_activity__FullScreenImage_Activity.this, R.string.io_customerly__download_complete, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 25, 400);
                        toast.show();
                    }
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.io_customerly__permission_request)
                        .setMessage(R.string.io_customerly__permission_request_explanation_write)
                        .setPositiveButton(android.R.string.ok, (dlg, which) ->
                                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE))
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private static final int PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 99;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.startAttachmentDownload();
                } else {
                    Toast.makeText(this, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

//    private void saveImageToGallery() {
//        if(this._ImageView != null) {
//            this._ImageView.setDrawingCacheEnabled(true);
//            MediaStore.Images.Media.insertImage(this.getContentResolver(), this._ImageView.getDrawingCache(), "Image", "Image");
//        }
//    }
}
