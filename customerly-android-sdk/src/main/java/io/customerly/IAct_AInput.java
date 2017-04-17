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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
abstract class IAct_AInput extends AppCompatActivity {

    static final String EXTRA_MUST_SHOW_BACK = "EXTRA_MUST_SHOW_BACK";
    private static final int FILE_SELECT_CODE = 5;

    protected boolean _MustShowBack;
    protected boolean _ActivityThemed = false;
    LinearLayout input_layout, input_attachments;
    EditText input_input;
    final ArrayList<IE_Attachment> _Attachments = new ArrayList<>(1);
    @NonNull private final IntentFilter _IntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    @NonNull private final BroadcastReceiver _BroadcastReceiver = new BroadcastReceiver() {
        boolean attendingReconnection = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = IU_Utils.checkConnection(context);
            if(connected) {
                if(this.attendingReconnection) {
                    this.attendingReconnection = false;
                    onReconnection();
                }
            } else {
                this.attendingReconnection = true;
            }
        }
    };

    protected abstract void onReconnection();

    @Override
    protected void onResume() {
        super.onResume();
        this.registerReceiver(this._BroadcastReceiver, this._IntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this._BroadcastReceiver);
    }

    /**
     * Initialize the layout ( must contain layout_powered_by and layout_input_layout )</br>
     * It colors the actionbar, the status bar and initializes the listeners of the input
     * @param pLayoutRes The layout resID
     * @return true if the SDK is configured or false otherwise anc finish is called
     */
    final boolean onCreateLayout(@LayoutRes int pLayoutRes) {
        if(Customerly.get()._isConfigured()) {
            super.setContentView(pLayoutRes);
            //View binding
            final ActionBar actionBar = this.getSupportActionBar();
            final TextView powered_by = (TextView) this.findViewById(R.id.io_customerly__powered_by);
            this.input_input = (EditText) this.findViewById(R.id.io_customerly__input_edit_text);
            View input_button_attach = this.findViewById(R.id.io_customerly__input_button_attach);
            this.input_layout = (LinearLayout) this.findViewById(R.id.io_customerly__input_layout);
            this.input_attachments = (LinearLayout) this.findViewById(R.id.io_customerly__input_attachments);

            this._MustShowBack = this.getIntent().getBooleanExtra(EXTRA_MUST_SHOW_BACK, false);
            if (actionBar != null) {

                String title;
                if(Customerly.get().__PING__LAST_widget_color != 0) {
                    actionBar.setBackgroundDrawable(new ColorDrawable(Customerly.get().__PING__LAST_widget_color));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        this.getWindow().setStatusBarColor(IU_Utils.alterColor(Customerly.get().__PING__LAST_widget_color, 0.8f));
                    }

                    if (IU_Utils.getContrastColor(Customerly.get().__PING__LAST_widget_color) == Color.BLACK) {
                        actionBar.setHomeAsUpIndicator(this.getIntent() != null && this._MustShowBack ? R.drawable.io_customerly__ic_arrow_back_black_24dp : R.drawable.io_customerly__ic_clear_black_24dp);
                        title = String.format("<font color='#000000'>%1$s</font>", actionBar.getTitle());

                    } else {
                        actionBar.setHomeAsUpIndicator(this.getIntent() != null && this._MustShowBack ? R.drawable.io_customerly__ic_arrow_back_white_24dp : R.drawable.io_customerly__ic_clear_white_24dp);
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

            if (Customerly.get().__PING__LAST_powered_by) {
                SpannableString redBoldSpannable = new SpannableString(BuildConfig.CUSTOMERLY_SDK_NAME);
                redBoldSpannable.setSpan(new ForegroundColorSpan(IU_Utils.getColorFromResource(this.getResources(), R.color.io_customerly__blue_malibu)), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                redBoldSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                powered_by.setText(new SpannableStringBuilder(this.getString(R.string.io_customerly__powered_by_)).append(redBoldSpannable));
                powered_by.setOnClickListener(v -> IU_Utils.intentUrl(this, BuildConfig.CUSTOMERLY_WEB_SITE));
                powered_by.setVisibility(View.VISIBLE);
            }else {
                powered_by.setVisibility(View.GONE);
            }

            input_button_attach.setOnClickListener(this._AttachButtonListener);

            this.findViewById(R.id.io_customerly__input_button_send).setOnClickListener(btn -> {
                if(IU_Utils.checkConnection(this)) {
                    String message = this.input_input.getText().toString().trim();
                    IE_Attachment[] attachmentsArray = this._Attachments.toArray(new IE_Attachment[this._Attachments.size()]);
                    if(message.length() != 0 || attachmentsArray.length != 0) {
                        this.input_input.setText(null);
                        this._Attachments.clear();
                        this.input_attachments.removeAllViews();
                        this.onInputActionSend_PerformSend(message, attachmentsArray, null);
                    }
                } else {
                    Toast.makeText(this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                }
            });

            String themeUrl = Customerly.get().__PING__LAST_widget_background_url;
            if(themeUrl != null) {
                ImageView themeIV = (ImageView)this.findViewById(R.id.io_customerly__background_theme);
                Customerly.get()._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                        .centerCrop()
                        .load(themeUrl)
                        .into(this, themeIV));
                themeIV.setVisibility(View.VISIBLE);
                this._ActivityThemed = true;
            }

            return true;
        } else {
            this.finish();
            return false;
        }
    }

    @NonNull private final View.OnClickListener _AttachButtonListener = btn -> {
        if (this._Attachments.size() >= 10) {
            Snackbar.make(btn, R.string.io_customerly__attachments_max_count_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, v -> {}).setActionTextColor(Customerly.get().__PING__LAST_widget_color).show();
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN //Manifest.permission.READ_EXTERNAL_STORAGE has been added in api
            || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    this.startActivityForResult(
                            Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),
                                    this.getString(R.string.io_customerly__choose_a_file_to_attach)), FILE_SELECT_CODE);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, this.getString(R.string.io_customerly__install_a_file_manager), Toast.LENGTH_SHORT).show();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.io_customerly__permission_request)
                            .setMessage(R.string.io_customerly__permission_request_explanation_read)
                            .setPositiveButton(android.R.string.ok, (dlg, which) ->
                                    ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST__READ_EXTERNAL_STORAGE))
                            .show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST__READ_EXTERNAL_STORAGE);
                }
            }
        }
    };

    private static final int PERMISSION_REQUEST__READ_EXTERNAL_STORAGE = 1234;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST__READ_EXTERNAL_STORAGE: {
                int length = Math.min(grantResults.length, permissions.length);
                if (length > 0) {
                    for(int i = 0; i < length; i++) {
                        if(Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])
                                && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            this._AttachButtonListener.onClick(null);
                            return;
                        }
                    }
                }
                Toast.makeText(this, R.string.io_customerly__permission_denied_read, Toast.LENGTH_LONG).show();
            }
        }
    }

    protected abstract void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull IE_Attachment[] pAttachments, @Nullable String ghostToVisitorEmail);

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri fileUri = data.getData();
                    if(fileUri != null) {
                        try {
                            for(IE_Attachment att : this._Attachments) {
                                if(fileUri.equals(att.uri)) {
                                    Snackbar.make(this.input_input, R.string.io_customerly__attachments_already_attached_error, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly.get().__PING__LAST_widget_color).show();
                                    this.input_input.requestFocus();
                                    return;
                                }
                            }
                            if(IU_Utils.getFileSizeFromUri(this, fileUri) > 5000000) {
                                Snackbar.make(this.input_input, R.string.io_customerly__attachments_max_size_error, Snackbar.LENGTH_INDEFINITE)
                                        .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly.get().__PING__LAST_widget_color).show();
                                this.input_input.requestFocus();
                                return;
                            }

                            new IE_Attachment(this, fileUri).addAttachmentToInput(this);
                        } catch (Exception exception) {
                            IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__ATTACHMENT_ERROR, "Error while attaching file: " + exception.getMessage(), exception);
                        }
                    }
                    this.input_input.requestFocus();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void restoreAttachments() {
        if(this.input_attachments != null) {
            this.input_attachments.removeAllViews();
        }
        if(this._Attachments.size() != 0) {
            this._Attachments.clear();
        }
    }
}
