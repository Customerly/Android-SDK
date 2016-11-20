package io.customerly;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
public abstract class Internal_activity__A_Customerly_Activity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 5;

    protected LinearLayout input_layout, input_attachments;
    protected EditText input_input;
    protected View input_button_attach;
    protected ArrayList<Internal_entity__Attachment> _Attachments = new ArrayList<>(1);
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
        this.registerReceiver(this._BroadcastReceiver, this._IntentFilter);
        Customerly._do(crm -> crm.setCustomerlyActivityActive(true));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this._BroadcastReceiver);
        Customerly._do(crm -> crm.setCustomerlyActivityActive(false));
    }

    /**
     * Inizializza il layout passato ( deve contenere il layout_offerto_da, il layout_input_layout )</br>
     * Colora l'actionbar, la statusbar, e inizializza i listener dell'input
     * @param pLayoutRes Il resID del layout
     * @return l'istanza della Customerly se disponibile e tutto ok altrimenti restituisce null e chiama finish()
     */
    @Nullable protected final Customerly onCreateLayout(@LayoutRes int pLayoutRes) {
        final Customerly retCustomerly = Customerly._do(crm -> {
            super.setContentView(pLayoutRes);
            //View binding
            final ActionBar actionBar = this.getSupportActionBar();
            final TextView offerto_da = (TextView) this.findViewById(R.id.io_customershero__offerto_da);
            this.input_input = (EditText) this.findViewById(R.id.io_customershero__input_edittext);
            this.input_button_attach = this.findViewById(R.id.io_customershero__input_button_attach);
            this.input_layout = (LinearLayout) this.findViewById(R.id.io_customershero__input_layout);
            this.input_attachments = (LinearLayout) this.findViewById(R.id.io_customershero__input_attachments);

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setBackgroundDrawable(new ColorDrawable(crm.__PING__LAST_widget_color));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    this.getWindow().setStatusBarColor(Internal_Utils__Utils.alterColor(crm.__PING__LAST_widget_color, 0.8f));
            }
            if (crm.__PING__LAST_powered_by) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(this.getString(R.string.io_customershero__offerto_da_));
                SpannableString redBoldSpannable= new SpannableString(this.getString(R.string.io_customershero__crmhero));
                redBoldSpannable.setSpan(new ForegroundColorSpan(crm.__PING__LAST_widget_color), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                redBoldSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, redBoldSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                ssb.append(redBoldSpannable);
                offerto_da.setText(ssb);
                offerto_da.setOnClickListener(v -> Internal_Utils__Utils.intentUrl(this, this.getString(R.string.io_customershero__url_offerto_da_crmhero)));
            }

            this.input_button_attach.setOnClickListener(this._AttachButtonListener);

            this.findViewById(R.id.io_customershero__input_button_send).setOnClickListener(btn -> {
                if(Internal_Utils__Utils.checkConnection(this)) {
                    String message = this.input_input.getText().toString().trim();
                    Internal_entity__Attachment[] attachmentsArray = this._Attachments.toArray(new Internal_entity__Attachment[this._Attachments.size()]);
                    if(message.length() != 0 || attachmentsArray.length != 0) {
                        this.input_input.setText(null);
                        this._Attachments.clear();
                        this.input_attachments.removeAllViews();
                        this.onInputActionSend_PerformSend(crm, message, attachmentsArray, null);
                    }
                } else {
                    Toast.makeText(this.getApplicationContext(), R.string.io_customershero__errore_connessione_probabile, Toast.LENGTH_SHORT).show();
                }
            });

            return crm;
        }, null);

        if(retCustomerly == null) {
            this.finish();
            return null;
        } else {
            return retCustomerly;
        }
    }

    @NonNull private final View.OnClickListener _AttachButtonListener = btn -> {
        if (this._Attachments.size() >= 10) {
            Snackbar.make(btn, R.string.io_customershero__attachments_maxcount_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, v -> {
                    }).setActionTextColor(Customerly._do(crm -> crm.__PING__LAST_widget_color, Customerly.DEF_WIDGETCOLOR_INT)).show();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    this.startActivityForResult(
                            Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),
                                    this.getString(R.string.io_customershero__scegli_file)), FILE_SELECT_CODE);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(this, this.getString(R.string.io_customershero__installa_un_file_manager), Toast.LENGTH_SHORT).show();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.io_customershero__permission_request)
                            .setMessage(R.string.io_customershero__permission_request_explanation_read)
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
                    Toast.makeText(this, R.string.io_customershero__permission_denied_read, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected abstract void onInputActionSend_PerformSend(@NonNull Customerly pCustomerly, @NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail);

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
                                    Snackbar.make(this.input_input, R.string.io_customershero__attachments_alreadyattached_error, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly._do(crm -> crm.__PING__LAST_widget_color, Customerly.DEF_WIDGETCOLOR_INT)).show();
                                    this.input_input.requestFocus();
                                    return;
                                }
                            }
                            if(Internal_Utils__Utils.getFileSizeFromUri(this, fileUri) > 5000000) {
                                Snackbar.make(this.input_input, R.string.io_customershero__attachments_maxsize_error, Snackbar.LENGTH_INDEFINITE)
                                        .setAction(android.R.string.ok, v -> { }).setActionTextColor(Customerly._do(crm -> crm.__PING__LAST_widget_color, Customerly.DEF_WIDGETCOLOR_INT)).show();
                                this.input_input.requestFocus();
                                return;
                            }

                            new Internal_entity__Attachment(this, fileUri).addAttachmentToInput(this);
                        } catch (Exception exception) {
                            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__ATTACHMENT_ERROR, "Error while attaching file: " + exception.getMessage(), exception);
                        }
                    }
                    this.input_input.requestFocus();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void restoreAttachments() {
        if(this.input_attachments != null) {
            this.input_attachments.removeAllViews();
        }
        if(this._Attachments.size() != 0) {
            this._Attachments.clear();
        }
    }
}
