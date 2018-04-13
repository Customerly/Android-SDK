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

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

/**
 * Created by Gianni on 23/09/16.
 * Project: Customerly Android SDK
 */
@RestrictTo(android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public final class IAct_FullScreenImage extends AppCompatActivity implements Customerly.SDKActivity {
    static final String EXTRA_IMAGE_SOURCE = "EXTRA_IMAGE_SOURCE";

    private String _SourceUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getIntent() != null) {
            this._SourceUrl = this.getIntent().getStringExtra(EXTRA_IMAGE_SOURCE);
            if(this._SourceUrl != null) {
                ImageView _ImageView = new TouchImageView(this);
                _ImageView.setBackgroundColor(Color.WHITE);
                _ImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                _ImageView.setAdjustViewBounds(true);
                try {
                    Customerly.get()._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                            .fitCenter()
                            .load(this._SourceUrl)
                            .into(this, _ImageView)
                            .placeholder(R.drawable.io_customerly__pic_placeholder_fullscreen));
                    super.setContentView(_ImageView);

                    final ActionBar actionBar = this.getSupportActionBar();
                    if (actionBar != null) {
                        String title;
                        if(Customerly.get().__PING__LAST_widget_color != 0) {
                            actionBar.setBackgroundDrawable(new ColorDrawable(Customerly.get().__PING__LAST_widget_color));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                this.getWindow().setStatusBarColor(IU_Utils.alterColor(Customerly.get().__PING__LAST_widget_color, 0.8f));
                            }

                            if (IU_Utils.getContrastColor(Customerly.get().__PING__LAST_widget_color) == Color.BLACK) {
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
                    XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__GLIDE_ERROR, "Error during Glide loading in FullScreenImage_Activity", glideException);
                }
            }
        }
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.io_customerly__menu_download_image, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        XXXIE_JwtToken jwt = Customerly.get()._JwtToken;
        if (jwt == null || jwt.isAnonymous()) {
            this.onLogoutUser();
        }
    }

    @Override
    public void onLogoutUser() {
        this.finish();
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
            int index = this._SourceUrl.lastIndexOf('/');
            String filename;
            if(index != -1 && index < this._SourceUrl.length() - 1) {
                filename = this._SourceUrl.substring(index + 1);
            } else {
                index = this._SourceUrl.lastIndexOf('\\');
                if (index != -1 && index < this._SourceUrl.length() - 1) {
                    filename = this._SourceUrl.substring(index + 1);
                } else {
                    filename = this.getString(R.string.io_customerly__image);
                }
            }
            IBR_DownloadBroadcastReceiver.startDownload(this, filename, this._SourceUrl);
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

    private static final int PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 4321;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE: {
                int length = Math.min(grantResults.length, permissions.length);
                if (length > 0) {
                    for(int i = 0; i < length; i++) {
                        if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            this.startAttachmentDownload();
                            return;
                        }
                    }
                }
                Toast.makeText(this, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onNewSocketMessages(@NonNull ArrayList<XXXIE_Message> messages) { }

    //    private void saveImageToGallery() {
//        if(this._ImageView != null) {
//            this._ImageView.setDrawingCacheEnabled(true);
//            MediaStore.Images.Media.insertImage(this.getContentResolver(), this._ImageView.getDrawingCache(), "Image", "Image");
//        }
//    }
}
