package io.customerly;

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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
abstract class Internal_activity__AInput_Customerly_Activity extends AppCompatActivity {

    static final String EXTRA_MUST_SHOW_BACK = "EXTRA_MUST_SHOW_BACK";
    private static final int FILE_SELECT_CODE = 5;

    LinearLayout input_layout, input_attachments;
    EditText input_input;
    final ArrayList<Internal_entity__Attachment> _Attachments = new ArrayList<>(1);
    @NonNull private final IntentFilter _IntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    @NonNull private final BroadcastReceiver _BroadcastReceiver = new BroadcastReceiver() {
        boolean attendingReconnection = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = Internal_Utils__Utils.checkConnection(context);
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
        Customerly._Instance._CurrentActivityClass = this.getClass();
        this.registerReceiver(this._BroadcastReceiver, this._IntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Customerly._Instance._CurrentActivityClass = null;
        this.unregisterReceiver(this._BroadcastReceiver);
    }

    /**
     * Initialize the layout ( must contain layout_powered_by and layout_input_layout )</br>
     * It colors the actionbar, the status bar and initializes the listeners of the input
     * @param pLayoutRes The layout resID
     * @return true if the SDK is configured or false otherwise anc finish is called
     */
    final boolean onCreateLayout(@LayoutRes int pLayoutRes) {
        if(Customerly._Instance._isConfigured()) {
            super.setContentView(pLayoutRes);
            //View binding
            final ActionBar actionBar = this.getSupportActionBar();
            final TextView powered_by = (TextView) this.findViewById(R.id.io_customerly__powered_by);
            this.input_input = (EditText) this.findViewById(R.id.io_customerly__input_edit_text);
            View input_button_attach = this.findViewById(R.id.io_customerly__input_button_attach);
            this.input_layout = (LinearLayout) this.findViewById(R.id.io_customerly__input_layout);
            this.input_attachments = (LinearLayout) this.findViewById(R.id.io_customerly__input_attachments);

            if (actionBar != null) {

                String title;
                if(Customerly._Instance.__PING__LAST_widget_color != 0) {
                    actionBar.setBackgroundDrawable(new ColorDrawable(Customerly._Instance.__PING__LAST_widget_color));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        this.getWindow().setStatusBarColor(Internal_Utils__Utils.alterColor(Customerly._Instance.__PING__LAST_widget_color, 0.8f));
                    }

                    if (Internal_Utils__Utils.getContrastColor(Customerly._Instance.__PING__LAST_widget_color) == Color.BLACK) {
                        actionBar.setHomeAsUpIndicator(this.getIntent() != null && this.getIntent().getBooleanExtra(EXTRA_MUST_SHOW_BACK, false) ? R.drawable.io_customerly__ic_arrow_back_black_24dp : R.drawable.io_customerly__ic_clear_black_24dp);
                        title = String.format("<font color='#000000'>%1$s</font>", actionBar.getTitle());

                    } else {
                        actionBar.setHomeAsUpIndicator(this.getIntent() != null && this.getIntent().getBooleanExtra(EXTRA_MUST_SHOW_BACK, false) ? R.drawable.io_customerly__ic_arrow_back_white_24dp : R.drawable.io_customerly__ic_clear_white_24dp);
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

            if (Customerly._Instance.__PING__LAST_powered_by) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(this.getString(R.string.io_customerly__powered_by_));
                SpannableString redBoldSpannable= new SpannableString(BuildConfig.CUSTOMERLY_SDK_NAME);

                if(Customerly._Instance.__PING__LAST_widget_color != 0) {
                    int linkColor = Customerly._Instance.__PING__LAST_widget_color;

                    while (Internal_Utils__Utils.getContrastColor(linkColor) == Color.BLACK) {
                        linkColor = Internal_Utils__Utils.alterColor(linkColor, 0.95f);
                    }

                    redBoldSpannable.setSpan(new ForegroundColorSpan(linkColor), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                redBoldSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                ssb.append(redBoldSpannable);
                powered_by.setText(ssb);
                powered_by.setOnClickListener(v -> Internal_Utils__Utils.intentUrl(this, BuildConfig.CUSTOMERLY_WEB_SITE));
                powered_by.setVisibility(View.VISIBLE);
            }/* else {
                powered_by.setVisibility(View.GONE);//Da layout
            }*/

            input_button_attach.setOnClickListener(this._AttachButtonListener);

            this.findViewById(R.id.io_customerly__input_button_send).setOnClickListener(btn -> {
                if(Internal_Utils__Utils.checkConnection(this)) {
                    String message = this.input_input.getText().toString().trim();
                    Internal_entity__Attachment[] attachmentsArray = this._Attachments.toArray(new Internal_entity__Attachment[this._Attachments.size()]);
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

            return true;
        } else {
            this.finish();
            return false;
        }
    }

    @NonNull private final View.OnClickListener _AttachButtonListener = btn -> {
        if (this._Attachments.size() >= 10) {
            Snackbar.make(btn, R.string.io_customerly__attachments_max_count_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, v -> {}).setActionTextColor(Customerly._Instance.__PING__LAST_widget_color).show();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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

    private static final int PERMISSION_REQUEST__READ_EXTERNAL_STORAGE = 99;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST__READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this._AttachButtonListener.onClick(null);
                } else {
                    Toast.makeText(this, R.string.io_customerly__permission_denied_read, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected abstract void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail);

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
                            for(Internal_entity__Attachment att : this._Attachments) {
                                if(fileUri.equals(att.uri)) {
                                    Snackbar.make(this.input_input, R.string.io_customerly__attachments_already_attached_error, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly._Instance.__PING__LAST_widget_color).show();
                                    this.input_input.requestFocus();
                                    return;
                                }
                            }
                            if(Internal_Utils__Utils.getFileSizeFromUri(this, fileUri) > 5000000) {
                                Snackbar.make(this.input_input, R.string.io_customerly__attachments_max_size_error, Snackbar.LENGTH_INDEFINITE)
                                        .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly._Instance.__PING__LAST_widget_color).show();
                                this.input_input.requestFocus();
                                return;
                            }

                            new Internal_entity__Attachment(this, fileUri).addAttachmentToInput(this);
                        } catch (Exception exception) {
                            Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__ATTACHMENT_ERROR, "Error while attaching file: " + exception.getMessage(), exception);
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
